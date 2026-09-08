import { apiClient } from './client'
import type { Review } from './packages'

export async function createReview(
  packageId: number,
  score: number,
  comment?: string,
): Promise<Review> {
  const { data } = await apiClient.post<Review>('/reviews', { packageId, score, comment })
  return data
}
