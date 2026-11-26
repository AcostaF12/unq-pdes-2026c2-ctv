import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { listCities, type City } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { createFlight, listFlights, type FlightSearchParams } from '../../api/flights'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { Select } from '../../components/Select/Select'
import { queryKeys } from '../../query/keys'
import { ADMIN_PATH } from '../../router/paths'
import { AdminFlow } from './AdminFlow'
import { flightSchema, toFlightPayload, type FlightFormValues } from './flight.schema'
import '../Packages/packages.css'
import '../../components/PackageForm/PackageForm.css'
import './admin.css'
import './AdminFlights.css'

const EMPTY_FORM: FlightFormValues = {
  airline: '',
  origin: '',
  destination: '',
  flightDate: '',
  departureTime: '',
  capacity: '',
  availability: '',
}

const EMPTY_FILTERS = { origin: '', destination: '', date: '' }

function flightCount(filtered: boolean, total: number): string {
  return filtered ? `${total} con estos filtros` : `${total} en el listado`
}

function cityLabel(cities: City[], code: string): string {
  const city = cities.find((candidate) => candidate.code === code)
  return city ? `${city.name} (${code})` : code
}

function CityOptions({ cities }: { cities: City[] }) {
  return cities.map((city) => (
    <option key={city.code} value={city.code}>
      {city.name} ({city.code})
    </option>
  ))
}

export function AdminFlights() {
  const queryClient = useQueryClient()
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [notice, setNotice] = useState<string | null>(null)

  const citiesQuery = useQuery({ queryKey: queryKeys.cities, queryFn: listCities })

  const params = useMemo<FlightSearchParams>(
    () => ({
      ...(filters.origin ? { origin: filters.origin } : {}),
      ...(filters.destination ? { destination: filters.destination } : {}),
      ...(filters.date ? { date: filters.date } : {}),
    }),
    [filters],
  )
  const flightsQuery = useQuery({
    queryKey: queryKeys.flights.search(params),
    queryFn: () => listFlights(params),
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FlightFormValues>({
    resolver: zodResolver(flightSchema),
    defaultValues: EMPTY_FORM,
  })

  const createMutation = useMutation({
    mutationFn: createFlight,
    onSuccess: async () => {
      reset(EMPTY_FORM)
      setNotice('Vuelo creado.')
      await queryClient.invalidateQueries({ queryKey: queryKeys.flights.all })
    },
  })

  const cities = citiesQuery.data ?? []
  const flights = useMemo(
    () =>
      [...(flightsQuery.data ?? [])].sort((a, b) =>
        `${a.flightDate} ${a.departureTime}`.localeCompare(`${b.flightDate} ${b.departureTime}`),
      ),
    [flightsQuery.data],
  )
  const hasFilters = Object.keys(params).length > 0
  const createError = createMutation.isError
    ? (getApiErrorMessage(createMutation.error) ?? 'No pudimos crear el vuelo. Intentá de nuevo.')
    : null

  const setFilter = (name: keyof typeof EMPTY_FILTERS, value: string) => {
    setFilters((current) => ({ ...current, [name]: value }))
  }

  let list
  if (flightsQuery.error) {
    list = (
      <EmptyState
        tone="error"
        title="Algo salió mal"
        message={getApiErrorMessage(flightsQuery.error) ?? 'No pudimos cargar los vuelos. Intentá de nuevo.'}
        action={
          <Button type="button" onClick={() => void flightsQuery.refetch()}>
            Reintentar
          </Button>
        }
      />
    )
  } else if (flightsQuery.isPending) {
    list = <ListSkeleton variant="rows" count={4} label="Cargando vuelos" />
  } else if (flights.length === 0) {
    list = (
      <EmptyState
        title="Sin vuelos"
        message={hasFilters ? 'No hay vuelos para esos filtros.' : 'Todavía no hay vuelos cargados.'}
      />
    )
  } else {
    list = (
      <>
        <p className="flight-index__count">{flightCount(hasFilters, flights.length)}</p>
        <ul className="flight-index__list" aria-label="Vuelos cargados">
          {flights.map((flight) => (
            <li key={flight.id} className="flight-index__row" data-testid="flight-item">
              <span className="flight-index__mark" aria-hidden="true">
                {flight.airline.trim().charAt(0).toUpperCase() || '·'}
              </span>
              <div className="flight-index__main">
                <h3>{flight.airline}</h3>
                <p>
                  {cityLabel(cities, flight.origin)} → {cityLabel(cities, flight.destination)}
                </p>
                <p className="flight-index__when">
                  {flight.flightDate} · {flight.departureTime}
                </p>
              </div>
              <p className="flight-index__seats">
                {flight.availability} / {flight.capacity} lugares
              </p>
            </li>
          ))}
        </ul>
      </>
    )
  }

  return (
    <section className="page flight-index">
      <Link className="catalog__back" to={ADMIN_PATH}>
        ← Administración
      </Link>
      <AdminFlow current="flights" />
      <header className="flight-index__intro">
        <p className="flight-index__eyebrow">Catálogo</p>
        <h1 className="page__title">Vuelos</h1>
        <p className="flight-index__lead">Cargá la ida y la vuelta. Después se eligen en el paquete.</p>
      </header>

      <form
        className="catalog__form package-form"
        aria-label="Nuevo vuelo"
        data-testid="flight-form"
        noValidate
        onSubmit={handleSubmit((values) => {
          setNotice(null)
          createMutation.mutate(toFlightPayload(values))
        })}
      >
        <h2>Nuevo vuelo</h2>
        <div className="admin-form__grid">
          <Input label="Aerolínea" error={errors.airline?.message} {...register('airline')} />
          <Select label="Origen" error={errors.origin?.message} {...register('origin')}>
            <option value="">Elegí una ciudad</option>
            <CityOptions cities={cities} />
          </Select>
          <Select label="Destino" error={errors.destination?.message} {...register('destination')}>
            <option value="">Elegí una ciudad</option>
            <CityOptions cities={cities} />
          </Select>
          <Input label="Fecha" type="date" error={errors.flightDate?.message} {...register('flightDate')} />
          <Input
            label="Hora de salida"
            type="time"
            error={errors.departureTime?.message}
            {...register('departureTime')}
          />
          <Input
            label="Capacidad"
            type="number"
            min={1}
            error={errors.capacity?.message}
            {...register('capacity')}
          />
          <Input
            label="Lugares disponibles"
            type="number"
            min={0}
            error={errors.availability?.message}
            {...register('availability')}
          />
        </div>
        {createError ? (
          <p className="package-form__error" role="alert">
            {createError}
          </p>
        ) : null}
        {notice ? (
          <p className="admin-notice" role="status">
            {notice}
          </p>
        ) : null}
        <div className="page__actions">
          <Button type="submit" loading={createMutation.isPending}>
            Crear vuelo
          </Button>
        </div>
      </form>

      <div className="admin-filters" role="search" aria-label="Filtrar vuelos">
        <Select
          label="Filtrar por origen"
          value={filters.origin}
          onChange={(event) => setFilter('origin', event.target.value)}
        >
          <option value="">Todos</option>
          <CityOptions cities={cities} />
        </Select>
        <Select
          label="Filtrar por destino"
          value={filters.destination}
          onChange={(event) => setFilter('destination', event.target.value)}
        >
          <option value="">Todos</option>
          <CityOptions cities={cities} />
        </Select>
        <Input
          label="Filtrar por fecha"
          type="date"
          value={filters.date}
          onChange={(event) => setFilter('date', event.target.value)}
        />
        <Button
          type="button"
          variant="ghost"
          disabled={!hasFilters}
          onClick={() => setFilters(EMPTY_FILTERS)}
        >
          Limpiar filtros
        </Button>
      </div>

      {list}
    </section>
  )
}
