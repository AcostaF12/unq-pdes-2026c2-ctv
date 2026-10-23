import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deleteHotel, getHotel } from '../../api/hotels'
import { getApiErrorMessage, isNotFoundError } from '../../api/errors'
import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HotelPhoto } from '../../components/HotelPhoto/HotelPhoto'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { RelatedPackages } from '../../components/RelatedPackages/RelatedPackages'
import { queryKeys } from '../../query/keys'
import '../Packages/packages.css'
import './HotelDetail.css'

export function HotelDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { isAdmin } = useAuth()
  const hotelId = Number(id)
  const invalidId = !Number.isFinite(hotelId)
  const [actionError, setActionError] = useState<string | null>(null)

  const hotelQuery = useQuery({
    queryKey: queryKeys.hotels.detail(hotelId),
    queryFn: () => getHotel(hotelId),
    enabled: !invalidId,
  })

  const deleteMutation = useMutation({
    mutationFn: deleteHotel,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.hotels.all })
      navigate('/hotels')
    },
    onError: (reason: unknown) => {
      setActionError(getApiErrorMessage(reason) ?? 'No pudimos eliminar el hotel.')
    },
  })

  const hotel = hotelQuery.data
  const notFound = invalidId || isNotFoundError(hotelQuery.error)
  const loadError = hotelQuery.error && !notFound ? hotelQuery.error : null

  const handleDelete = () => {
    if (!hotel) {
      return
    }
    const confirmed = window.confirm(`¿Eliminar ${hotel.name}?`)
    if (!confirmed) {
      return
    }

    setActionError(null)
    deleteMutation.mutate(hotel.id)
  }

  if (hotelQuery.isPending && !invalidId) {
    return (
      <section className="page">
        <ListSkeleton variant="detail" label="Cargando hotel" />
      </section>
    )
  }

  if (notFound) {
    return (
      <EmptyState
        title="No encontramos este hotel"
        message="Puede que lo hayan eliminado o que el enlace esté mal."
        action={
          <Link className="hotel-detail__back" to="/hotels">
            Volver al catálogo
          </Link>
        }
      />
    )
  }

  if (loadError && !hotel) {
    return (
      <EmptyState
        tone="error"
        title="Algo salió mal"
        message={getApiErrorMessage(loadError) ?? 'No pudimos cargar este hotel. Intentá de nuevo.'}
        action={
          <Button type="button" onClick={() => void hotelQuery.refetch()}>
            Reintentar
          </Button>
        }
      />
    )
  }

  if (!hotel) {
    return null
  }

  return (
    <article className="page hotel-detail">
      <Link className="hotel-detail__back reveal" to="/hotels" style={{ ['--i' as string]: 0 }}>
        ← Hoteles
      </Link>
      <header className="hotel-detail__hero reveal" style={{ ['--i' as string]: 1 }}>
        <HotelPhoto
          src={hotel.photoUrl}
          alt=""
          cityCode={hotel.city.code}
          size="large"
        />
        {isAdmin ? (
          <div className="hotel-detail__actions">
            <Link className="hotel-detail__edit" to={`/hotels/${hotel.id}/edit`}>
              Editar
            </Link>
            <Button
              type="button"
              variant="ghost"
              onClick={handleDelete}
              loading={deleteMutation.isPending}
            >
              Eliminar
            </Button>
          </div>
        ) : null}
        <div className="hotel-detail__identity">
          <p className="hotel-detail__city">{hotel.city.name}</p>
          <h1 className="page__title">{hotel.name}</h1>
        </div>
      </header>
      {actionError ? (
        <p className="hotel-detail__error" role="alert">
          {actionError}
        </p>
      ) : null}
      <div className="reveal" style={{ ['--i' as string]: 2 }}>
        <RelatedPackages
          title="Paquetes en este hotel"
          filters={{ hotel: hotel.name }}
          seeAllTo={`/trips?hotel=${encodeURIComponent(hotel.name)}`}
        />
      </div>
    </article>
  )
}
