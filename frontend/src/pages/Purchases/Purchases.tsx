import { useState, type FormEvent } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { getMyPurchases, type MyPurchasesFilters } from '../../api/purchases'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HeroScene } from '../../components/HeroScene/HeroScene'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { Select } from '../../components/Select/Select'
import { queryKeys } from '../../query/keys'
import { HistoryPagination } from '../../components/HistoryPagination/HistoryPagination'
import { PurchaseEmptyScene, PurchaseTicket } from './PurchaseTicket'
import './Purchases.css'

interface PurchaseFilterDraft {
  packageName: string
  destination: string
  from: string
  to: string
  minPrice: string
  maxPrice: string
}

const EMPTY_DRAFT: PurchaseFilterDraft = {
  packageName: '',
  destination: '',
  from: '',
  to: '',
  minPrice: '',
  maxPrice: '',
}

const SORT_OPTIONS = [
  { value: 'date,desc', label: 'Más recientes primero' },
  { value: 'date,asc', label: 'Más antiguas primero' },
  { value: 'price,desc', label: 'Precio: mayor a menor' },
  { value: 'price,asc', label: 'Precio: menor a mayor' },
]

const DEFAULT_SORT = 'date,desc'
const PAGE_SIZE = 20

function toQueryFilters(draft: PurchaseFilterDraft, sort: string, page: number): MyPurchasesFilters {
  const packageName = draft.packageName.trim()
  const minPrice = draft.minPrice.trim() === '' ? undefined : Number(draft.minPrice)
  const maxPrice = draft.maxPrice.trim() === '' ? undefined : Number(draft.maxPrice)
  return {
    page,
    size: PAGE_SIZE,
    ...(packageName ? { packageName } : {}),
    ...(draft.destination ? { destination: draft.destination } : {}),
    ...(draft.from ? { from: draft.from } : {}),
    ...(draft.to ? { to: draft.to } : {}),
    ...(minPrice != null && Number.isFinite(minPrice) ? { minPrice } : {}),
    ...(maxPrice != null && Number.isFinite(maxPrice) ? { maxPrice } : {}),
    ...(sort !== DEFAULT_SORT ? { sort } : {}),
  }
}

function priceRangeError(draft: PurchaseFilterDraft): string {
  const min = draft.minPrice.trim() === '' ? null : Number(draft.minPrice)
  const max = draft.maxPrice.trim() === '' ? null : Number(draft.maxPrice)
  if ((min != null && !(min >= 0)) || (max != null && !(max >= 0))) {
    return 'Ingresá montos válidos.'
  }
  if (min != null && max != null && min > max) {
    return 'El precio mínimo no puede superar al máximo.'
  }
  return ''
}

export function Purchases() {
  const [page, setPage] = useState(0)
  const [draft, setDraft] = useState<PurchaseFilterDraft>(EMPTY_DRAFT)
  const [applied, setApplied] = useState<PurchaseFilterDraft>(EMPTY_DRAFT)
  const [sort, setSort] = useState(DEFAULT_SORT)
  const [formError, setFormError] = useState('')

  const filters = toQueryFilters(applied, sort, page)
  const hasFilters = Object.keys(filters).some((key) => !['page', 'size', 'sort'].includes(key))

  const citiesQuery = useQuery({ queryKey: queryKeys.cities, queryFn: listCities })
  const purchasesQuery = useQuery({
    queryKey: queryKeys.purchases.minePage(filters),
    queryFn: () => getMyPurchases(filters),
    placeholderData: keepPreviousData,
  })

  const purchases = purchasesQuery.data?.content ?? []
  const cities = citiesQuery.data ?? []

  const update = (field: keyof PurchaseFilterDraft, value: string) => {
    setDraft((current) => ({ ...current, [field]: value }))
    if (formError) {
      setFormError('')
    }
  }

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const error = priceRangeError(draft)
    if (error) {
      setFormError(error)
      return
    }
    setPage(0)
    setApplied(draft)
  }

  const clearFilters = () => {
    setDraft(EMPTY_DRAFT)
    setApplied(EMPTY_DRAFT)
    setFormError('')
    setPage(0)
  }

  const changeSort = (value: string) => {
    setSort(value)
    setPage(0)
  }

  return (
    <section className="page history-page">
      <HeroScene
        compact
        eyebrow="Tu historial"
        title="Mis compras"
        subtitle="Todos tus viajes y comprobantes en un solo lugar."
      />
      <form className="history-filters" aria-label="Filtros de compras" onSubmit={applyFilters}>
        <header className="history-filters__header">
          <h2 className="history-filters__title">Filtros</h2>
          <p className="history-filters__hint">Buscá por paquete, destino, fechas o precio.</p>
        </header>
        <div className="history-filters__fields">
          <div className="history-filters__group">
            <Input
              label="Paquete"
              value={draft.packageName}
              onChange={(event) => update('packageName', event.target.value)}
            />
            <Select
              label="Destino"
              value={draft.destination}
              onChange={(event) => update('destination', event.target.value)}
            >
              <option value="">Todos</option>
              {cities.map((city) => (
                <option key={city.code} value={city.code}>
                  {city.name}
                </option>
              ))}
            </Select>
          </div>
          <div className="history-filters__group">
            <Input
              label="Desde"
              type="date"
              max={draft.to || undefined}
              value={draft.from}
              onChange={(event) => update('from', event.target.value)}
            />
            <Input
              label="Hasta"
              type="date"
              min={draft.from || undefined}
              value={draft.to}
              onChange={(event) => update('to', event.target.value)}
            />
          </div>
          <div className="history-filters__group">
            <Input
              label="Precio mínimo"
              type="number"
              min="0"
              inputMode="decimal"
              value={draft.minPrice}
              onChange={(event) => update('minPrice', event.target.value)}
            />
            <Input
              label="Precio máximo"
              type="number"
              min="0"
              inputMode="decimal"
              value={draft.maxPrice}
              onChange={(event) => update('maxPrice', event.target.value)}
              error={formError}
            />
          </div>
        </div>
        <div className="history-filters__actions">
          <Button type="submit">Filtrar compras</Button>
          {hasFilters ? (
            <Button type="button" variant="ghost" onClick={clearFilters}>
              Limpiar
            </Button>
          ) : null}
        </div>
      </form>
      <div className="history-toolbar">
        <p className="catalog__count" aria-live="polite">
          {purchasesQuery.data ? (
            <>
              <strong>{purchasesQuery.data.totalElements}</strong>{' '}
              {purchasesQuery.data.totalElements === 1 ? 'compra' : 'compras'}
            </>
          ) : null}
        </p>
        <Select
          label="Ordenar compras"
          value={sort}
          onChange={(event) => changeSort(event.target.value)}
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </Select>
      </div>
      {purchasesQuery.isPending ? (
        <ListSkeleton variant="rows" label="Cargando compras" />
      ) : purchasesQuery.error ? (
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={
            getApiErrorMessage(purchasesQuery.error) ??
            'No pudimos cargar tus compras. Intentá de nuevo.'
          }
          action={
            <Button type="button" onClick={() => void purchasesQuery.refetch()}>
              Reintentar
            </Button>
          }
        />
      ) : purchases.length === 0 && hasFilters ? (
        <EmptyState
          title="Sin resultados"
          message="Ninguna compra coincide con esos filtros."
          illustration={<PurchaseEmptyScene />}
          action={
            <Button type="button" variant="ghost" onClick={clearFilters}>
              Limpiar filtros
            </Button>
          }
        />
      ) : purchases.length === 0 ? (
        <EmptyState
          title="Todavía no hay viajes comprados"
          message="Cuando compres un paquete, el pasaje aparece acá."
          illustration={<PurchaseEmptyScene />}
          action={
            <Link className="empty-state__cta" to="/trips">
              Ver paquetes
            </Link>
          }
        />
      ) : (
        <ul
          className={
            purchasesQuery.isPlaceholderData ? 'purchase-list purchase-list--stale' : 'purchase-list'
          }
        >
          {purchases.map((purchase, index) => (
            <li key={purchase.id}>
              <PurchaseTicket purchase={purchase} index={index} />
            </li>
          ))}
        </ul>
      )}
      {purchasesQuery.data ? <HistoryPagination result={purchasesQuery.data} onPageChange={setPage} /> : null}
    </section>
  )
}
