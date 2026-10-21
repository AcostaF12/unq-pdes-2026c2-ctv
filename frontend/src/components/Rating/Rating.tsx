import { formatScore } from '../../lib/format'
import './Rating.css'

interface RatingProps {
  /** Average on a 0-10 scale; null/undefined renders the "no reviews" state. */
  score: number | null | undefined
  count?: number
  size?: 'sm' | 'md'
  className?: string
}

const STARS = '★★★★★'

export function Rating({ score, count, size = 'sm', className }: RatingProps) {
  const classes = ['rating', `rating--${size}`, className].filter(Boolean).join(' ')

  if (score == null) {
    return <p className={`${classes} rating--empty`}>Sin reseñas todavía</p>
  }

  const percent = Math.max(0, Math.min(100, (score / 10) * 100))
  const countLabel =
    count == null ? '' : ` · ${count} ${count === 1 ? 'reseña' : 'reseñas'}`

  return (
    <p className={classes} aria-label={`Puntaje ${formatScore(score)} de 10${countLabel}`}>
      <span className="rating__stars" aria-hidden="true">
        <span className="rating__stars-base">{STARS}</span>
        <span className="rating__stars-fill" style={{ width: `${percent}%` }}>
          {STARS}
        </span>
      </span>
      <span className="rating__value" aria-hidden="true">
        {formatScore(score)}
        <span className="rating__count">{countLabel}</span>
      </span>
    </p>
  )
}
