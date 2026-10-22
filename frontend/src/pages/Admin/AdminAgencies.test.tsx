import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createAgency, listAgencies, updateAgency } from '../../api/agencies'
import { createTestQueryClient } from '../../test/queryClient'
import { AdminAgencies } from './AdminAgencies'

vi.mock('../../api/agencies', () => ({ createAgency: vi.fn(), listAgencies: vi.fn(), updateAgency: vi.fn() }))
function renderAgencies() {
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AdminAgencies />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}
describe('administración de agencias', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listAgencies).mockResolvedValue([{ id: 7, name: 'Sur' }])
    vi.mocked(createAgency).mockResolvedValue({ id: 8, name: 'Norte' })
    vi.mocked(updateAgency).mockResolvedValue({ id: 7, name: 'Sur Tours' })
  })
  it('creates an agency and clears the form', async () => {
    const user = userEvent.setup()
    renderAgencies()
    await user.type(screen.getByLabelText('Nombre de la agencia'), ' Norte ')
    await user.click(screen.getByRole('button', { name: 'Crear agencia' }))
    await waitFor(() => expect(createAgency).toHaveBeenCalledWith('Norte'))
    expect(await screen.findByRole('status')).toHaveTextContent('Agencia guardada')
    expect(screen.getByLabelText('Nombre de la agencia')).toHaveValue('')
  })
  it('edits an existing agency', async () => {
    const user = userEvent.setup()
    renderAgencies()
    await user.click(await screen.findByRole('button', { name: 'Editar Sur' }))
    await user.type(screen.getByLabelText('Nombre de la agencia'), ' Tours')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(updateAgency).toHaveBeenCalledWith(7, 'Sur Tours'))
  })
})
