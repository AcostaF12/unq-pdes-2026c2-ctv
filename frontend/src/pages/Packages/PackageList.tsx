import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listAgencies } from '../../api/agencies'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { searchPackages } from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HeroScene } from '../../components/HeroScene/HeroScene'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { PackageCard } from '../../components/PackageCard/PackageCard'
import { Pagination } from '../../components/Pagination/Pagination'
import { Select } from '../../components/Select/Select'
import { useAuth } from '../../auth/AuthContext'
import { useFavorites } from '../../hooks/useFavorites'
import { readPreferences } from '../../preferences/preferences'
import { queryKeys } from '../../query/keys'
import { PackageSearchForm } from './search/PackageSearchForm'
import { buildPackageFilterChips } from './search/packageSearchChips'
import { hasPackageSearchFilters } from './search/packageSearchFilters'
import {
  PACKAGE_SORT_OPTIONS,
  parseSortOptionValue,
  sortToOptionValue,
} from './search/packageSearchSort'
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
  const { user, isBuyer } = useAuth()
  const favorites = useFavorites({ enabled: isBuyer })
  const savedOrigin = originFromPreferences(user?.id)
  const search = usePackageSearch(savedOrigin)

  const citiesQuery = useQuery({
    queryKey: queryKeys.cities,
    queryFn: listCities,
  })

  const agenciesQuery = useQuery({
    queryKey: queryKeys.agencies,
    queryFn: listAgencies,
  })

  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.search(search.query),
    queryFn: () => searchPackages(search.query),
    placeholderData: keepPreviousData,
  })

  const cities = citiesQuery.data ?? []
  const agencies = agenciesQuery.data ?? []
  const packages = packagesQuery.data?.content ?? []
  const totalPages = packagesQuery.data?.totalPages ?? 0
  const totalElements = packagesQuery.data?.totalElements ?? 0
  const hasFilters = hasPackageSearchFilters(search.filters)
  const chips = buildPackageFilterChips(search.appliedValues, cities, agencies)

  return (
    <section className="page">
      <HeroScene
        eyebrow="Vuelo + hotel"
        title="Paquetes"
        subtitle="Elegí tu próximo escape y comprá en un solo paso."
      />
      <PackageSearchForm
        cities={cities}
        agencies={agencies}
        values={search.formValues}
        appliedValues={search.appliedValues}
        error={search.error}
        onChange={search.updateField}
        onQuickDestination={(code) => search.applyPatch({ destination: code })}
        onSubmit={search.submit}
      />
      <div className="catalog__toolbar">
        <div className="catalog__summary">
          <p className="catalog__count" aria-live="polite">
            {packagesQuery.data ? (
              <>
                <strong>{totalElements}</strong>{' '}
                {totalElements === 1 ? 'paquete encontrado' : 'paquetes encontrados'}
              </>
            ) : (
              'Buscando paquetes…'
            )}
          </p>
          {chips.length > 0 ? (
            <div className="catalog__chips" aria-label="Filtros aplicados">
              {chips.map((chip) => (
                <span key={chip.key} className="chip chip--removable">
                  {chip.label}
                  <button
                    type="button"
                    className="chip__remove"
                    aria-label={`Quitar filtro: ${chip.label}`}
                    onClick={() => search.applyPatch(chip.clear)}
                  >
                    ×
                  </button>
                </span>
              ))}
              <button type="button" className="catalog__clear" onClick={search.clear}>
                Limpiar todo
              </button>
            </div>
          ) : null}
        </div>
        <div className="catalog__sort">
          <Select
            label="Ordenar por"
            value={sortToOptionValue(search.sort)}
            onChange={(event) => search.setSort(parseSortOptionValue(event.target.value))}
          >
            {PACKAGE_SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </div>
      </div>
      {packagesQuery.isPending ? (
        <ListSkeleton variant="cards" label="Cargando paquetes" />
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
        <ul
          className={
            packagesQuery.isPlaceholderData ? 'catalog__grid catalog__grid--stale' : 'catalog__grid'
          }
        >
          {packages.map((item, index) => (
            <li key={item.id}>
              <PackageCard
                travelPackage={item}
                index={index}
                favorite={
                  isBuyer
                    ? {
                        active: favorites.isFavorite(item.id),
                        pending: !favorites.ready || favorites.isToggling(item.id),
                        onToggle: () => favorites.toggle(item),
                      }
                    : undefined
                }
              />
            </li>
          ))}
        </ul>
      )}
      <Pagination page={search.page} totalPages={totalPages} onPageChange={search.setPage} />
    </section>
  )
}
