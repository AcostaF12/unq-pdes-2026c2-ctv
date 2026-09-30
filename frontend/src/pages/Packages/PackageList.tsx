import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { searchPackages } from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HotelPhoto } from '../../components/HotelPhoto/HotelPhoto'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { Pagination } from '../../components/Pagination/Pagination'
import { useAuth } from '../../auth/AuthContext'
import { readPreferences } from '../../preferences/preferences'
import { queryKeys } from '../../query/keys'
import { PackageSearchForm } from './search/PackageSearchForm'
import { hasPackageSearchFilters } from './search/packageSearchFilters'
import { parseSortOptionValue } from './search/packageSearchSort'
import { usePackageSearch } from './search/usePackageSearch'
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
  const search = usePackageSearch(savedOrigin)

  const citiesQuery = useQuery({
    queryKey: queryKeys.cities,
    queryFn: listCities,
  })

  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.search(search.query),
    queryFn: () => searchPackages(search.query),
  })

  const cities = citiesQuery.data ?? []
  const packages = packagesQuery.data?.content ?? []
  const totalPages = packagesQuery.data?.totalPages ?? 0
  const hasFilters = hasPackageSearchFilters(search.filters)

  return (
    <section className="page">
      <header className="catalog__intro">
        <p className="catalog__eyebrow">Elegí tu escape</p>
        <h1 className="page__title">Paquetes</h1>
      </header>
      <PackageSearchForm
        cities={cities}
        values={search.formValues}
        error={search.error}
        sort={search.sort}
        onChange={search.updateField}
        onSortChange={(value) => search.setSort(parseSortOptionValue(value))}
        onSubmit={search.submit}
      />
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
              ? 'No hay paquetes para esos filtros. Probá con otro nombre, origen, destino o rango de precio.'
              : 'Todavía no hay paquetes publicados.'
          }
          illustration={<CatalogEmptyScene />}
          action={
            hasFilters ? (
              <Button type="button" variant="ghost" onClick={search.clear}>
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
      <Pagination page={search.page} totalPages={totalPages} onPageChange={search.setPage} />
    </section>
  )
}
