import { Fragment, useState, type FormEvent } from 'react'
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
import { AdminStatusBadge } from './AdminStatusBadge'
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
      {notice ? <p className="admin-notice" role="status">{notice}</p> : null}
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
        <div className="admin-table-wrap" role="region" aria-label="Agencias registradas" tabIndex={0}>
          <table className="admin-table">
            <caption>Agencias registradas</caption>
            <thead>
              <tr>
                <th scope="col">Agencia</th>
                <th scope="col">Estado</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {agencies.data.map((agency) => {
                const active = agency.active !== false
                return (
                  <Fragment key={agency.id}>
                    <tr>
                      <td>
                        <span className="admin-table__name">{agency.name}</span>
                      </td>
                      <td>
                        <AdminStatusBadge
                          active={active}
                          activeLabel="Activa"
                          inactiveLabel="Inactiva"
                        />
                      </td>
                      <td className="admin-table__actions-cell">
                        <div className="admin-table__actions">
                          <Button
                            variant="ghost"
                            disabled={busy}
                            aria-label={`Editar ${agency.name}`}
                            onClick={() => openForm(agency)}
                          >
                            Editar
                          </Button>
                          <Button
                            variant="ghost"
                            disabled={busy || editing !== null}
                            aria-label={`${active ? 'Desactivar' : 'Reactivar'} ${agency.name}`}
                            onClick={() => {
                              toggle.reset()
                              setNotice(null)
                              if (!active) {
                                toggle.mutate(agency)
                              } else {
                                setConfirming(agency)
                              }
                            }}
                          >
                            {active ? 'Desactivar' : 'Reactivar'}
                          </Button>
                        </div>
                      </td>
                    </tr>
                    {confirming?.id === agency.id ? (
                      <tr className="admin-table__confirm">
                        <td colSpan={3}>
                          <div
                            className="admin-table__confirm-panel"
                            role="group"
                            aria-label={`Confirmar desactivación de ${agency.name}`}
                          >
                            <p>
                              La agencia no podrá operar y sus paquetes dejarán de ofrecerse. Las compras e
                              historiales se conservan.
                            </p>
                            <div className="admin-table__actions">
                              <Button disabled={busy} onClick={() => toggle.mutate(agency)}>
                                Confirmar desactivación
                              </Button>
                              <Button variant="ghost" disabled={busy} onClick={() => setConfirming(null)}>
                                Cancelar
                              </Button>
                            </div>
                          </div>
                        </td>
                      </tr>
                    ) : null}
                  </Fragment>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
