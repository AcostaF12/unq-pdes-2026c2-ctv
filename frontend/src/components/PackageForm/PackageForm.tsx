import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { getApiErrorMessage } from '../../api/errors'
import { listFlights } from '../../api/flights'
import { listHotels, type Hotel } from '../../api/hotels'
import type { Agency, Flight, TravelPackage, TravelPackagePayload } from '../../api/packages'
import { queryKeys } from '../../query/keys'
import { ADMIN_FLIGHTS_PATH, ADMIN_HOTEL_NEW_PATH } from '../../router/paths'
import { Button } from '../Button/Button'
import { describeFlight } from '../FlightPicker/flightOptions'
import { FlightPicker, type FlightSelection } from '../FlightPicker/FlightPicker'
import { Input } from '../Input/Input'
import { ListSkeleton } from '../ListSkeleton/ListSkeleton'
import { Select } from '../Select/Select'
import { buildPackageSchema, toPackagePayload, type PackageFormValues } from './package.schema'
import './PackageForm.css'

interface PackageFormProps {
  title: string
  submitLabel: string
  submitting: boolean
  serverError: string | null
  onSubmit: (payload: TravelPackagePayload) => void
  agencies?: Agency[]
  initialPackage?: TravelPackage
  onCancel?: () => void
}

function hotelsByCity(hotels: Hotel[]): { label: string; hotels: Hotel[] }[] {
  const groups = new Map<string, { label: string; hotels: Hotel[] }>()
  const sorted = [...hotels].sort(
    (left, right) =>
      left.city.name.localeCompare(right.city.name, 'es') || left.name.localeCompare(right.name, 'es'),
  )
  for (const hotel of sorted) {
    const current = groups.get(hotel.city.code)
    if (current) {
      current.hotels.push(hotel)
      continue
    }
    groups.set(hotel.city.code, {
      label: `${hotel.city.name} (${hotel.city.code})`,
      hotels: [hotel],
    })
  }
  return [...groups.values()]
}

function hotelLabel(hotel: Hotel): string {
  return `${hotel.name} (${hotel.city.code})`
}

function initialValues(initialPackage: TravelPackage | undefined): PackageFormValues {
  return {
    name: initialPackage?.name ?? '',
    agencyId: initialPackage ? String(initialPackage.agency.id) : '',
    hotelId: initialPackage ? String(initialPackage.hotel.id) : '',
    outboundFlightId: initialPackage ? String(initialPackage.outboundFlightId) : '',
    returnFlightId: initialPackage ? String(initialPackage.returnFlightId) : '',
    price: initialPackage ? String(initialPackage.price) : '',
  }
}

function flightLine(flight: Flight | undefined, id: string): string {
  if (flight) {
    return describeFlight(flight)
  }
  return id ? `Vuelo ${id}` : 'Sin elegir'
}

export function PackageForm({
  title,
  submitLabel,
  submitting,
  serverError,
  onSubmit,
  agencies,
  initialPackage,
  onCancel,
}: PackageFormProps) {
  const withAgency = agencies !== undefined
  const schema = useMemo(() => buildPackageSchema(withAgency), [withAgency])

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors, isSubmitted },
  } = useForm<PackageFormValues>({
    resolver: zodResolver(schema),
    defaultValues: initialValues(initialPackage),
  })

  const hotelsQuery = useQuery({
    queryKey: queryKeys.hotels.all,
    queryFn: listHotels,
  })
  const [name, price, agencyId, hotelId, outboundFlightId, returnFlightId] = useWatch({
    control,
    name: ['name', 'price', 'agencyId', 'hotelId', 'outboundFlightId', 'returnFlightId'],
  })
  const summaryFlights = useQuery({
    queryKey: queryKeys.flights.search({}),
    queryFn: () => listFlights(),
    enabled: Boolean(hotelId),
  })

  if (hotelsQuery.isPending) {
    return <ListSkeleton variant="rows" count={3} withMedia={false} label="Cargando formulario del paquete" />
  }

  if (hotelsQuery.error) {
    return (
      <p className="package-form__error" role="alert">
        {getApiErrorMessage(hotelsQuery.error) ?? 'No pudimos cargar los hoteles. Intentá de nuevo.'}
      </p>
    )
  }

  const hotels = hotelsQuery.data
  const selectedHotel = hotels.find((hotel) => String(hotel.id) === hotelId)
  const hotelGroups = hotelsByCity(hotels)
  const agencyName = agencies?.find((agency) => String(agency.id) === agencyId)?.name
  const outboundFlight = summaryFlights.data?.find((flight) => String(flight.id) === outboundFlightId)
  const returnFlight = summaryFlights.data?.find((flight) => String(flight.id) === returnFlightId)

  const selectFlights = (selection: FlightSelection) => {
    setValue('outboundFlightId', selection.outboundFlightId, { shouldValidate: isSubmitted })
    setValue('returnFlightId', selection.returnFlightId, { shouldValidate: isSubmitted })
  }

  const chooseHotel = (nextId: string) => {
    if (nextId === hotelId) {
      return
    }
    setValue('hotelId', nextId, { shouldValidate: isSubmitted })
    selectFlights({ outboundFlightId: '', returnFlightId: '' })
  }

  const save = handleSubmit((values) => onSubmit(toPackagePayload(values, withAgency)))

  return (
    <form
      className="catalog__form package-form"
      aria-label={title}
      data-testid="package-form"
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        void save()
      }}
    >
      <header className="package-form__header">
        <h2>{title}</h2>
        <p className="package-form__hint">Elegí el hotel y aparecen los vuelos de ida y de vuelta.</p>
      </header>
      <div className="package-form__grid">
        <Input label="Nombre" error={errors.name?.message} {...register('name')} />
        <Input
          label="Precio"
          type="number"
          min={1}
          step="0.01"
          error={errors.price?.message}
          {...register('price')}
        />
        {withAgency ? (
          <Select label="Agencia" error={errors.agencyId?.message} {...register('agencyId')}>
            <option value="">Elegí una agencia</option>
            {agencies.map((agency) => (
              <option key={agency.id} value={agency.id}>
                {agency.name}
              </option>
            ))}
          </Select>
        ) : null}
        <div className="package-form__span">
          {hotels.length === 0 ? (
            <p className="package-form__hint">
              Todavía no hay hoteles cargados.
              {withAgency ? (
                <>
                  {' '}
                  <Link to={ADMIN_HOTEL_NEW_PATH}>Cargar un hotel</Link>
                </>
              ) : null}
            </p>
          ) : (
            <Select
              label="Hotel"
              error={errors.hotelId?.message}
              value={hotelId}
              data-testid="package-hotel"
              onChange={(event) => chooseHotel(event.target.value)}
            >
              <option value="">Elegí un hotel</option>
              {hotelGroups.map((group) => (
                <optgroup key={group.label} label={group.label}>
                  {group.hotels.map((hotel) => (
                    <option key={hotel.id} value={hotel.id}>
                      {hotelLabel(hotel)}
                    </option>
                  ))}
                </optgroup>
              ))}
            </Select>
          )}
        </div>
      </div>
      <p className="package-form__hint">
        {withAgency ? (
          <>
            Si falta el vuelo de ida o el de vuelta, <Link to={ADMIN_FLIGHTS_PATH}>cargalo en Vuelos</Link>.
          </>
        ) : (
          'Elegí un vuelo de ida y uno de vuelta entre los que ya existen. Al crearlo queda publicado.'
        )}
      </p>
      <FlightPicker
        destinationCode={selectedHotel?.city.code ?? null}
        outboundFlightId={outboundFlightId}
        returnFlightId={returnFlightId}
        onChange={selectFlights}
        outboundError={errors.outboundFlightId?.message}
        returnError={errors.returnFlightId?.message}
      />
      {selectedHotel ? (
        <dl className="package-summary" data-testid="package-summary">
          <div>
            <dt>Nombre</dt>
            <dd>{name || 'Sin nombre'}</dd>
          </div>
          <div>
            <dt>Precio</dt>
            <dd>{price ? `USD ${price}` : 'Sin precio'}</dd>
          </div>
          {withAgency ? (
            <div>
              <dt>Agencia</dt>
              <dd>{agencyName ?? 'Sin agencia'}</dd>
            </div>
          ) : null}
          <div>
            <dt>Hotel</dt>
            <dd>{`${selectedHotel.name} · ${selectedHotel.city.name}`}</dd>
          </div>
          <div>
            <dt>Ida</dt>
            <dd>{summaryFlights.isPending ? 'Cargando vuelo…' : flightLine(outboundFlight, outboundFlightId)}</dd>
          </div>
          <div>
            <dt>Vuelta</dt>
            <dd>{summaryFlights.isPending ? 'Cargando vuelo…' : flightLine(returnFlight, returnFlightId)}</dd>
          </div>
        </dl>
      ) : null}

      {serverError ? (
        <p className="package-form__error" role="alert">
          {serverError}
        </p>
      ) : null}

      <div className="page__actions package-form__actions">
        <Button type="submit" loading={submitting}>
          {submitLabel}
        </Button>
        {onCancel ? (
          <Button type="button" variant="ghost" disabled={submitting} onClick={onCancel}>
            Cancelar
          </Button>
        ) : null}
      </div>
    </form>
  )
}
