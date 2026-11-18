import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { getApiErrorMessage } from '../../api/errors'
import { listHotels } from '../../api/hotels'
import type { Agency, TravelPackage, TravelPackagePayload } from '../../api/packages'
import { queryKeys } from '../../query/keys'
import { Button } from '../Button/Button'
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

  const [hotelId, outboundFlightId, returnFlightId] = useWatch({
    control,
    name: ['hotelId', 'outboundFlightId', 'returnFlightId'],
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
  const hotelField = register('hotelId')

  const selectFlights = (selection: FlightSelection) => {
    setValue('outboundFlightId', selection.outboundFlightId, { shouldValidate: isSubmitted })
    setValue('returnFlightId', selection.returnFlightId, { shouldValidate: isSubmitted })
  }

  return (
    <form
      className="catalog__form package-form"
      aria-label={title}
      data-testid="package-form"
      noValidate
      onSubmit={handleSubmit((values) => onSubmit(toPackagePayload(values, withAgency)))}
    >
      <h2>{title}</h2>
      <Input label="Nombre" error={errors.name?.message} {...register('name')} />
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
      <Select
        label="Hotel"
        error={errors.hotelId?.message}
        {...hotelField}
        onChange={(event) => {
          void hotelField.onChange(event)
          selectFlights({ outboundFlightId: '', returnFlightId: '' })
        }}
      >
        <option value="">Elegí un hotel</option>
        {hotels.map((hotel) => (
          <option key={hotel.id} value={hotel.id}>
            {hotel.name} ({hotel.city.code})
          </option>
        ))}
      </Select>
      <FlightPicker
        destinationCode={selectedHotel?.city.code ?? null}
        outboundFlightId={outboundFlightId}
        returnFlightId={returnFlightId}
        onChange={selectFlights}
        outboundError={errors.outboundFlightId?.message}
        returnError={errors.returnFlightId?.message}
      />
      <Input
        label="Precio"
        type="number"
        min={1}
        step="0.01"
        error={errors.price?.message}
        {...register('price')}
      />

      {serverError ? (
        <p className="package-form__error" role="alert">
          {serverError}
        </p>
      ) : null}

      <div className="page__actions">
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
