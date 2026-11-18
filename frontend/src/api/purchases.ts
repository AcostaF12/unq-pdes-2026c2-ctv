import { apiClient } from './client'
import type { AuthenticatedUser } from './auth'
import type { Flight, Review, TravelPackage } from './packages'

export type BuyerIdentity = Pick<AuthenticatedUser, 'id' | 'username' | 'firstName' | 'lastName'>

export type PurchaseStatus = 'ACTIVE' | 'CANCELLED'

export interface Purchase {
  id: number
  buyer: BuyerIdentity
  travelPackage: TravelPackage
  purchasePrice: number
  purchasedAt: string
  /** Always sent by the backend; optional so older fixtures keep reading as ACTIVE. */
  status?: PurchaseStatus
  cancelledAt?: string | null
}

export interface PurchaseDetail extends Purchase {
  outboundFlight: Flight | null
  returnFlight: Flight | null
  review: Review | null
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

export type MyPurchasesFilters = Pick<PurchaseHistoryFilters, 'page' | 'size' | 'packageName' | 'from' | 'to'> & {
  destination?: string
  minPrice?: number
  maxPrice?: number
  sort?: string
}

export async function getMyPurchases(filters: MyPurchasesFilters): Promise<PurchaseHistoryPage> {
  const { data } = await apiClient.get<PurchaseHistoryPage>('/purchases/me', { params: filters })
  return data
}

export async function getAgencyPurchases(filters: PurchaseHistoryFilters): Promise<PurchaseHistoryPage> {
  const { data } = await apiClient.get<PurchaseHistoryPage>('/purchases/agency', { params: filters })
  return data
}

export async function getAdminSales(filters: PurchaseHistoryFilters): Promise<PurchaseHistoryPage> {
  const { data } = await apiClient.get<PurchaseHistoryPage>('/admin/sales', { params: filters })
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

export async function getPurchase(id: number): Promise<PurchaseDetail> {
  const { data } = await apiClient.get<PurchaseDetail>(`/purchases/${id}`)
  return data
}

export async function cancelPurchase(id: number): Promise<void> {
  await apiClient.delete(`/purchases/${id}`)
}
