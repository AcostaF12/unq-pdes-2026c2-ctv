import { describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider, useAuth } from './AuthContext'
import { persistSession, getStoredUser } from '../api/session'
import { getCurrentUser } from '../api/auth'
import { notifyUnauthorized } from '../api/unauthorized'
import { queryKeys } from '../query/keys'
import { queryClient } from '../query/queryClient'

vi.mock('../api/auth', () => ({
  getCurrentUser: vi.fn(),
}))

const mockedGetCurrentUser = vi.mocked(getCurrentUser)

function Probe() {
  const { user, isReady, logout, updateUser } = useAuth()
  return (
    <div>
      <span>ready:{String(isReady)}</span>
      <span>{user?.username ?? 'anon'}</span>
      <span>{user?.firstName ?? 'sin-nombre'}</span>
      <button type="button" onClick={logout}>
        Salir
      </button>
      <button type="button" onClick={() => (user ? updateUser({ ...user, firstName: 'Lara' }) : undefined)}>
        Actualizar
      </button>
    </div>
  )
}

describe('AuthProvider', () => {
  it('refreshes changed roles on focus and clears cached data from the previous agency', async () => {
    const previous = {
      id: 1,
      username: 'seller',
      firstName: 'Ana',
      lastName: 'Pérez',
      role: 'AGENCY',
      agency: { id: 7, name: 'Sur' },
    }
    persistSession('token', previous)
    mockedGetCurrentUser.mockResolvedValueOnce(previous)
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )
    await waitFor(() => expect(mockedGetCurrentUser).toHaveBeenCalledTimes(1))
    await act(async () => {})
    queryClient.setQueryData(queryKeys.packages.agency, ['private agency data'])
    mockedGetCurrentUser.mockResolvedValueOnce({ ...previous, role: 'BUYER', agency: undefined })
    fireEvent(window, new Event('focus'))
    await waitFor(() => expect(getStoredUser()?.role).toBe('BUYER'))
    expect(queryClient.getQueryData(queryKeys.packages.agency)).toBeUndefined()
  })

  it('does not restore a session when an in-flight refresh completes after logout', async () => {
    const previous = { id: 1, username: 'buyer', firstName: 'Ana', lastName: 'Pérez', role: 'BUYER' }
    persistSession('token', previous)
    let resolve: (value: typeof previous) => void = () => {}
    mockedGetCurrentUser.mockReturnValue(
      new Promise((done) => {
        resolve = done
      }),
    )
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )
    fireEvent.click(screen.getByRole('button', { name: 'Salir' }))
    await act(async () => {
      resolve(previous)
    })
    expect(screen.getByText('anon')).toBeInTheDocument()
    expect(getStoredUser()).toBeNull()
  })

  it('is ready immediately when there is no session', () => {
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    expect(screen.getByText('ready:true')).toBeInTheDocument()
    expect(screen.getByText('anon')).toBeInTheDocument()
  })

  it('shows the stored user without waiting for /users/me', () => {
    mockedGetCurrentUser.mockReturnValue(new Promise(() => {}))
    persistSession('token', {
      id: 1,
      username: 'buyer',
      firstName: 'Bruno',
      lastName: 'Buyer',
      role: 'BUYER',
    })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    expect(screen.getByText('ready:true')).toBeInTheDocument()
    expect(screen.getByText('buyer')).toBeInTheDocument()
  })

  it('clears the query cache on logout', async () => {
    const user = userEvent.setup()
    queryClient.setQueryData(queryKeys.favorites, [{ id: 1 }])

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Salir' }))
    expect(queryClient.getQueryData(queryKeys.favorites)).toBeUndefined()
  })

  it('clears the query cache on 401', async () => {
    queryClient.setQueryData(queryKeys.favorites, [{ id: 1 }])

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await screen.findByText('ready:true')
    notifyUnauthorized()
    expect(queryClient.getQueryData(queryKeys.favorites)).toBeUndefined()
  })

  it('updates the stored user without rotating the token', async () => {
    const user = userEvent.setup()
    mockedGetCurrentUser.mockReturnValue(new Promise(() => {}))
    persistSession('token', {
      id: 1,
      username: 'buyer',
      firstName: 'Bruno',
      lastName: 'Buyer',
      role: 'BUYER',
    })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Actualizar' }))
    expect(screen.getByText('Lara')).toBeInTheDocument()
    await waitFor(() => {
      expect(getStoredUser()?.firstName).toBe('Lara')
    })
    expect(localStorage.getItem('ctv.token')).toBe('token')
  })
})
