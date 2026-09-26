import { apiClient } from './client'
import type { TravelPackage } from './packages'

export interface BuyerIdentity {
  id: number
  username: string
  firstName: string
  lastName: string
}

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
