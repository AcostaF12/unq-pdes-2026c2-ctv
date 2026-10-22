import { useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getAdminUsers, getAdminFavorites, getAdminReviews, getAdminPurchases } from '../../api/admin'
import type { PagedResult } from '../../api/packages'
import { getApiErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { Pagination } from '../../components/Pagination/Pagination'
import './AdminQueries.css'

interface ReadPageProps<T extends { id: number }> {
  title: string
  resource: string
  load: (page: number) => Promise<PagedResult<T>>
  headings: string[]
  cells: (row: T) => ReactNode[]
}

function ReadPage<T extends { id: number }>({ title, resource, load, headings, cells }: ReadPageProps<T>) {
  const [page, setPage] = useState(0)
  const query = useQuery({ queryKey: ['admin', resource, page], queryFn: () => load(page) })
  return (
    <section className="page">
      <Link className="catalog__back" to="/admin">← Administración</Link>
      <h1 className="page__title">{title}</h1>
      <p className="catalog__muted">Consulta de sólo lectura.</p>
      {query.isPending ? <ListSkeleton variant="rows" count={4} label={`Cargando ${title.toLowerCase()}`} /> : query.isError ? (
        <EmptyState tone="error" title="No se pudo cargar la consulta"
          message={getApiErrorMessage(query.error) ?? 'Intentá de nuevo.'}
          action={<Button onClick={() => void query.refetch()}>Reintentar</Button>} />
      ) : query.data.content.length === 0 ? <EmptyState title="Sin resultados" message="No hay registros para mostrar en esta página." /> : (
        <div className="admin-query-scroll" role="region" aria-label={`Tabla de ${title.toLowerCase()}`} tabIndex={0}>
          <table className="admin-query-table">
            <caption>{title}. Total: {query.data.totalElements}</caption>
            <thead><tr>{headings.map((heading) => <th key={heading} scope="col">{heading}</th>)}</tr></thead>
            <tbody>{query.data.content.map((row) => <tr key={row.id}>
              {cells(row).map((cell, index) => <td key={headings[index]}>{cell}</td>)}
            </tr>)}</tbody>
          </table>
        </div>
      )}
      {query.data && !query.isError ? <Pagination page={page} totalPages={query.data.totalPages} onPageChange={setPage} /> : null}
    </section>
  )
}

const roleNames: Record<string, string> = { ADMIN: 'Administrador', AGENCY: 'Agencia', BUYER: 'Comprador' }
const amount = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'USD' })
const date = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' })

export function AdminUsers() {
  return <ReadPage title="Usuarios" resource="users" load={getAdminUsers}
    headings={['Usuario', 'Nombre', 'Rol', 'Agencia']}
    cells={(row) => [row.username, `${row.firstName} ${row.lastName}`, roleNames[row.role] ?? row.role,
      row.agency ? `${row.agency.name}${row.agency.active === false ? ' (inactiva)' : ''}` : '—']} />
}

export function AdminFavorites() {
  return <ReadPage title="Favoritos" resource="favorites" load={getAdminFavorites}
    headings={['Comprador', 'Paquete', 'Agencia']}
    cells={(row) => [row.buyerUsername, row.packageName, row.agencyName]} />
}

export function AdminReviews() {
  return <ReadPage title="Reseñas" resource="reviews" load={getAdminReviews}
    headings={['Comprador', 'Paquete', 'Puntaje', 'Comentario']}
    cells={(row) => [row.buyerUsername, row.packageName, `${row.score} / 10`, row.comment || 'Sin comentario']} />
}

export function AdminPurchases() {
  return <ReadPage title="Compras" resource="purchases" load={getAdminPurchases}
    headings={['Compra', 'Comprador', 'Paquete', 'Agencia', 'Fecha', 'Precio abonado']}
    cells={(row) => [row.id, row.buyer.username, row.travelPackage.name, row.travelPackage.agency.name,
      date.format(new Date(row.purchasedAt)), amount.format(row.purchasePrice)]} />
}
