import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import { getMyPurchases } from '../../api/purchases'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import { HistoryPagination } from '../../components/HistoryPagination/HistoryPagination'
import { PurchaseEmptyScene, PurchaseTicket } from './PurchaseTicket'
import './Purchases.css'

export function Purchases() {
  const [page, setPage] = useState(0)
  const size = 20
  const purchasesQuery = useQuery({
    queryKey: queryKeys.purchases.minePage(page, size),
    queryFn: () => getMyPurchases({ page, size }),
  })

  const purchases = purchasesQuery.data?.content ?? []

  return (
    <section className="page">
      <header className="purchase-intro">
        <p className="purchase-intro__eyebrow">Tu historial</p>
        <h1 className="page__title">Mis compras</h1>
      </header>
      {purchasesQuery.isPending ? (
        <ListSkeleton variant="rows" label="Cargando compras" />
      ) : purchasesQuery.error ? (
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={
            getApiErrorMessage(purchasesQuery.error) ??
            'No pudimos cargar tus compras. Intentá de nuevo.'
          }
          action={
            <Button type="button" onClick={() => void purchasesQuery.refetch()}>
              Reintentar
            </Button>
          }
        />
      ) : purchases.length === 0 ? (
        <EmptyState
          title="Todavía no hay viajes comprados"
          message="Cuando compres un paquete, el pasaje aparece acá."
          illustration={<PurchaseEmptyScene />}
          action={
            <Link className="empty-state__cta" to="/trips">
              Ver paquetes
            </Link>
          }
        />
      ) : (
        <ul className="purchase-list">
          {purchases.map((purchase) => (
            <li key={purchase.id}>
              <PurchaseTicket purchase={purchase} />
            </li>
          ))}
        </ul>
      )}
      {purchasesQuery.data ? <HistoryPagination result={purchasesQuery.data} onPageChange={setPage} /> : null}
    </section>
  )
}
