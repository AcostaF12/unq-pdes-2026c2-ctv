import axios from 'axios'

export interface ApiErrorResponse {
  httpCode: number
  httpStatus: string
  errorData: { description: string }
  timestamp: string
}

export function getApiErrorMessage(error: unknown): string | undefined {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.errorData?.description
  }
  return undefined
}

export function isNotFoundError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 404
}
