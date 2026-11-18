import type { PurchaseHistoryPage } from '../../api/purchases'
import { Button } from '../Button/Button'

export function HistoryPagination({
  result,
  onPageChange,
}: {
  result: PurchaseHistoryPage
  onPageChange: (page: number) => void
}) {
  if (result.totalPages < 2) return null
  const goTo = (next: number) => {
    onPageChange(next)
    window.scrollTo?.({ top: 0, behavior: 'smooth' })
  }
  return (
    <nav className="history-pagination" aria-label="Paginación del historial">
      <Button type="button" variant="ghost" disabled={result.first} onClick={() => goTo(result.page - 1)}>
        Anterior
      </Button>
      <span>Página {result.page + 1} de {result.totalPages}</span>
      <Button type="button" variant="ghost" disabled={result.last} onClick={() => goTo(result.page + 1)}>
        Siguiente
      </Button>
    </nav>
  )
}
