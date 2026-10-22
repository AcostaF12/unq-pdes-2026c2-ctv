import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createManagedUser,
  listManagedUsers,
  updateManagedUser,
  type ManagedUser,
} from '../../api/adminUsers'
import { listAgencies } from '../../api/agencies'
import { useAuth } from '../../auth/AuthContext'
import { createTestQueryClient } from '../../test/queryClient'
import { apiError } from '../../test/fixtures'
import { AdminUsers } from './AdminUsers'

vi.mock('../../api/adminUsers', () => ({
  createManagedUser: vi.fn(),
  listManagedUsers: vi.fn(),
  updateManagedUser: vi.fn(),
}))
vi.mock('../../api/agencies', () => ({ listAgencies: vi.fn() }))
vi.mock('../../auth/AuthContext', () => ({ useAuth: vi.fn() }))

const buyer: ManagedUser = {
  id: 2,
  username: 'buyer',
  firstName: 'Ana',
  lastName: 'Pérez',
  role: 'BUYER',
  enabled: true,
  agency: null,
}
const admin = { ...buyer, id: 1, username: 'admin', role: 'ADMIN' as const }
const agency = { id: 7, name: 'Viajes del Sur' }

function renderUsers() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AdminUsers />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('administración de usuarios', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(useAuth).mockReturnValue({ user: admin, updateUser: vi.fn() } as unknown as ReturnType<
      typeof useAuth
    >)
    vi.mocked(listAgencies).mockResolvedValue([agency])
    vi.mocked(listManagedUsers).mockResolvedValue({
      content: [buyer, admin],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })
    vi.mocked(createManagedUser).mockResolvedValue(buyer)
    vi.mocked(updateManagedUser).mockResolvedValue(buyer)
  })

  it('lists account status and creates a seller with the selected agency', async () => {
    const user = userEvent.setup()
    renderUsers()
    expect(await screen.findByText('Ana Pérez (@buyer)')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Nuevo usuario' }))
    await user.type(screen.getByLabelText('Usuario'), 'seller')
    await user.type(screen.getByLabelText('Contraseña inicial'), 'secret123')
    await user.type(screen.getByLabelText('Nombre'), 'Lara')
    await user.type(screen.getByLabelText('Apellido'), 'Gómez')
    await user.selectOptions(screen.getByLabelText('Rol'), 'AGENCY')
    expect(screen.getByLabelText('Agencia')).toBeRequired()
    await user.selectOptions(screen.getByLabelText('Agencia'), '7')
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }))
    await waitFor(() =>
      expect(createManagedUser).toHaveBeenCalledWith({
        username: 'seller',
        password: 'secret123',
        firstName: 'Lara',
        lastName: 'Gómez',
        role: 'AGENCY',
        agencyId: 7,
      }),
    )
    expect(await screen.findByRole('status')).toHaveTextContent('Usuario guardado')
  })

  it('disables a user through the update API', async () => {
    const user = userEvent.setup()
    renderUsers()
    await user.click(await screen.findByRole('button', { name: 'Editar buyer' }))
    await user.selectOptions(screen.getByLabelText('Estado'), 'disabled')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() =>
      expect(updateManagedUser).toHaveBeenCalledWith(2, {
        firstName: 'Ana',
        lastName: 'Pérez',
        role: 'BUYER',
        enabled: false,
      }),
    )
  })

  it('removes agencyId when a seller becomes a buyer', async () => {
    const user = userEvent.setup()
    vi.mocked(listManagedUsers).mockResolvedValue({
      content: [{ ...buyer, role: 'AGENCY', agency }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    renderUsers()
    await user.click(await screen.findByRole('button', { name: 'Editar buyer' }))
    await user.selectOptions(screen.getByLabelText('Rol'), 'BUYER')
    expect(screen.queryByLabelText('Agencia')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() =>
      expect(updateManagedUser).toHaveBeenCalledWith(2, {
        firstName: 'Ana',
        lastName: 'Pérez',
        role: 'BUYER',
        enabled: true,
      }),
    )
  })

  it('prevents self lockout in the form', async () => {
    const user = userEvent.setup()
    renderUsers()
    await user.click(await screen.findByRole('button', { name: 'Editar admin' }))
    expect(screen.getByLabelText('Rol')).toBeDisabled()
    expect(screen.getByLabelText('Estado')).toBeDisabled()
    expect(screen.getByLabelText('Nombre')).toBeEnabled()
  })

  it('keeps user input and displays server errors', async () => {
    const user = userEvent.setup()
    vi.mocked(updateManagedUser).mockRejectedValue(apiError('No existe la agencia.'))
    renderUsers()
    await user.click(await screen.findByRole('button', { name: 'Editar buyer' }))
    await user.type(screen.getByLabelText('Nombre'), ' María')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No existe la agencia.')
    expect(screen.getByLabelText('Nombre')).toHaveValue('Ana María')
  })

  it('retries loading failures and paginates users', async () => {
    const user = userEvent.setup()
    vi.mocked(listManagedUsers)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValue({ content: [buyer], page: 0, size: 20, totalElements: 21, totalPages: 2 })
    renderUsers()
    await user.click(await screen.findByRole('button', { name: 'Reintentar' }))
    await user.click(await screen.findByRole('button', { name: 'Siguiente' }))
    await waitFor(() => expect(listManagedUsers).toHaveBeenCalledWith(1))
  })

  it('does not submit an incomplete seller and permits cancelling', async () => {
    const user = userEvent.setup()
    renderUsers()
    await screen.findByText('Ana Pérez (@buyer)')
    await user.click(screen.getByRole('button', { name: 'Nuevo usuario' }))
    await user.selectOptions(screen.getByLabelText('Rol'), 'AGENCY')
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }))
    expect(createManagedUser).not.toHaveBeenCalled()
    await user.click(
      within(screen.getByRole('form', { name: 'Nuevo usuario' })).getByRole('button', { name: 'Cancelar' }),
    )
    expect(screen.queryByLabelText('Contraseña inicial')).not.toBeInTheDocument()
  })
})
