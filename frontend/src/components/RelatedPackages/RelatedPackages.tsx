import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { searchPackages, type PackageSearchParams } from '../../api/packages'
import { queryKeys } from '../../query/keys'
import { PackageCard } from '../PackageCard/PackageCard'
import './RelatedPackages.css'

interface RelatedPackagesProps {
  title: string
  /** Server-side filter that picks the related trips (destination, hotel...). */
  filters: Pick<PackageSearchParams, 'destination' | 'hotel'>
  excludeId?: number
  /** Where "Ver todos" leads; omitted when there is nothing broader to browse. */
  seeAllTo?: string
  limit?: number
}

export function RelatedPackages({ title, filters, excludeId, seeAllTo, limit = 3 }: RelatedPackagesProps) {
  // One extra row so that excluding the current package still leaves `limit` cards.
  const query = { ...filters, size: limit + 1 }
  const relatedQuery = useQuery({
    queryKey: queryKeys.packages.search(query),
    queryFn: () => searchPackages(query),
  })

  const items = (relatedQuery.data?.content ?? []).filter((item) => item.id !== excludeId).slice(0, limit)
  if (items.length === 0) {
    return null
  }

  return (
    <section className="related" aria-label={title}>
      <header className="related__head">
        <h2>{title}</h2>
        {seeAllTo ? (
          <Link className="related__all" to={seeAllTo}>
            Ver todos →
          </Link>
        ) : null}
      </header>
      <ul className="catalog__grid">
        {items.map((item, index) => (
          <li key={item.id}>
            <PackageCard travelPackage={item} index={index} />
          </li>
        ))}
      </ul>
    </section>
  )
}
