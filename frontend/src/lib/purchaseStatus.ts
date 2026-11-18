import type { Purchase } from '../api/purchases'

export function isCancelled(purchase: Pick<Purchase, 'status'>): boolean {
  return purchase.status === 'CANCELLED'
}
