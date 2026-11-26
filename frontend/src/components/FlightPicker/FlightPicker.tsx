import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { listFlights } from '../../api/flights'
import type { Flight } from '../../api/packages'
import { queryKeys } from '../../query/keys'
import { Input } from '../Input/Input'
import { Select } from '../Select/Select'
import { describeFlight, describeSeats, outboundOptions, returnOptions } from './flightOptions'
import './FlightPicker.css'

const flightDateFormat = new Intl.DateTimeFormat('es-AR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
})

function formatFlightDate(isoDate: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  if (!match) {
    return isoDate
  }
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  return flightDateFormat.format(new Date(Date.UTC(year, month - 1, day)))
}

function flightsByDate(flights: Flight[]): { date: string; flights: Flight[] }[] {
  const groups: { date: string; flights: Flight[] }[] = []
  for (const flight of flights) {
    const current = groups[groups.length - 1]
    if (current && current.date === flight.flightDate) {
      current.flights.push(flight)
    } else {
      groups.push({ date: flight.flightDate, flights: [flight] })
    }
  }
  return groups
}

export interface FlightSelection {
  outboundFlightId: string
  returnFlightId: string
}

interface FlightPickerProps extends FlightSelection {
  destinationCode: string | null
  onChange: (selection: FlightSelection) => void
  outboundError?: string
  returnError?: string
}

interface FlightChoicesProps {
  label: string
  testId: string
  flights: Flight[]
  value: string
  disabled: boolean
  placeholder: string
  error?: string
  onSelect: (flightId: string) => void
}

function FlightChoices({
  label,
  testId,
  flights,
  value,
  disabled,
  placeholder,
  error,
  onSelect,
}: FlightChoicesProps) {
  return (
    <div
      className="flight-picker__group"
      role="radiogroup"
      aria-label={label}
      aria-disabled={disabled || undefined}
      aria-invalid={Boolean(error) || undefined}
      data-testid={testId}
    >
      <p className="flight-picker__group-title">{label}</p>
      {disabled || flights.length === 0 ? <p className="flight-picker__placeholder">{placeholder}</p> : (
        flightsByDate(flights).map((group) => (
          <div key={group.date} className="flight-picker__day">
            <p className="flight-picker__day-label">{formatFlightDate(group.date)}</p>
            {group.flights.map((flight) => (
              <label
                key={flight.id}
                className={value === String(flight.id) ? 'choice-card choice-card--selected' : 'choice-card'}
                title={describeFlight(flight)}
              >
                <input
                  type="radio"
                  name={testId}
                  value={flight.id}
                  aria-label={describeFlight(flight)}
                  checked={value === String(flight.id)}
                  onChange={() => onSelect(String(flight.id))}
                />
                <span className="choice-card__title">{flight.airline}</span>
                <span className="choice-card__meta">
                  {flight.origin} → {flight.destination}
                </span>
                <span className="choice-card__meta">
                  {formatFlightDate(flight.flightDate)} · {flight.departureTime.slice(0, 5)}
                </span>
                <span className="choice-card__meta">{describeSeats(flight.availability)}</span>
              </label>
            ))}
          </div>
        ))
      )}
      {error ? (
        <p className="flight-picker__error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  )
}

export function FlightPicker({
  destinationCode,
  outboundFlightId,
  returnFlightId,
  onChange,
  outboundError,
  returnError,
}: FlightPickerProps) {
  const [origin, setOrigin] = useState('')
  const [outboundDate, setOutboundDate] = useState('')
  const [returnDate, setReturnDate] = useState('')
  const citiesQuery = useQuery({ queryKey: queryKeys.cities, queryFn: listCities })
  const outboundParams = {
    destination: destinationCode ?? '',
    ...(origin ? { origin } : {}),
    ...(outboundDate ? { date: outboundDate } : {}),
  }
  const outboundQuery = useQuery({
    queryKey: queryKeys.flights.search(outboundParams),
    queryFn: () => listFlights(outboundParams),
    enabled: destinationCode !== null,
  })

  const outbound = useMemo(
    () => outboundOptions(outboundQuery.data ?? [], destinationCode),
    [outboundQuery.data, destinationCode],
  )
  const selectedOutbound = outbound.find((flight) => String(flight.id) === outboundFlightId)

  const returnParams = selectedOutbound
    ? {
        origin: selectedOutbound.destination,
        destination: selectedOutbound.origin,
        ...(returnDate ? { date: returnDate } : {}),
      }
    : null
  const returnQuery = useQuery({
    queryKey: queryKeys.flights.search(returnParams ?? {}),
    queryFn: () => listFlights(returnParams ?? {}),
    enabled: returnParams !== null,
  })
  const inbound = useMemo(
    () => returnOptions(returnQuery.data ?? [], selectedOutbound),
    [returnQuery.data, selectedOutbound],
  )

  useEffect(() => {
    if (!selectedOutbound || !returnQuery.isSuccess || returnQuery.isFetching) {
      return
    }
    const available = returnOptions(returnQuery.data ?? [], selectedOutbound)
    const stillValid = available.some((flight) => String(flight.id) === returnFlightId)
    if (returnFlightId && stillValid) {
      return
    }
    const nextReturn = available.length === 1 ? String(available[0].id) : ''
    if (nextReturn === returnFlightId) {
      return
    }
    onChange({ outboundFlightId, returnFlightId: nextReturn })
  }, [
    selectedOutbound,
    returnQuery.isSuccess,
    returnQuery.isFetching,
    returnQuery.data,
    returnFlightId,
    outboundFlightId,
    onChange,
  ])

  const loadError = outboundQuery.error ?? returnQuery.error ?? citiesQuery.error

  let outboundPlaceholder = 'Elegí un vuelo de ida'
  if (destinationCode === null) {
    outboundPlaceholder = 'Elegí un hotel primero'
  } else if (outboundQuery.isPending) {
    outboundPlaceholder = 'Cargando vuelos…'
  } else if (outbound.length === 0) {
    outboundPlaceholder = `No hay vuelos hacia ${destinationCode}`
  }

  let returnPlaceholder = 'Elegí un vuelo de vuelta'
  if (!selectedOutbound) {
    returnPlaceholder = 'Elegí primero el vuelo de ida'
  } else if (returnQuery.isPending) {
    returnPlaceholder = 'Cargando vuelos…'
  } else if (inbound.length === 0) {
    returnPlaceholder = 'No hay vuelos de regreso para esa ruta'
  }

  return (
    <div className="flight-picker" data-testid="flight-picker">
      <div className="flight-picker__filters" role="group" aria-label="Filtrar vuelos">
        <p className="flight-picker__hint">Filtros opcionales. El destino lo determina el hotel elegido.</p>
        <Select
          label="Origen de ida"
          data-testid="package-flight-origin"
          value={origin}
          disabled={citiesQuery.isPending || citiesQuery.isError}
          onChange={(event) => {
            setOrigin(event.target.value)
            onChange({ outboundFlightId: '', returnFlightId: '' })
          }}
        >
          <option value="">{citiesQuery.isPending ? 'Cargando ciudades…' : 'Todos los orígenes'}</option>
          {(citiesQuery.data ?? []).map((city) => (
            <option key={city.code} value={city.code}>{city.name} ({city.code})</option>
          ))}
        </Select>
        <Input
          label="Fecha de ida"
          data-testid="package-outbound-date"
          type="date"
          value={outboundDate}
          onChange={(event) => {
            setOutboundDate(event.target.value)
            onChange({ outboundFlightId: '', returnFlightId: '' })
          }}
        />
        <Input
          label="Fecha de vuelta"
          data-testid="package-return-date"
          type="date"
          value={returnDate}
          onChange={(event) => {
            setReturnDate(event.target.value)
            onChange({ outboundFlightId, returnFlightId: '' })
          }}
        />
      </div>
      <FlightChoices
        label="Vuelo de ida"
        testId="package-outbound-flight"
        flights={outbound}
        value={outboundFlightId}
        disabled={destinationCode === null || outboundQuery.isPending}
        placeholder={outboundPlaceholder}
        error={outboundError}
        onSelect={(nextId) => {
          const nextFlight = outbound.find((flight) => String(flight.id) === nextId)
          const sameRoute = Boolean(
            selectedOutbound &&
              nextFlight &&
              nextFlight.origin === selectedOutbound.origin &&
              nextFlight.destination === selectedOutbound.destination,
          )
          onChange({
            outboundFlightId: nextId,
            returnFlightId: sameRoute ? returnFlightId : '',
          })
        }}
      />
      <FlightChoices
        label="Vuelo de vuelta"
        testId="package-return-flight"
        flights={inbound}
        value={returnFlightId}
        disabled={!selectedOutbound || returnQuery.isPending}
        placeholder={returnPlaceholder}
        error={returnError}
        onSelect={(nextId) => onChange({ outboundFlightId, returnFlightId: nextId })}
      />
      {loadError ? (
        <p className="flight-picker__error" role="alert">
          {getApiErrorMessage(loadError) ?? 'No pudimos cargar los vuelos. Intentá de nuevo.'}
        </p>
      ) : null}
    </div>
  )
}
