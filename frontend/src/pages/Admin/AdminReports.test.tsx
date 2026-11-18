import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { getAdminReports } from '../../api/admin'
import { createTestQueryClient } from '../../test/queryClient'
import { apiError } from '../../test/fixtures'
import { AdminReports } from './AdminReports'

vi.mock('../../api/admin', () => ({ getAdminReports: vi.fn() }))
const reports = {
  topBuyers: [{ id: 1, name: 'ana', total: 7 }],
  topAgencies: [{ id: 2, name: 'Despegar', total: 12 }],
  topDestinations: [{ code: 'PAR', name: 'Paris', total: 9 }],
  topRatedDestinations: [{ code: 'ROM', name: 'Roma', average: 8.75, reviewCount: 4 }],
}
function renderPage() {
  return render(<QueryClientProvider client={createTestQueryClient()}><MemoryRouter><AdminReports /></MemoryRouter></QueryClientProvider>)
}
describe('AdminReports', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getAdminReports).mockResolvedValue(reports)
  })
  it('shows four rankings with counts and rating sample size', async () => {
    renderPage()
    const buyers = await screen.findByRole('table', { name: 'Compradores con más compras' })
    expect(within(buyers).getByText('ana')).toBeInTheDocument()
    expect(within(buyers).getByText('7')).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Agencias con más ventas' })).toHaveTextContent('Despegar')
    expect(screen.getByRole('table', { name: 'Destinos más vendidos' })).toHaveTextContent('Paris')
    const ratings = screen.getByRole('table', { name: 'Destinos mejor puntuados' })
    expect(ratings).toHaveTextContent('Roma')
    expect(ratings).toHaveTextContent('8,75')
    expect(within(ratings).getByText('4')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar|Eliminar/ })).not.toBeInTheDocument()
  })
  it('handles fewer than five rows and each empty report', async () => {
    vi.mocked(getAdminReports).mockResolvedValue({ topBuyers: [], topAgencies: [], topDestinations: [], topRatedDestinations: [] })
    renderPage()
    expect(await screen.findAllByText('Sin datos para este reporte.')).toHaveLength(4)
  })
  it('retries a failed request', async () => {
    const user = userEvent.setup()
    vi.mocked(getAdminReports).mockRejectedValueOnce(apiError('Acceso denegado', 403))
    renderPage()
    expect(await screen.findByText('Acceso denegado')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('ana')).toBeInTheDocument()
  })
})
