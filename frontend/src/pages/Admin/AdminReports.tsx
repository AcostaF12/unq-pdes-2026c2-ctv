import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getAdminReports } from '../../api/admin'
import { getApiErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import './AdminQueries.css'

interface RankingProps {
  title: string
  headings: string[]
  rows: ReactNode[][]
}

function Ranking({ title, headings, rows }: RankingProps) {
  return (
    <section>
      <h2>{title}</h2>
      {rows.length === 0 ? <p>Sin datos para este reporte.</p> : (
        <div className="admin-query-scroll" role="region" aria-label={title} tabIndex={0}>
          <table className="admin-query-table">
            <caption>{title}</caption>
            <thead><tr><th scope="col">Puesto</th>{headings.map((heading) => <th scope="col" key={heading}>{heading}</th>)}</tr></thead>
            <tbody>{rows.map((cells, rank) => <tr key={rank}>
              <td>{rank + 1}</td>{cells.map((cell, index) => <td key={headings[index]}>{cell}</td>)}
            </tr>)}</tbody>
          </table>
        </div>
      )}
    </section>
  )
}

const rating = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 })

export function AdminReports() {
  const query = useQuery({ queryKey: ['admin', 'reports'], queryFn: getAdminReports })
  const data = query.data
  return (
    <section className="page">
      <Link className="catalog__back" to="/admin">← Administración</Link>
      <h1 className="page__title">Reportes Top 5</h1>
      <p className="catalog__muted">Histórico completo, incluidas agencias inactivas. Se muestran hasta cinco resultados por reporte.</p>
      <p className="catalog__muted">Los puntajes son el promedio de las reseñas de cada destino, de 0 a 10. Los destinos vendidos corresponden al viaje guardado en cada compra.</p>
      {query.isPending ? <ListSkeleton variant="rows" count={4} label="Cargando reportes" /> : query.isError ? (
        <EmptyState tone="error" title="No se pudieron cargar los reportes" message={getApiErrorMessage(query.error) ?? 'Intentá de nuevo.'}
          action={<Button onClick={() => void query.refetch()}>Reintentar</Button>} />
      ) : data ? <div className="admin-report-list">
        <Ranking title="Compradores con más compras" headings={['Comprador', 'Compras']}
          rows={data.topBuyers.map((row) => [row.name, row.total])} />
        <Ranking title="Destinos mejor puntuados" headings={['Destino', 'Código', 'Promedio / 10', 'Reseñas']}
          rows={data.topRatedDestinations.map((row) => [row.name, row.code, rating.format(row.average), row.reviewCount])} />
        <Ranking title="Agencias con más ventas" headings={['Agencia', 'Ventas']}
          rows={data.topAgencies.map((row) => [row.name, row.total])} />
        <Ranking title="Destinos más vendidos" headings={['Destino', 'Código', 'Ventas']}
          rows={data.topDestinations.map((row) => [row.name, row.code, row.total])} />
      </div> : null}
    </section>
  )
}
