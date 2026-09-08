import { apiClient } from './client'
import type { Agency } from './packages'

export interface Purchase {
  id: number
  packageId: number
  packageName: string
  agency: Agency
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
