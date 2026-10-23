import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import { getAdminSales, getAgencyPurchases } from '../../api/purchases'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HeroScene } from '../../components/HeroScene/HeroScene'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import { HistoryPagination } from '../../components/HistoryPagination/HistoryPagination'
import { PurchaseEmptyScene, PurchaseTicket } from './PurchaseTicket'
import './Purchases.css'

export function AgencySales({ admin = false }: { admin?: boolean }) {
  const [page, setPage] = useState(0)
  const [draft, setDraft] = useState({ buyerUsername: '', packageName: '', from: '', to: '' })
  const [filters, setFilters] = useState(draft)
  const size = 20
  const salesQuery = useQuery({
    queryKey: admin ? ['admin', 'sales', { ...filters, page, size }] : queryKeys.purchases.agencyPage({ ...filters, page, size }),
    queryFn: () => admin ? getAdminSales({ ...filters, page, size }) : getAgencyPurchases({ ...filters, page, size }),
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
    <section className="page history-page">
      {admin ? <Link className="catalog__back" to="/admin">← Administración</Link> : null}
      <HeroScene
        compact
        eyebrow={admin ? 'Administración' : 'Tu agencia'}
        title={admin ? 'Ventas de todas las agencias' : 'Ventas de la agencia'}
        subtitle={admin ? 'Consultá compradores, agencias y el detalle de cada venta.' : 'Seguí cada compra de tus paquetes.'}
      />
      <form className="history-filters" aria-label="Filtros de ventas" onSubmit={applyFilters}>
        <header className="history-filters__header">
          <h2 className="history-filters__title">Filtros</h2>
          <p className="history-filters__hint">Buscá por comprador, paquete o fechas.</p>
        </header>
        <div className="history-filters__fields">
          <div className="history-filters__group">
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
          </div>
          <div className="history-filters__group">
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
          </div>
        </div>
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
          message={admin ? 'Las ventas que coincidan con los filtros aparecerán acá.' : 'Cuando alguien compre un paquete de tu agencia, el pasaje aparece acá.'}
          illustration={<PurchaseEmptyScene />}
        />
      ) : (
        <ul className="purchase-list">
          {sales.map((sale, index) => (
            <li key={sale.id}>
              <PurchaseTicket purchase={sale} audience={admin ? 'admin' : 'agency'} index={index} />
            </li>
          ))}
        </ul>
      )}
      {salesQuery.data ? <HistoryPagination result={salesQuery.data} onPageChange={setPage} /> : null}
    </section>
  )
}
