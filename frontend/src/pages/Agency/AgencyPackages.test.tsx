import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { listFlights } from '../../api/flights'
import { listHotels } from '../../api/hotels'
import { createPackage, deletePackage, getAgencyPackages } from '../../api/packages'
import { allFlights, apiError, buenosAiresHotel, parisHotel, parisPackage } from '../../test/fixtures'
import { fillParisPackage } from '../../test/packageFormHelpers'
import { createTestQueryClient } from '../../test/queryClient'
import { AgencyPackages } from './AgencyPackages'

vi.mock('../../api/flights', () => ({ listFlights: vi.fn() }))
vi.mock('../../api/hotels', () => ({ listHotels: vi.fn() }))
vi.mock('../../api/packages', () => ({
  getAgencyPackages: vi.fn(),
  createPackage: vi.fn(),
  deletePackage: vi.fn(),
}))

const mockedCreatePackage = vi.mocked(createPackage)

function renderAgencyPackages() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AgencyPackages />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AgencyPackages page', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getAgencyPackages).mockResolvedValue([parisPackage])
    vi.mocked(listHotels).mockResolvedValue([parisHotel, buenosAiresHotel])
    vi.mocked(listFlights).mockResolvedValue(allFlights)
  })

  it('no longer asks for raw flight ids', async () => {
    renderAgencyPackages()

    expect(await screen.findByLabelText('Vuelo de ida')).toBeInTheDocument()
    expect(screen.getByLabelText('Vuelo de vuelta')).toBeInTheDocument()
    expect(screen.queryByLabelText('ID vuelo ida')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('ID vuelo vuelta')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Agencia')).not.toBeInTheDocument()
  })

  it('creates a package choosing the flights from the picker, without an agency id', async () => {
    const user = userEvent.setup()
    mockedCreatePackage.mockResolvedValue(parisPackage)
    renderAgencyPackages()

    await screen.findByLabelText('Vuelo de ida')
    await fillParisPackage(user, 'Paris Express', '2500')
    await user.click(screen.getByRole('button', { name: 'Crear paquete' }))

    await waitFor(() => {
      expect(mockedCreatePackage).toHaveBeenCalledTimes(1)
    })
    expect(mockedCreatePackage.mock.calls[0][0]).toEqual({
      name: 'Paris Express',
      hotelId: 1,
      outboundFlightId: 1,
      returnFlightId: 2,
      price: 2500,
    })
    expect(await screen.findByText('Paquete creado.')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByLabelText('Nombre')).toHaveValue('')
    })
  })

  it('shows the server message when the package is rejected', async () => {
    const user = userEvent.setup()
    mockedCreatePackage.mockRejectedValue(apiError('El vuelo de vuelta debe ser la ruta inversa'))
    renderAgencyPackages()

    await screen.findByLabelText('Vuelo de ida')
    await fillParisPackage(user, 'Paris Express', '2500')
    await user.click(screen.getByRole('button', { name: 'Crear paquete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El vuelo de vuelta debe ser la ruta inversa')
  })

  it('still lets the agency delete one of its packages', async () => {
    const user = userEvent.setup()
    vi.mocked(deletePackage).mockResolvedValue(undefined)
    renderAgencyPackages()

    await user.click(await screen.findByRole('button', { name: 'Eliminar' }))

    await waitFor(() => {
      expect(deletePackage).toHaveBeenCalled()
    })
    expect(vi.mocked(deletePackage).mock.calls[0][0]).toBe(10)
  })
})
