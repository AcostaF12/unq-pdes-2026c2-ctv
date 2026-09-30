import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getApiErrorMessage } from '../../api/errors'
import { getAgencyPurchases } from '../../api/purchases'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
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
      <header className="purchase-intro">
        <p className="purchase-intro__eyebrow">Tu agencia</p>
        <h1 className="page__title">Ventas de la agencia</h1>
      </header>
      <form className="history-filters" onSubmit={applyFilters}>
        <label>
          Usuario comprador
          <input
            value={draft.buyerUsername}
            onChange={(event) => setDraft({ ...draft, buyerUsername: event.target.value })}
          />
        </label>
        <label>
          Paquete
          <input
            value={draft.packageName}
            onChange={(event) => setDraft({ ...draft, packageName: event.target.value })}
          />
        </label>
        <label>
          Desde
          <input
            type="date"
            max={draft.to || undefined}
            value={draft.from}
            onChange={(event) => setDraft({ ...draft, from: event.target.value })}
          />
        </label>
        <label>
          Hasta
          <input
            type="date"
            min={draft.from || undefined}
            value={draft.to}
            onChange={(event) => setDraft({ ...draft, to: event.target.value })}
          />
        </label>
        <Button type="submit">Filtrar ventas</Button>
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
          {sales.map((sale) => (
            <li key={sale.id}>
              <PurchaseTicket purchase={sale} audience="agency" />
            </li>
          ))}
        </ul>
      )}
      {salesQuery.data ? <HistoryPagination result={salesQuery.data} onPageChange={setPage} /> : null}
    </section>
  )
}
