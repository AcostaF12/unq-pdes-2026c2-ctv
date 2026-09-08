import { ListSkeleton } from '../ListSkeleton/ListSkeleton'
import './PageFallback.css'

export function PageFallback() {
  return (
    <section className="page-fallback">
      <ListSkeleton variant="cards" count={3} label="Cargando página" />
    </section>
  )
}
