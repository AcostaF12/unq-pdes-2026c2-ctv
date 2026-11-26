import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getAdminReports } from '../../api/admin'
import { getApiErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import './AdminQueries.css'

interface RankBar {
  label: string
  value: number
  display: string
}

interface RankingProps {
  title: string
  headings: string[]
  rows: ReactNode[][]
  bars: RankBar[]
}

function Ranking({ title, headings, rows, bars }: RankingProps) {
  const max = Math.max(...bars.map((bar) => bar.value), 0)
  return (
    <section className="admin-report">
      <h2>{title}</h2>
      {rows.length === 0 ? <p className="admin-report__empty">Sin datos para este reporte.</p> : (
        <div className="admin-query-scroll" role="region" aria-label={title} tabIndex={0}>
          <table className="admin-query-table">
            <caption>{title}</caption>
            <thead><tr><th scope="col">Puesto</th>{headings.map((heading) => <th scope="col" key={heading}>{heading}</th>)}</tr></thead>
            <tbody>{rows.map((cells, rank) => <tr key={bars[rank]?.label ?? rank}>
              <th scope="row">{rank + 1}</th>
              {cells.map((cell, index) => <td key={headings[index]}>
                {index === 0 ? (
                  <>
                    <span className="admin-rank__label">{cell}</span>
                    <span className="admin-rank__track" aria-hidden="true">
                      <span
                        className="admin-rank__bar"
                        style={{ width: `${max === 0 ? 0 : ((bars[rank]?.value ?? 0) / max) * 100}%` }}
                      />
                    </span>
                  </>
                ) : cell}
              </td>)}
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
    <section className="page admin-reports">
      <Link className="catalog__back" to="/admin">← Administración</Link>
      <header className="admin-reports__intro">
        <h1 className="page__title">Reportes Top 5</h1>
        <p className="admin-reports__note">
          Histórico completo, incluidas agencias inactivas. Cada reporte muestra hasta cinco filas.
          El puntaje es el promedio de reseñas de cada destino, de 0 a 10. Los destinos vendidos son
          los del viaje de cada compra.
        </p>
      </header>
      {query.isPending ? <ListSkeleton variant="rows" count={4} label="Cargando reportes" /> : query.isError ? (
        <EmptyState tone="error" title="No se pudieron cargar los reportes" message={getApiErrorMessage(query.error) ?? 'Intentá de nuevo.'}
          action={<Button onClick={() => void query.refetch()}>Reintentar</Button>} />
      ) : data ? <div className="admin-report-list">
        <Ranking title="Compradores con más compras" headings={['Comprador', 'Compras']}
          rows={data.topBuyers.map((row) => [row.name, row.total])}
          bars={data.topBuyers.map((row) => ({ label: row.name, value: row.total, display: String(row.total) }))} />
        <Ranking title="Destinos mejor puntuados" headings={['Destino', 'Código', 'Promedio / 10', 'Reseñas']}
          rows={data.topRatedDestinations.map((row) => [row.name, row.code, rating.format(row.average), row.reviewCount])}
          bars={data.topRatedDestinations.map((row) => ({ label: row.name, value: row.average, display: rating.format(row.average) }))} />
        <Ranking title="Agencias con más ventas" headings={['Agencia', 'Ventas']}
          rows={data.topAgencies.map((row) => [row.name, row.total])}
          bars={data.topAgencies.map((row) => ({ label: row.name, value: row.total, display: String(row.total) }))} />
        <Ranking title="Destinos más vendidos" headings={['Destino', 'Código', 'Ventas']}
          rows={data.topDestinations.map((row) => [row.name, row.code, row.total])}
          bars={data.topDestinations.map((row) => ({ label: row.name, value: row.total, display: String(row.total) }))} />
      </div> : null}
    </section>
  )
}
