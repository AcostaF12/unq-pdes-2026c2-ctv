import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { createAgency, listAgencies, updateAgency } from '../../api/agencies'
import { createTestQueryClient } from '../../test/queryClient'
import { apiError } from '../../test/fixtures'
import { AdminAgencies } from './AdminAgencies'

vi.mock('../../api/agencies', () => ({ listAgencies: vi.fn(), createAgency: vi.fn(), updateAgency: vi.fn() }))
const agency = { id: 1, name: 'Despegar', active: true }
function renderPage() {
  return render(<QueryClientProvider client={createTestQueryClient()}><MemoryRouter><AdminAgencies /></MemoryRouter></QueryClientProvider>)
}
describe('AdminAgencies', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listAgencies).mockResolvedValue([agency])
  })
  it('creates an agency with its account and clears the password', async () => {
    const user = userEvent.setup()
    vi.mocked(createAgency).mockResolvedValue(agency)
    renderPage()
    await screen.findByText('Despegar')
    for (const [label, value] of [['Nombre de agencia', 'Nueva'], ['Usuario', 'newagency'], ['Contraseña', 'secret123'], ['Nombre del responsable', 'Ana'], ['Apellido del responsable', 'Perez']]) {
      await user.type(screen.getByLabelText(label), value)
    }
    await user.click(screen.getByRole('button', { name: 'Crear agencia' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Agencia creada.')
    expect(createAgency).toHaveBeenCalledWith({ name: 'Nueva', username: 'newagency', password: 'secret123', firstName: 'Ana', lastName: 'Perez' })
    expect(screen.getByLabelText('Contraseña')).toHaveValue('')
  })
  it('requires confirmation to deactivate and allows reactivation', async () => {
    const user = userEvent.setup()
    vi.mocked(updateAgency).mockImplementation(async (id, data) => {
      const updated = { id, ...data }
      vi.mocked(listAgencies).mockResolvedValue([updated])
      return updated
    })
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Desactivar Despegar' }))
    expect(updateAgency).not.toHaveBeenCalled()
    expect(screen.getByText(/no podrá operar/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Confirmar desactivación' }))
    await screen.findByText('Inactiva')
    expect(updateAgency).toHaveBeenCalledWith(1, { name: 'Despegar', active: false })
    await user.click(screen.getByRole('button', { name: 'Reactivar Despegar' }))
    expect(await screen.findByText('Activa')).toBeInTheDocument()
  })
  it('renames without changing account or active state', async () => {
    const user = userEvent.setup()
    vi.mocked(updateAgency).mockResolvedValue({ ...agency, name: 'Nuevo nombre' })
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Editar Despegar' }))
    expect(screen.queryByLabelText('Contraseña')).not.toBeInTheDocument()
    await user.clear(screen.getByLabelText('Nombre de agencia'))
    await user.type(screen.getByLabelText('Nombre de agencia'), 'Nuevo nombre')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(updateAgency).toHaveBeenCalledWith(1, { name: 'Nuevo nombre', active: true }))
  })
  it('shows server errors without clearing the form', async () => {
    const user = userEvent.setup()
    vi.mocked(updateAgency).mockRejectedValue(apiError('No tenés permiso', 403))
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Editar Despegar' }))
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No tenés permiso')
    expect(screen.getByLabelText('Nombre de agencia')).toHaveValue('Despegar')
  })
})
