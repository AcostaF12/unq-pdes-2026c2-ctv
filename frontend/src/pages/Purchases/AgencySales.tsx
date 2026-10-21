import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getApiErrorMessage } from '../../api/errors'
import { getAgencyPurchases } from '../../api/purchases'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HeroScene } from '../../components/HeroScene/HeroScene'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import { HistoryPagination } from '../../components/HistoryPagination/HistoryPagination'
import { PurchaseEmptyScene, PurchaseTicket } from './PurchaseTicket'
import './Purchases.css'

export function AgencySales() {
  const [page, setPage] = useState(0)
  const [draft, setDraft] = useState({ buyerUsername: '', packageName: '', from: '', to: '' })
  const [filters, setFilters] = useState(draft)
  const size = 20
  const salesQuery = useQuery({
    queryKey: queryKeys.purchases.agencyPage({ ...filters, page, size }),
    queryFn: () => getAgencyPurchases({ ...filters, page, size }),
  })

  const sales = salesQuery.data?.content ?? []

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setPage(0)
    setFilters({
      buyerUsername: draft.buyerUsername.trim(),
      packageName: draft.packageName.trim(),
      from: draft.from,
      to: draft.to,
    })
  }

  return (
    <section className="page">
      <HeroScene
        compact
        eyebrow="Tu agencia"
        title="Ventas de la agencia"
        subtitle="Seguí cada compra de tus paquetes."
      />
      <form className="history-filters" onSubmit={applyFilters}>
        <Input
          label="Usuario comprador"
          value={draft.buyerUsername}
          onChange={(event) => setDraft({ ...draft, buyerUsername: event.target.value })}
        />
        <Input
          label="Paquete"
          value={draft.packageName}
          onChange={(event) => setDraft({ ...draft, packageName: event.target.value })}
        />
        <Input
          label="Desde"
          type="date"
          max={draft.to || undefined}
          value={draft.from}
          onChange={(event) => setDraft({ ...draft, from: event.target.value })}
        />
        <Input
          label="Hasta"
          type="date"
          min={draft.from || undefined}
          value={draft.to}
          onChange={(event) => setDraft({ ...draft, to: event.target.value })}
        />
        <div className="history-filters__actions">
          <Button type="submit">Filtrar ventas</Button>
        </div>
      </form>
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
          {sales.map((sale, index) => (
            <li key={sale.id}>
              <PurchaseTicket purchase={sale} audience="agency" index={index} />
            </li>
          ))}
        </ul>
      )}
      {salesQuery.data ? <HistoryPagination result={salesQuery.data} onPageChange={setPage} /> : null}
    </section>
  )
}
