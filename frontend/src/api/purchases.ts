import { apiClient } from './client'
import type { AuthenticatedUser } from './auth'
import type { TravelPackage } from './packages'

export type BuyerIdentity = Pick<AuthenticatedUser, 'id' | 'username' | 'firstName' | 'lastName'>

export interface Purchase {
  id: number
  buyer: BuyerIdentity
  travelPackage: TravelPackage
  purchasePrice: number
  purchasedAt: string
}

export interface PurchaseHistoryPage {
  content: Purchase[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface PurchaseHistoryFilters {
  page: number
  size: number
  buyerUsername?: string
  packageName?: string
  from?: string
  to?: string
}

export async function getMyPurchases(
  filters: Pick<PurchaseHistoryFilters, 'page' | 'size'>,
): Promise<PurchaseHistoryPage> {
  const { data } = await apiClient.get<PurchaseHistoryPage>('/purchases/me', { params: filters })
  return data
}

export async function getAgencyPurchases(filters: PurchaseHistoryFilters): Promise<PurchaseHistoryPage> {
  const { data } = await apiClient.get<PurchaseHistoryPage>('/purchases/agency', { params: filters })
  return data
}

export async function hasPurchasedPackage(packageId: number): Promise<boolean> {
  const { data } = await apiClient.get<boolean>(`/purchases/me/packages/${packageId}`)
  return data
}

export async function buyPackage(packageId: number): Promise<Purchase> {
  const { data } = await apiClient.post<Purchase>('/purchases', { packageId })
  return data
}
