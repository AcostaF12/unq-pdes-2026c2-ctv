import type { ReactNode } from 'react'
import './EmptyState.css'

type EmptyStateTone = 'empty' | 'error'

interface EmptyStateProps {
  title: string
  message: string
  action?: ReactNode
  illustration?: ReactNode
  className?: string
  tone?: EmptyStateTone
  eyebrow?: string
}

function ErrorScene() {
  return (
    <div className="empty-state__scene" aria-hidden="true">
      <svg className="empty-state__route" viewBox="0 0 240 56" fill="none">
        <path
          className="empty-state__route-line"
          d="M12 32 H92"
          stroke="var(--color-ink)"
          strokeWidth="3"
        />
        <circle cx="108" cy="32" r="5" fill="var(--color-secondary)" stroke="var(--color-ink)" strokeWidth="3" />
        <path
          className="empty-state__route-line empty-state__route-line--broken"
          d="M124 32 H228"
          stroke="var(--color-ink)"
          strokeWidth="3"
          strokeDasharray="7 9"
        />
      </svg>
      <span className="empty-state__stamp">Retraso</span>
    </div>
  )
}

export function EmptyState({
  title,
  message,
  action,
  illustration,
  className,
  tone = 'empty',
  eyebrow,
}: EmptyStateProps) {
  const classes = ['empty-state', `empty-state--${tone}`, className].filter(Boolean).join(' ')
  const scene = illustration ?? (tone === 'error' ? <ErrorScene /> : null)
  const errorEyebrow = eyebrow ?? (tone === 'error' ? 'Señal perdida' : undefined)

  return (
    <div className={classes} role={tone === 'error' ? 'alert' : 'status'}>
      {scene ? <div className="empty-state__illustration">{scene}</div> : null}
      {errorEyebrow ? <p className="empty-state__eyebrow">{errorEyebrow}</p> : null}
      <h2 className="empty-state__title">{title}</h2>
      <p className="empty-state__message">{message}</p>
      {action ? <div className="empty-state__action">{action}</div> : null}
    </div>
  )
}
