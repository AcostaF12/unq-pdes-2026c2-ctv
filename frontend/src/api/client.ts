import axios from 'axios'
import { getToken, clearSession } from './session'
import { notifyUnauthorized } from './unauthorized'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && !isAuthEndpoint(error.config?.url)) {
      clearSession()
      notifyUnauthorized()
    }
    return Promise.reject(error)
  },
)

function isAuthEndpoint(url: string | undefined): boolean {
  if (!url) {
    return false
  }
  return url.includes('/auth/login') || url.includes('/auth/register')
}
