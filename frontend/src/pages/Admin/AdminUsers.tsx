import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  createManagedUser,
  listManagedUsers,
  updateManagedUser,
  type CreateManagedUser,
  type ManagedUser,
  type UpdateManagedUser,
} from '../../api/adminUsers'
import { listAgencies } from '../../api/agencies'
import { getApiErrorMessage } from '../../api/errors'
import { useAuth } from '../../auth/AuthContext'
import { roleLabel } from '../../auth/roles'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { Pagination } from '../../components/Pagination/Pagination'
import { queryKeys } from '../../query/keys'
import { ADMIN_PATH } from '../../router/paths'
import { AdminStatusBadge } from './AdminStatusBadge'
import { ManagedUserForm } from './ManagedUserForm'
import '../Packages/packages.css'
import './admin.css'

function userRoleLabel(user: ManagedUser) {
  return user.role === 'AGENCY' ? 'Vendedor' : roleLabel(user.role)
}

export function AdminUsers() {
  const client = useQueryClient()
  const { user: currentUser, updateUser } = useAuth()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<ManagedUser | 'new' | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const users = useQuery({ queryKey: [...queryKeys.adminUsers, page], queryFn: () => listManagedUsers(page) })
  const agencies = useQuery({ queryKey: queryKeys.agencies, queryFn: listAgencies })
  const save = useMutation({
    mutationFn: (payload: { create: CreateManagedUser } | { id: number; update: UpdateManagedUser }) =>
      'create' in payload ? createManagedUser(payload.create) : updateManagedUser(payload.id, payload.update),
    onSuccess: async (saved) => {
      if (saved.id === currentUser?.id)
        updateUser({ ...currentUser, ...saved, agency: saved.agency ?? undefined })
      setEditing(null)
      setNotice('Usuario guardado. Los permisos se aplican en la próxima petición al servidor.')
      await client.invalidateQueries({ queryKey: queryKeys.adminUsers })
    },
  })
  const open = (next: typeof editing) => {
    save.reset()
    setNotice(null)
    setEditing(next)
  }
  const loadError = users.error ?? agencies.error

  return (
    <section className="page">
      <Link className="catalog__back" to={ADMIN_PATH}>
        ← Administración
      </Link>
      <div className="page__header">
        <h1 className="page__title">Usuarios y vendedores</h1>
        {!editing ? <Button onClick={() => open('new')}>Nuevo usuario</Button> : null}
      </div>
      <p className="catalog__muted">
        Asigná roles y agencias. Deshabilitar una cuenta bloquea el acceso y conserva sus compras y ventas.
      </p>
      {notice ? (
        <p className="admin-notice" role="status">
          {notice}
        </p>
      ) : null}
      {loadError ? (
        <EmptyState
          tone="error"
          title="No pudimos cargar los usuarios"
          message={getApiErrorMessage(loadError) ?? 'Intentá de nuevo.'}
          action={
            <Button
              onClick={() => {
                void users.refetch()
                void agencies.refetch()
              }}
            >
              Reintentar
            </Button>
          }
        />
      ) : users.isPending || agencies.isPending ? (
        <ListSkeleton variant="rows" label="Cargando usuarios" />
      ) : (
        <>
          {editing ? (
            <ManagedUserForm
              key={editing === 'new' ? 'new' : editing.id}
              user={editing === 'new' ? undefined : editing}
              self={editing !== 'new' && editing.id === currentUser?.id}
              agencies={agencies.data ?? []}
              pending={save.isPending}
              error={save.error ? (getApiErrorMessage(save.error) ?? 'No pudimos guardar el usuario.') : null}
              onCreate={(payload) => save.mutate({ create: payload })}
              onUpdate={(payload) => {
                if (editing !== 'new') save.mutate({ id: editing.id, update: payload })
              }}
              onCancel={() => open(null)}
            />
          ) : null}
          {!users.data?.content.length ? (
            <EmptyState title="Sin usuarios" message="Creá el primer usuario desde este panel." />
          ) : (
            <div
              className="admin-table-wrap"
              role="region"
              aria-label="Usuarios registrados"
              tabIndex={0}
            >
              <table className="admin-table">
                <caption>Usuarios registrados</caption>
                <thead>
                  <tr>
                    <th scope="col">Usuario</th>
                    <th scope="col">Rol</th>
                    <th scope="col">Agencia</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {users.data.content.map((user) => (
                    <tr key={user.id}>
                      <td>
                        <div className="admin-table__identity">
                          <span className="admin-table__name">
                            {user.firstName} {user.lastName} (@{user.username})
                          </span>
                        </div>
                      </td>
                      <td>{userRoleLabel(user)}</td>
                      <td>
                        {user.agency ? (
                          user.agency.name
                        ) : (
                          <span className="admin-table__muted">—</span>
                        )}
                      </td>
                      <td>
                        <AdminStatusBadge
                          active={user.enabled}
                          activeLabel="Habilitado"
                          inactiveLabel="Deshabilitado"
                        />
                      </td>
                      <td className="admin-table__actions-cell">
                        <div className="admin-table__actions">
                          <Button
                            variant="ghost"
                            disabled={save.isPending}
                            aria-label={`Editar ${user.username}`}
                            onClick={() => open(user)}
                          >
                            Editar
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <Pagination page={page} totalPages={users.data?.totalPages ?? 0} onPageChange={setPage} />
        </>
      )}
    </section>
  )
}
