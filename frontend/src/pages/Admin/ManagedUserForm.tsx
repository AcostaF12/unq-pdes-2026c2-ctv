import { useState, type FormEvent } from 'react'
import type { CreateManagedUser, ManagedUser, UpdateManagedUser } from '../../api/adminUsers'
import type { Agency } from '../../api/packages'
import { USER_ROLES, roleLabel, type UserRole } from '../../auth/roles'
import { Button } from '../../components/Button/Button'
import { Input } from '../../components/Input/Input'
import { Select } from '../../components/Select/Select'

interface Props {
  user?: ManagedUser
  self: boolean
  agencies: Agency[]
  pending: boolean
  error: string | null
  onCreate: (payload: CreateManagedUser) => void
  onUpdate: (payload: UpdateManagedUser) => void
  onCancel: () => void
}

export function ManagedUserForm({
  user,
  self,
  agencies,
  pending,
  error,
  onCreate,
  onUpdate,
  onCancel,
}: Props) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [firstName, setFirstName] = useState(user?.firstName ?? '')
  const [lastName, setLastName] = useState(user?.lastName ?? '')
  const [role, setRole] = useState<UserRole>(user?.role ?? 'BUYER')
  const [agencyId, setAgencyId] = useState(String(user?.agency?.id ?? ''))
  const [enabled, setEnabled] = useState(user?.enabled ?? true)
  const [validation, setValidation] = useState<string | null>(null)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!firstName.trim() || !lastName.trim()) {
      setValidation('Completá nombre y apellido.')
      return
    }
    if (!user && new TextEncoder().encode(password).length > 72) {
      setValidation('La contraseña supera los 72 bytes. Usá una más corta.')
      return
    }
    setValidation(null)
    const payload = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      role,
      ...(role === 'AGENCY' ? { agencyId: Number(agencyId) } : {}),
    }
    if (user) onUpdate({ ...payload, enabled })
    else onCreate({ ...payload, username, password })
  }

  return (
    <form
      className="admin-card"
      onSubmit={submit}
      aria-label={user ? `Editar usuario ${user.username}` : 'Nuevo usuario'}
    >
      <h2>{user ? `Editar @${user.username}` : 'Nuevo usuario'}</h2>
      <fieldset className="admin-form__fieldset" disabled={pending}>
        <div className="admin-form__grid">
          {!user ? (
            <>
              <Input
                label="Usuario"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                minLength={3}
                maxLength={60}
                pattern="[A-Za-z0-9_.\-]{3,60}"
                autoComplete="off"
              />
              <Input
                label="Contraseña inicial"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={6}
                maxLength={72}
                autoComplete="new-password"
              />
            </>
          ) : null}
          <Input
            label="Nombre"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            required
            maxLength={80}
          />
          <Input
            label="Apellido"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            required
            maxLength={80}
          />
          <Select
            label="Rol"
            value={role}
            onChange={(e) => setRole(e.target.value as UserRole)}
            disabled={self}
          >
            {USER_ROLES.map((value) => (
              <option key={value} value={value}>
                {value === 'AGENCY' ? 'Vendedor (agencia)' : roleLabel(value)}
              </option>
            ))}
          </Select>
          {role === 'AGENCY' ? (
            <Select label="Agencia" value={agencyId} onChange={(e) => setAgencyId(e.target.value)} required>
              <option value="">Seleccioná una agencia</option>
              {agencies.map((agency) => (
                <option key={agency.id} value={agency.id}>
                  {agency.name}
                </option>
              ))}
            </Select>
          ) : null}
          {user ? (
            <Select
              label="Estado"
              value={enabled ? 'enabled' : 'disabled'}
              disabled={self}
              onChange={(e) => setEnabled(e.target.value === 'enabled')}
            >
              <option value="enabled">Habilitado</option>
              <option value="disabled">Deshabilitado</option>
            </Select>
          ) : null}
        </div>
        {self ? (
          <p className="catalog__muted">
            Tu propia cuenta debe conservar el rol de administrador y permanecer habilitada.
          </p>
        ) : null}
        {role === 'AGENCY' && !agencies.length ? (
          <p role="status">Primero creá una agencia en Administración → Agencias.</p>
        ) : null}
        {validation || error ? (
          <p className="package-form__error" role="alert">
            {validation ?? error}
          </p>
        ) : null}
        <div className="page__actions">
          <Button type="submit" loading={pending} disabled={role === 'AGENCY' && !agencies.length}>
            {user ? 'Guardar cambios' : 'Crear usuario'}
          </Button>
          <Button type="button" variant="ghost" onClick={onCancel}>
            Cancelar
          </Button>
        </div>
      </fieldset>
    </form>
  )
}
