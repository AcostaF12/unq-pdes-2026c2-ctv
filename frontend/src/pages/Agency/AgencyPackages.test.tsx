import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { listCities } from '../../api/cities'
import { listFlights } from '../../api/flights'
import { listHotels } from '../../api/hotels'
import {
  createPackage,
  deletePackage,
  getAgencyPackages,
  updatePackage,
  setPackageActive,
} from '../../api/packages'
import { allFlights, apiError, buenosAiresHotel, parisHotel, parisPackage } from '../../test/fixtures'
import { fillParisPackage } from '../../test/packageFormHelpers'
import { createTestQueryClient } from '../../test/queryClient'
import { AgencyPackages } from './AgencyPackages'

vi.mock('../../api/flights', () => ({ listFlights: vi.fn() }))
vi.mock('../../api/cities', () => ({ listCities: vi.fn() }))
vi.mock('../../api/hotels', () => ({ listHotels: vi.fn() }))
vi.mock('../../api/packages', () => ({
  getAgencyPackages: vi.fn(),
  createPackage: vi.fn(),
  deletePackage: vi.fn(),
  updatePackage: vi.fn(),
  setPackageActive: vi.fn(),
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
    vi.mocked(listCities).mockResolvedValue([parisHotel.city, buenosAiresHotel.city])
  })

  it('archives a package while retaining access to editing', async () => {
    vi.mocked(setPackageActive).mockResolvedValue({ ...parisPackage, active: false })
    vi.mocked(getAgencyPackages)
      .mockResolvedValueOnce([parisPackage])
      .mockResolvedValue([{ ...parisPackage, active: false }])
    const user = userEvent.setup()
    renderAgencyPackages()
    await user.click(await screen.findByRole('button', { name: 'Archivar' }))
    expect(setPackageActive).toHaveBeenCalledWith(parisPackage.id, false)
    expect(await screen.findByText('Paquete archivado. El historial se conserva.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Archivados' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Publicados' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Publicar' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: `Editar ${parisPackage.name}` })).toBeEnabled()
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

  it.each(['create', 'edit'])('saves the selected flights after filtering by origin and dates on %s', async (mode) => {
    const user = userEvent.setup()
    mockedCreatePackage.mockResolvedValue(parisPackage)
    vi.mocked(updatePackage).mockResolvedValue(parisPackage)
    renderAgencyPackages()
    if (mode === 'edit') {
      await user.click(await screen.findByRole('button', { name: `Editar ${parisPackage.name}` }))
      await waitFor(() => expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('2'))
    } else {
      await user.type(await screen.findByLabelText('Nombre'), parisPackage.name)
      await user.selectOptions(screen.getByLabelText('Hotel'), '1')
      await user.type(screen.getByLabelText('Precio'), '1800')
    }

    await user.selectOptions(screen.getByLabelText('Origen de ida'), 'BUE')
    fireEvent.change(screen.getByLabelText('Fecha de ida'), { target: { value: '2026-12-01' } })
    fireEvent.change(screen.getByLabelText('Fecha de vuelta'), { target: { value: '2026-12-10' } })
    await waitFor(() => expect(listFlights).toHaveBeenCalledWith({
      origin: 'BUE', destination: 'PAR', date: '2026-12-01',
    }))
    await waitFor(() => expect(screen.getByLabelText('Vuelo de ida').querySelector('option[value="1"]')).not.toBeNull())
    await user.selectOptions(screen.getByLabelText('Vuelo de ida'), '1')
    await waitFor(() => expect(listFlights).toHaveBeenCalledWith({
      origin: 'PAR', destination: 'BUE', date: '2026-12-10',
    }))
    await waitFor(() => expect(screen.getByLabelText('Vuelo de vuelta').querySelector('option[value="2"]')).not.toBeNull())
    await user.selectOptions(screen.getByLabelText('Vuelo de vuelta'), '2')
    await user.click(screen.getByRole('button', { name: mode === 'edit' ? 'Guardar cambios' : 'Crear paquete' }))

    const payload = {
      name: parisPackage.name, hotelId: 1, outboundFlightId: 1, returnFlightId: 2, price: 1800,
    }
    if (mode === 'edit') {
      await waitFor(() => expect(updatePackage).toHaveBeenCalledWith(10, payload))
      expect(createPackage).not.toHaveBeenCalled()
    } else {
      await waitFor(() => expect(createPackage).toHaveBeenCalled())
      expect(mockedCreatePackage.mock.calls[0][0]).toEqual(payload)
      expect(updatePackage).not.toHaveBeenCalled()
    }
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

  it('edits an owned package with its current values and refreshes the list', async () => {
    const user = userEvent.setup()
    const updated = { ...parisPackage, price: 2300 }
    vi.mocked(updatePackage).mockImplementation(async () => {
      vi.mocked(getAgencyPackages).mockResolvedValue([updated])
      return updated
    })
    renderAgencyPackages()
    await user.click(await screen.findByRole('button', { name: `Editar ${parisPackage.name}` }))
    expect(screen.getByLabelText('Nombre')).toHaveValue(parisPackage.name)
    expect(screen.getByLabelText('Precio')).toHaveValue(1800)
    await waitFor(() => expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('2'))
    await user.clear(screen.getByLabelText('Precio'))
    await user.type(screen.getByLabelText('Precio'), '2300')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByText('Paquete actualizado.')).toBeInTheDocument()
    expect(updatePackage).toHaveBeenCalledWith(10, {
      name: parisPackage.name,
      hotelId: 1,
      outboundFlightId: 1,
      returnFlightId: 2,
      price: 2300,
    })
    expect(createPackage).not.toHaveBeenCalled()
    expect(await screen.findByText('USD 2300')).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre')).toHaveValue('')
  })

  it('cancels editing without saving', async () => {
    const user = userEvent.setup()
    renderAgencyPackages()
    await user.click(await screen.findByRole('button', { name: `Editar ${parisPackage.name}` }))
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(screen.getByRole('button', { name: 'Crear paquete' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre')).toHaveValue('')
    expect(updatePackage).not.toHaveBeenCalled()
  })

  it('disables cancellation until the pending edit finishes', async () => {
    const user = userEvent.setup()
    let finish!: (value: typeof parisPackage) => void
    vi.mocked(updatePackage).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      }),
    )
    renderAgencyPackages()
    await user.click(await screen.findByRole('button', { name: `Editar ${parisPackage.name}` }))
    await waitFor(() => expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('2'))
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
    finish(parisPackage)
    expect(await screen.findByText('Paquete actualizado.')).toBeInTheDocument()
  })

  it('keeps the edit form and values when the API rejects the update', async () => {
    const user = userEvent.setup()
    vi.mocked(updatePackage).mockRejectedValue(apiError('No tenés permiso para editar este paquete', 403))
    renderAgencyPackages()
    await user.click(await screen.findByRole('button', { name: `Editar ${parisPackage.name}` }))
    await waitFor(() => expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('2'))
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No tenés permiso para editar este paquete')
    expect(screen.getByLabelText('Nombre')).toHaveValue(parisPackage.name)
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeInTheDocument()
  })
})
