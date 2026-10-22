import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listAgencies } from '../../api/agencies'
import { getApiErrorMessage } from '../../api/errors'
import {
  createPackage,
  deletePackage,
  searchPackages,
  updatePackage,
  type TravelPackage,
  type TravelPackagePayload,
} from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { PackageForm } from '../../components/PackageForm/PackageForm'
import { Pagination } from '../../components/Pagination/Pagination'
import { queryKeys } from '../../query/keys'
import { ADMIN_PATH } from '../../router/paths'
import '../Packages/packages.css'
import './admin.css'

const PAGE_SIZE = 10

type FormState = { mode: 'closed' } | { mode: 'create' } | { mode: 'edit'; travelPackage: TravelPackage }

interface ConfirmDeleteProps {
  travelPackage: TravelPackage
  pending: boolean
  error: string | null
  onConfirm: () => void
  onCancel: () => void
}

function ConfirmDelete({ travelPackage, pending, error, onConfirm, onCancel }: ConfirmDeleteProps) {
  return (
    <div
      className="admin-confirm"
      role="group"
      aria-label={`Confirmar eliminación de ${travelPackage.name}`}
      data-testid="confirm-delete"
    >
      <p>
        ¿Eliminar el paquete <strong>{travelPackage.name}</strong>? Esta acción no se puede deshacer.
      </p>
      {error ? (
        <p className="package-form__error" role="alert">
          {error}
        </p>
      ) : null}
      <div className="page__actions">
        <Button type="button" loading={pending} onClick={onConfirm}>
          Confirmar eliminación
        </Button>
        <Button type="button" variant="ghost" autoFocus onClick={onCancel}>
          Cancelar
        </Button>
      </div>
    </div>
  )
}

export function AdminPackages() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [form, setForm] = useState<FormState>({ mode: 'closed' })
  const [pendingDeleteId, setPendingDeleteId] = useState<number | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const searchParams = { page, size: PAGE_SIZE, sort: 'name,asc' }
  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.search(searchParams),
    queryFn: () => searchPackages(searchParams),
  })
  const agenciesQuery = useQuery({
    queryKey: queryKeys.agencies,
    queryFn: listAgencies,
  })

  const invalidatePackages = () => queryClient.invalidateQueries({ queryKey: queryKeys.packages.all })

  const saveMutation = useMutation({
    mutationFn: (payload: TravelPackagePayload) =>
      form.mode === 'edit' ? updatePackage(form.travelPackage.id, payload) : createPackage(payload),
    onSuccess: async () => {
      setNotice(form.mode === 'edit' ? 'Paquete actualizado.' : 'Paquete creado.')
      setForm({ mode: 'closed' })
      await invalidatePackages()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deletePackage,
    onSuccess: async () => {
      setPendingDeleteId(null)
      setNotice('Paquete eliminado.')
      if (packagesQuery.data?.content.length === 1) {
        setPage((current) => Math.max(0, current - 1))
      }
      await invalidatePackages()
    },
  })

  const openForm = (next: FormState) => {
    saveMutation.reset()
    setNotice(null)
    setForm(next)
  }

  const askDelete = (id: number) => {
    deleteMutation.reset()
    setNotice(null)
    setPendingDeleteId(id)
  }

  const result = packagesQuery.data
  const packages = result?.content ?? []
  const loadError = packagesQuery.error ?? agenciesQuery.error
  const saveError = saveMutation.isError
    ? (getApiErrorMessage(saveMutation.error) ?? 'No pudimos guardar el paquete. Intentá de nuevo.')
    : null
  const deleteError = deleteMutation.isError
    ? (getApiErrorMessage(deleteMutation.error) ?? 'No pudimos eliminar el paquete. Intentá de nuevo.')
    : null

  let list
  if (loadError) {
    list = (
      <EmptyState
        tone="error"
        title="Algo salió mal"
        message={getApiErrorMessage(loadError) ?? 'No pudimos cargar los paquetes. Intentá de nuevo.'}
        action={
          <Button
            type="button"
            onClick={() => {
              void packagesQuery.refetch()
              void agenciesQuery.refetch()
            }}
          >
            Reintentar
          </Button>
        }
      />
    )
  } else if (packagesQuery.isPending) {
    list = <ListSkeleton variant="rows" count={4} label="Cargando paquetes" />
  } else if (packages.length === 0) {
    list = <EmptyState title="Sin paquetes" message="Todavía no hay paquetes cargados." />
  } else {
    list = (
      <ul className="admin-list" aria-label="Paquetes cargados">
        {packages.map((item) => (
          <li key={item.id} className="admin-row" data-testid="package-item">
            <div className="admin-row__main">
              <h3 className="admin-row__title">
                <Link to={`/trips/${item.id}`}>{item.name}</Link>
              </h3>
              <p className="catalog__muted">Agencia: {item.agency.name}</p>
              <p className="catalog__route">
                {item.origin.name} → {item.destination.name}
              </p>
              <p className="catalog__muted">Hotel: {item.hotel.name}</p>
            </div>
            <p className="admin-row__meta">USD {item.price}</p>
            <div className="admin-row__actions">
              <Button
                type="button"
                variant="ghost"
                aria-label={`Editar ${item.name}`}
                onClick={() => openForm({ mode: 'edit', travelPackage: item })}
              >
                Editar
              </Button>
              <Button
                type="button"
                variant="ghost"
                aria-label={`Eliminar ${item.name}`}
                onClick={() => askDelete(item.id)}
              >
                Eliminar
              </Button>
            </div>
            {pendingDeleteId === item.id ? (
              <ConfirmDelete
                travelPackage={item}
                pending={deleteMutation.isPending}
                error={deleteError}
                onConfirm={() => deleteMutation.mutate(item.id)}
                onCancel={() => setPendingDeleteId(null)}
              />
            ) : null}
          </li>
        ))}
      </ul>
    )
  }

  return (
    <section className="page">
      <Link className="catalog__back" to={ADMIN_PATH}>
        ← Administración
      </Link>
      <div className="page__header">
        <h1 className="page__title">Paquetes</h1>
        {form.mode === 'closed' ? (
          <Button type="button" onClick={() => openForm({ mode: 'create' })}>
            Nuevo paquete
          </Button>
        ) : null}
      </div>

      {notice ? (
        <p className="admin-notice" role="status">
          {notice}
        </p>
      ) : null}

      {form.mode !== 'closed' && agenciesQuery.data ? (
        <PackageForm
          key={form.mode === 'edit' ? form.travelPackage.id : 'new'}
          title={form.mode === 'edit' ? 'Editar paquete' : 'Nuevo paquete'}
          submitLabel={form.mode === 'edit' ? 'Guardar cambios' : 'Crear paquete'}
          submitting={saveMutation.isPending}
          serverError={saveError}
          agencies={agenciesQuery.data}
          initialPackage={form.mode === 'edit' ? form.travelPackage : undefined}
          onSubmit={(payload) => saveMutation.mutate(payload)}
          onCancel={() => openForm({ mode: 'closed' })}
        />
      ) : null}

      {list}

      {result ? <Pagination page={page} totalPages={result.totalPages} onPageChange={setPage} /> : null}
    </section>
  )
}
