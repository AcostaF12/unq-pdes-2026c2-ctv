import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState, type CSSProperties } from 'react'
import {
  getAdminSystem,
  type AdminConcurrency,
  type AdminHttpTraffic,
  type AdminSystemSnapshot,
} from '../../api/admin'
import { getApiErrorMessage } from '../../api/errors'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { observabilityConsoles } from './observability'
import { OperationFlow } from './OperationFlow'
import { formatMetric, statusLabel, statusTone, systemName } from './systemLabels'
import '../Packages/packages.css'
import './admin.css'
import './AdminSystem.css'

const REFRESH_MS = 5_000
const SERIES_LIMIT = 30

function usePurchaseSeries(snapshot: AdminSystemSnapshot | undefined): number[] {
  const [series, setSeries] = useState<number[]>([])
  const lastAt = useRef<string | null>(null)
  useEffect(() => {
    if (!snapshot || lastAt.current === snapshot.capturedAt) return
    lastAt.current = snapshot.capturedAt
    const purchase = snapshot.http.endpoints.find(
      (endpoint) => endpoint.method === 'POST' && endpoint.uri === '/purchases',
    )
    if (purchase?.p95Millis == null) return
    const sample = purchase.p95Millis
    setSeries((current) => [...current, sample].slice(-SERIES_LIMIT))
  }, [snapshot])
  return series
}

function FailureRing({ percent }: { percent: number | null }) {
  const radius = 16
  const length = 2 * Math.PI * radius
  const ratio = percent === null ? 0 : Math.min(percent, 100) / 100
  return (
    <svg className="ops-ring" viewBox="0 0 44 44" aria-hidden="true">
      <circle className="ops-ring__track" cx="22" cy="22" r={radius} />
      <circle
        className="ops-ring__value"
        cx="22"
        cy="22"
        r={radius}
        strokeDasharray={`${ratio * length} ${length}`}
      />
    </svg>
  )
}

const capturedAtFormat = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'short',
  timeStyle: 'medium',
})

function capturedAtLabel(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return capturedAtFormat.format(date)
}

function fillWidth(value: number | null, max: number | null): string {
  if (value === null || max === null || max <= 0) {
    return '0%'
  }
  return `${Math.min(value / max, 1) * 100}%`
}

function Meter({
  label,
  value,
  max,
}: {
  label: string
  value: number | null
  max: number | null
}) {
  return (
    <div className="admin-ops__meter">
      <div className="admin-ops__meter-top">
        <span>{label}</span>
        <strong>
          {formatMetric(value)}
          {max === null ? '' : ` / ${formatMetric(max)}`}
        </strong>
      </div>
      <div className="admin-ops__track" aria-hidden="true">
        <span style={{ width: fillWidth(value, max) }} />
      </div>
    </div>
  )
}

function StatusPill({ status }: { status: string }) {
  return <span className={`admin-badge admin-badge--${statusTone(status)}`}>{statusLabel(status)}</span>
}

function healthReading(snapshot: AdminSystemSnapshot): { tone: 'ok' | 'wait' | 'bad'; title: string; body: string } {
  const down = snapshot.systems.find((system) => statusTone(system.status) === 'bad')
  const { state } = snapshot.circuitBreaker
  if (state === 'OPEN' || state === 'FORCED_OPEN') {
    return {
      tone: 'bad',
      title: 'El corte hacia vuelos está abierto',
      body: 'Durante diez segundos el backend no llama a vuelos y la compra responde 503.',
    }
  }
  if (down) {
    return {
      tone: 'bad',
      title: `${systemName(down.name)} está caído`,
      body: 'Una compra necesita backend, base y vuelos al mismo tiempo.',
    }
  }
  if (state === 'HALF_OPEN') {
    return {
      tone: 'wait',
      title: 'Vuelos se está probando',
      body: 'El corte deja pasar pocas llamadas para ver si el servicio volvió.',
    }
  }
  if (snapshot.concurrency.poolPending != null && snapshot.concurrency.poolPending > 0) {
    return {
      tone: 'wait',
      title: 'Hay conexiones esperando',
      body: 'El pool corto es el límite de la compra, no la cantidad de hilos.',
    }
  }
  return {
    tone: 'ok',
    title: 'El proceso está sano',
    body: 'Backend, base y vuelos responden. El corte hacia vuelos está cerrado.',
  }
}

function ConcurrencyBoard({ concurrency }: { concurrency: AdminConcurrency }) {
  return (
    <section className="admin-ops__panel" aria-labelledby="admin-ops-concurrency">
      <h2 id="admin-ops-concurrency">Concurrencia</h2>
      <p>
        Pool de la base y hilos que atienden pedidos ahora. Si pendientes se separa de cero, el pool corto
        es el límite. El último asiento sigue protegido por el lock del vuelo.
      </p>
      <Meter label="Conexiones activas" value={concurrency.poolActive} max={concurrency.poolMax} />
      <dl className="admin-ops__stats">
        <div>
          <dt>Libres</dt>
          <dd>{formatMetric(concurrency.poolIdle)}</dd>
        </div>
        <div className={concurrency.poolPending != null && concurrency.poolPending > 0 ? 'admin-ops__stat--hot' : undefined}>
          <dt>Pendientes</dt>
          <dd>{formatMetric(concurrency.poolPending)}</dd>
        </div>
        <div>
          <dt>Hilos ocupados</dt>
          <dd>{formatMetric(concurrency.threadsBusy)}</dd>
        </div>
        <div>
          <dt>Hilos actuales</dt>
          <dd>{formatMetric(concurrency.threadsCurrent)}</dd>
        </div>
      </dl>
      <Meter label="Tope de hilos" value={concurrency.threadsBusy} max={concurrency.threadsMax} />
    </section>
  )
}

function TrafficBoard({ http }: { http: AdminHttpTraffic }) {
  return (
    <section className="admin-ops__panel admin-ops__span" aria-labelledby="admin-ops-http">
      <h2 id="admin-ops-http">Tráfico desde el arranque</h2>
      <p>Acumulado del proceso, sin contar los chequeos de salud.</p>
      <dl className="admin-ops__stats">
        <div>
          <dt>Requests</dt>
          <dd>{formatMetric(http.requestCount)}</dd>
        </div>
        <div>
          <dt>Errores 4xx</dt>
          <dd>{formatMetric(http.clientErrors)}</dd>
        </div>
        <div>
          <dt>Errores 5xx</dt>
          <dd>{formatMetric(http.serverErrors)}</dd>
        </div>
        <div>
          <dt>Promedio</dt>
          <dd>{http.meanMillis === null ? '—' : `${formatMetric(http.meanMillis)} ms`}</dd>
        </div>
      </dl>
      {http.endpoints.length === 0 ? (
        <p className="admin-ops__empty">Todavía no hay requests medidos.</p>
      ) : (
        <>
          <EndpointBars endpoints={http.endpoints} />
          <div className="admin-table-scroll">
          <table>
            <caption>Endpoints con más requests</caption>
            <thead>
              <tr>
                <th scope="col">Método</th>
                <th scope="col">Ruta</th>
                <th scope="col">Requests</th>
                <th scope="col">5xx</th>
                <th scope="col">p95</th>
              </tr>
            </thead>
            <tbody>
              {http.endpoints.map((endpoint) => (
                <tr key={`${endpoint.method} ${endpoint.uri}`}>
                  <td>{endpoint.method}</td>
                  <td>{endpoint.uri}</td>
                  <td>{formatMetric(endpoint.requestCount)}</td>
                  <td>{formatMetric(endpoint.serverErrors)}</td>
                  <td>{endpoint.p95Millis === null ? '—' : `${formatMetric(endpoint.p95Millis)} ms`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        </>
      )}
    </section>
  )
}

function EndpointBars({ endpoints }: { endpoints: AdminHttpTraffic['endpoints'] }) {
  const max = Math.max(...endpoints.map((endpoint) => endpoint.p95Millis ?? 0), 1)
  return (
    <ul className="admin-ops__bars" aria-label="p95 por endpoint">
      {endpoints.map((endpoint) => (
        <li key={`${endpoint.method} ${endpoint.uri}`} data-method={endpoint.method}>
          <span>
            <span className="admin-ops__verb">{endpoint.method}</span> {endpoint.uri}
          </span>
          <span className="admin-ops__bars-track" aria-hidden="true">
            <span style={{ width: `${((endpoint.p95Millis ?? 0) / max) * 100}%` }} />
          </span>
          <strong>{endpoint.p95Millis === null ? '—' : `${formatMetric(endpoint.p95Millis)} ms`}</strong>
        </li>
      ))}
    </ul>
  )
}

function SnapshotBoard({ snapshot }: { snapshot: AdminSystemSnapshot }) {
  const { circuitBreaker } = snapshot
  const reading = healthReading(snapshot)
  return (
    <div className="admin-ops__board">
      <section className="admin-ops__hero" aria-label="Salud">
        <div className="admin-ops__status-top">
          <h2>Salud del proceso</h2>
          <p className="admin-ops__captured">
            <span className="admin-ops__live-dot" aria-hidden="true" />
            Lectura de las {capturedAtLabel(snapshot.capturedAt)}. Se actualiza cada {REFRESH_MS / 1000} segundos.
          </p>
        </div>
        <p className={`admin-ops__reading admin-ops__reading--${reading.tone}`}>
          <strong>{reading.title}</strong>
          <span>{reading.body}</span>
        </p>
        <ul className="admin-ops__systems">
          {snapshot.systems.map((system, index) => (
            <li
              key={system.name}
              className={`admin-ops__system admin-ops__system--${statusTone(system.status)} reveal`}
              style={{ '--i': index } as CSSProperties}
            >
              <span>{systemName(system.name)}</span>
              <StatusPill status={system.status} />
            </li>
          ))}
        </ul>
        <div className="admin-ops__breaker">
          <span className="admin-ops__kicker">{systemName(circuitBreaker.name)}</span>
          <StatusPill status={circuitBreaker.state} />
          <p className="admin-ops__rate">
            {circuitBreaker.failureRate === null
              ? 'Sin muestras suficientes'
              : `${formatMetric(circuitBreaker.failureRate)} % de fallos`}
          </p>
          <FailureRing percent={circuitBreaker.failureRate} />
        </div>
      </section>

      <details className="admin-ops__more">
        <summary>Pool, hilos y tráfico</summary>
        <div className="admin-ops__more-body">
          <ConcurrencyBoard concurrency={snapshot.concurrency} />
          <TrafficBoard http={snapshot.http} />
        </div>
      </details>
    </div>
  )
}

export function AdminSystem() {
  const query = useQuery({
    queryKey: ['admin', 'system'],
    queryFn: getAdminSystem,
    refetchInterval: REFRESH_MS,
    staleTime: REFRESH_MS,
    refetchOnWindowFocus: false,
  })
  const series = usePurchaseSeries(query.data)

  return (
    <section className="page admin-ops" data-testid="admin-system">
      <div className="admin-ops__heading">
        <div>
          <p className="catalog__eyebrow">Operación</p>
          <h1 className="page__title">Concurrencia y monitoreo</h1>
        </div>
        <Button type="button" variant="ghost" onClick={() => void query.refetch()} loading={query.isPending}>
          Actualizar
        </Button>
      </div>

      <OperationFlow http={query.data?.http} concurrency={query.data?.concurrency} series={series} />

      {query.isPending ? <ListSkeleton variant="rows" count={3} label="Cargando el estado del sistema" /> : null}
      {query.isError ? (
        <EmptyState
          tone="error"
          title="No se pudo leer el estado"
          message={getApiErrorMessage(query.error) ?? 'Intentá de nuevo.'}
        />
      ) : null}
      {query.data ? <SnapshotBoard snapshot={query.data} /> : null}

      <section className="admin-ops__panel" aria-labelledby="admin-ops-consoles">
        <h2 id="admin-ops-consoles">Consolas</h2>
        <ul className="admin-ops__consoles">
          {observabilityConsoles().map((item, index) => (
            <li key={item.id} className="reveal" style={{ '--i': index } as CSSProperties}>
              <a href={item.href} target="_blank" rel="noreferrer">
                <strong>{item.label}</strong>
                <span>{item.detail}</span>
              </a>
            </li>
          ))}
        </ul>
      </section>
    </section>
  )
}
