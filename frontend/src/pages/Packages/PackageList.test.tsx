import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { PackageList } from './PackageList'
import { AuthProvider } from '../../auth/AuthContext'
import { persistSession } from '../../api/session'
import { getCurrentUser } from '../../api/auth'
import { listCities } from '../../api/cities'
import { searchPackages } from '../../api/packages'
import { writePreferences } from '../../preferences/preferences'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/auth', () => ({
  getCurrentUser: vi.fn(),
}))

vi.mock('../../api/cities', () => ({
  listCities: vi.fn(),
}))

vi.mock('../../api/packages', () => ({
  searchPackages: vi.fn(),
}))

const buyer = {
  id: 4,
  username: 'buyer',
  firstName: 'Bruno',
  lastName: 'Buyer',
  role: 'BUYER',
}

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedListCities = vi.mocked(listCities)
const mockedSearchPackages = vi.mocked(searchPackages)

function emptyPage() {
  return { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
}

function travelPackage(overrides: { id?: number; name?: string } = {}) {
  return {
    id: overrides.id ?? 1,
    name: overrides.name ?? 'París Romántico',
    price: 1500,
    agency: { id: 1, name: 'Despegar' },
    hotel: { id: 1, name: 'Hotel Paris', photoUrl: '', city: { code: 'PAR', name: 'Paris' } },
    origin: { code: 'BUE', name: 'Buenos Aires' },
    destination: { code: 'PAR', name: 'Paris' },
    outboundFlightId: 1,
    returnFlightId: 2,
  }
}

function renderPackageList() {
  persistSession('token', buyer)
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <MemoryRouter>
          <PackageList />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('PackageList', () => {
  beforeEach(() => {
    localStorage.clear()
    mockedGetCurrentUser.mockReset()
    mockedListCities.mockReset()
    mockedSearchPackages.mockReset()
    mockedGetCurrentUser.mockResolvedValue(buyer)
    mockedListCities.mockResolvedValue([{ code: 'BUE', name: 'Buenos Aires' }])
    mockedSearchPackages.mockResolvedValue(emptyPage())
  })

  it('uses the saved origin city as the default filter', async () => {
    writePreferences(buyer.id, {
      originCityCode: 'BUE',
      maxBudget: '800',
      applyOriginOnSearch: true,
    })

    renderPackageList()

    expect(await screen.findByLabelText('Origen')).toHaveValue('BUE')
    expect(mockedSearchPackages).toHaveBeenCalledWith({ origin: 'BUE' })
  })

  it('sends the selected inclusive price range when searching', async () => {
    const user = userEvent.setup()
    renderPackageList()

    await screen.findByLabelText('Nombre')
    await user.type(screen.getByLabelText('Precio mínimo'), '900')
    await user.type(screen.getByLabelText('Precio máximo'), '1500')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(mockedSearchPackages).toHaveBeenLastCalledWith({ minPrice: 900, maxPrice: 1500 })
  })

  it('sends every advanced filter in one search request', async () => {
    const user = userEvent.setup()
    mockedListCities.mockResolvedValue([
      { code: 'BUE', name: 'Buenos Aires' },
      { code: 'PAR', name: 'Paris' },
    ])
    renderPackageList()

    await screen.findByLabelText('Nombre')
    await user.type(screen.getByLabelText('Nombre'), 'Paris Complete')
    await user.selectOptions(screen.getByLabelText('Origen'), 'BUE')
    await user.selectOptions(screen.getByLabelText('Destino'), 'PAR')
    await user.type(screen.getByLabelText('Precio mínimo'), '1200')
    await user.type(screen.getByLabelText('Precio máximo'), '1800')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(mockedSearchPackages).toHaveBeenLastCalledWith({
      name: 'Paris Complete',
      origin: 'BUE',
      destination: 'PAR',
      minPrice: 1200,
      maxPrice: 1800,
    })
  })

  it('does not submit an inverted price range', async () => {
    const user = userEvent.setup()
    renderPackageList()

    await screen.findByLabelText('Nombre')
    await user.type(screen.getByLabelText('Precio mínimo'), '1500')
    await user.type(screen.getByLabelText('Precio máximo'), '900')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('El precio mínimo no puede superar al precio máximo.')).toBeInTheDocument()
    expect(mockedSearchPackages).toHaveBeenCalledTimes(1)
  })

  it('shows an empty catalog state when there are no packages', async () => {
    renderPackageList()

    expect(await screen.findByText('Todavía no hay paquetes publicados.')).toBeInTheDocument()
    expect(screen.getByRole('status')).toBeInTheDocument()
  })

  it('shows an error and allows retry', async () => {
    const user = userEvent.setup()
    mockedSearchPackages.mockRejectedValueOnce(new Error('boom'))
    mockedSearchPackages.mockResolvedValueOnce(emptyPage())

    renderPackageList()

    expect(
      await screen.findByText('No pudimos cargar los paquetes. Intentá de nuevo.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('alert')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByText('Todavía no hay paquetes publicados.')).toBeInTheDocument()
  })

  it('requests the next page when paginating', async () => {
    const user = userEvent.setup()
    mockedSearchPackages.mockResolvedValue({
      content: [travelPackage()],
      page: 0,
      size: 20,
      totalElements: 21,
      totalPages: 2,
    })

    renderPackageList()

    const nextPageButton = await screen.findByRole('button', { name: 'Siguiente' })
    await user.click(nextPageButton)

    expect(mockedSearchPackages).toHaveBeenLastCalledWith({ page: 1 })
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()
  })

  it('sends the selected sort order', async () => {
    const user = userEvent.setup()
    renderPackageList()

    await screen.findByLabelText('Nombre')
    await user.selectOptions(screen.getByLabelText('Ordenar por'), 'price-asc')

    expect(mockedSearchPackages).toHaveBeenLastCalledWith({ sort: 'price,asc' })
  })
})
