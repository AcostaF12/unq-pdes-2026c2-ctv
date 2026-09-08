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
