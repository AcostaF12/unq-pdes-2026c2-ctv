import type { Flight } from '../api/packages'

export function formatFlightWhen(date: string, time: string): string {
  const stamp = new Date(`${date}T${time}`)
  if (Number.isNaN(stamp.getTime())) {
    return `${date} · ${time.slice(0, 5)}`
  }
  return stamp.toLocaleString('es-AR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  })
}

export function formatShortDate(date: string): string {
  const stamp = new Date(`${date}T00:00:00`)
  if (Number.isNaN(stamp.getTime())) {
    return date
  }
  return stamp.toLocaleDateString('es-AR', { day: '2-digit', month: 'short', year: 'numeric' })
}

export function formatPurchaseDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString('es-AR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export function formatPurchaseDateTime(isoDate: string): string {
  return new Date(isoDate).toLocaleString('es-AR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function confirmationCode(purchaseId: number | null | undefined): string {
  return `CTV-${String(purchaseId ?? 0).padStart(6, '0')}`
}

export function flightNumber(flight: Pick<Flight, 'id' | 'airline'>): string {
  const prefix = flight.airline
    .split(/\s+/)
    .map((word) => word[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
  return `${prefix || 'CT'}${String(flight.id).padStart(3, '0')}`
}

export function tripNights(outboundDate: string, returnDate: string): number | null {
  const start = new Date(`${outboundDate}T00:00:00`).getTime()
  const end = new Date(`${returnDate}T00:00:00`).getTime()
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) {
    return null
  }
  return Math.round((end - start) / 86_400_000)
}

export function seatsLabel(availability: number): { text: string; tone: 'ok' | 'low' | 'out' } {
  if (availability <= 0) {
    return { text: 'Sin asientos', tone: 'out' }
  }
  if (availability <= 12) {
    return {
      text: `¡Últimos ${availability} ${availability === 1 ? 'asiento' : 'asientos'}!`,
      tone: 'low',
    }
  }
  return { text: `${availability} asientos disponibles`, tone: 'ok' }
}

export function formatScore(score: number): string {
  return score.toFixed(1)
}
