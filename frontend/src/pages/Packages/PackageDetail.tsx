import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { addFavorite, getFavorites, removeFavorite } from '../../api/favorites'
import { getApiErrorMessage } from '../../api/errors'
import { getPackage } from '../../api/packages'
import { buyPackage, getMyPurchases } from '../../api/purchases'
import { createReview } from '../../api/reviews'
import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { PhotoCarousel } from '../../components/PhotoCarousel/PhotoCarousel'
import { queryKeys } from '../../query/keys'
import './packages.css'

function formatFlightWhen(date: string, time: string): string {
  const stamp = new Date(`${date}T${time}`)
  if (Number.isNaN(stamp.getTime())) {
    return `${date} · ${time.slice(0, 5)}`
  }
  return stamp.toLocaleString('es-AR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function PackageDetail() {
  const { id } = useParams()
  const packageId = Number(id)
  const invalidId = !Number.isFinite(packageId)
  const { isBuyer } = useAuth()
  const queryClient = useQueryClient()
  const [score, setScore] = useState('8')
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(invalidId ? 'Paquete inválido.' : null)
  const [message, setMessage] = useState<string | null>(null)

  const detailQuery = useQuery({
    queryKey: queryKeys.packages.detail(packageId),
    queryFn: () => getPackage(packageId),
    enabled: !invalidId,
  })

  const favoritesQuery = useQuery({
    queryKey: queryKeys.favorites,
    queryFn: getFavorites,
    enabled: !invalidId && isBuyer,
  })

  const purchasesQuery = useQuery({
    queryKey: queryKeys.purchases.mine,
    queryFn: getMyPurchases,
    enabled: !invalidId && isBuyer,
  })

  const favorited = favoritesQuery.data?.some((item) => item.packageId === packageId) ?? false
  const purchased = purchasesQuery.data?.some((item) => item.packageId === packageId) ?? false
  const detail = detailQuery.data

  const buyMutation = useMutation({
    mutationFn: () => buyPackage(packageId),
    onSuccess: async () => {
      setMessage('Compra realizada. Los asientos de ida y vuelta quedaron reservados.')
      await queryClient.invalidateQueries({ queryKey: queryKeys.purchases.mine })
      await queryClient.invalidateQueries({ queryKey: queryKeys.packages.detail(packageId) })
    },
    onError: (reason: unknown) => {
      setError(getApiErrorMessage(reason) ?? 'No se pudo completar la compra.')
    },
  })

  const favoriteMutation = useMutation({
    mutationFn: async () => {
      if (favorited) {
        await removeFavorite(packageId)
        return
      }
      await addFavorite(packageId)
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.favorites })
    },
    onError: (reason: unknown) => {
      setError(getApiErrorMessage(reason) ?? 'No se pudo actualizar favoritos.')
    },
  })

  const reviewMutation = useMutation({
    mutationFn: () => createReview(packageId, Number(score), comment || undefined),
    onSuccess: async () => {
      setComment('')
      setMessage('Reseña publicada.')
      await queryClient.invalidateQueries({ queryKey: queryKeys.packages.detail(packageId) })
    },
    onError: (reason: unknown) => {
      setError(getApiErrorMessage(reason) ?? 'No se pudo publicar la reseña.')
    },
  })

  const onBuy = () => {
    setError(null)
    setMessage(null)
    buyMutation.mutate()
  }

  const onToggleFavorite = () => {
    setError(null)
    favoriteMutation.mutate()
  }

  const onReview = (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    reviewMutation.mutate()
  }

  const loading =
    detailQuery.isPending || (isBuyer && (favoritesQuery.isPending || purchasesQuery.isPending))

  if (!invalidId && loading) {
    return (
      <section className="page">
        <ListSkeleton variant="detail" label="Cargando paquete" />
      </section>
    )
  }

  if (!detail) {
    const missingPackage = !invalidId && !detailQuery.error
    return (
      <section className="page">
        <EmptyState
          tone={missingPackage ? 'empty' : 'error'}
          title={missingPackage ? 'No encontramos este paquete' : 'Algo salió mal'}
          message={
            missingPackage
              ? 'Puede que lo hayan eliminado o que el enlace esté mal.'
              : (error ??
                getApiErrorMessage(detailQuery.error) ??
                'No pudimos cargar este paquete. Intentá de nuevo.')
          }
          action={
            <Link className="empty-state__cta" to="/trips">
              Volver al catálogo
            </Link>
          }
        />
      </section>
    )
  }

  return (
    <section className="page">
      <Link className="catalog__back" to="/trips">
        ← Volver
      </Link>
      <article className="package-detail">
        <div className="package-detail__hero">
          <PhotoCarousel
            key={detail.destination.code}
            cityCode={detail.destination.code}
            alt={detail.hotel.name}
          />
          <p className="package-detail__route">
            {detail.origin.name} → {detail.destination.name}
          </p>
        </div>
        <div className="package-detail__body">
          <header className="package-detail__heading">
            <div>
              <h1 className="page__title">{detail.name}</h1>
              <p className="package-detail__meta">
                {detail.agency.name} · {detail.hotel.name}
              </p>
              {detail.averageScore != null ? (
                <p className="package-detail__meta">
                  Puntaje promedio: {detail.averageScore.toFixed(1)} / 10
                </p>
              ) : null}
            </div>
            <p className="package-detail__price">USD {detail.price}</p>
          </header>

          <div className="package-detail__flights">
            {detail.outboundFlight ? (
              <div className="flight-card">
                <p className="flight-card__label">Ida</p>
                <p className="flight-card__route">
                  {detail.outboundFlight.origin} → {detail.outboundFlight.destination}
                </p>
                <p className="flight-card__when">
                  {detail.outboundFlight.airline} ·{' '}
                  {formatFlightWhen(
                    detail.outboundFlight.flightDate,
                    detail.outboundFlight.departureTime,
                  )}
                </p>
              </div>
            ) : null}
            {detail.returnFlight ? (
              <div className="flight-card">
                <p className="flight-card__label">Vuelta</p>
                <p className="flight-card__route">
                  {detail.returnFlight.origin} → {detail.returnFlight.destination}
                </p>
                <p className="flight-card__when">
                  {detail.returnFlight.airline} ·{' '}
                  {formatFlightWhen(
                    detail.returnFlight.flightDate,
                    detail.returnFlight.departureTime,
                  )}
                </p>
              </div>
            ) : null}
          </div>

          {error ? (
            <p className="catalog__error" role="alert">
              {error}
            </p>
          ) : null}
          {message ? <p className="catalog__muted">{message}</p> : null}
          {isBuyer ? (
            <div className="catalog__actions">
              <Button
                type="button"
                onClick={onBuy}
                disabled={purchased}
                loading={buyMutation.isPending}
              >
                {purchased ? 'Ya comprado' : 'Comprar'}
              </Button>
              <Button
                type="button"
                variant="ghost"
                onClick={onToggleFavorite}
                loading={favoriteMutation.isPending}
              >
                {favorited ? 'Quitar favorito' : 'Favorito'}
              </Button>
            </div>
          ) : null}
        </div>
      </article>

      <section className="package-reviews">
        <h2>Reseñas</h2>
        {detail.reviews.length === 0 ? (
          <p className="catalog__muted">Todavía no hay reseñas.</p>
        ) : null}
        {detail.reviews.map((review) => (
          <article className="review-item" key={review.id}>
            <p className="review-item__head">
              <strong>{review.buyerUsername}</strong> · {review.score}/10
            </p>
            {review.comment ? <p>{review.comment}</p> : null}
          </article>
        ))}
        {isBuyer && purchased ? (
          <form className="catalog__form" onSubmit={onReview}>
            <Input
              label="Puntaje (0-10)"
              type="number"
              min={0}
              max={10}
              value={score}
              onChange={(event) => setScore(event.target.value)}
            />
            <Input
              label="Comentario"
              value={comment}
              onChange={(event) => setComment(event.target.value)}
            />
            <Button type="submit" loading={reviewMutation.isPending}>
              Publicar reseña
            </Button>
          </form>
        ) : null}
      </section>
    </section>
  )
}
