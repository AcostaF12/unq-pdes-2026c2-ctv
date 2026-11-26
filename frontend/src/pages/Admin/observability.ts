interface ObservabilityConsole {
  id: string
  label: string
  detail: string
  href: string
}

const PUBLISHED_PORTS = {
  grafana: '3001',
  kibana: '5601',
  zipkin: '9411',
  prometheus: '9090',
} as const

function configuredUrl(value: unknown): string | undefined {
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return trimmed === '' ? undefined : trimmed
}

function consoleHref(configured: unknown, port: string): string {
  const override = configuredUrl(configured)
  if (override) {
    return override
  }
  const { protocol, hostname } = window.location
  return `${protocol}//${hostname}:${port}`
}

export function observabilityConsoles(): ObservabilityConsole[] {
  return [
    {
      id: 'grafana',
      label: 'Grafana',
      detail: 'Requests, p95, pool de conexiones, circuit breaker y memoria.',
      href: consoleHref(import.meta.env.VITE_GRAFANA_URL, PUBLISHED_PORTS.grafana),
    },
    {
      id: 'kibana',
      label: 'Kibana',
      detail: 'Logs de backend y vuelos, con traceId para cruzar una traza.',
      href: consoleHref(import.meta.env.VITE_KIBANA_URL, PUBLISHED_PORTS.kibana),
    },
    {
      id: 'zipkin',
      label: 'Zipkin',
      detail: 'La llamada del backend al servicio de vuelos, como una sola traza.',
      href: consoleHref(import.meta.env.VITE_ZIPKIN_URL, PUBLISHED_PORTS.zipkin),
    },
    {
      id: 'prometheus',
      label: 'Prometheus',
      detail: 'Series de los dashboards. En la VM este puerto no se publica.',
      href: consoleHref(import.meta.env.VITE_PROMETHEUS_URL, PUBLISHED_PORTS.prometheus),
    },
  ]
}
