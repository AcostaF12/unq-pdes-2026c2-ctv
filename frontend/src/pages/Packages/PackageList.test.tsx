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
    mockedSearchPackages.mockResolvedValue([])
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
    mockedSearchPackages.mockResolvedValueOnce([])

    renderPackageList()

    expect(
      await screen.findByText('No pudimos cargar los paquetes. Intentá de nuevo.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('alert')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByText('Todavía no hay paquetes publicados.')).toBeInTheDocument()
  })
})
