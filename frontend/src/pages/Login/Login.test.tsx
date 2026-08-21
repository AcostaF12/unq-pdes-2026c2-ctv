import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import axios from 'axios'
import { Login } from './Login'
import { login } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  login: vi.fn(),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

const mockedLogin = vi.mocked(login)

function renderLogin() {
  return render(
    <MemoryRouter>
      <Login />
    </MemoryRouter>,
  )
}

describe('Login page', () => {
  beforeEach(() => {
    mockedLogin.mockReset()
    mockNavigate.mockReset()
  })

  it('01 - renders the username and password fields with accessible labels', () => {
    renderLogin()

    expect(screen.getByLabelText('Usuario')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /ingresar/i })).toBeInTheDocument()
  })

  it('02 - renders the password field as type="password" by default', () => {
    renderLogin()

    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password')
  })

  it('03 - toggles the password field visibility when clicking the show/hide button', async () => {
    const user = userEvent.setup()
    renderLogin()

    const toggle = screen.getByRole('button', { name: /mostrar/i })
    const passwordInput = screen.getByLabelText('Contraseña')

    expect(passwordInput).toHaveAttribute('type', 'password')

    await user.click(toggle)
    expect(passwordInput).toHaveAttribute('type', 'text')
    expect(screen.getByRole('button', { name: /ocultar/i })).toHaveAttribute(
      'aria-pressed',
      'true',
    )

    await user.click(screen.getByRole('button', { name: /ocultar/i }))
    expect(passwordInput).toHaveAttribute('type', 'password')
  })

  it('04 - shows validation errors when submitting an empty form and does not call the API', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    expect(await screen.findByText('Ingresá tu usuario')).toBeInTheDocument()
    expect(screen.getByText('Ingresá tu contraseña')).toBeInTheDocument()
    expect(mockedLogin).not.toHaveBeenCalled()
  })

  it('05 - shows a validation error when the password is shorter than 6 characters', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('Usuario'), 'juan')
    await user.type(screen.getByLabelText('Contraseña'), '123')
    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    expect(await screen.findByText('Debe tener al menos 6 caracteres')).toBeInTheDocument()
    expect(mockedLogin).not.toHaveBeenCalled()
  })

  it('06 - marks invalid fields with aria-invalid and an alert role on the error message', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    const usernameInput = await screen.findByLabelText('Usuario')
    expect(usernameInput).toHaveAttribute('aria-invalid', 'true')

    const alerts = screen.getAllByRole('alert')
    expect(alerts.length).toBeGreaterThan(0)
  })

  it('07 - submits valid credentials, calls the API and navigates on success', async () => {
    const user = userEvent.setup()
    mockedLogin.mockResolvedValueOnce({
      token: 'fake-token',
      user: { id: 1, username: 'juan', firstName: 'Juan', lastName: 'Perez', role: 'COMPRADOR' },
    })
    renderLogin()

    await user.type(screen.getByLabelText('Usuario'), 'juan')
    await user.type(screen.getByLabelText('Contraseña'), 'secreto123')
    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    await waitFor(() => {
      expect(mockedLogin).toHaveBeenCalledWith({ username: 'juan', password: 'secreto123' })
    })
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/trips')
    })
  })

  it('08 - shows a specific error message when the API responds with 401', async () => {
    const user = userEvent.setup()
    const axiosError = Object.assign(new Error('Unauthorized'), {
      isAxiosError: true,
      response: { status: 401 },
    })
    vi.spyOn(axios, 'isAxiosError').mockReturnValue(true)
    mockedLogin.mockRejectedValueOnce(axiosError)
    renderLogin()

    await user.type(screen.getByLabelText('Usuario'), 'juan')
    await user.type(screen.getByLabelText('Contraseña'), 'wrongpass')
    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    expect(await screen.findByText('Usuario o contraseña incorrectos.')).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()

    vi.mocked(axios.isAxiosError).mockRestore()
  })

  it('09 - shows a generic connection error message for non-HTTP failures', async () => {
    const user = userEvent.setup()
    mockedLogin.mockRejectedValueOnce(new Error('Network Error'))
    renderLogin()

    await user.type(screen.getByLabelText('Usuario'), 'juan')
    await user.type(screen.getByLabelText('Contraseña'), 'secreto123')
    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    expect(
      await screen.findByText('No pudimos conectar con el servidor. Intentá de nuevo.'),
    ).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('10 - disables the submit button and shows a loading label while submitting', async () => {
    const user = userEvent.setup()
    let resolveLogin: (value: Awaited<ReturnType<typeof login>>) => void = () => {}
    mockedLogin.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveLogin = resolve
        }),
    )
    renderLogin()

    await user.type(screen.getByLabelText('Usuario'), 'juan')
    await user.type(screen.getByLabelText('Contraseña'), 'secreto123')
    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    const loadingButton = await screen.findByRole('button', { name: /cargando/i })
    expect(loadingButton).toBeDisabled()

    resolveLogin({
      token: 'fake-token',
      user: { id: 1, username: 'juan', firstName: 'Juan', lastName: 'Perez', role: 'COMPRADOR' },
    })

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalled()
    })
  })

  it('11 - clears the previous server error once the user resubmits successfully', async () => {
    const user = userEvent.setup()
    const axiosError = Object.assign(new Error('Unauthorized'), {
      isAxiosError: true,
      response: { status: 401 },
    })
    vi.spyOn(axios, 'isAxiosError').mockReturnValue(true)
    mockedLogin.mockRejectedValueOnce(axiosError)
    renderLogin()

    await user.type(screen.getByLabelText('Usuario'), 'juan')
    await user.type(screen.getByLabelText('Contraseña'), 'wrongpass')
    await user.click(screen.getByRole('button', { name: /ingresar/i }))
    expect(await screen.findByText('Usuario o contraseña incorrectos.')).toBeInTheDocument()

    mockedLogin.mockResolvedValueOnce({
      token: 'fake-token',
      user: { id: 1, username: 'juan', firstName: 'Juan', lastName: 'Perez', role: 'COMPRADOR' },
    })
    await user.clear(screen.getByLabelText('Contraseña'))
    await user.type(screen.getByLabelText('Contraseña'), 'correctpass')
    await user.click(screen.getByRole('button', { name: /ingresar/i }))

    await waitFor(() => {
      expect(
        screen.queryByText('Usuario o contraseña incorrectos.'),
      ).not.toBeInTheDocument()
    })

    vi.mocked(axios.isAxiosError).mockRestore()
  })

  it('12 - renders a link to the registration page', () => {
    renderLogin()

    const link = screen.getByRole('link', { name: /registrate/i })
    expect(link).toHaveAttribute('href', '/register')
  })
})
