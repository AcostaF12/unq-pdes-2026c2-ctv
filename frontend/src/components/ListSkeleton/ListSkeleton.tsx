import './ListSkeleton.css'

export type ListSkeletonVariant = 'cards' | 'rows' | 'detail'

interface ListSkeletonProps {
  variant?: ListSkeletonVariant
  count?: number
  withMedia?: boolean
  label?: string
}

const VARIANT_DEFAULTS: Record<
  ListSkeletonVariant,
  { count: number; withMedia: boolean; label: string }
> = {
  cards: { count: 6, withMedia: true, label: 'Cargando listado' },
  rows: { count: 4, withMedia: false, label: 'Cargando listado' },
  detail: { count: 1, withMedia: true, label: 'Cargando detalle' },
}

function variantClassName(variant: ListSkeletonVariant): string {
  switch (variant) {
    case 'cards':
      return 'list-skeleton list-skeleton--cards'
    case 'rows':
      return 'list-skeleton list-skeleton--rows'
    case 'detail':
      return 'list-skeleton list-skeleton--detail'
    default: {
      const exhaustive: never = variant
      return exhaustive
    }
  }
}

export function ListSkeleton({
  variant = 'cards',
  count,
  withMedia,
  label,
}: ListSkeletonProps) {
  const defaults = VARIANT_DEFAULTS[variant]
  const itemCount = count ?? defaults.count
  const showMedia = withMedia ?? defaults.withMedia
  const statusLabel = label ?? defaults.label
  const items = Array.from({ length: itemCount }, (_, index) => index)

  return (
    <ul className={variantClassName(variant)} aria-busy="true" aria-label={statusLabel} role="status">
      {items.map((index) => (
        <li key={index} className="list-skeleton__item" aria-hidden="true">
          {showMedia ? <div className="list-skeleton__media" /> : null}
          <div className="list-skeleton__body">
            <span className="list-skeleton__line list-skeleton__line--title" />
            <span className="list-skeleton__line" />
            <span className="list-skeleton__line list-skeleton__line--short" />
          </div>
        </li>
      ))}
    </ul>
  )
}
