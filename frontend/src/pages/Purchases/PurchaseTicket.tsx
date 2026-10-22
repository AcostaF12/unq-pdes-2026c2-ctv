import { Link } from 'react-router-dom'
import type { Purchase } from '../../api/purchases'
import { confirmationCode, formatPurchaseDate } from '../../lib/format'

interface PurchaseTicketProps {
  purchase: Purchase
  audience?: 'buyer' | 'agency' | 'admin'
  index?: number
}

export function PurchaseEmptyScene() {
  return (
    <div className="purchase-empty" aria-hidden="true">
      <span className="purchase-empty__cloud purchase-empty__cloud--a" />
      <span className="purchase-empty__cloud purchase-empty__cloud--b" />
      <div className="purchase-empty__ticket">
        <div className="purchase-empty__body">
          <span className="purchase-empty__stamp">PASE</span>
          <span className="purchase-empty__bar" />
          <span className="purchase-empty__bar purchase-empty__bar--short" />
        </div>
        <div className="purchase-empty__stub">
          <span className="purchase-empty__code">01</span>
        </div>
      </div>
    </div>
  )
}

export function PurchaseTicket({ purchase, audience = 'buyer', index = 0 }: PurchaseTicketProps) {
  const { travelPackage } = purchase
  const tripPath = `/trips/${travelPackage.id}`
  const detailPath = audience === 'admin' ? `/admin/sales/${purchase.id}` : `/purchases/${purchase.id}`
  const counterparty =
    audience !== 'buyer'
      ? `${purchase.buyer.firstName} ${purchase.buyer.lastName} (@${purchase.buyer.username})`
      : travelPackage.agency.name

  return (
    <article className="purchase-ticket reveal" style={{ ['--i' as string]: index }}>
      <div className="purchase-ticket__body">
        <p className="purchase-ticket__meta">{counterparty}</p>
        {audience === 'admin' ? <p className="purchase-ticket__meta">{travelPackage.agency.name}</p> : null}
        <h2 className="purchase-ticket__name">
          <Link to={tripPath}>{travelPackage.name}</Link>
        </h2>
        <p className="purchase-ticket__trip">
          {travelPackage.origin.name} → {travelPackage.destination.name}
        </p>
        <p className="purchase-ticket__hotel">Hotel: {travelPackage.hotel.name}</p>
        <p className="purchase-ticket__footer">
          <time className="purchase-ticket__date" dateTime={purchase.purchasedAt}>
            {formatPurchaseDate(purchase.purchasedAt)}
          </time>
          <span className="purchase-ticket__code">{confirmationCode(purchase.id)}</span>
        </p>
      </div>
      <div className="purchase-ticket__stub">
        <p className="purchase-ticket__price">
          <span className="purchase-ticket__price-label">Precio abonado</span>
          USD {purchase.purchasePrice}
        </p>
        <Link className="purchase-ticket__link" to={detailPath}>
          Ver detalle
        </Link>
        <Link className="purchase-ticket__link" to={tripPath}>
          Ver viaje
        </Link>
      </div>
    </article>
  )
}
