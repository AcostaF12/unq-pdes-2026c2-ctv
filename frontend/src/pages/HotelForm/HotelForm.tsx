import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { listCities } from '../../api/cities'
import { createHotel, getHotel, updateHotel } from '../../api/hotels'
import { getApiErrorMessage, isNotFoundError } from '../../api/errors'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { Select } from '../../components/Select/Select'
import { queryKeys } from '../../query/keys'
import { hotelSchema, type HotelFormValues } from './hotel.schema'
import './HotelForm.css'

export function HotelForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const hotelId = id ? Number(id) : null
  const isEdit = hotelId !== null
  const invalidEdit = isEdit && !Number.isFinite(hotelId)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<HotelFormValues>({
    resolver: zodResolver(hotelSchema),
    defaultValues: {
      name: '',
      cityCode: '',
      photoUrl: '',
    },
  })

  const citiesQuery = useQuery({
    queryKey: queryKeys.cities,
    queryFn: listCities,
    enabled: !invalidEdit,
  })

  const hotelQuery = useQuery({
    queryKey: queryKeys.hotels.detail(hotelId ?? 0),
    queryFn: () => getHotel(hotelId as number),
    enabled: isEdit && !invalidEdit && hotelId !== null,
  })

  useEffect(() => {
    if (!hotelQuery.data) {
      return
    }
    reset({
      name: hotelQuery.data.name,
      cityCode: hotelQuery.data.city.code,
      photoUrl: hotelQuery.data.photoUrl,
    })
  }, [hotelQuery.data, reset])

  const saveMutation = useMutation({
    mutationFn: async (values: HotelFormValues) => {
      if (isEdit && hotelId !== null) {
        return updateHotel(hotelId, values)
      }
      return createHotel(values)
    },
    onSuccess: async (hotel) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.hotels.all })
      await queryClient.invalidateQueries({ queryKey: queryKeys.hotels.detail(hotel.id) })
      navigate(`/hotels/${hotel.id}`)
    },
    onError: (reason: unknown) => {
      if (axios.isAxiosError(reason) && reason.response?.status === 403) {
        setServerError('Esta acción es solo para administradores.')
        return
      }
      setServerError(getApiErrorMessage(reason) ?? 'No pudimos guardar el hotel. Intentá de nuevo.')
    },
  })

  const onSubmit = (values: HotelFormValues) => {
    setServerError(null)
    saveMutation.mutate(values)
  }

  const loading = citiesQuery.isPending || (isEdit && hotelQuery.isPending)
  const notFound = invalidEdit || isNotFoundError(hotelQuery.error)
  const loadError = citiesQuery.error ?? (hotelQuery.error && !notFound ? hotelQuery.error : null)

  if (loading) {
    return (
      <section className="page">
        <ListSkeleton variant="rows" count={4} label="Cargando formulario" />
      </section>
    )
  }

  if (notFound) {
    return (
      <EmptyState
        title="No encontramos este hotel"
        message="No se puede editar un hotel que no existe."
        action={
          <Link className="hotel-form__back" to="/hotels">
            Volver al catálogo
          </Link>
        }
      />
    )
  }

  if (loadError) {
    return (
      <EmptyState
        tone="error"
        title="Algo salió mal"
        message={getApiErrorMessage(loadError) ?? 'No pudimos cargar el formulario. Intentá de nuevo.'}
      />
    )
  }

  return (
    <section className="page">
      <Link className="hotel-form__back" to={isEdit && hotelId ? `/hotels/${hotelId}` : '/hotels'}>
        ← Volver
      </Link>
      <h1 className="page__title">{isEdit ? 'Editar hotel' : 'Nuevo hotel'}</h1>

      <form className="hotel-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Input label="Nombre" error={errors.name?.message} {...register('name')} />
        <Select label="Ciudad" error={errors.cityCode?.message} {...register('cityCode')}>
          <option value="">Elegí una ciudad</option>
          {(citiesQuery.data ?? []).map((city) => (
            <option key={city.code} value={city.code}>
              {city.name} ({city.code})
            </option>
          ))}
        </Select>
        <Input label="URL de la foto" error={errors.photoUrl?.message} {...register('photoUrl')} />

        {serverError ? (
          <p className="hotel-form__error" role="alert">
            {serverError}
          </p>
        ) : null}

        <div className="page__actions">
          <Button type="submit" loading={isSubmitting || saveMutation.isPending}>
            {isEdit ? 'Guardar' : 'Crear hotel'}
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              navigate(isEdit && hotelId ? `/hotels/${hotelId}` : '/hotels')
            }}
          >
            Cancelar
          </Button>
        </div>
      </form>
    </section>
  )
}
