import { apiClient } from './client'
import type { Agency, PagedResult } from './packages'
import type { Purchase } from './purchases'

export interface CountRanking {
  id: number
  name: string
  total: number
}

export interface DestinationCountRanking {
  code: string
  name: string
  total: number
}

export interface DestinationRatingRanking {
  code: string
  name: string
  average: number
  reviewCount: number
}

export interface AdminReportsData {
  topBuyers: CountRanking[]
  topAgencies: CountRanking[]
  topDestinations: DestinationCountRanking[]
  topRatedDestinations: DestinationRatingRanking[]
}

export async function getAdminReports(): Promise<AdminReportsData> {
  const { data } = await apiClient.get<AdminReportsData>('/admin/reports')
  return data
}

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

export async function getAdminUsers(page: number, size = PAGE_SIZE): Promise<PagedResult<AdminUser>> {
  const { data } = await apiClient.get<PagedResult<AdminUser>>('/admin/users', { params: { page, size } })
  return data
}

export async function getAdminFavorites(page: number, size = PAGE_SIZE): Promise<PagedResult<AdminFavorite>> {
  const { data } = await apiClient.get<PagedResult<AdminFavorite>>('/admin/favorites', { params: { page, size } })
  return data
}

export async function getAdminReviews(page: number, size = PAGE_SIZE): Promise<PagedResult<AdminReview>> {
  const { data } = await apiClient.get<PagedResult<AdminReview>>('/admin/reviews', { params: { page, size } })
  return data
}

export async function getAdminPurchases(page: number, size = PAGE_SIZE): Promise<PagedResult<Purchase>> {
  const { data } = await apiClient.get<PagedResult<Purchase>>('/admin/purchases', { params: { page, size } })
  return data
}

export interface AdminSystemComponent {
  name: string
  status: string
}

export interface AdminHttpEndpoint {
  method: string
  uri: string
  requestCount: number
  serverErrors: number
  p95Millis: number | null
}

export interface AdminHttpTraffic {
  requestCount: number
  clientErrors: number
  serverErrors: number
  meanMillis: number | null
  endpoints: AdminHttpEndpoint[]
}

export interface AdminConcurrency {
  poolActive: number | null
  poolIdle: number | null
  poolPending: number | null
  poolMax: number | null
  threadsBusy: number | null
  threadsCurrent: number | null
  threadsMax: number | null
}

export interface AdminCircuitBreaker {
  name: string
  state: string
  failureRate: number | null
}

export interface AdminSystemSnapshot {
  capturedAt: string
  systems: AdminSystemComponent[]
  http: AdminHttpTraffic
  concurrency: AdminConcurrency
  circuitBreaker: AdminCircuitBreaker
}

export async function getAdminSystem(): Promise<AdminSystemSnapshot> {
  const { data } = await apiClient.get<AdminSystemSnapshot>('/admin/system')
  return data
}
