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

function renderShell() {
  persistSession('token', buyer)
  mockedGetCurrentUser.mockResolvedValue(buyer)

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
})
