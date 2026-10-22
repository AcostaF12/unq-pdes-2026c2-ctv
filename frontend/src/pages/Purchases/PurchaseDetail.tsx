import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage, isNotFoundError } from '../../api/errors'
import type { Flight } from '../../api/packages'
import { getPurchase } from '../../api/purchases'
import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HotelPhoto } from '../../components/HotelPhoto/HotelPhoto'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import {
  confirmationCode,
  flightNumber,
  formatFlightWhen,
  formatPurchaseDateTime,
  tripNights,
} from '../../lib/format'
import { queryKeys } from '../../query/keys'
import './Purchases.css'
import './PurchaseDetail.css'

function ItineraryLeg({
  label,
  flight,
  fallbackId,
  from,
  to,
}: {
  label: string
  flight: Flight | null
  fallbackId: number
  from: string
  to: string
}) {
  return (
    <li className="itinerary__leg">
      <span className="itinerary__label">{label}</span>
      <p className="itinerary__route">
        {flight?.origin ?? from} → {flight?.destination ?? to}
      </p>
      {flight ? (
        <>
          <p className="itinerary__when">{formatFlightWhen(flight.flightDate, flight.departureTime)}</p>
          <p className="itinerary__meta">
            {flight.airline} · Vuelo {flightNumber(flight)}
          </p>
        </>
      ) : (
        <p className="itinerary__meta">Vuelo #{fallbackId} · datos no disponibles por ahora</p>
      )}
    </li>
  )
}

export function PurchaseDetail() {
  const { id } = useParams()
  const purchaseId = Number(id)
  const invalidId = !Number.isInteger(purchaseId)
  const { isBuyer, isAdmin } = useAuth()
  const backPath = isAdmin ? '/admin/sales' : isBuyer ? '/purchases' : '/agency/sales'
  const backLabel = isBuyer ? '← Mis compras' : '← Ventas'

  const purchaseQuery = useQuery({
    queryKey: queryKeys.purchases.detail(purchaseId),
    queryFn: () => getPurchase(purchaseId),
    enabled: !invalidId,
  })

  if (!invalidId && purchaseQuery.isPending) {
    return (
      <section className="page">
        <ListSkeleton variant="detail" label="Cargando compra" />
      </section>
    )
  }

  const purchase = purchaseQuery.data
  if (!purchase) {
    const missing = invalidId || isNotFoundError(purchaseQuery.error)
    return (
      <section className="page">
        <EmptyState
          tone={missing ? 'empty' : 'error'}
          title={missing ? 'No encontramos esta compra' : 'Algo salió mal'}
          message={
            missing
              ? 'Puede que el enlace esté mal o que no tengas acceso a ella.'
              : (getApiErrorMessage(purchaseQuery.error) ??
                'No pudimos cargar esta compra. Intentá de nuevo.')
          }
          action={
            <Link className="empty-state__cta" to={backPath}>
              Volver
            </Link>
          }
        />
      </section>
    )
  }

  const { travelPackage, buyer, outboundFlight, returnFlight, review } = purchase
  const nights =
    outboundFlight && returnFlight ? tripNights(outboundFlight.flightDate, returnFlight.flightDate) : null

  return (
    <section className="page purchase-detail">
      <Link className="catalog__back purchase-detail__back" to={backPath}>
        {backLabel}
      </Link>

      <header className="purchase-detail__intro">
        <p className="purchase-intro__eyebrow">Compra confirmada</p>
        <h1 className="page__title">{travelPackage.name}</h1>
        <p className="purchase-detail__sub">
          <span className="purchase-ticket__code">{confirmationCode(purchase.id)}</span>
          <time dateTime={purchase.purchasedAt}>{formatPurchaseDateTime(purchase.purchasedAt)}</time>
        </p>
      </header>

      <article className="pass reveal" style={{ ['--i' as string]: 1 }}>
        <div className="pass__main">
          <div className="pass__cities" aria-label={`${travelPackage.origin.name} a ${travelPackage.destination.name}`}>
            <div>
              <strong>{travelPackage.origin.code}</strong>
              <span>{travelPackage.origin.name}</span>
            </div>
            <span className="pass__plane" aria-hidden="true">
              ✈
            </span>
            <div>
              <strong>{travelPackage.destination.code}</strong>
              <span>{travelPackage.destination.name}</span>
            </div>
          </div>
          <ol className="itinerary">
            <ItineraryLeg
              label="Ida"
              flight={outboundFlight}
              fallbackId={travelPackage.outboundFlightId}
              from={travelPackage.origin.code}
              to={travelPackage.destination.code}
            />
            <ItineraryLeg
              label="Vuelta"
              flight={returnFlight}
              fallbackId={travelPackage.returnFlightId}
              from={travelPackage.destination.code}
              to={travelPackage.origin.code}
            />
          </ol>
          {nights != null ? (
            <p className="pass__nights">
              {nights} {nights === 1 ? 'noche' : 'noches'} en {travelPackage.hotel.city.name}
            </p>
          ) : null}
        </div>
        <div className="pass__stub">
          <p className="purchase-ticket__price">
            <span className="purchase-ticket__price-label">Precio abonado</span>
            USD {purchase.purchasePrice}
          </p>
          <span className="pass__barcode" aria-hidden="true" />
          <span className="pass__stamp" aria-hidden="true">
            Confirmada
          </span>
        </div>
      </article>

      <div className="purchase-detail__grid">
        <section className="detail-card reveal" style={{ ['--i' as string]: 2 }}>
          <h2>Pasajero</h2>
          <p className="detail-card__strong">
            {buyer.firstName} {buyer.lastName}
          </p>
          <p className="catalog__muted">@{buyer.username}</p>
        </section>
        <section className="detail-card reveal" style={{ ['--i' as string]: 3 }}>
          <h2>Agencia</h2>
          <p className="detail-card__strong">{travelPackage.agency.name}</p>
          <p className="catalog__muted">Paquete #{travelPackage.id}</p>
        </section>
        <section className="detail-card detail-card--hotel reveal" style={{ ['--i' as string]: 4 }}>
          <HotelPhoto
            src={travelPackage.hotel.photoUrl}
            alt={travelPackage.hotel.name}
            cityCode={travelPackage.hotel.city.code}
          />
          <div className="detail-card__hotel-body">
            <h2>Hotel</h2>
            <p className="detail-card__strong">{travelPackage.hotel.name}</p>
            <p className="catalog__muted">{travelPackage.hotel.city.name}</p>
          </div>
        </section>
      </div>

      {isBuyer ? (
        <section className="detail-card reveal" style={{ ['--i' as string]: 5 }}>
          <h2>Tu reseña</h2>
          {review ? (
            <>
              <p className="detail-card__strong">{review.score}/10</p>
              {review.comment ? <p>{review.comment}</p> : null}
            </>
          ) : (
            <>
              <p className="catalog__muted">Todavía no calificaste este viaje.</p>
              <Link className="purchase-ticket__link" to={`/trips/${travelPackage.id}`}>
                Dejar reseña →
              </Link>
            </>
          )}
        </section>
      ) : null}

      <div className="purchase-detail__actions">
        <Button type="button" onClick={() => window.print()}>
          Imprimir comprobante
        </Button>
        <Link className="empty-state__cta empty-state__cta--ghost" to={`/trips/${travelPackage.id}`}>
          Ver el paquete
        </Link>
      </div>
    </section>
  )
}
