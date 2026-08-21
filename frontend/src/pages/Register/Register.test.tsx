import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import axios from 'axios'
import { Register } from './Register'
import { register } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  register: vi.fn(),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

const mockedRegister = vi.mocked(register)

function renderRegister() {
  return render(
    <MemoryRouter>
      <Register />
    </MemoryRouter>,
  )
}

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Nombre'), 'Juan')
  await user.type(screen.getByLabelText('Apellido'), 'Perez')
  await user.type(screen.getByLabelText('Usuario'), 'juanperez')
  await user.type(screen.getByLabelText('Contraseña'), 'secreto123')
  await user.type(screen.getByLabelText('Confirmar contraseña'), 'secreto123')
}

describe('Register page', () => {
  beforeEach(() => {
    mockedRegister.mockReset()
    mockNavigate.mockReset()
  })

  it('renders all the fields with accessible labels', () => {
    renderRegister()

    expect(screen.getByLabelText('Nombre')).toBeInTheDocument()
    expect(screen.getByLabelText('Apellido')).toBeInTheDocument()
    expect(screen.getByLabelText('Usuario')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument()
    expect(screen.getByLabelText('Confirmar contraseña')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /crear cuenta/i })).toBeInTheDocument()
  })

  it('renders both password fields as type="password" by default', () => {
    renderRegister()

    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password')
    expect(screen.getByLabelText('Confirmar contraseña')).toHaveAttribute('type', 'password')
  })

  it('toggles each password field visibility independently', async () => {
    const user = userEvent.setup()
    renderRegister()

    const [passwordToggle, confirmToggle] = screen.getAllByRole('button', { name: /mostrar/i })

    await user.click(passwordToggle)
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'text')
    expect(screen.getByLabelText('Confirmar contraseña')).toHaveAttribute('type', 'password')

    await user.click(confirmToggle)
    expect(screen.getByLabelText('Confirmar contraseña')).toHaveAttribute('type', 'text')
  })

  it('shows validation errors when submitting an empty form and does not call the API', async () => {
    const user = userEvent.setup()
    renderRegister()

    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    expect(await screen.findByText('Ingresá tu nombre')).toBeInTheDocument()
    expect(screen.getByText('Ingresá tu apellido')).toBeInTheDocument()
    expect(screen.getByText('Ingresá un usuario')).toBeInTheDocument()
    expect(screen.getByText('Ingresá una contraseña')).toBeInTheDocument()
    expect(mockedRegister).not.toHaveBeenCalled()
  })

  it('shows a validation error when the password is shorter than 6 characters', async () => {
    const user = userEvent.setup()
    renderRegister()

    await user.type(screen.getByLabelText('Nombre'), 'Juan')
    await user.type(screen.getByLabelText('Apellido'), 'Perez')
    await user.type(screen.getByLabelText('Usuario'), 'juanperez')
    await user.type(screen.getByLabelText('Contraseña'), '123')
    await user.type(screen.getByLabelText('Confirmar contraseña'), '123')
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    expect(await screen.findByText('Debe tener al menos 6 caracteres')).toBeInTheDocument()
    expect(mockedRegister).not.toHaveBeenCalled()
  })

  it('shows an error when the passwords do not match', async () => {
    const user = userEvent.setup()
    renderRegister()

    await user.type(screen.getByLabelText('Nombre'), 'Juan')
    await user.type(screen.getByLabelText('Apellido'), 'Perez')
    await user.type(screen.getByLabelText('Usuario'), 'juanperez')
    await user.type(screen.getByLabelText('Contraseña'), 'secreto123')
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'diferente123')
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument()
    expect(mockedRegister).not.toHaveBeenCalled()
  })

  it('marks invalid fields with aria-invalid and an alert role on the error message', async () => {
    const user = userEvent.setup()
    renderRegister()

    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    const usernameInput = await screen.findByLabelText('Usuario')
    expect(usernameInput).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getAllByRole('alert').length).toBeGreaterThan(0)
  })

  it('submits valid data, calls the API without confirmPassword, and auto-logs in by redirecting to /trips', async () => {
    const user = userEvent.setup()
    mockedRegister.mockResolvedValueOnce({
      token: 'fake-token',
      user: { id: 1, username: 'juanperez', firstName: 'Juan', lastName: 'Perez', role: 'COMPRADOR' },
    })
    renderRegister()

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    await waitFor(() => {
      expect(mockedRegister).toHaveBeenCalledWith({
        username: 'juanperez',
        password: 'secreto123',
        firstName: 'Juan',
        lastName: 'Perez',
      })
    })
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/trips')
    })
  })

  it('shows the backend message when the username is already taken', async () => {
    const user = userEvent.setup()
    const axiosError = Object.assign(new Error('Bad Request'), {
      isAxiosError: true,
      response: {
        status: 400,
        data: { errorData: { description: "The username 'juanperez' is already taken." } },
      },
    })
    vi.spyOn(axios, 'isAxiosError').mockReturnValue(true)
    mockedRegister.mockRejectedValueOnce(axiosError)
    renderRegister()

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    expect(
      await screen.findByText("The username 'juanperez' is already taken."),
    ).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()

    vi.mocked(axios.isAxiosError).mockRestore()
  })

  it('shows a generic connection error message for non-HTTP failures', async () => {
    const user = userEvent.setup()
    mockedRegister.mockRejectedValueOnce(new Error('Network Error'))
    renderRegister()

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    expect(
      await screen.findByText('No pudimos conectar con el servidor. Intentá de nuevo.'),
    ).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('disables the submit button and shows a loading label while submitting', async () => {
    const user = userEvent.setup()
    let resolveRegister: (value: Awaited<ReturnType<typeof register>>) => void = () => {}
    mockedRegister.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveRegister = resolve
        }),
    )
    renderRegister()

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))

    const loadingButton = await screen.findByRole('button', { name: /cargando/i })
    expect(loadingButton).toBeDisabled()

    resolveRegister({
      token: 'fake-token',
      user: { id: 1, username: 'juanperez', firstName: 'Juan', lastName: 'Perez', role: 'COMPRADOR' },
    })

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalled()
    })
  })

  it('renders a link back to the login page', () => {
    renderRegister()

    const link = screen.getByRole('link', { name: /iniciá sesión/i })
    expect(link).toHaveAttribute('href', '/login')
  })
})
