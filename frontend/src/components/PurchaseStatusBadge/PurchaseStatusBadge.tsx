import type { Purchase } from '../../api/purchases'
import { formatPurchaseDate } from '../../lib/format'
import { isCancelled } from '../../lib/purchaseStatus'
import './PurchaseStatusBadge.css'

interface PurchaseStatusBadgeProps {
  purchase: Pick<Purchase, 'status' | 'cancelledAt'>
}

/** Only cancelled purchases get a badge: active ones keep their usual look. */
export function PurchaseStatusBadge({ purchase }: PurchaseStatusBadgeProps) {
  if (!isCancelled(purchase)) {
    return null
  }
  const title = purchase.cancelledAt ? `Cancelada el ${formatPurchaseDate(purchase.cancelledAt)}` : undefined
  return (
    <span className="purchase-status purchase-status--cancelled" title={title}>
      Cancelada
    </span>
  )
}
