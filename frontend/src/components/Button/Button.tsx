import type { ButtonHTMLAttributes } from 'react'
import './Button.css'

type ButtonVariant = 'primary' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  loading?: boolean
}

export function Button({
  variant = 'primary',
  loading = false,
  disabled,
  children,
  className,
  ...rest
}: ButtonProps) {
  const classes = ['btn', `btn--${variant}`, className].filter(Boolean).join(' ')

  return (
    <button className={classes} disabled={disabled || loading} {...rest}>
      {loading ? 'CARGANDO…' : children}
    </button>
  )
}
