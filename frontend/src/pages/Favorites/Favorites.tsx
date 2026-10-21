import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import type { TravelPackage } from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { PackageCard } from '../../components/PackageCard/PackageCard'
import { Select } from '../../components/Select/Select'
import { useFavorites } from '../../hooks/useFavorites'
import '../Packages/packages.css'
import './Favorites.css'

type FavoriteSort = 'recent' | 'price-asc' | 'price-desc' | 'name'

const SORT_OPTIONS: { value: FavoriteSort; label: string }[] = [
  { value: 'recent', label: 'Agregados recientemente' },
  { value: 'price-asc', label: 'Precio: menor a mayor' },
  { value: 'price-desc', label: 'Precio: mayor a menor' },
  { value: 'name', label: 'Nombre (A-Z)' },
]

const LEAVE_ANIMATION_MS = 260

function sortPackages(items: TravelPackage[], sort: FavoriteSort): TravelPackage[] {
  const sorted = [...items]
  switch (sort) {
    case 'price-asc':
      return sorted.sort((a, b) => a.price - b.price)
    case 'price-desc':
      return sorted.sort((a, b) => b.price - a.price)
    case 'name':
      return sorted.sort((a, b) => a.name.localeCompare(b.name, 'es'))
    default:
      return sorted.reverse()
  }
}

export function Favorites() {
  const favorites = useFavorites({ notify: false })
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<FavoriteSort>('recent')
  const [leavingId, setLeavingId] = useState<number | null>(null)

  const { favoritesQuery } = favorites
  const loadError = favoritesQuery.error

  const packages = useMemo(() => {
    const needle = query.trim().toLowerCase()
    const all = favorites.favorites.map((favorite) => favorite.travelPackage)
    const filtered = needle
      ? all.filter((item) =>
          [item.name, item.hotel.name, item.destination.name, item.agency.name].some((field) =>
            field.toLowerCase().includes(needle),
          ),
        )
      : all
    return sortPackages(filtered, sort)
  }, [favorites.favorites, query, sort])

  const onRemove = (item: TravelPackage) => {
    setLeavingId(item.id)
    window.setTimeout(() => {
      favorites.toggle(item)
      setLeavingId(null)
    }, LEAVE_ANIMATION_MS)
  }

  const total = favorites.favorites.length
  const hasSavedItems = total > 0

  return (
    <section className="page">
      <header className="favorites__intro">
        <p className="catalog__eyebrow">Guardados para después</p>
        <h1 className="page__title">Favoritos</h1>
      </header>
      {favorites.error && !loadError ? (
        <p className="catalog__error" role="alert">
          {favorites.error}
        </p>
      ) : null}
      {hasSavedItems ? (
        <div className="favorites__toolbar">
          <Input
            label="Buscar en favoritos"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Nombre, hotel, destino o agencia"
          />
          <Select
            label="Ordenar favoritos"
            value={sort}
            onChange={(event) => setSort(event.target.value as FavoriteSort)}
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
          <p className="catalog__count" aria-live="polite">
            <strong>{packages.length}</strong> de {total}{' '}
            {total === 1 ? 'viaje guardado' : 'viajes guardados'}
          </p>
        </div>
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
      ) : !hasSavedItems ? (
        <EmptyState
          title="Sin favoritos"
          message="Todavía no guardaste ningún viaje. Explorá el catálogo y marcá los que te gusten."
          action={
            <Link className="empty-state__cta" to="/trips">
              Explorar paquetes
            </Link>
          }
        />
      ) : packages.length === 0 ? (
        <EmptyState
          title="Nada coincide"
          message="Ningún favorito coincide con tu búsqueda."
          action={
            <Button type="button" variant="ghost" onClick={() => setQuery('')}>
              Limpiar búsqueda
            </Button>
          }
        />
      ) : (
        <ul className="catalog__grid">
          {packages.map((item, index) => (
            <li key={item.id}>
              <PackageCard
                travelPackage={item}
                index={index}
                leaving={leavingId === item.id}
                favorite={{
                  active: true,
                  pending: favorites.isToggling(item.id) || leavingId === item.id,
                  onToggle: () => onRemove(item),
                }}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
