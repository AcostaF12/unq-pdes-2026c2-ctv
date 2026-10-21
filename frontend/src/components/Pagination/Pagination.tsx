import { Button } from '../Button/Button'
import './Pagination.css'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  const goTo = (next: number) => {
    onPageChange(next)
    window.scrollTo?.({ top: 0, behavior: 'smooth' })
  }

  return (
    <nav className="pagination" aria-label="Paginación de resultados">
      <Button type="button" variant="ghost" disabled={page === 0} onClick={() => goTo(page - 1)}>
        Anterior
      </Button>
      <span className="pagination__status">
        Página {page + 1} de {totalPages}
      </span>
      <Button
        type="button"
        variant="ghost"
        disabled={page + 1 >= totalPages}
        onClick={() => goTo(page + 1)}
      >
        Siguiente
      </Button>
    </nav>
  )
}
