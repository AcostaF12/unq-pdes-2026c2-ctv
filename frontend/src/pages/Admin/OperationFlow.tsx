import type { CSSProperties, ReactNode } from 'react'
import type { AdminConcurrency, AdminHttpEndpoint, AdminHttpTraffic } from '../../api/admin'
import { formatMetric } from './systemLabels'

const P95_SCALE_MS = 1000

const PURCHASE_STEPS = [
  {
    title: 'Leer el paquete',
    lane: 'Leer',
    detail: 'Sin lock. Publicado y agencia activa.',
    weight: 14,
  },
  {
    title: 'Vender la ida',
    lane: 'Ida',
    detail: 'Lock de la fila del vuelo. Ahí se frena la sobreventa.',
    weight: 42,
  },
  {
    title: 'Vender la vuelta',
    lane: 'Vuelta',
    detail: 'Si falla, se cancela la ida y no se guarda la compra.',
    weight: 28,
  },
  {
    title: 'Guardar',
    lane: 'Ticket',
    detail: 'Lock corto. Si cambió, se cancelan las dos ventas.',
    weight: 16,
  },
] as const

const CANCEL_STEPS = [
  {
    title: 'Quién y cuándo',
    detail: 'Comprador o admin, antes de que parta la ida.',
  },
  {
    title: 'Devolver asientos',
    detail: 'Un 404 de una venta ya devuelta cuenta como liberada.',
  },
  {
    title: 'Marcar cancelada',
    detail: 'Una sola transacción gana. La otra recibe 400.',
  },
] as const

const STRESS_CASES = [
  {
    name: 'last-seat-stress',
    load: '5 lugares, 12 compradores',
    expects: '5 compras. El resto se rechaza.',
  },
  {
    name: 'purchase-throughput',
    load: '8 lugares, 6 compradores',
    expects: 'Las 6 responden 201.',
  },
  {
    name: 'cancel-rebuy',
    load: '5 lugares',
    expects: 'El stock final coincide con las activas.',
  },
  {
    name: 'double-cancel',
    load: '20 cancelaciones',
    expects: 'Una 204, el resto 400. Un lugar por vuelo.',
  },
] as const

const VM_CASES = [
  {
    name: 'catalog-load',
    load: '20 usuarios, 2 min',
    expects: 'Búsqueda sostenida del catálogo.',
  },
  {
    name: 'login-load',
    load: '10 usuarios, 1 min',
    expects: 'Login sostenido.',
  },
  {
    name: 'last-seat-stress',
    load: '5 lugares, 30 compradores',
    expects: '5 compras. El resto se rechaza.',
  },
  {
    name: 'catalog-spike',
    load: 'de 5 a 50 usuarios',
    expects: 'Pico de unos 3 minutos y vuelta a la base.',
  },
] as const

const DEMO_PACKAGES = [
  { name: 'Nueva York Navidad', seats: '4 y 4' },
  { name: 'Río Playa y Samba', seats: '8 y 12' },
  { name: 'Tokio Neón', seats: '3 y 9' },
  { name: 'Dubái Lujo en el Desierto', seats: '2 y 6' },
] as const

function purchaseEndpoint(http: AdminHttpTraffic | undefined) {
  return http?.endpoints.find((endpoint) => endpoint.method === 'POST' && endpoint.uri === '/purchases')
}

type ReadingTone = 'ok' | 'wait' | 'bad'

function purchaseReading(
  purchase: AdminHttpEndpoint | undefined,
  pending: number | null | undefined,
): { tone: ReadingTone; title: string; body: string } {
  if (pending != null && pending > 0) {
    return {
      tone: 'bad',
      title: 'El pool está en cola',
      body: 'Hay conexiones pendientes. Si ese número se separa de cero, el límite es el pool, no la red.',
    }
  }
  if (!purchase || purchase.p95Millis === null) {
    return {
      tone: 'wait',
      title: 'Todavía no hay una compra medida',
      body: 'El p95 aparece con el primer POST /purchases de este proceso.',
    }
  }
  if (purchase.serverErrors > 0) {
    return {
      tone: 'bad',
      title: 'La compra está devolviendo 5xx',
      body: 'Esos errores no son un sin lugar. Un sin lugar es 400 y no se reintenta.',
    }
  }
  if (purchase.p95Millis >= 500) {
    return {
      tone: 'wait',
      title: 'La compra está lenta',
      body: 'Ida y vuelta se venden en serie. Por encima de 500 ms suele ser la cola del lock del vuelo.',
    }
  }
  if (purchase.p95Millis >= 200) {
    return {
      tone: 'wait',
      title: 'El tiempo está en las dos ventas',
      body: 'El catálogo no es el cuello. Este p95 es la ida, más la vuelta, más el insert.',
    }
  }
  return {
    tone: 'ok',
    title: 'La compra está ágil',
    body: 'p95 por debajo de 200 ms. El lock del último asiento puede subirlo si varios piden el mismo vuelo.',
  }
}

const ARC_R = 52
const ARC_LEN = Math.PI * ARC_R

function gaugeTone(millis: number | null): 'ok' | 'wait' | 'bad' | 'empty' {
  if (millis === null) return 'empty'
  if (millis < 200) return 'ok'
  if (millis < 500) return 'wait'
  return 'bad'
}

function LatencyGauge({ millis }: { millis: number | null }) {
  const ratio = millis === null ? 0 : Math.min(millis / P95_SCALE_MS, 1)
  const rest = ARC_LEN * (1 - ratio)
  return (
    <figure className={`ops-arc ops-arc--${gaugeTone(millis)}`}>
      <svg viewBox="0 0 140 88" aria-hidden="true">
        <path className="ops-arc__track" d="M 18 76 A 52 52 0 0 1 122 76" />
        <path
          className="ops-arc__value"
          d="M 18 76 A 52 52 0 0 1 122 76"
          style={{ ['--len' as string]: `${ARC_LEN}`, ['--rest' as string]: `${rest}` }}
        />
      </svg>
      <figcaption>
        <span>0</span>
        <span>500</span>
        <span>1000 ms</span>
      </figcaption>
    </figure>
  )
}

function WeightChart() {
  const width = 1000
  const segments = PURCHASE_STEPS.reduce<Array<{ step: (typeof PURCHASE_STEPS)[number]; index: number; seg: number; x: number }>>(
    (placed, step, index) => {
      const seg = (step.weight / 100) * width
      const x = placed.reduce((sum, item) => sum + item.seg, 0)
      return [...placed, { step, index, seg, x }]
    },
    [],
  )
  return (
    <svg className="ops-weight" viewBox={`0 0 ${width} 56`} role="img" aria-label="Peso habitual del camino de compra">
      <line className="ops-weight__axis" x1="0" y1="20" x2={width} y2="20" />
      {segments.map(({ step, index, seg, x }) => (
        <g key={step.title} className={`ops-weight__seg ops-weight__seg--${index + 1}`}>
          <rect x={x + 6} y="14" width={Math.max(seg - 12, 0)} height="6" rx="3" />
          <text x={x + seg / 2} y="46" textAnchor="middle">
            {step.lane}
          </text>
        </g>
      ))}
    </svg>
  )
}

function Sparkline({ values }: { values: number[] }) {
  const width = 320
  const height = 72
  const max = Math.max(200, ...values, 0)
  const points = values.map((value, index) => {
    const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width
    const y = height - 10 - (value / max) * (height - 20)
    return { x, y }
  })
  const line = points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ')
  const area = points.length > 1 ? `${line} L ${width} ${height} L 0 ${height} Z` : ''
  return (
    <figure className="ops-spark">
      <svg viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
        <line className="ops-spark__base" x1="0" y1={height - 10} x2={width} y2={height - 10} />
        {area === '' ? null : <path className="ops-spark__area" d={area} />}
        {points.length > 1 ? <path className="ops-spark__line" d={line} /> : null}
        {points.map((point) => (
          <circle key={`${point.x}-${point.y}`} className="ops-spark__dot" cx={point.x} cy={point.y} r="3.5" />
        ))}
      </svg>
      <figcaption>p95 de esta sesión</figcaption>
    </figure>
  )
}

function ScenarioGrid({
  caption,
  cases,
}: {
  caption: string
  cases: ReadonlyArray<{ name: string; load: string; expects: string }>
}) {
  return (
    <div className="admin-ops__cases">
      <h4>{caption}</h4>
      <ul>
        {cases.map((scenario) => (
          <li key={`${caption}-${scenario.name}`}>
            <strong>{scenario.name}</strong>
            <span>{scenario.load}</span>
            <p>{scenario.expects}</p>
          </li>
        ))}
      </ul>
    </div>
  )
}

function FlowList({
  steps,
  label,
  columns = 'four',
}: {
  steps: ReadonlyArray<{ title: string; detail: string; weight?: number }>
  label: string
  columns?: 'three' | 'four'
}) {
  return (
    <ol className={`admin-ops__flow admin-ops__flow--${columns}`} aria-label={label}>
      {steps.map((step, index) => (
        <li key={step.title} className="admin-ops__flow-step reveal" style={{ '--i': index } as CSSProperties}>
          <span className="admin-ops__flow-index">{index + 1}</span>
          <strong>{step.title}</strong>
          <span className="admin-ops__flow-detail">{step.detail}</span>
        </li>
      ))}
    </ol>
  )
}

function More({ title, children }: { title: string; children: ReactNode }) {
  return (
    <details className="admin-ops__more">
      <summary>{title}</summary>
      <div className="admin-ops__more-body">{children}</div>
    </details>
  )
}

export function OperationFlow({
  http,
  concurrency,
  series = [],
}: {
  http?: AdminHttpTraffic
  concurrency?: AdminConcurrency
  series?: number[]
}) {
  const purchase = purchaseEndpoint(http)
  const p95 = purchase ? `${formatMetric(purchase.p95Millis)} ms` : null
  const reading = http ? purchaseReading(purchase, concurrency?.poolPending) : null

  return (
    <section className="admin-ops__panel admin-ops__panel--flow" aria-labelledby="admin-ops-flow">
      <header className="admin-ops__panel-head">
        <h2 id="admin-ops-flow">Flujo de una compra</h2>
        <p>La ida y la vuelta se venden antes del lock del paquete. El stress no gasta el catálogo de demo.</p>
      </header>

      <WeightChart />
      <p className="admin-ops__lane-note">
        El ancho es el peso habitual de cada paso. El número de este proceso es el p95.
      </p>

      <FlowList steps={PURCHASE_STEPS} label="Pasos de la compra" />

      {reading ? (
        <div className="admin-ops__readout">
          <LatencyGauge millis={purchase?.p95Millis ?? null} />
          <div className="admin-ops__readout-copy">
            <dl className="admin-ops__live" aria-label="Lectura en vivo de la compra">
              <div>
                <dt>p95 de POST /purchases</dt>
                <dd>{p95 ?? 'Sin muestras'}</dd>
              </div>
              <div>
                <dt>Compras medidas</dt>
                <dd>{purchase ? formatMetric(purchase.requestCount) : '0'}</dd>
              </div>
              <div>
                <dt>Conexiones pendientes</dt>
                <dd>{formatMetric(concurrency?.poolPending ?? null)}</dd>
              </div>
            </dl>
            <p className={`admin-ops__reading admin-ops__reading--${reading.tone}`}>
              <strong>{reading.title}</strong>
              <span>{reading.body}</span>
            </p>
            {series.length === 0 ? null : <Sparkline values={series} />}
          </div>
        </div>
      ) : (
        <p className="admin-ops__empty">El p95 aparece cuando el proceso ya midió compras.</p>
      )}

      <More title="Cancelación, estrés y demo">
        <h3>Cancelar</h3>
        <FlowList steps={CANCEL_STEPS} label="Pasos de la cancelación" columns="three" />
        <h3>Suite corta</h3>
        <p className="admin-ops__empty">
          <span className="admin-ops__code">npm run test:stress</span> y Kubernetes, con dos réplicas.
        </p>
        <ScenarioGrid caption="Suite corta, en local y en Kubernetes" cases={STRESS_CASES} />
        <h3>Load Tests en la VM</h3>
        <p className="admin-ops__empty">El workflow contra la VM. El catálogo y el pico no corren en kind.</p>
        <ScenarioGrid caption="Load Tests contra la VM" cases={VM_CASES} />
        <div className="admin-ops__demo">
          <p>
            Demo con <span className="admin-ops__code">buyer</span> / <span className="admin-ops__code">buyer123</span>.
            Pocos lugares; el resto sale lleno.
          </p>
          <ul className="admin-ops__seed">
            {DEMO_PACKAGES.map((item) => (
              <li key={item.name}>
                <strong>{item.name}</strong>
                <span>{item.seats}</span>
              </li>
            ))}
          </ul>
        </div>
      </More>
    </section>
  )
}
