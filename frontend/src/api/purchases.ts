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

export async function getMyPurchases(): Promise<Purchase[]> {
  const { data } = await apiClient.get<Purchase[]>('/purchases/me')
  return data
}

export async function getAgencyPurchases(): Promise<Purchase[]> {
  const { data } = await apiClient.get<Purchase[]>('/purchases/agency')
  return data
}

export async function buyPackage(packageId: number): Promise<Purchase> {
  const { data } = await apiClient.post<Purchase>('/purchases', { packageId })
  return data
}
