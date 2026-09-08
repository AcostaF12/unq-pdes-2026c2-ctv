import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useLocation } from 'react-router-dom'
import { changePassword, updateCurrentUser } from '../../api/auth'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button/Button'
import { Input } from '../../components/Input/Input'
import { PasswordToggle } from '../../components/PasswordToggle/PasswordToggle'
import { Select } from '../../components/Select/Select'
import { UserAvatar } from '../../components/UserAvatar/UserAvatar'
import { useAuth } from '../../auth/AuthContext'
import { roleLabel } from '../../auth/roles'
import {
  DEFAULT_PREFERENCES,
  profileCompleteness,
  readPreferences,
  writePreferences,
} from '../../preferences/preferences'
import { queryKeys } from '../../query/keys'
import {
  passwordSchema,
  preferencesSchema,
  profileSchema,
  type PasswordFormValues,
  type PreferencesFormValues,
  type ProfileFormValues,
} from './profile.schema'
import './Profile.css'

const ACCOUNT_SECTIONS = [
  {
    id: 'perfil',
    title: 'Perfil',
    keywords: 'nombre apellido usuario rol agencia completar datos',
  },
  {
    id: 'preferencias',
    title: 'Preferencias',
    keywords: 'origen ciudad presupuesto buscar paquetes',
  },
  {
    id: 'password',
    title: 'Contraseña',
    keywords: 'password restablecer cambiar seguridad',
  },
] as const

function translateProfileError(error: unknown, fallback: string): string {
  const description = getApiErrorMessage(error)
  if (description === 'The current password is incorrect.') {
    return 'La contraseña actual es incorrecta.'
  }
  return description ?? fallback
}

export function Profile() {
  const { user, updateUser } = useAuth()
  const location = useLocation()
  const [sectionQuery, setSectionQuery] = useState('')
  const [profileStatus, setProfileStatus] = useState<string | null>(null)
  const [profileError, setProfileError] = useState<string | null>(null)
  const [preferencesStatus, setPreferencesStatus] = useState<string | null>(null)
  const [passwordStatus, setPasswordStatus] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const citiesQuery = useQuery({
    queryKey: queryKeys.cities,
    queryFn: listCities,
  })

  const [prefsTick, setPrefsTick] = useState(0)
  const storedPreferences = useMemo(
    () => (user ? readPreferences(user.id) : null),
    [user, prefsTick],
  )

  const profileForm = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
    },
  })

  const preferencesForm = useForm<PreferencesFormValues>({
    resolver: zodResolver(preferencesSchema),
    defaultValues: storedPreferences ?? DEFAULT_PREFERENCES,
  })

  const passwordForm = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  })

  useEffect(() => {
    if (!location.hash) {
      return
    }
    const id = location.hash.slice(1)
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [location.hash])

  const visibleSections = useMemo(() => {
    const query = sectionQuery.trim().toLowerCase()
    if (!query) {
      return ACCOUNT_SECTIONS
    }
    return ACCOUNT_SECTIONS.filter((section) =>
      `${section.title} ${section.keywords}`.toLowerCase().includes(query),
    )
  }, [sectionQuery])

  if (!user || !storedPreferences) {
    return null
  }

  const completeness = profileCompleteness(user, storedPreferences)
  const cities = citiesQuery.data ?? []
  const showSection = (id: string) => visibleSections.some((section) => section.id === id)

  const onSaveProfile = profileForm.handleSubmit(async (values) => {
    setProfileStatus(null)
    setProfileError(null)
    try {
      const nextUser = await updateCurrentUser(values)
      updateUser(nextUser)
      setProfileStatus('Perfil actualizado.')
    } catch (error) {
      setProfileError(translateProfileError(error, 'No pudimos guardar el perfil.'))
    }
  })

  const onSavePreferences = preferencesForm.handleSubmit((values) => {
    writePreferences(user.id, values)
    setPrefsTick((tick) => tick + 1)
    setPreferencesStatus('Preferencias guardadas en este dispositivo.')
  })

  const onSavePassword = passwordForm.handleSubmit(async (values) => {
    setPasswordStatus(null)
    setPasswordError(null)
    try {
      await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      })
      passwordForm.reset()
      setPasswordStatus('Contraseña actualizada.')
    } catch (error) {
      setPasswordError(translateProfileError(error, 'No pudimos cambiar la contraseña.'))
    }
  })

  return (
    <section className="page account-page">
      <header className="account-hero">
        <UserAvatar firstName={user.firstName} lastName={user.lastName} size="lg" />
        <div className="account-hero__copy">
          <p className="account-hero__eyebrow">Tu cuenta</p>
          <h1 className="page__title">
            {user.firstName} {user.lastName}
          </h1>
          <p className="account-hero__meta">
            {roleLabel(user.role)} · @{user.username}
            {user.agency ? ` · ${user.agency.name}` : ''}
          </p>
          <p className="account-hero__progress">
            Completitud {completeness.percent}% ({completeness.completed}/{completeness.total})
          </p>
          <div
            className="account-hero__bar"
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={completeness.percent}
            aria-label="Completitud del perfil"
          >
            <span style={{ width: `${completeness.percent}%` }} />
          </div>
        </div>
      </header>

      <Input
        label="Buscar en tu cuenta"
        value={sectionQuery}
        onChange={(event) => setSectionQuery(event.target.value)}
        placeholder="Perfil, preferencias, contraseña…"
      />

      {visibleSections.length === 0 ? (
        <p className="account-empty">No hay secciones que coincidan con esa búsqueda.</p>
      ) : null}

      {showSection('perfil') ? (
        <section className="account-card" id="perfil">
          <h2>Completar perfil</h2>
          <p className="account-card__hint">El usuario y el rol no se pueden cambiar.</p>
          <form className="account-card__form" onSubmit={onSaveProfile} noValidate>
            <div className="account-card__row">
              <Input
                label="Nombre"
                error={profileForm.formState.errors.firstName?.message}
                {...profileForm.register('firstName')}
              />
              <Input
                label="Apellido"
                error={profileForm.formState.errors.lastName?.message}
                {...profileForm.register('lastName')}
              />
            </div>
            <Input label="Usuario" value={user.username} disabled />
            <Input label="Rol" value={roleLabel(user.role)} disabled />
            {user.agency ? <Input label="Agencia" value={user.agency.name} disabled /> : null}
            {profileError ? (
              <p className="account-card__error" role="alert">
                {profileError}
              </p>
            ) : null}
            {profileStatus ? (
              <p className="account-card__status" role="status">
                {profileStatus}
              </p>
            ) : null}
            <Button type="submit" loading={profileForm.formState.isSubmitting}>
              Guardar perfil
            </Button>
          </form>
        </section>
      ) : null}

      {showSection('preferencias') ? (
        <section className="account-card" id="preferencias">
          <h2>Preferencias</h2>
          <p className="account-card__hint">Se guardan en este navegador, no en el servidor.</p>
          <form className="account-card__form" onSubmit={onSavePreferences} noValidate>
            <Select label="Ciudad de origen" {...preferencesForm.register('originCityCode')}>
              <option value="">Sin definir</option>
              {cities.map((city) => (
                <option key={city.code} value={city.code}>
                  {city.name}
                </option>
              ))}
            </Select>
            <Input
              label="Presupuesto máximo (USD)"
              type="number"
              min={1}
              step="1"
              error={preferencesForm.formState.errors.maxBudget?.message}
              {...preferencesForm.register('maxBudget')}
            />
            <label className="account-check">
              <input type="checkbox" {...preferencesForm.register('applyOriginOnSearch')} />
              Recordar origen al buscar paquetes
            </label>
            {preferencesStatus ? (
              <p className="account-card__status" role="status">
                {preferencesStatus}
              </p>
            ) : null}
            <Button type="submit">Guardar preferencias</Button>
          </form>
        </section>
      ) : null}

      {showSection('password') ? (
        <section className="account-card" id="password">
          <h2>Cambiar contraseña</h2>
          <p className="account-card__hint">Necesitás la contraseña actual. No hay recupero por mail.</p>
          <form className="account-card__form" onSubmit={onSavePassword} noValidate>
            <Input
              label="Contraseña actual"
              type={showCurrentPassword ? 'text' : 'password'}
              autoComplete="current-password"
              error={passwordForm.formState.errors.currentPassword?.message}
              rightElement={
                <PasswordToggle
                  visible={showCurrentPassword}
                  onToggle={() => setShowCurrentPassword((value) => !value)}
                />
              }
              {...passwordForm.register('currentPassword')}
            />
            <Input
              label="Contraseña nueva"
              type={showNewPassword ? 'text' : 'password'}
              autoComplete="new-password"
              error={passwordForm.formState.errors.newPassword?.message}
              rightElement={
                <PasswordToggle
                  visible={showNewPassword}
                  onToggle={() => setShowNewPassword((value) => !value)}
                />
              }
              {...passwordForm.register('newPassword')}
            />
            <Input
              label="Confirmar contraseña"
              type={showConfirmPassword ? 'text' : 'password'}
              autoComplete="new-password"
              error={passwordForm.formState.errors.confirmPassword?.message}
              rightElement={
                <PasswordToggle
                  visible={showConfirmPassword}
                  onToggle={() => setShowConfirmPassword((value) => !value)}
                />
              }
              {...passwordForm.register('confirmPassword')}
            />
            {passwordError ? (
              <p className="account-card__error" role="alert">
                {passwordError}
              </p>
            ) : null}
            {passwordStatus ? (
              <p className="account-card__status" role="status">
                {passwordStatus}
              </p>
            ) : null}
            <Button type="submit" loading={passwordForm.formState.isSubmitting}>
              Actualizar contraseña
            </Button>
          </form>
        </section>
      ) : null}
    </section>
  )
}
