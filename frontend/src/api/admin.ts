import { apiClient } from './client'
import type { Agency, PagedResult } from './packages'
import type { Purchase } from './purchases'

export interface AdminUser {
  id: number
  username: string
  firstName: string
  lastName: string
  role: string
  agency?: Agency
}

export interface AdminFavorite {
  id: number
  buyerUsername: string
  packageId: number
  packageName: string
  agencyName: string
}

export interface AdminReview {
  id: number
  buyerUsername: string
  packageId: number
  packageName: string
  score: number
  comment: string | null
}

const PAGE_SIZE = 20

export async function getAdminUsers(page: number): Promise<PagedResult<AdminUser>> {
  const { data } = await apiClient.get<PagedResult<AdminUser>>('/admin/users', { params: { page, size: PAGE_SIZE } })
  return data
}

export async function getAdminFavorites(page: number): Promise<PagedResult<AdminFavorite>> {
  const { data } = await apiClient.get<PagedResult<AdminFavorite>>('/admin/favorites', { params: { page, size: PAGE_SIZE } })
  return data
}

export async function getAdminReviews(page: number): Promise<PagedResult<AdminReview>> {
  const { data } = await apiClient.get<PagedResult<AdminReview>>('/admin/reviews', { params: { page, size: PAGE_SIZE } })
  return data
}

export async function getAdminPurchases(page: number): Promise<PagedResult<Purchase>> {
  const { data } = await apiClient.get<PagedResult<Purchase>>('/admin/purchases', { params: { page, size: PAGE_SIZE } })
  return data
}
