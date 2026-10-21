import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { listAgencies } from '../../api/agencies'
import { listFlights } from '../../api/flights'
import { listHotels } from '../../api/hotels'
import { createPackage, deletePackage, searchPackages, updatePackage } from '../../api/packages'
import { allFlights, apiError, buenosAiresHotel, parisHotel, parisPackage } from '../../test/fixtures'
import { fillParisPackage } from '../../test/packageFormHelpers'
import { createTestQueryClient } from '../../test/queryClient'
import { AdminPackages } from './AdminPackages'

vi.mock('../../api/agencies', () => ({ listAgencies: vi.fn() }))
vi.mock('../../api/flights', () => ({ listFlights: vi.fn() }))
vi.mock('../../api/hotels', () => ({ listHotels: vi.fn() }))
vi.mock('../../api/packages', () => ({
  searchPackages: vi.fn(),
  createPackage: vi.fn(),
  updatePackage: vi.fn(),
  deletePackage: vi.fn(),
}))

const mockedSearchPackages = vi.mocked(searchPackages)
const mockedCreatePackage = vi.mocked(createPackage)
const mockedUpdatePackage = vi.mocked(updatePackage)
const mockedDeletePackage = vi.mocked(deletePackage)

function pageOf(content: (typeof parisPackage)[], totalPages = 1) {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages }
}

function renderAdminPackages() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AdminPackages />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminPackages page', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockedSearchPackages.mockResolvedValue(pageOf([parisPackage]))
    vi.mocked(listAgencies).mockResolvedValue([
      { id: 7, name: 'Viajes del Sur' },
      { id: 8, name: 'Norte Tours' },
    ])
    vi.mocked(listHotels).mockResolvedValue([parisHotel, buenosAiresHotel])
    vi.mocked(listFlights).mockResolvedValue(allFlights)
  })

  it('lists every package with its agency, route and price', async () => {
    renderAdminPackages()

    const item = await screen.findByTestId('package-item')
    expect(within(item).getByText('Escapada a París')).toBeInTheDocument()
    expect(within(item).getByText('Agencia: Viajes del Sur')).toBeInTheDocument()
    expect(within(item).getByText('Buenos Aires → París')).toBeInTheDocument()
    expect(within(item).getByText('USD 1800')).toBeInTheDocument()
    expect(mockedSearchPackages).toHaveBeenCalledWith({ page: 0, size: 10, sort: 'name,asc' })
  })

  it('shows an empty state when there are no packages', async () => {
    mockedSearchPackages.mockResolvedValue(pageOf([]))
    renderAdminPackages()

    expect(await screen.findByText('Todavía no hay paquetes cargados.')).toBeInTheDocument()
  })

  it('shows validation errors and does not submit an incomplete form', async () => {
    const user = userEvent.setup()
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Nuevo paquete' }))
    await user.click(await screen.findByRole('button', { name: 'Crear paquete' }))

    await waitFor(() => {
      expect(screen.getAllByRole('alert').map((alert) => alert.textContent)).toEqual([
        'Ingresá el nombre',
        'Elegí una agencia',
        'Elegí un hotel',
        'Elegí el vuelo de ida',
        'Elegí el vuelo de vuelta',
        'Ingresá el precio',
      ])
    })
    expect(screen.getByLabelText('Hotel')).toHaveAttribute('aria-invalid', 'true')
    expect(mockedCreatePackage).not.toHaveBeenCalled()
  })

  it('creates a package for the chosen agency using flights picked from the hotel city', async () => {
    const user = userEvent.setup()
    mockedCreatePackage.mockResolvedValue(parisPackage)
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Nuevo paquete' }))
    await user.selectOptions(await screen.findByLabelText('Agencia'), '8')
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
      agencyId: 8,
    })
    expect(await screen.findByText('Paquete creado.')).toBeInTheDocument()
    expect(screen.queryByTestId('package-form')).not.toBeInTheDocument()
  })

  it('shows the server message when the backend rejects the package', async () => {
    const user = userEvent.setup()
    mockedCreatePackage.mockRejectedValue(apiError('Ya existe un paquete con ese nombre en la agencia'))
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Nuevo paquete' }))
    await user.selectOptions(await screen.findByLabelText('Agencia'), '7')
    await fillParisPackage(user, 'Escapada a París', '1800')
    await user.click(screen.getByRole('button', { name: 'Crear paquete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un paquete con ese nombre en la agencia')
    expect(screen.getByTestId('package-form')).toBeInTheDocument()
  })

  it('edits a package with its current values prefilled', async () => {
    const user = userEvent.setup()
    mockedUpdatePackage.mockResolvedValue(parisPackage)
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Editar Escapada a París' }))

    expect(await screen.findByLabelText('Nombre')).toHaveValue('Escapada a París')
    expect(screen.getByLabelText('Agencia')).toHaveValue('7')
    expect(screen.getByLabelText('Hotel')).toHaveValue('1')
    await waitFor(() => {
      expect(screen.getByLabelText('Vuelo de ida')).toHaveValue('1')
    })
    await waitFor(() => {
      expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('2')
    })

    await user.clear(screen.getByLabelText('Precio'))
    await user.type(screen.getByLabelText('Precio'), '2100')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => {
      expect(mockedUpdatePackage).toHaveBeenCalledTimes(1)
    })
    expect(mockedUpdatePackage.mock.calls[0][0]).toBe(10)
    expect(mockedUpdatePackage.mock.calls[0][1]).toEqual({
      name: 'Escapada a París',
      hotelId: 1,
      outboundFlightId: 1,
      returnFlightId: 2,
      price: 2100,
      agencyId: 7,
    })
    expect(await screen.findByText('Paquete actualizado.')).toBeInTheDocument()
  })

  it('asks for confirmation inside the page before deleting and lets the admin cancel', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm')
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Eliminar Escapada a París' }))

    const confirmation = screen.getByRole('group', { name: 'Confirmar eliminación de Escapada a París' })
    expect(confirmation).toHaveTextContent('Esta acción no se puede deshacer')
    expect(mockedDeletePackage).not.toHaveBeenCalled()

    await user.click(within(confirmation).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByTestId('confirm-delete')).not.toBeInTheDocument()
    expect(mockedDeletePackage).not.toHaveBeenCalled()
    expect(confirmSpy).not.toHaveBeenCalled()
  })

  it('deletes the package once the admin confirms', async () => {
    const user = userEvent.setup()
    mockedDeletePackage.mockResolvedValue(undefined)
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Eliminar Escapada a París' }))
    mockedSearchPackages.mockResolvedValue(pageOf([]))
    await user.click(screen.getByRole('button', { name: 'Confirmar eliminación' }))

    await waitFor(() => {
      expect(mockedDeletePackage).toHaveBeenCalledTimes(1)
    })
    expect(mockedDeletePackage.mock.calls[0][0]).toBe(10)
    expect(await screen.findByText('Paquete eliminado.')).toBeInTheDocument()
    expect(await screen.findByText('Todavía no hay paquetes cargados.')).toBeInTheDocument()
  })

  it('shows the server message when the package cannot be deleted', async () => {
    const user = userEvent.setup()
    mockedDeletePackage.mockRejectedValue(apiError('El paquete tiene compras asociadas', 409))
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Eliminar Escapada a París' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar eliminación' }))

    const confirmation = await screen.findByTestId('confirm-delete')
    expect(await within(confirmation).findByRole('alert')).toHaveTextContent('El paquete tiene compras asociadas')
  })

  it('pages through the catalog', async () => {
    const user = userEvent.setup()
    mockedSearchPackages.mockResolvedValue(pageOf([parisPackage], 2))
    renderAdminPackages()

    await user.click(await screen.findByRole('button', { name: 'Siguiente' }))

    await waitFor(() => {
      expect(mockedSearchPackages).toHaveBeenLastCalledWith({ page: 1, size: 10, sort: 'name,asc' })
    })
  })
})
