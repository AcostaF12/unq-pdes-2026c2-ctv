import { useCallback, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getApiErrorMessage } from '../api/errors'
import { addFavorite, getFavorites, removeFavorite, type Favorite } from '../api/favorites'
import type { TravelPackage } from '../api/packages'
import { queryKeys } from '../query/keys'
import { useToast } from '../components/Toast/ToastProvider'

interface ToggleVariables {
  travelPackage: TravelPackage
  isFavorite: boolean
}

interface ToggleContext {
  previous: Favorite[] | undefined
}

interface UseFavoritesOptions {
  enabled?: boolean
  /** Show a toast on every successful toggle (the favorites page keeps its own feedback). */
  notify?: boolean
}

/**
 * Single source of truth for favorites: one cached list, optimistic toggling
 * with rollback and a stable `isFavorite` lookup for cards and the detail page.
 */
export function useFavorites({ enabled = true, notify = true }: UseFavoritesOptions = {}) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [error, setError] = useState<string | null>(null)

  const favoritesQuery = useQuery({
    queryKey: queryKeys.favorites,
    queryFn: getFavorites,
    enabled,
  })

  const favorites = favoritesQuery.data
  const favoriteIds = useMemo(
    () => new Set((favorites ?? []).map((favorite) => favorite.packageId)),
    [favorites],
  )

  const mutation = useMutation<void, unknown, ToggleVariables, ToggleContext>({
    mutationFn: async ({ travelPackage, isFavorite }) => {
      if (isFavorite) {
        await removeFavorite(travelPackage.id)
        return
      }
      await addFavorite(travelPackage.id)
    },
    onMutate: async ({ travelPackage, isFavorite }) => {
      setError(null)
      await queryClient.cancelQueries({ queryKey: queryKeys.favorites })
      const previous = queryClient.getQueryData<Favorite[]>(queryKeys.favorites)
      const current = previous ?? []
      queryClient.setQueryData<Favorite[]>(
        queryKeys.favorites,
        isFavorite
          ? current.filter((favorite) => favorite.packageId !== travelPackage.id)
          : [...current, { id: -travelPackage.id, packageId: travelPackage.id, travelPackage }],
      )
      return { previous }
    },
    onError: (reason, variables, context) => {
      if (context) {
        queryClient.setQueryData(queryKeys.favorites, context.previous)
      }
      const fallback = variables.isFavorite
        ? 'No se pudo quitar el favorito.'
        : 'No se pudo guardar el favorito.'
      const message = getApiErrorMessage(reason) ?? fallback
      setError(message)
      toast.show(message, 'error')
    },
    onSuccess: (_data, variables) => {
      if (notify) {
        toast.show(
          variables.isFavorite ? 'Quitado de tus favoritos.' : 'Guardado en tus favoritos.',
          'success',
        )
      }
    },
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.favorites })
    },
  })

  const { mutate } = mutation
  const toggle = useCallback(
    (travelPackage: TravelPackage) => {
      mutate({ travelPackage, isFavorite: favoriteIds.has(travelPackage.id) })
    },
    [mutate, favoriteIds],
  )

  const isFavorite = useCallback((packageId: number) => favoriteIds.has(packageId), [favoriteIds])
  const isToggling = (packageId: number) =>
    mutation.isPending && mutation.variables?.travelPackage.id === packageId

  return {
    favoritesQuery,
    favorites: favorites ?? [],
    favoriteIds,
    isFavorite,
    isToggling,
    toggle,
    error,
    clearError: () => setError(null),
  }
}
