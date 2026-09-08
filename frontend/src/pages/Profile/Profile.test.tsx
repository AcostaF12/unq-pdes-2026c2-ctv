import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { Profile } from './Profile'
import { AuthProvider } from '../../auth/AuthContext'
import { persistSession } from '../../api/session'
import { changePassword, getCurrentUser, updateCurrentUser } from '../../api/auth'
import { listCities } from '../../api/cities'
import { writePreferences } from '../../preferences/preferences'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/auth', () => ({
  getCurrentUser: vi.fn(),
  updateCurrentUser: vi.fn(),
  changePassword: vi.fn(),
}))

vi.mock('../../api/cities', () => ({
  listCities: vi.fn(),
}))

const agencyUser = {
  id: 9,
  username: 'agency',
  firstName: 'Laura',
  lastName: 'Demo',
  role: 'AGENCY',
  agency: { id: 1, name: 'Despegar' },
}

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedUpdateCurrentUser = vi.mocked(updateCurrentUser)
const mockedChangePassword = vi.mocked(changePassword)
const mockedListCities = vi.mocked(listCities)

function renderProfile() {
  persistSession('token', agencyUser)
  mockedGetCurrentUser.mockResolvedValue(agencyUser)
  mockedListCities.mockResolvedValue([
    { code: 'BUE', name: 'Buenos Aires' },
    { code: 'PAR', name: 'Paris' },
  ])

  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <MemoryRouter>
          <Profile />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('Profile page', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    mockedUpdateCurrentUser.mockReset()
    mockedChangePassword.mockReset()
    mockedListCities.mockReset()
  })

  it('shows user data including agency', async () => {
    renderProfile()

    expect(await screen.findByRole('heading', { name: 'Laura Demo' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('agency')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Despegar')).toBeInTheDocument()
  })

  it('filters account sections from the search box', async () => {
    const user = userEvent.setup()
    renderProfile()

    await screen.findByRole('heading', { name: 'Laura Demo' })
    await user.type(screen.getByLabelText('Buscar en tu cuenta'), 'contraseña')

    expect(screen.getByRole('heading', { name: 'Cambiar contraseña' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Preferencias' })).not.toBeInTheDocument()
  })

  it('saves profile name changes', async () => {
    const user = userEvent.setup()
    mockedUpdateCurrentUser.mockResolvedValue({
      ...agencyUser,
      firstName: 'Lara',
      lastName: 'Diaz',
    })
    renderProfile()

    const firstName = await screen.findByLabelText('Nombre')
    await user.clear(firstName)
    await user.type(firstName, 'Lara')
    await user.clear(screen.getByLabelText('Apellido'))
    await user.type(screen.getByLabelText('Apellido'), 'Diaz')
    await user.click(screen.getByRole('button', { name: 'Guardar perfil' }))

    await waitFor(() => {
      expect(mockedUpdateCurrentUser).toHaveBeenCalledWith({
        firstName: 'Lara',
        lastName: 'Diaz',
      })
    })
    expect(await screen.findByText('Perfil actualizado.')).toBeInTheDocument()
  })

  it('saves local preferences and changes password', async () => {
    const user = userEvent.setup()
    mockedChangePassword.mockResolvedValue()
    renderProfile()

    await screen.findByLabelText('Ciudad de origen')
    await user.selectOptions(screen.getByLabelText('Ciudad de origen'), 'BUE')
    await user.type(screen.getByLabelText('Presupuesto máximo (USD)'), '1200')
    await user.click(screen.getByRole('button', { name: 'Guardar preferencias' }))

    expect(await screen.findByText('Preferencias guardadas en este dispositivo.')).toBeInTheDocument()
    expect(writePreferences).toBeDefined()

    await user.type(screen.getByLabelText('Contraseña actual'), 'secret1')
    await user.type(screen.getByLabelText('Contraseña nueva'), 'secret2')
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'secret2')
    await user.click(screen.getByRole('button', { name: 'Actualizar contraseña' }))

    await waitFor(() => {
      expect(mockedChangePassword).toHaveBeenCalledWith({
        currentPassword: 'secret1',
        newPassword: 'secret2',
      })
    })
    expect(await screen.findByText('Contraseña actualizada.')).toBeInTheDocument()
  })
})
