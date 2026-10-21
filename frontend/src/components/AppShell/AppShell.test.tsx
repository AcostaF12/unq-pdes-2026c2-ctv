import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AppShell } from './AppShell'
import { AuthProvider } from '../../auth/AuthContext'
import { persistSession } from '../../api/session'
import { getCurrentUser } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  getCurrentUser: vi.fn(),
}))

const mockedGetCurrentUser = vi.mocked(getCurrentUser)

const buyer = {
  id: 1,
  username: 'agus',
  firstName: 'Agustin',
  lastName: 'Di Santo',
  role: 'BUYER',
}

function renderShell(role = 'BUYER') {
  const currentUser = { ...buyer, role }
  persistSession('token', currentUser)
  mockedGetCurrentUser.mockResolvedValue(currentUser)

  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/trips']}>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/trips" element={<div>Catalogo</div>} />
            <Route path="/profile" element={<div>Cuenta</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('AppShell', () => {
  it('shows the brand and account menu instead of a profile nav item', async () => {
    const user = userEvent.setup()
    renderShell()

    expect(await screen.findByRole('link', { name: /compra tu viaje/i })).toHaveAttribute(
      'href',
      '/trips',
    )
    expect(screen.queryByRole('link', { name: 'Perfil' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /agustin di santo/i }))
    expect(screen.getByRole('menuitem', { name: 'Mi cuenta' })).toBeInTheDocument()
  })

  it('gives admins one Administración entry instead of the lone new hotel link', async () => {
    renderShell('ADMIN')

    expect(await screen.findByRole('link', { name: 'Administración' })).toHaveAttribute('href', '/admin')
    expect(screen.getByRole('link', { name: 'Hoteles' })).toHaveAttribute('href', '/hotels')
    expect(screen.getByRole('link', { name: 'Paquetes' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Nuevo hotel' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Favoritos' })).not.toBeInTheDocument()
  })

  it.each([
    ['BUYER', ['Favoritos', 'Mis compras']],
    ['AGENCY', ['Agencia', 'Ventas']],
  ])('hides the admin entry from the %s role', async (role, ownLinks) => {
    renderShell(role)

    for (const name of ownLinks) {
      expect(await screen.findByRole('link', { name })).toBeInTheDocument()
    }
    expect(screen.queryByRole('link', { name: 'Administración' })).not.toBeInTheDocument()
  })

  it('offers a skip link to the main content and focuses it after navigating', async () => {
    const user = userEvent.setup()
    renderShell()

    const skip = await screen.findByRole('link', { name: 'Saltar al contenido' })
    expect(skip).toHaveAttribute('href', '#main-content')
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content')

    await user.click(await screen.findByRole('button', { name: /agustin di santo/i }))
    await user.click(screen.getByRole('menuitem', { name: 'Mi cuenta' }))

    expect(await screen.findByText('Cuenta')).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveFocus()
  })

  it('marks only the sales link as current on the agency sales page', async () => {
    const currentUser = { ...buyer, role: 'AGENCY' }
    persistSession('token', currentUser)
    mockedGetCurrentUser.mockResolvedValue(currentUser)
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/agency/sales']}>
          <Routes>
            <Route element={<AppShell />}>
              <Route path="/agency/sales" element={<div>Ventas</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthProvider>,
    )

    expect(await screen.findByRole('link', { name: 'Ventas' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Agencia' })).not.toHaveAttribute('aria-current')
  })
})
