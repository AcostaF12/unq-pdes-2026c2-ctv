import { useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import {
  createPackage,
  deletePackage,
  getAgencyPackages,
  updatePackage,
  setPackageActive,
  type TravelPackage,
  type TravelPackagePayload,
} from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { PackageForm } from '../../components/PackageForm/PackageForm'
import { queryKeys } from '../../query/keys'
import { AdminStatusBadge } from '../Admin/AdminStatusBadge'
import '../Admin/admin.css'
import '../Packages/packages.css'
import './AgencyPackages.css'

type PackageActionIcon = 'archive' | 'publish' | 'edit' | 'delete'

const ACTION_ICON_PATHS: Record<PackageActionIcon, ReactNode> = {
  archive: (
    <>
      <path d="M4 8h16v11a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V8z" />
      <path d="M3 8h18" />
      <path d="M9 4h6l1 4H8z" />
      <path d="M10 13h4" />
    </>
  ),
  publish: (
    <>
      <path d="M12 16V6" />
      <path d="M8 9l4-4 4 4" />
      <path d="M5 19h14" />
    </>
  ),
  edit: (
    <>
      <path d="M12 20h8" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4z" />
    </>
  ),
  delete: (
    <>
      <path d="M4 7h16" />
      <path d="M9 7V5h6v2" />
      <path d="M7 7l1 13h8l1-13" />
    </>
  ),
}

function PackageActionIcon({ name }: { name: PackageActionIcon }) {
  return (
    <svg className="agency-package__icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      {ACTION_ICON_PATHS[name]}
    </svg>
  )
}

interface PackageRowProps {
  item: TravelPackage
  archived?: boolean
  statusPending: boolean
  savePending: boolean
  editing: boolean
  deleting: boolean
  onStatus: () => void
  onEdit: () => void
  onDelete: () => void
}

function PackageRow({
  item,
  archived = false,
  statusPending,
  savePending,
  editing,
  deleting,
  onStatus,
  onEdit,
  onDelete,
}: PackageRowProps) {
  const archivedItem = item.active === false

  return (
    <li className={archived ? 'agency-package agency-package--archived' : 'agency-package'}>
      <div className="agency-package__main">
        <div className="agency-package__top">
          <h3 className="agency-package__title">
            <Link to={`/trips/${item.id}`}>{item.name}</Link>
          </h3>
          {archived ? null : (
            <AdminStatusBadge active activeLabel="Publicado" inactiveLabel="Archivado" />
          )}
        </div>
        <p className="agency-package__route">
          {item.origin.name} → {item.destination.name}
        </p>
      </div>
      <p className="agency-package__price">USD {item.price}</p>
      <div className="agency-package__actions">
        <Button
          type="button"
          variant="ghost"
          className="agency-package__icon-btn"
          aria-label={archivedItem ? 'Publicar' : 'Archivar'}
          title={archivedItem ? 'Publicar' : 'Archivar'}
          disabled={statusPending}
          onClick={onStatus}
        >
          <PackageActionIcon name={archivedItem ? 'publish' : 'archive'} />
        </Button>
        <Button
          type="button"
          variant="ghost"
          className="agency-package__icon-btn"
          aria-label={`Editar ${item.name}`}
          title={`Editar ${item.name}`}
          disabled={savePending}
          onClick={onEdit}
        >
          <PackageActionIcon name="edit" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          className="agency-package__icon-btn agency-package__icon-btn--danger"
          aria-label="Eliminar"
          title="Eliminar"
          disabled={savePending || editing}
          onClick={onDelete}
          loading={deleting}
        >
          <PackageActionIcon name="delete" />
        </Button>
      </div>
    </li>
  )
}

function AgencyPackageLists({
  packages,
  archiveOpen,
  onArchiveToggle,
  statusPending,
  savePending,
  editingId,
  deletingId,
  onStatus,
  onEdit,
  onDelete,
}: {
  packages: TravelPackage[]
  archiveOpen: boolean
  onArchiveToggle: (open: boolean) => void
  statusPending: boolean
  savePending: boolean
  editingId: number | undefined
  deletingId: number | undefined
  onStatus: (item: TravelPackage) => void
  onEdit: (item: TravelPackage) => void
  onDelete: (id: number) => void
}) {
  const published = packages.filter((item) => item.active !== false)
  const archived = packages.filter((item) => item.active === false)

  return (
    <>
      <section className="agency-catalog" aria-labelledby="published-packages">
        <div className="agency-packages__list-head">
          <h2 id="published-packages" className="agency-packages__list-title">
            Publicados
          </h2>
          <p className="agency-packages__count">
            {published.length === 0 ? 'Ninguno a la venta' : `${published.length} a la venta`}
          </p>
        </div>
        {published.length === 0 ? (
          <p className="agency-catalog__empty">No hay paquetes publicados.</p>
        ) : (
          <ul className="agency-packages__grid">
            {published.map((item) => (
              <PackageRow
                key={item.id}
                item={item}
                statusPending={statusPending}
                savePending={savePending}
                editing={editingId === item.id}
                deleting={deletingId === item.id}
                onStatus={() => onStatus(item)}
                onEdit={() => onEdit(item)}
                onDelete={() => onDelete(item.id)}
              />
            ))}
          </ul>
        )}
      </section>

      {archived.length > 0 ? (
        <section className="agency-archive" aria-labelledby="archived-packages">
          <details className="agency-archive__panel" open={archiveOpen}>
            <summary
              className="agency-archive__summary"
              onClick={(event) => {
                event.preventDefault()
                onArchiveToggle(!archiveOpen)
              }}
            >
              <span className="agency-archive__chevron" aria-hidden="true" />
              <h2 id="archived-packages" className="agency-packages__list-title">
                Archivados
              </h2>
              <span className="agency-packages__count">
                {archived.length === 1 ? '1 paquete' : `${archived.length} paquetes`}
              </span>
            </summary>
            <p className="agency-archive__hint">
              No aparecen en el catálogo. Podés volver a publicarlos cuando quieras.
            </p>
            <ul className="agency-packages__grid">
              {archived.map((item) => (
                <PackageRow
                  key={item.id}
                  item={item}
                  archived
                  statusPending={statusPending}
                  savePending={savePending}
                  editing={editingId === item.id}
                  deleting={deletingId === item.id}
                  onStatus={() => onStatus(item)}
                  onEdit={() => onEdit(item)}
                  onDelete={() => onDelete(item.id)}
                />
              ))}
            </ul>
          </details>
        </section>
      ) : null}
    </>
  )
}

export function AgencyPackages() {
  const queryClient = useQueryClient()
  const [formKey, setFormKey] = useState(0)
  const [editingPackage, setEditingPackage] = useState<TravelPackage | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [archiveOpen, setArchiveOpen] = useState(true)

  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.agency,
    queryFn: getAgencyPackages,
  })

  const packages = packagesQuery.data ?? []

  const invalidatePackages = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.packages.all })
  }

  const saveMutation = useMutation({
    mutationFn: (payload: TravelPackagePayload) =>
      editingPackage ? updatePackage(editingPackage.id, payload) : createPackage(payload),
    onSuccess: async () => {
      setFormKey((key) => key + 1)
      setMessage(editingPackage ? 'Paquete actualizado.' : 'Paquete creado.')
      setEditingPackage(null)
      await invalidatePackages()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deletePackage,
    onSuccess: async () => {
      await invalidatePackages()
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => setPackageActive(id, active),
    onSuccess: async (saved) => {
      if (!saved.active) {
        setArchiveOpen(true)
      }
      setMessage(saved.active ? 'Paquete publicado.' : 'Paquete archivado. El historial se conserva.')
      await invalidatePackages()
    },
  })

  const openForm = (travelPackage: TravelPackage | null) => {
    saveMutation.reset()
    setMessage(null)
    setEditingPackage(travelPackage)
    setFormKey((key) => key + 1)
  }

  const onSave = (payload: TravelPackagePayload) => {
    setMessage(null)
    saveMutation.mutate(payload)
  }

  const loadError = packagesQuery.error
  const deleteError = deleteMutation.error
  const loading = packagesQuery.isPending

  return (
    <section className="page agency-packages">
      <header className="agency-packages__intro">
        <p className="catalog__eyebrow">Tu catálogo</p>
        <h1 className="page__title">Paquetes de la agencia</h1>
        <p className="catalog__muted">Creá viajes, publicá los que están a la venta y archivá el resto.</p>
      </header>
      {statusMutation.error ? (
        <p role="alert" className="catalog__error">
          {getApiErrorMessage(statusMutation.error) ?? 'No se pudo cambiar el estado.'}
        </p>
      ) : null}
      {deleteError && !loadError ? (
        <p className="catalog__error" role="alert">
          {getApiErrorMessage(deleteError) ?? 'No se pudo eliminar el paquete.'}
        </p>
      ) : null}
      {message ? (
        <p className="catalog__muted" role="status">
          {message}
        </p>
      ) : null}

      <PackageForm
        key={formKey}
        title={editingPackage ? 'Editar paquete' : 'Nuevo paquete'}
        submitLabel={editingPackage ? 'Guardar cambios' : 'Crear paquete'}
        initialPackage={editingPackage ?? undefined}
        submitting={saveMutation.isPending}
        serverError={
          saveMutation.isError
            ? (getApiErrorMessage(saveMutation.error) ?? 'No se pudo guardar el paquete.')
            : null
        }
        onSubmit={onSave}
        onCancel={editingPackage ? () => openForm(null) : undefined}
      />

      {loadError ? (
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={getApiErrorMessage(loadError) ?? 'No pudimos cargar la agencia. Intentá de nuevo.'}
          action={
            <Button
              type="button"
              onClick={() => {
                void packagesQuery.refetch()
              }}
            >
              Reintentar
            </Button>
          }
        />
      ) : loading ? (
        <ListSkeleton variant="cards" withMedia={false} label="Cargando paquetes de la agencia" />
      ) : packages.length === 0 ? (
        <EmptyState title="Sin paquetes" message="Creá el primero con el formulario de arriba." />
      ) : (
        <AgencyPackageLists
          packages={packages}
          archiveOpen={archiveOpen}
          onArchiveToggle={setArchiveOpen}
          statusPending={statusMutation.isPending}
          savePending={saveMutation.isPending}
          editingId={editingPackage?.id}
          deletingId={deleteMutation.isPending ? deleteMutation.variables : undefined}
          onStatus={(item) => statusMutation.mutate({ id: item.id, active: item.active === false })}
          onEdit={openForm}
          onDelete={(id) => deleteMutation.mutate(id)}
        />
      )}
    </section>
  )
}
