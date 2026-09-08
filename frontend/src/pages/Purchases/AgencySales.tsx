import { useQuery } from '@tanstack/react-query'
import { getApiErrorMessage } from '../../api/errors'
import { getAgencyPurchases } from '../../api/purchases'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import { PurchaseEmptyScene, PurchaseTicket } from './PurchaseTicket'
import './Purchases.css'

export function AgencySales() {
  const salesQuery = useQuery({
    queryKey: queryKeys.purchases.agency,
    queryFn: getAgencyPurchases,
  })

  const sales = salesQuery.data ?? []

  return (
    <section className="page">
      <header className="purchase-intro">
        <p className="purchase-intro__eyebrow">Tu agencia</p>
        <h1 className="page__title">Ventas de la agencia</h1>
      </header>
      {salesQuery.isPending ? (
        <ListSkeleton variant="rows" label="Cargando ventas" />
      ) : salesQuery.error ? (
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={
            getApiErrorMessage(salesQuery.error) ?? 'No pudimos cargar las ventas. Intentá de nuevo.'
          }
          action={
            <Button type="button" onClick={() => void salesQuery.refetch()}>
              Reintentar
            </Button>
          }
        />
      ) : sales.length === 0 ? (
        <EmptyState
          title="Todavía no hay ventas"
          message="Cuando alguien compre un paquete de tu agencia, el pasaje aparece acá."
          illustration={<PurchaseEmptyScene />}
        />
      ) : (
        <ul className="purchase-list">
          {sales.map((sale) => (
            <li key={sale.id}>
              <PurchaseTicket purchase={sale} showAgency={false} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
