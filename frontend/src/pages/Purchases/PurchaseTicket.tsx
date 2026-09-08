import { Link } from 'react-router-dom'
import type { Purchase } from '../../api/purchases'

interface PurchaseTicketProps {
  purchase: Purchase
  showAgency?: boolean
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

function formatPurchaseDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString('es-AR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export function PurchaseTicket({ purchase, showAgency = true }: PurchaseTicketProps) {
  const tripPath = `/trips/${purchase.packageId}`

  return (
    <article className="purchase-ticket">
      <div className="purchase-ticket__body">
        <p className="purchase-ticket__meta">{showAgency ? purchase.agency.name : 'Venta'}</p>
        <h2 className="purchase-ticket__name">
          <Link to={tripPath}>{purchase.packageName}</Link>
        </h2>
        <time className="purchase-ticket__date" dateTime={purchase.purchasedAt}>
          {formatPurchaseDate(purchase.purchasedAt)}
        </time>
      </div>
      <div className="purchase-ticket__stub">
        <p className="purchase-ticket__price">USD {purchase.purchasePrice}</p>
        <Link className="purchase-ticket__link" to={tripPath}>
          Ver viaje
        </Link>
      </div>
    </article>
  )
}
