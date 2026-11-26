const numberFormat = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })

export function systemName(name: string): string {
  switch (name) {
    case 'backend':
      return 'Backend'
    case 'database':
      return 'Base de datos'
    case 'flights':
      return 'Vuelos'
    case 'flightsService':
      return 'Servicio de vuelos'
    default:
      return name
  }
}

export function statusLabel(status: string): string {
  switch (status) {
    case 'UP':
      return 'En línea'
    case 'DOWN':
      return 'Caído'
    case 'OUT_OF_SERVICE':
      return 'Fuera de servicio'
    case 'UNKNOWN':
      return 'Sin dato'
    case 'CLOSED':
      return 'Cerrado'
    case 'OPEN':
      return 'Abierto'
    case 'HALF_OPEN':
      return 'Probando'
    case 'DISABLED':
      return 'Desactivado'
    case 'FORCED_OPEN':
      return 'Forzado abierto'
    case 'METRICS_ONLY':
      return 'Solo métricas'
    default:
      return status
  }
}

export function statusTone(status: string): 'ok' | 'bad' | 'wait' {
  switch (status) {
    case 'UP':
    case 'CLOSED':
      return 'ok'
    case 'DOWN':
    case 'OPEN':
    case 'FORCED_OPEN':
    case 'OUT_OF_SERVICE':
      return 'bad'
    default:
      return 'wait'
  }
}

export function formatMetric(value: number | null): string {
  if (value === null || Number.isNaN(value)) {
    return '—'
  }
  return numberFormat.format(value)
}
