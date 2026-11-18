import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import type { TravelPackage } from '../../api/packages'
import { PackageCard } from './PackageCard'

const base: TravelPackage = {
  id: 10,
  name: 'París Romántico',
  price: 1500,
  agency: { id: 1, name: 'Despegar' },
  hotel: {
    id: 2,
    name: 'Hotel Palacio de París',
    photoUrl: 'https://example.com/par.jpg',
    city: { code: 'PAR', name: 'Paris' },
  },
  origin: { code: 'BUE', name: 'Buenos Aires' },
  destination: { code: 'PAR', name: 'Paris' },
  outboundFlightId: 1,
  returnFlightId: 2,
}

function renderCard(travelPackage: TravelPackage, favorite?: Parameters<typeof PackageCard>[0]['favorite']) {
  return render(
    <MemoryRouter>
      <PackageCard travelPackage={travelPackage} favorite={favorite} />
    </MemoryRouter>,
  )
}

describe('PackageCard', () => {
  it('links to the detail and shows route, hotel, agency, rating and price', () => {
    renderCard({ ...base, averageScore: 7.5, reviewCount: 4 })

    expect(screen.getByRole('link')).toHaveAttribute('href', '/trips/10')
    expect(screen.getByText('Buenos Aires → Paris')).toBeInTheDocument()
    expect(screen.getByText('Hotel Palacio de París')).toBeInTheDocument()
    expect(screen.getByText('Despegar')).toBeInTheDocument()
    expect(screen.getByLabelText('Puntaje 7.5 de 10 · 4 reseñas')).toBeInTheDocument()
    expect(screen.getByText('USD 1500')).toBeInTheDocument()
    expect(screen.queryByText('★ Destacado')).not.toBeInTheDocument()
  })

  it('shows the empty rating state when there are no reviews', () => {
    renderCard(base)

    expect(screen.getByText('Sin reseñas todavía')).toBeInTheDocument()
  })

  it('highlights packages with an excellent score backed by several reviews', () => {
    renderCard({ ...base, averageScore: 9.4, reviewCount: 3 })

    expect(screen.getByText('★ Destacado')).toBeInTheDocument()
  })

  it('does not highlight an excellent score backed by a single review', () => {
    renderCard({ ...base, averageScore: 10, reviewCount: 1 })

    expect(screen.queryByText('★ Destacado')).not.toBeInTheDocument()
  })

  it('renders the heart only when favorites are available and reports toggles', async () => {
    const user = userEvent.setup()
    const onToggle = vi.fn()
    const { unmount } = renderCard(base)
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    unmount()

    renderCard(base, { active: false, pending: false, onToggle })
    await user.click(screen.getByRole('button', { name: 'Agregar a favoritos' }))
    expect(onToggle).toHaveBeenCalledTimes(1)
  })

  it('disables the heart while a toggle is pending', () => {
    renderCard(base, { active: true, pending: true, onToggle: vi.fn() })

    expect(screen.getByRole('button', { name: 'Quitar de favoritos' })).toBeDisabled()
  })
})
