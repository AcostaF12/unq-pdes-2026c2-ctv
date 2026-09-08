import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ListSkeleton } from './ListSkeleton'

describe('ListSkeleton', () => {
  it('renders a status label and the default amount of card items', () => {
    render(<ListSkeleton />)

    expect(screen.getByRole('status', { name: 'Cargando listado' })).toBeInTheDocument()
    expect(screen.getByRole('status').querySelectorAll('.list-skeleton__item')).toHaveLength(6)
    expect(screen.getByRole('status').querySelectorAll('.list-skeleton__media')).toHaveLength(6)
  })

  it('renders row items without media by default', () => {
    render(<ListSkeleton variant="rows" count={3} label="Cargando compras" />)

    expect(screen.getByRole('status', { name: 'Cargando compras' })).toBeInTheDocument()
    expect(screen.getByRole('status').querySelectorAll('.list-skeleton__item')).toHaveLength(3)
    expect(screen.getByRole('status').querySelectorAll('.list-skeleton__media')).toHaveLength(0)
  })

  it('renders a single detail skeleton with media', () => {
    render(<ListSkeleton variant="detail" />)

    expect(screen.getByRole('status', { name: 'Cargando detalle' })).toBeInTheDocument()
    expect(screen.getByRole('status').querySelectorAll('.list-skeleton__item')).toHaveLength(1)
    expect(screen.getByRole('status').querySelectorAll('.list-skeleton__media')).toHaveLength(1)
  })
})
