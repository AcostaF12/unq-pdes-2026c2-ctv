import { useState } from 'react'
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
import '../Packages/packages.css'

export function AgencyPackages() {
  const queryClient = useQueryClient()
  const [formKey, setFormKey] = useState(0)
  const [editingPackage, setEditingPackage] = useState<TravelPackage | null>(null)
  const [message, setMessage] = useState<string | null>(null)

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
    <section className="page">
      <h1 className="page__title">Paquetes de la agencia</h1>
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
      ) : (
        <ul className="catalog__grid">
          {packages.map((item) => (
            <li key={item.id} className="catalog__card">
              <Link to={`/trips/${item.id}`}>
                <h2>{item.name}</h2>
              </Link>
              <p className="catalog__muted">
                {item.origin.name} → {item.destination.name}
              </p>
              <p className="catalog__price">USD {item.price}</p>
              <p className="catalog__muted">{item.active === false ? 'Archivado' : 'Publicado'}</p>
              <Button
                type="button"
                variant="ghost"
                disabled={statusMutation.isPending}
                onClick={() => statusMutation.mutate({ id: item.id, active: item.active === false })}
              >
                {item.active === false ? 'Publicar' : 'Archivar'}
              </Button>
              <Button
                type="button"
                variant="ghost"
                aria-label={`Editar ${item.name}`}
                disabled={saveMutation.isPending}
                onClick={() => openForm(item)}
              >
                Editar
              </Button>
              <Button
                type="button"
                variant="ghost"
                disabled={saveMutation.isPending || editingPackage?.id === item.id}
                onClick={() => deleteMutation.mutate(item.id)}
                loading={deleteMutation.isPending && deleteMutation.variables === item.id}
              >
                Eliminar
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
