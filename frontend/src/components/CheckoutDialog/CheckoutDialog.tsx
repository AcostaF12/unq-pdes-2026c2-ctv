import { useEffect, useRef, type KeyboardEvent } from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import type { TravelPackageDetail } from '../../api/packages'
import type { Purchase } from '../../api/purchases'
import { confirmationCode, formatFlightWhen, seatsLabel } from '../../lib/format'
import { Button } from '../Button/Button'
import './CheckoutDialog.css'

export type CheckoutStatus = 'review' | 'processing' | 'success'

interface CheckoutDialogProps {
  detail: TravelPackageDetail
  passengerName: string
  status: CheckoutStatus
  purchase?: Purchase
  error?: string | null
  onConfirm: () => void
  onClose: () => void
}

const STEPS = ['Reservando asiento de ida', 'Reservando asiento de vuelta', 'Confirmando tu compra']

const CONFETTI = Array.from({ length: 18 }, (_, index) => ({
  left: `${(index * 53) % 100}%`,
  delay: `${(index % 6) * 90}ms`,
  hue: index % 3,
  drift: `${((index * 37) % 60) - 30}px`,
}))

const FOCUSABLE = 'a[href], button:not(:disabled), [tabindex]:not([tabindex="-1"])'

export function CheckoutDialog({
  detail,
  passengerName,
  status,
  purchase,
  error,
  onConfirm,
  onClose,
}: CheckoutDialogProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  const returnFocusRef = useRef<HTMLElement | null>(null)
  const busy = status === 'processing'
  const soldOut = [detail.outboundFlight, detail.returnFlight].some(
    (flight) => flight != null && flight.availability <= 0,
  )

  useEffect(() => {
    returnFocusRef.current = document.activeElement as HTMLElement | null
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
      returnFocusRef.current?.focus?.()
    }
  }, [])

  // Move focus into the dialog whenever its content changes phase.
  useEffect(() => {
    const target = dialogRef.current?.querySelector<HTMLElement>('[data-autofocus]')
    ;(target ?? dialogRef.current)?.focus()
  }, [status])

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape' && !busy) {
      event.stopPropagation()
      onClose()
      return
    }
    if (event.key !== 'Tab' || !dialogRef.current) {
      return
    }
    const items = Array.from(dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE))
    if (items.length === 0) {
      return
    }
    const first = items[0]
    const last = items[items.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return createPortal(
    <div
      className="checkout__backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !busy) {
          onClose()
        }
      }}
    >
      <div
        ref={dialogRef}
        className={`checkout checkout--${status}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="checkout-title"
        tabIndex={-1}
        onKeyDown={onKeyDown}
      >
        {status === 'review' ? (
          <>
            <p className="checkout__eyebrow">Último paso</p>
            <h2 id="checkout-title" className="checkout__title">
              Confirmá tu compra
            </h2>
            <dl className="checkout__summary">
              <div>
                <dt>Paquete</dt>
                <dd>{detail.name}</dd>
              </div>
              <div>
                <dt>Ruta</dt>
                <dd>
                  {detail.origin.name} → {detail.destination.name}
                </dd>
              </div>
              <div>
                <dt>Hotel</dt>
                <dd>
                  {detail.hotel.name} · {detail.agency.name}
                </dd>
              </div>
              {detail.outboundFlight ? (
                <div>
                  <dt>Ida</dt>
                  <dd>
                    {formatFlightWhen(detail.outboundFlight.flightDate, detail.outboundFlight.departureTime)}
                    <span className={`checkout__seats checkout__seats--${seatsLabel(detail.outboundFlight.availability).tone}`}>
                      {seatsLabel(detail.outboundFlight.availability).text}
                    </span>
                  </dd>
                </div>
              ) : null}
              {detail.returnFlight ? (
                <div>
                  <dt>Vuelta</dt>
                  <dd>
                    {formatFlightWhen(detail.returnFlight.flightDate, detail.returnFlight.departureTime)}
                    <span className={`checkout__seats checkout__seats--${seatsLabel(detail.returnFlight.availability).tone}`}>
                      {seatsLabel(detail.returnFlight.availability).text}
                    </span>
                  </dd>
                </div>
              ) : null}
              <div>
                <dt>Pasajero</dt>
                <dd>{passengerName}</dd>
              </div>
            </dl>
            <p className="checkout__total">
              <span>Total a pagar</span>
              <strong>USD {detail.price}</strong>
            </p>
            {error ? (
              <p className="catalog__error" role="alert">
                {error}
              </p>
            ) : null}
            {soldOut ? (
              <p className="catalog__error" role="alert">
                Uno de los vuelos ya no tiene asientos disponibles.
              </p>
            ) : null}
            <div className="checkout__actions">
              <Button type="button" onClick={onConfirm} disabled={soldOut} data-autofocus>
                Confirmar compra
              </Button>
              <Button type="button" variant="ghost" onClick={onClose}>
                Cancelar
              </Button>
            </div>
          </>
        ) : null}

        {status === 'processing' ? (
          <div className="checkout__processing" role="status" aria-live="polite">
            <h2 id="checkout-title" className="checkout__title">
              Procesando tu compra
            </h2>
            <div className="checkout__runway" aria-hidden="true">
              <span className="checkout__runway-line" />
              <span className="checkout__runway-plane">✈</span>
            </div>
            <ol className="checkout__steps">
              {STEPS.map((step, index) => (
                <li key={step} className="checkout__step" style={{ ['--step' as string]: index }}>
                  <span className="checkout__step-dot" aria-hidden="true" />
                  {step}
                </li>
              ))}
            </ol>
            <p className="catalog__muted">No cierres esta ventana.</p>
          </div>
        ) : null}

        {status === 'success' && purchase ? (
          <div className="checkout__success">
            <div className="checkout__confetti" aria-hidden="true">
              {CONFETTI.map((piece, index) => (
                <span
                  key={index}
                  className={`checkout__confetti-piece checkout__confetti-piece--${piece.hue}`}
                  style={{ left: piece.left, animationDelay: piece.delay, ['--drift' as string]: piece.drift }}
                />
              ))}
            </div>
            <p className="checkout__eyebrow">¡Listo, {passengerName.split(' ')[0]}!</p>
            <h2 id="checkout-title" className="checkout__title">
              Compra confirmada
            </h2>
            <p className="checkout__message">
              Compra realizada. Los asientos de ida y vuelta quedaron reservados.
            </p>
            <div className="boarding-pass">
              <div className="boarding-pass__main">
                <p className="boarding-pass__label">Código de reserva</p>
                <p className="boarding-pass__code">{confirmationCode(purchase.id)}</p>
                <div className="boarding-pass__route" aria-hidden="true">
                  <span>{detail.origin.code}</span>
                  <span className="boarding-pass__plane">✈</span>
                  <span>{detail.destination.code}</span>
                </div>
                <p className="boarding-pass__name">{detail.name}</p>
                <p className="boarding-pass__meta">Pasajero: {passengerName}</p>
              </div>
              <div className="boarding-pass__stub">
                <p className="boarding-pass__label">Abonado</p>
                <p className="boarding-pass__price">USD {purchase.purchasePrice}</p>
                <span className="boarding-pass__barcode" aria-hidden="true" />
              </div>
              <span className="boarding-pass__stamp" aria-hidden="true">
                Confirmada
              </span>
            </div>
            <div className="checkout__actions">
              <Link className="empty-state__cta" to={`/purchases/${purchase.id}`} data-autofocus>
                Ver detalle de mi compra
              </Link>
              <Button type="button" variant="ghost" onClick={onClose}>
                Seguir explorando
              </Button>
            </div>
          </div>
        ) : null}
      </div>
    </div>,
    document.body,
  )
}
