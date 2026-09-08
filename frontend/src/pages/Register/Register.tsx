import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { BrandMark } from '../../components/BrandMark/BrandMark'
import { Button } from '../../components/Button/Button'
import { Input } from '../../components/Input/Input'
import { PasswordToggle } from '../../components/PasswordToggle/PasswordToggle'
import { register as registerUser } from '../../api/auth'
import { useAuth } from '../../auth/AuthContext'
import { getApiErrorMessage } from '../../api/errors'
import { HOME_PATH } from '../../router/paths'
import { registerSchema, type RegisterFormValues } from './register.schema'
import './Register.css'

export function Register() {
  const navigate = useNavigate()
  const { setSession } = useAuth()
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (values: RegisterFormValues) => {
    setServerError(null)
    try {
      const auth = await registerUser({
        username: values.username,
        password: values.password,
        firstName: values.firstName,
        lastName: values.lastName,
      })
      setSession(auth)
      navigate(HOME_PATH)
    } catch (error) {
      setServerError(
        getApiErrorMessage(error) ?? 'No pudimos conectar con el servidor. Intentá de nuevo.',
      )
    }
  }

  return (
    <div className="page-center">
      <div className="register-card">
        <BrandMark variant="auth" />
        <h1 className="register-card__title">Crear cuenta</h1>

        <form className="register-card__form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="register-card__row">
            <Input label="Nombre" error={errors.firstName?.message} {...register('firstName')} />
            <Input label="Apellido" error={errors.lastName?.message} {...register('lastName')} />
          </div>

          <Input
            label="Usuario"
            autoComplete="username"
            error={errors.username?.message}
            {...register('username')}
          />

          <Input
            label="Contraseña"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            error={errors.password?.message}
            rightElement={
              <PasswordToggle
                visible={showPassword}
                onToggle={() => setShowPassword((value) => !value)}
              />
            }
            {...register('password')}
          />

          <Input
            label="Confirmar contraseña"
            type={showConfirmPassword ? 'text' : 'password'}
            autoComplete="new-password"
            error={errors.confirmPassword?.message}
            rightElement={
              <PasswordToggle
                visible={showConfirmPassword}
                onToggle={() => setShowConfirmPassword((value) => !value)}
              />
            }
            {...register('confirmPassword')}
          />

          {serverError && (
            <p className="register-card__error" role="alert">
              {serverError}
            </p>
          )}

          <Button type="submit" loading={isSubmitting}>
            Crear cuenta
          </Button>
        </form>

        <p className="register-card__footer">
          ¿Ya tenés cuenta? <Link to="/login">Iniciá sesión</Link>
        </p>
      </div>
    </div>
  )
}
