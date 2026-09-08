import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { userInitials } from '../UserAvatar/userInitials'
import { AccountMenu } from './AccountMenu'

describe('AccountMenu', () => {
  it('opens account links from the user trigger and logs out', async () => {
    const user = userEvent.setup()
    const onLogout = vi.fn()

    render(
      <MemoryRouter>
        <AccountMenu firstName="Agustin" lastName="Di Santo" username="agus" onLogout={onLogout} />
      </MemoryRouter>,
    )

    expect(screen.queryByRole('menu', { name: 'Cuenta' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /agustin di santo/i }))

    expect(screen.getByRole('menuitem', { name: 'Mi cuenta' })).toHaveAttribute('href', '/profile')
    expect(screen.getByRole('menuitem', { name: 'Preferencias' })).toHaveAttribute(
      'href',
      '/profile#preferencias',
    )
    expect(screen.getByRole('menuitem', { name: 'Contraseña' })).toHaveAttribute(
      'href',
      '/profile#password',
    )

    await user.click(screen.getByRole('menuitem', { name: 'Salir' }))
    expect(onLogout).toHaveBeenCalledTimes(1)
  })

  it('closes the menu with Escape', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AccountMenu firstName="Agustin" lastName="Di Santo" username="agus" onLogout={() => {}} />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: /agustin di santo/i }))
    expect(screen.getByRole('menu', { name: 'Cuenta' })).toBeInTheDocument()

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('menu', { name: 'Cuenta' })).not.toBeInTheDocument()
  })

  it('builds initials from first and last name', () => {
    expect(userInitials('Agustin', 'Di Santo')).toBe('AD')
  })
})
