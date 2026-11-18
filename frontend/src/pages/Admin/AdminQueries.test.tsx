import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { getAdminUsers, getAdminFavorites, getAdminReviews, getAdminPurchases } from '../../api/admin'
import { createTestQueryClient } from '../../test/queryClient'
import { apiError, parisPackage } from '../../test/fixtures'
import { AdminUsers, AdminFavorites, AdminReviews, AdminPurchases } from './AdminQueries'

vi.mock('../../api/admin', () => ({ getAdminUsers: vi.fn(), getAdminFavorites: vi.fn(), getAdminReviews: vi.fn(), getAdminPurchases: vi.fn() }))
const buyer = { id: 1, username: 'buyer', role: 'BUYER', firstName: 'Ana', lastName: 'Perez' }
function renderPage(element: React.ReactNode) {
  return render(<QueryClientProvider client={createTestQueryClient()}><MemoryRouter>{element}</MemoryRouter></QueryClientProvider>)
}
function page<T>(content: T[]) { return { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0 } }
describe('Administrative queries', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getAdminUsers).mockResolvedValue(page([buyer]))
    vi.mocked(getAdminFavorites).mockResolvedValue(page([{ id: 1, buyerUsername: 'buyer', packageId: 10, packageName: 'Paris favorito', agencyName: 'Agencia Sur' }]))
    vi.mocked(getAdminReviews).mockResolvedValue(page([{ id: 1, buyerUsername: 'buyer', packageId: 10, packageName: 'Paris reseñado', score: 9, comment: 'Excelente viaje' }]))
    vi.mocked(getAdminPurchases).mockResolvedValue(page([{ id: 1, buyer, travelPackage: { ...parisPackage, name: 'Viaje histórico' }, purchasePrice: 1234, purchasedAt: '2026-10-20T12:00:00' }]))
  })
  it('shows users without write actions', async () => {
    renderPage(<AdminUsers />)
    expect(await screen.findByText('buyer')).toBeInTheDocument()
    expect(screen.getByText('Ana Perez')).toBeInTheDocument()
    expect(screen.getByText('Comprador')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar|Eliminar|Crear/ })).not.toBeInTheDocument()
  })
  it('shows who saved a package and its agency', async () => {
    renderPage(<AdminFavorites />)
    expect(await screen.findByText('Paris favorito')).toBeInTheDocument()
    expect(screen.getByText('buyer')).toBeInTheDocument()
    expect(screen.getByText('Agencia Sur')).toBeInTheDocument()
  })
  it('shows review score and comment as text', async () => {
    renderPage(<AdminReviews />)
    expect(await screen.findByText('Excelente viaje')).toBeInTheDocument()
    expect(screen.getByText('9 / 10')).toBeInTheDocument()
    expect(screen.getByText('Paris reseñado')).toBeInTheDocument()
  })
  it('shows historical purchase data and price', async () => {
    renderPage(<AdminPurchases />)
    expect(await screen.findByText('Viaje histórico')).toBeInTheDocument()
    expect(screen.getByText('buyer')).toBeInTheDocument()
    expect(screen.getByText(/1.?234/)).toBeInTheDocument()
    expect(screen.getByText('Viajes del Sur')).toBeInTheDocument()
  })
  it('requests the next page instead of loading all users', async () => {
    const user = userEvent.setup()
    vi.mocked(getAdminUsers).mockImplementation(async (number) => ({ ...page([{ ...buyer, username: number ? 'second' : 'first' }]), page: number, totalElements: 21, totalPages: 2 }))
    renderPage(<AdminUsers />)
    await screen.findByText('first')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(await screen.findByText('second')).toBeInTheDocument()
    expect(getAdminUsers).toHaveBeenLastCalledWith(1)
  })
  it('shows an empty state', async () => {
    vi.mocked(getAdminUsers).mockResolvedValue(page([]))
    renderPage(<AdminUsers />)
    expect(await screen.findByText('Sin resultados')).toBeInTheDocument()
  })
  it('shows permission errors and allows retry', async () => {
    const user = userEvent.setup()
    vi.mocked(getAdminUsers).mockRejectedValueOnce(apiError('Acceso denegado', 403))
    renderPage(<AdminUsers />)
    expect(await screen.findByText('Acceso denegado')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('buyer')).toBeInTheDocument()
  })
})
