import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { createAgency, listAgencies, updateAgency } from '../../api/agencies'
import { getApiErrorMessage } from '../../api/errors'
import type { Agency } from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { Input } from '../../components/Input/Input'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import { ADMIN_PATH } from '../../router/paths'
import '../Packages/packages.css'
import './admin.css'

export function AdminAgencies() {
  const client = useQueryClient()
  const agencies = useQuery({ queryKey: queryKeys.agencies, queryFn: listAgencies })
  const [editing, setEditing] = useState<Agency | null>(null)
  const [name, setName] = useState('')
  const [notice, setNotice] = useState<string | null>(null)
  const save = useMutation({
    mutationFn: () => (editing ? updateAgency(editing.id, name.trim()) : createAgency(name.trim())),
    onSuccess: async () => {
      setName('')
      setEditing(null)
      setNotice('Agencia guardada.')
      await Promise.all([
        client.invalidateQueries({ queryKey: queryKeys.agencies }),
        client.invalidateQueries({ queryKey: queryKeys.adminUsers }),
        client.invalidateQueries({ queryKey: queryKeys.packages.all }),
      ])
    },
  })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (name.trim()) save.mutate()
  }
  return (
    <section className="page">
      <Link className="catalog__back" to={ADMIN_PATH}>
        ← Administración
      </Link>
      <h1 className="page__title">Agencias</h1>
      <p className="catalog__muted">Creá la agencia antes de asignarle vendedores y paquetes.</p>
      <form
        className="admin-card"
        onSubmit={submit}
        aria-label={editing ? 'Editar agencia' : 'Nueva agencia'}
      >
        <h2>{editing ? 'Editar agencia' : 'Nueva agencia'}</h2>
        <Input
          label="Nombre de la agencia"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          maxLength={160}
          disabled={save.isPending}
        />
        {save.error ? (
          <p role="alert" className="package-form__error">
            {getApiErrorMessage(save.error) ?? 'No pudimos guardar la agencia.'}
          </p>
        ) : null}
        <div className="page__actions">
          <Button type="submit" loading={save.isPending} disabled={!name.trim()}>
            {editing ? 'Guardar cambios' : 'Crear agencia'}
          </Button>
          {editing ? (
            <Button
              type="button"
              variant="ghost"
              disabled={save.isPending}
              onClick={() => {
                setEditing(null)
                setName('')
                save.reset()
              }}
            >
              Cancelar
            </Button>
          ) : null}
        </div>
      </form>
      {notice ? (
        <p role="status" className="admin-notice">
          {notice}
        </p>
      ) : null}
      {agencies.isPending ? (
        <ListSkeleton variant="rows" label="Cargando agencias" />
      ) : agencies.error ? (
        <EmptyState
          tone="error"
          title="No pudimos cargar las agencias"
          message="Intentá de nuevo."
          action={<Button onClick={() => void agencies.refetch()}>Reintentar</Button>}
        />
      ) : !agencies.data.length ? (
        <EmptyState title="Sin agencias" message="Completá el formulario para crear una." />
      ) : (
        <ul className="admin-list" aria-label="Agencias registradas">
          {agencies.data.map((agency) => (
            <li key={agency.id} className="admin-row">
              <h2 className="admin-row__title">{agency.name}</h2>
              <Button
                variant="ghost"
                disabled={save.isPending}
                aria-label={`Editar ${agency.name}`}
                onClick={() => {
                  setEditing(agency)
                  setName(agency.name)
                  setNotice(null)
                  save.reset()
                }}
              >
                Editar
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
