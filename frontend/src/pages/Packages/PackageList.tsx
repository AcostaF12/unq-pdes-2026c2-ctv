import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { searchPackages } from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HotelPhoto } from '../../components/HotelPhoto/HotelPhoto'
import { Input } from '../../components/Input/Input'
import { Select } from '../../components/Select/Select'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { useAuth } from '../../auth/AuthContext'
import { readPreferences } from '../../preferences/preferences'
import { queryKeys, type PackageSearchFilters } from '../../query/keys'
import './packages.css'

function CatalogEmptyScene() {
  return (
    <div className="catalog-empty" aria-hidden="true">
      <span className="catalog-empty__cloud catalog-empty__cloud--a" />
      <span className="catalog-empty__cloud catalog-empty__cloud--b" />
      <svg className="catalog-empty__route" viewBox="0 0 220 72" fill="none">
        <path
          className="catalog-empty__route-line"
          d="M18 48 C 58 12, 102 60, 148 28 S 202 18, 206 36"
        />
      </svg>
      <svg className="catalog-empty__plane" viewBox="0 0 152 64" fill="none">
        <path
          d="M72 36 L116 30 L98 62 L54 62 Z"
          fill="var(--color-primary)"
          stroke="var(--color-ink)"
          strokeWidth="2.4"
          strokeLinejoin="miter"
        />
        <path d="M10 30 H42 L38 38 H10 Z" fill="var(--color-ink)" />
        <path
          d="M28 22 L28 4 L54 22 Z"
          fill="var(--color-primary)"
          stroke="var(--color-ink)"
          strokeWidth="2.4"
          strokeLinejoin="miter"
        />
        <path d="M26 24 C26 17 38 14 54 14 H112 C128 14 140 20 146 28 C140 36 128 42 112 42 H54 C38 42 26 38 26 32 Z" fill="var(--color-ink)" />
        <path d="M118 16 C130 18 138 22 144 28 C136 26 126 18 118 17 Z" fill="var(--color-secondary)" />
        <rect
          x="74"
          y="40"
          width="30"
          height="12"
          rx="6"
          fill="var(--color-secondary)"
          stroke="var(--color-ink)"
          strokeWidth="2.4"
        />
        <g fill="var(--color-surface)">
          <circle cx="62" cy="25" r="2.2" />
          <circle cx="73" cy="25" r="2.2" />
          <circle cx="84" cy="25" r="2.2" />
          <circle cx="95" cy="25" r="2.2" />
          <circle cx="106" cy="25" r="2.2" />
        </g>
      </svg>
    </div>
  )
}

function compactFilters(filters: PackageSearchFilters): PackageSearchFilters {
  return {
    ...(filters.name ? { name: filters.name } : {}),
    ...(filters.origin ? { origin: filters.origin } : {}),
    ...(filters.destination ? { destination: filters.destination } : {}),
  }
}

function originFromPreferences(userId: number | undefined): string {
  if (userId == null) {
    return ''
  }
  const preferences = readPreferences(userId)
  return preferences.applyOriginOnSearch ? preferences.originCityCode : ''
}

export function PackageList() {
  const { user } = useAuth()
  const savedOrigin = originFromPreferences(user?.id)
  const [name, setName] = useState('')
  const [origin, setOrigin] = useState(savedOrigin)
  const [destination, setDestination] = useState('')
  const [filters, setFilters] = useState<PackageSearchFilters>(() => compactFilters({ origin: savedOrigin }))

  const citiesQuery = useQuery({
    queryKey: queryKeys.cities,
    queryFn: listCities,
  })

  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.search(filters),
    queryFn: () => searchPackages(Object.keys(filters).length ? filters : undefined),
  })

  const cities = citiesQuery.data ?? []
  const packages = packagesQuery.data ?? []
  const hasFilters = Object.keys(filters).length > 0

  const onSearch = (event: FormEvent) => {
    event.preventDefault()
    setFilters(
      compactFilters({
        name,
        origin,
        destination,
      }),
    )
  }

  const onClearFilters = () => {
    setName('')
    setOrigin('')
    setDestination('')
    setFilters({})
  }

  return (
    <section className="page">
      <header className="catalog__intro">
        <p className="catalog__eyebrow">Elegí tu escape</p>
        <h1 className="page__title">Paquetes</h1>
      </header>
      <form className="catalog__search" onSubmit={onSearch}>
        <div className="catalog__search-copy">
          <h2>Buscá tu viaje</h2>
          <p>Filtrá por nombre, ciudad de origen o destino.</p>
        </div>
        <div className="catalog__filters">
          <Input label="Nombre" value={name} onChange={(event) => setName(event.target.value)} />
          <Select label="Origen" value={origin} onChange={(event) => setOrigin(event.target.value)}>
            <option value="">Todos</option>
            {cities.map((item) => (
              <option key={`origin-${item.code}`} value={item.code}>
                {item.name}
              </option>
            ))}
          </Select>
          <Select
            label="Destino"
            value={destination}
            onChange={(event) => setDestination(event.target.value)}
          >
            <option value="">Todos</option>
            {cities.map((item) => (
              <option key={`dest-${item.code}`} value={item.code}>
                {item.name}
              </option>
            ))}
          </Select>
          <Button type="submit" className="catalog__filters-submit">
            Buscar
          </Button>
        </div>
      </form>
      {packagesQuery.isPending ? (
        <ListSkeleton variant="cards" withMedia={false} label="Cargando paquetes" />
      ) : packagesQuery.error ? (
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={getApiErrorMessage(packagesQuery.error) ?? 'No pudimos cargar los paquetes. Intentá de nuevo.'}
          action={
            <Button type="button" onClick={() => void packagesQuery.refetch()}>
              Reintentar
            </Button>
          }
        />
      ) : packages.length === 0 ? (
        <EmptyState
          title="Ningún viaje por acá"
          message={
            hasFilters
              ? 'No hay paquetes para esos filtros. Probá con otro nombre, origen o destino.'
              : 'Todavía no hay paquetes publicados.'
          }
          illustration={<CatalogEmptyScene />}
          action={
            hasFilters ? (
              <Button type="button" variant="ghost" onClick={onClearFilters}>
                Limpiar filtros
              </Button>
            ) : undefined
          }
        />
      ) : (
        <ul className="catalog__grid">
          {packages.map((item) => (
            <li key={item.id}>
              <Link className="catalog__card" to={`/trips/${item.id}`}>
                <HotelPhoto
                  src={item.hotel.photoUrl}
                  alt={item.hotel.name}
                  cityCode={item.destination.code}
                />
                <div className="catalog__card-body">
                  <p className="catalog__route">
                    {item.origin.name} → {item.destination.name}
                  </p>
                  <h2>{item.name}</h2>
                  <p className="catalog__muted">{item.hotel.name}</p>
                  <p className="catalog__muted">{item.agency.name}</p>
                  <p className="catalog__price">USD {item.price}</p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
