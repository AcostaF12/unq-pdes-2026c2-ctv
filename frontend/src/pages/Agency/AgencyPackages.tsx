import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import {
  createPackage,
  deletePackage,
  getAgencyPackages,
  setPackageActive,
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
  const [message, setMessage] = useState<string | null>(null)

  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.agency,
    queryFn: getAgencyPackages,
  })

  const packages = packagesQuery.data ?? []

  const invalidatePackages = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.packages.all })
  }

  const createMutation = useMutation({
    mutationFn: createPackage,
    onSuccess: async () => {
      setFormKey((key) => key + 1)
      setMessage('Paquete creado.')
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

  const onCreate = (payload: TravelPackagePayload) => {
    setMessage(null)
    createMutation.mutate(payload)
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
        title="Nuevo paquete"
        submitLabel="Crear paquete"
        submitting={createMutation.isPending}
        serverError={
          createMutation.isError
            ? (getApiErrorMessage(createMutation.error) ?? 'No se pudo crear el paquete.')
            : null
        }
        onSubmit={onCreate}
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
