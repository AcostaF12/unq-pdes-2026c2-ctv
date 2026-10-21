import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import { getPackage, type Flight, type Review } from '../../api/packages'
import { buyPackage, hasPurchasedPackage } from '../../api/purchases'
import { createReview } from '../../api/reviews'
import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button/Button'
import { CheckoutDialog } from '../../components/CheckoutDialog/CheckoutDialog'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { FavoriteButton } from '../../components/FavoriteButton/FavoriteButton'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { PhotoCarousel } from '../../components/PhotoCarousel/PhotoCarousel'
import { RelatedPackages } from '../../components/RelatedPackages/RelatedPackages'
import { Rating } from '../../components/Rating/Rating'
import { useToast } from '../../components/Toast/ToastProvider'
import { useFavorites } from '../../hooks/useFavorites'
import { flightNumber, formatFlightWhen, formatScore, seatsLabel, tripNights } from '../../lib/format'
import { queryKeys } from '../../query/keys'
import './packages.css'

const SCORE_BUCKETS = [
  { label: 'Excelente (9-10)', min: 9, max: 10 },
  { label: 'Muy bueno (7-8)', min: 7, max: 8 },
  { label: 'Regular (5-6)', min: 5, max: 6 },
  { label: 'Malo (0-4)', min: 0, max: 4 },
]

function FlightCard({ label, flight }: { label: string; flight: Flight }) {
  const seats = seatsLabel(flight.availability)
  return (
    <div className="flight-card">
      <p className="flight-card__label">{label}</p>
      <p className="flight-card__route">
        {flight.origin} → {flight.destination}
      </p>
      <p className="flight-card__when">
        {flight.airline} · {formatFlightWhen(flight.flightDate, flight.departureTime)}
      </p>
      <p className="flight-card__meta">
        <span>Vuelo {flightNumber(flight)}</span>
        <span className={`flight-card__seats flight-card__seats--${seats.tone}`}>{seats.text}</span>
      </p>
    </div>
  )
}

function ReviewSummary({ reviews, average }: { reviews: Review[]; average: number | null }) {
  if (reviews.length === 0 || average == null) {
    return null
  }
  return (
    <div className="review-summary">
      <div className="review-summary__score">
        <strong>{formatScore(average)}</strong>
        <span>de 10</span>
        <Rating score={average} count={reviews.length} size="md" />
      </div>
      <ul className="review-summary__bars">
        {SCORE_BUCKETS.map((bucket) => {
          const count = reviews.filter(
            (review) => review.score >= bucket.min && review.score <= bucket.max,
          ).length
          return (
            <li key={bucket.label}>
              <span>{bucket.label}</span>
              <span className="review-summary__track" aria-hidden="true">
                <span
                  className="review-summary__fill"
                  style={{ width: `${(count / reviews.length) * 100}%` }}
                />
              </span>
              <span>{count}</span>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

export function PackageDetail() {
  const { id } = useParams()
  const packageId = Number(id)
  const invalidId = !Number.isFinite(packageId)
  const { user, isBuyer } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const favorites = useFavorites({ enabled: !invalidId && isBuyer })
  const [score, setScore] = useState(8)
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(invalidId ? 'Paquete inválido.' : null)
  const [checkoutOpen, setCheckoutOpen] = useState(false)

  const detailQuery = useQuery({
    queryKey: queryKeys.packages.detail(packageId),
    queryFn: () => getPackage(packageId),
    enabled: !invalidId,
  })

  const purchasesQuery = useQuery({
    queryKey: queryKeys.purchases.ownsPackage(packageId),
    queryFn: () => hasPurchasedPackage(packageId),
    enabled: !invalidId && isBuyer,
  })

  const purchased = purchasesQuery.data ?? false
  const detail = detailQuery.data
  const favorited = favorites.isFavorite(packageId)
  const alreadyReviewed = Boolean(
    user && detail?.reviews.some((review) => review.buyerUsername === user.username),
  )

  const buyMutation = useMutation({
    mutationFn: () => buyPackage(packageId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.purchases.mine })
      await queryClient.invalidateQueries({ queryKey: queryKeys.purchases.ownsPackage(packageId) })
      await queryClient.invalidateQueries({ queryKey: queryKeys.packages.detail(packageId) })
    },
  })

  const reviewMutation = useMutation({
    mutationFn: () => createReview(packageId, score, comment || undefined),
    onSuccess: async () => {
      setComment('')
      toast.show('Reseña publicada. ¡Gracias por contar tu experiencia!')
      await queryClient.invalidateQueries({ queryKey: queryKeys.packages.detail(packageId) })
      await queryClient.invalidateQueries({ queryKey: queryKeys.packages.all })
    },
    onError: (reason: unknown) => {
      setError(getApiErrorMessage(reason) ?? 'No se pudo publicar la reseña.')
    },
  })

  const openCheckout = () => {
    setError(null)
    buyMutation.reset()
    setCheckoutOpen(true)
  }

  const closeCheckout = () => {
    setCheckoutOpen(false)
    buyMutation.reset()
  }

  const onReview = (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    reviewMutation.mutate()
  }

  const loading = detailQuery.isPending || (isBuyer && purchasesQuery.isPending)

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

  const nights =
    detail.outboundFlight && detail.returnFlight
      ? tripNights(detail.outboundFlight.flightDate, detail.returnFlight.flightDate)
      : null
  const passengerName = user ? `${user.firstName} ${user.lastName}` : ''
  const checkoutStatus = buyMutation.isPending
    ? 'processing'
    : buyMutation.isSuccess
      ? 'success'
      : 'review'
  const favoriteError = favorites.error

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
            <div className="package-detail__titles">
              <h1 className="page__title">{detail.name}</h1>
              <p className="package-detail__meta">
                {detail.agency.name} · {detail.hotel.name}
              </p>
              <Rating score={detail.averageScore} count={detail.reviews.length} size="md" />
              <ul className="package-detail__facts">
                {nights != null ? (
                  <li className="chip">
                    {nights} {nights === 1 ? 'noche' : 'noches'}
                  </li>
                ) : null}
                <li className="chip">Vuelos incluidos</li>
                <li className="chip">Hotel en {detail.hotel.city.name}</li>
              </ul>
            </div>
            <p className="package-detail__price">USD {detail.price}</p>
          </header>

          <div className="package-detail__flights">
            {detail.outboundFlight ? <FlightCard label="Ida" flight={detail.outboundFlight} /> : null}
            {detail.returnFlight ? <FlightCard label="Vuelta" flight={detail.returnFlight} /> : null}
          </div>

          {error || favoriteError ? (
            <p className="catalog__error" role="alert">
              {error ?? favoriteError}
            </p>
          ) : null}
          {isBuyer ? (
            <div className="catalog__actions">
              <Button type="button" onClick={openCheckout} disabled={purchased}>
                {purchased ? 'Ya comprado' : 'Comprar'}
              </Button>
              <FavoriteButton
                variant="labeled"
                active={favorited}
                activeLabel="Quitar favorito"
                inactiveLabel="Favorito"
                disabled={favorites.isToggling(packageId)}
                onClick={() => favorites.toggle(detail)}
              />
              {purchased ? (
                <Link className="package-detail__link" to="/purchases">
                  Ver mis compras →
                </Link>
              ) : null}
            </div>
          ) : null}
        </div>
      </article>

      <section className="package-reviews">
        <h2>Reseñas</h2>
        <ReviewSummary reviews={detail.reviews} average={detail.averageScore} />
        {detail.reviews.length === 0 ? (
          <p className="catalog__muted">Todavía no hay reseñas.</p>
        ) : null}
        {detail.reviews.map((review, index) => (
          <article
            className="review-item reveal"
            key={review.id}
            style={{ ['--i' as string]: index }}
          >
            <span className="review-item__avatar" aria-hidden="true">
              {review.buyerUsername.slice(0, 1).toUpperCase()}
            </span>
            <div className="review-item__content">
              <p className="review-item__head">
                <strong>{review.buyerUsername}</strong>
                <span className="review-item__score">{review.score}/10</span>
              </p>
              {review.comment ? <p>{review.comment}</p> : null}
            </div>
          </article>
        ))}
        {isBuyer && purchased && alreadyReviewed ? (
          <p className="catalog__muted">Ya dejaste tu reseña para este viaje.</p>
        ) : null}
        {isBuyer && purchased && !alreadyReviewed ? (
          <form className="catalog__form" onSubmit={onReview}>
            <fieldset className="score-picker">
              <legend>Puntaje (0-10)</legend>
              <div className="score-picker__options">
                {Array.from({ length: 11 }, (_, value) => (
                  <label key={value} className="score-picker__option">
                    <input
                      type="radio"
                      name="review-score"
                      value={value}
                      checked={score === value}
                      onChange={() => setScore(value)}
                    />
                    <span>{value}</span>
                  </label>
                ))}
              </div>
            </fieldset>
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

      <RelatedPackages
        title={`Más viajes a ${detail.destination.name}`}
        filters={{ destination: detail.destination.code }}
        excludeId={detail.id}
        seeAllTo={`/trips?destination=${encodeURIComponent(detail.destination.code)}`}
      />

      {checkoutOpen ? (
        <CheckoutDialog
          detail={detail}
          passengerName={passengerName}
          status={checkoutStatus}
          purchase={buyMutation.data}
          error={buyMutation.isError ? (getApiErrorMessage(buyMutation.error) ?? 'No se pudo completar la compra.') : null}
          onConfirm={() => buyMutation.mutate()}
          onClose={closeCheckout}
        />
      ) : null}
    </section>
  )
}
