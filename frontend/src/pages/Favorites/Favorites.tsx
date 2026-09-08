import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import { getFavorites, removeFavorite } from '../../api/favorites'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import '../Packages/packages.css'

export function Favorites() {
  const queryClient = useQueryClient()

  const favoritesQuery = useQuery({
    queryKey: queryKeys.favorites,
    queryFn: getFavorites,
  })

  const removeMutation = useMutation({
    mutationFn: removeFavorite,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.favorites })
    },
  })

  const favorites = favoritesQuery.data ?? []
  const loadError = favoritesQuery.error
  const removeError = removeMutation.error

  return (
    <section className="page">
      <h1 className="page__title">Favoritos</h1>
      {removeError && !loadError ? (
        <p className="catalog__error" role="alert">
          {getApiErrorMessage(removeError) ?? 'No se pudo quitar el favorito.'}
        </p>
      ) : null}
      {favoritesQuery.isPending ? (
        <ListSkeleton variant="cards" withMedia={false} label="Cargando favoritos" />
      ) : loadError ? (
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={
            getApiErrorMessage(loadError) ?? 'No pudimos cargar tus favoritos. Intentá de nuevo.'
          }
          action={
            <Button type="button" onClick={() => void favoritesQuery.refetch()}>
              Reintentar
            </Button>
          }
        />
      ) : favorites.length === 0 ? (
        <EmptyState
          title="Sin favoritos"
          message="Todavía no guardaste ningún viaje. Explorá el catálogo y marcá los que te gusten."
          action={
            <Link className="empty-state__cta" to="/trips">
              Explorar paquetes
            </Link>
          }
        />
      ) : (
        <ul className="catalog__grid">
          {favorites.map((favorite) => (
            <li key={favorite.id} className="catalog__card">
              <Link to={`/trips/${favorite.packageId}`}>
                <h2>{favorite.travelPackage.name}</h2>
              </Link>
              <p className="catalog__price">USD {favorite.travelPackage.price}</p>
              <Button
                type="button"
                variant="ghost"
                onClick={() => removeMutation.mutate(favorite.packageId)}
                loading={removeMutation.isPending && removeMutation.variables === favorite.packageId}
              >
                Quitar
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
