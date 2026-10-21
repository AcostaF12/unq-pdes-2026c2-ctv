import type { ButtonHTMLAttributes } from 'react'
import './FavoriteButton.css'

interface FavoriteButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  active: boolean
  /** `icon` is the round overlay for cards; `labeled` shows the text next to the heart. */
  variant?: 'icon' | 'labeled'
  activeLabel?: string
  inactiveLabel?: string
}

export function HeartIcon({ filled }: { filled: boolean }) {
  return (
    <svg
      className={filled ? 'heart heart--filled' : 'heart'}
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M12 21s-7.5-4.6-9.6-9.2C.9 8.4 2.6 4.8 6.2 4.8c2.1 0 3.6 1.1 4.4 2.4l1.4 2 1.4-2c.8-1.3 2.3-2.4 4.4-2.4 3.6 0 5.3 3.6 3.8 7C19.5 16.4 12 21 12 21z" />
    </svg>
  )
}

export function FavoriteButton({
  active,
  variant = 'icon',
  activeLabel = 'Quitar de favoritos',
  inactiveLabel = 'Agregar a favoritos',
  className,
  ...rest
}: FavoriteButtonProps) {
  const classes = [
    'favorite-btn',
    `favorite-btn--${variant}`,
    active ? 'favorite-btn--active' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ')
  const label = active ? activeLabel : inactiveLabel

  return (
    <button
      type="button"
      className={classes}
      aria-label={variant === 'icon' ? label : undefined}
      title={variant === 'icon' ? label : undefined}
      {...rest}
    >
      {/* Re-keying on `active` replays the pop animation on every toggle. */}
      <span className="favorite-btn__icon" key={String(active)}>
        <HeartIcon filled={active} />
      </span>
      {variant === 'labeled' ? <span className="favorite-btn__text">{label}</span> : null}
    </button>
  )
}
