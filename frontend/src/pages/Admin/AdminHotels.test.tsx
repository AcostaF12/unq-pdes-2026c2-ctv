import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { deleteHotel, listHotels } from '../../api/hotels'
import { parisHotel } from '../../test/fixtures'
import { createTestQueryClient } from '../../test/queryClient'
import { AdminHotels } from './AdminHotels'

vi.mock('../../api/hotels', () => ({ listHotels: vi.fn(), deleteHotel: vi.fn() }))

function renderHotels() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AdminHotels />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminHotels page', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listHotels).mockResolvedValue([parisHotel])
    vi.mocked(deleteHotel).mockResolvedValue(undefined)
  })

  it('links the hotel flow without leaving admin', async () => {
    renderHotels()

    expect(await screen.findByRole('heading', { name: 'Hoteles' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Editar' })).toHaveAttribute('href', `/admin/hotels/${parisHotel.id}/edit`)
    expect(screen.getByRole('link', { name: 'Nuevo hotel' })).toHaveAttribute('href', '/admin/hotels/new')
    expect(screen.getByRole('link', { name: 'Ver ficha' })).toHaveAttribute('href', `/hotels/${parisHotel.id}`)
    expect(screen.getByRole('link', { name: '2. Vuelos' })).toHaveAttribute('href', '/admin/flights')
  })

  it('deletes a hotel after confirmation', async () => {
    const user = userEvent.setup()
    renderHotels()
    await screen.findByText(parisHotel.name)

    await user.click(screen.getByRole('button', { name: `Eliminar ${parisHotel.name}` }))
    await user.click(screen.getByRole('button', { name: 'Confirmar eliminación' }))

    expect(deleteHotel).toHaveBeenCalledWith(parisHotel.id, expect.anything())
  })
})
