import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth.store'

const api = axios.create({
  baseURL: 'https://smsapi.novastack.co.ke/api/v1',
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

let refreshing: Promise<boolean> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined

    if (status === 401 && original && !original._retry) {
      original._retry = true
      const auth = useAuthStore()

      if (!refreshing) {
        refreshing = auth.tryRefreshToken().finally(() => {
          refreshing = null
        })
      }

      const ok = await refreshing
      if (ok && auth.accessToken) {
        original.headers.Authorization = `Bearer ${auth.accessToken}`
        return api(original)
      }

      auth.logout(true)
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
        window.location.assign('/login?session=expired')
      }
    }

    const message =
      error.response?.data?.message || error.message || 'Something went wrong. Please try again.'

    return Promise.reject(new Error(message))
  },
)

export default api
