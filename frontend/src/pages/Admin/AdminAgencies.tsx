import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { createAgency, listAgencies, updateAgency, type CreateAgencyPayload } from '../../api/agencies'
import type { Agency } from '../../api/packages'
import { getApiErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button/Button'
import { Input } from '../../components/Input/Input'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import './admin.css'

const emptyForm: CreateAgencyPayload = { name: '', username: '', password: '', firstName: '', lastName: '' }

export function AdminAgencies() {
  const client = useQueryClient()
  const [editing, setEditing] = useState<Agency | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [confirming, setConfirming] = useState<Agency | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const agencies = useQuery({ queryKey: queryKeys.agencies, queryFn: listAgencies })
  const refresh = async () => {
    await client.invalidateQueries({ queryKey: queryKeys.agencies })
    await client.invalidateQueries({ queryKey: queryKeys.packages.all })
  }
  const save = useMutation({
    mutationFn: (payload: CreateAgencyPayload) => editing
      ? updateAgency(editing.id, { name: payload.name, active: editing.active !== false })
      : createAgency(payload),
    onSuccess: async () => {
      setNotice(editing ? 'Agencia actualizada.' : 'Agencia creada.')
      setEditing(null)
      setForm(emptyForm)
      await refresh()
    },
  })
  const toggle = useMutation({
    mutationFn: (agency: Agency) => updateAgency(agency.id, { name: agency.name, active: agency.active === false }),
    onSuccess: async (agency) => {
      setConfirming(null)
      setNotice(agency.active === false ? 'Agencia desactivada.' : 'Agencia reactivada.')
      await refresh()
    },
  })
  const busy = save.isPending || toggle.isPending
  const openForm = (agency: Agency | null) => {
    save.reset()
    toggle.reset()
    setConfirming(null)
    setNotice(null)
    setEditing(agency)
    setForm({ ...emptyForm, name: agency?.name ?? '' })
  }
  const submit = (event: FormEvent) => {
    event.preventDefault()
    setNotice(null)
    save.mutate({ ...form, name: form.name.trim(), username: form.username.trim(), firstName: form.firstName.trim(), lastName: form.lastName.trim() })
  }
  const field = (name: keyof CreateAgencyPayload, label: string, maxLength: number, type = 'text') => (
    <Input label={label} type={type} value={form[name]} required maxLength={maxLength}
      minLength={name === 'password' ? 6 : 1}
      autoComplete={name === 'password' ? 'new-password' : undefined}
      onChange={(event) => setForm({ ...form, [name]: event.target.value })} />
  )
  return (
    <section className="page">
      <Link className="catalog__back" to="/admin">← Administración</Link>
      <h1 className="page__title">Agencias</h1>
      {notice ? <p role="status">{notice}</p> : null}
      <form className="catalog__form" aria-label={editing ? 'Editar agencia' : 'Nueva agencia'} onSubmit={submit}>
        <h2>{editing ? 'Editar agencia' : 'Nueva agencia y cuenta'}</h2>
        <fieldset className="admin-form__grid" disabled={busy}>
          <legend>{editing ? 'Datos de la agencia' : 'Datos de la agencia y su responsable'}</legend>
          {field('name', 'Nombre de agencia', 160)}
          {!editing ? <>
            {field('username', 'Usuario', 60)}
            {field('password', 'Contraseña', 72, 'password')}
            {field('firstName', 'Nombre del responsable', 80)}
            {field('lastName', 'Apellido del responsable', 80)}
          </> : null}
        </fieldset>
        {save.isError ? <p role="alert">{getApiErrorMessage(save.error) ?? 'No se pudo guardar la agencia.'}</p> : null}
        <div className="page__actions">
          <Button type="submit" loading={save.isPending} disabled={busy}>{editing ? 'Guardar cambios' : 'Crear agencia'}</Button>
          {editing ? <Button type="button" variant="ghost" disabled={busy} onClick={() => openForm(null)}>Cancelar</Button> : null}
        </div>
      </form>
      {toggle.isError ? <p role="alert">{getApiErrorMessage(toggle.error) ?? 'No se pudo cambiar el estado.'}</p> : null}
      {agencies.isPending ? <ListSkeleton variant="rows" count={3} label="Cargando agencias" /> : agencies.isError ? (
        <EmptyState tone="error" title="No se pudieron cargar las agencias" message="Intentá de nuevo."
          action={<Button onClick={() => void agencies.refetch()}>Reintentar</Button>} />
      ) : agencies.data.length === 0 ? <EmptyState title="Sin agencias" message="Creá la primera agencia con su cuenta." /> : (
        <ul className="admin-list" aria-label="Agencias registradas">
          {agencies.data.map((agency) => <li className="admin-row" key={agency.id}>
            <div className="admin-row__main"><h2>{agency.name}</h2><p>{agency.active === false ? 'Inactiva' : 'Activa'}</p></div>
            <div className="page__actions">
              <Button variant="ghost" disabled={busy} aria-label={`Editar ${agency.name}`} onClick={() => openForm(agency)}>Editar</Button>
              <Button variant="ghost" disabled={busy || editing !== null} aria-label={`${agency.active === false ? 'Reactivar' : 'Desactivar'} ${agency.name}`}
                onClick={() => {
                  toggle.reset()
                  setNotice(null)
                  if (agency.active === false) {
                    toggle.mutate(agency)
                  } else {
                    setConfirming(agency)
                  }
                }}>
                {agency.active === false ? 'Reactivar' : 'Desactivar'}
              </Button>
            </div>
            {confirming?.id === agency.id ? <div role="group" aria-label={`Confirmar desactivación de ${agency.name}`}>
              <p>La agencia no podrá operar y sus paquetes dejarán de ofrecerse. Las compras e historiales se conservan.</p>
              <Button disabled={busy} onClick={() => toggle.mutate(agency)}>Confirmar desactivación</Button>
              <Button variant="ghost" disabled={busy} onClick={() => setConfirming(null)}>Cancelar</Button>
            </div> : null}
          </li>)}
        </ul>
      )}
    </section>
  )
}
