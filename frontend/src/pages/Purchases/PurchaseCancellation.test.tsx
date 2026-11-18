import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCurrentUser } from '../../api/auth'
import { cancelPurchase, getPurchase, type PurchaseDetail as PurchaseDetailModel } from '../../api/purchases'
import { persistSession } from '../../api/session'
import { AuthProvider } from '../../auth/AuthContext'
import { apiError, outboundToParis, parisPackage, returnFromParis } from '../../test/fixtures'
import { createTestQueryClient } from '../../test/queryClient'
import { PurchaseDetail } from './PurchaseDetail'
import { PurchaseTicket } from './PurchaseTicket'

vi.mock('../../api/auth', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../../api/purchases', () => ({ getPurchase: vi.fn(), cancelPurchase: vi.fn() }))

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedGetPurchase = vi.mocked(getPurchase)
const mockedCancelPurchase = vi.mocked(cancelPurchase)

const buyer = { id: 5, username: 'bruno', firstName: 'Bruno', lastName: 'Buyer', role: 'BUYER' }
const otherBuyer = { id: 6, username: 'carla', firstName: 'Carla', lastName: 'Compradora', role: 'BUYER' }
const agency = { id: 9, username: 'agency', firstName: 'Agus', lastName: 'Agency', role: 'AGENCY' }

const active: PurchaseDetailModel = {
  id: 12,
  buyer: { id: 5, username: 'bruno', firstName: 'Bruno', lastName: 'Buyer' },
  travelPackage: parisPackage,
  purchasePrice: 1750,
  purchasedAt: '2026-03-15T14:30:00',
  status: 'ACTIVE',
  cancelledAt: null,
  outboundFlight: outboundToParis,
  returnFlight: returnFromParis,
  review: null,
}

const cancelled: PurchaseDetailModel = { ...active, status: 'CANCELLED', cancelledAt: '2026-03-20T10:00:00' }

function renderDetail(viewer: typeof buyer | typeof agency = buyer) {
  persistSession('token', viewer)
  mockedGetCurrentUser.mockResolvedValue(viewer)
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <MemoryRouter initialEntries={['/purchases/12']}>
          <Routes>
            <Route path="/purchases/:id" element={<PurchaseDetail />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('Cancelar compra', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    mockedGetPurchase.mockReset()
    mockedCancelPurchase.mockReset()
  })

  it('asks for confirmation, cancels the purchase and then shows it as cancelled', async () => {
    const user = userEvent.setup()
    mockedGetPurchase.mockResolvedValueOnce(active).mockResolvedValue(cancelled)
    mockedCancelPurchase.mockResolvedValue()
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Cancelar compra' }))
    expect(screen.getByRole('group', { name: 'Confirmar cancelación de Escapada a París' })).toBeInTheDocument()
    expect(mockedCancelPurchase).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Confirmar cancelación' }))

    expect(mockedCancelPurchase).toHaveBeenCalledWith(12)
    expect(await screen.findByText('Compra cancelada')).toBeInTheDocument()
    expect(screen.getByText('Cancelada', { selector: '.purchase-status' })).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Cancelar compra' })).not.toBeInTheDocument())
    expect(mockedGetPurchase).toHaveBeenCalledTimes(2)
  })

  it('going back from the confirmation does not cancel anything', async () => {
    const user = userEvent.setup()
    mockedGetPurchase.mockResolvedValue(active)
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Cancelar compra' }))
    await user.click(screen.getByRole('button', { name: 'Volver' }))

    expect(screen.getByRole('button', { name: 'Cancelar compra' })).toBeInTheDocument()
    expect(mockedCancelPurchase).not.toHaveBeenCalled()
  })

  it('shows the backend reason when the purchase can no longer be cancelled', async () => {
    const user = userEvent.setup()
    mockedGetPurchase.mockResolvedValue(active)
    mockedCancelPurchase.mockRejectedValue(apiError('El vuelo de ida ya partió. La compra no puede cancelarse.'))
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Cancelar compra' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar cancelación' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El vuelo de ida ya partió')
  })

  it('a cancelled purchase shows its state, no cancel button and no review link', async () => {
    mockedGetPurchase.mockResolvedValue(cancelled)
    renderDetail()

    expect(await screen.findByText('Compra cancelada')).toBeInTheDocument()
    expect(screen.getByText('Cancelada', { selector: '.purchase-status' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar compra' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Dejar reseña →' })).not.toBeInTheDocument()
  })

  it('only the buyer who owns the purchase can cancel it', async () => {
    mockedGetPurchase.mockResolvedValue(active)
    renderDetail(agency)
    expect(await screen.findByText('Compra confirmada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar compra' })).not.toBeInTheDocument()
  })

  it('another buyer does not get the cancel button', async () => {
    mockedGetPurchase.mockResolvedValue(active)
    renderDetail(otherBuyer)
    expect(await screen.findByText('Compra confirmada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar compra' })).not.toBeInTheDocument()
  })

  it('purchase lists flag cancelled purchases with a badge', () => {
    render(
      <MemoryRouter>
        <PurchaseTicket purchase={cancelled} audience="agency" />
        <PurchaseTicket purchase={{ ...active, id: 13 }} audience="agency" />
      </MemoryRouter>,
    )

    expect(screen.getAllByText('Cancelada')).toHaveLength(1)
  })
})
