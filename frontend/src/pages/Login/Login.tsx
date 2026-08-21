import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import { Button } from '../../components/Button/Button'
import { Input } from '../../components/Input/Input'
import { PasswordToggle } from '../../components/PasswordToggle/PasswordToggle'
import { login } from '../../api/auth'
import { loginSchema, type LoginFormValues } from './login.schema'
import './Login.css'

export function Login() {
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (values: LoginFormValues) => {
    setServerError(null)
    try {
      await login(values)
      navigate('/trips')
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        setServerError('Usuario o contraseña incorrectos.')
      } else {
        setServerError('No pudimos conectar con el servidor. Intentá de nuevo.')
      }
    }
  }

  return (
    <div className="page-center">
      <div className="login-card">
        <p className="login-card__eyebrow">Compra Tu Viaje</p>
        <h1 className="login-card__title">Ingresar</h1>

        <form className="login-card__form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input
            label="Usuario"
            autoComplete="username"
            error={errors.username?.message}
            {...register('username')}
          />

          <Input
            label="Contraseña"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            error={errors.password?.message}
            rightElement={
              <PasswordToggle
                visible={showPassword}
                onToggle={() => setShowPassword((value) => !value)}
              />
            }
            {...register('password')}
          />

          {serverError && (
            <p className="login-card__error" role="alert">
              {serverError}
            </p>
          )}

          <Button type="submit" loading={isSubmitting}>
            Ingresar
          </Button>
        </form>

        <p className="login-card__footer">
          ¿No tenés cuenta? <a href="/register">Registrate</a>
        </p>
      </div>
    </div>
  )
}
