import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth.store'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'https://smsapi.novastack.co.ke/api/v1',
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = useAuthStore()
  const url = config.url || ''
  const isPublicDataBundle =
    url.includes('/data-bundles/offers') ||
    url.includes('/data-bundles/purchase') ||
    /\/data-bundles\/status\//.test(url)

  // Public data-bundle APIs must not send a stale JWT (avoids false 401s / session redirects).
  if (isPublicDataBundle) {
    delete config.headers.Authorization
  } else if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

let handlingSessionExpiry = false

function forceLogoutToLogin() {
  if (handlingSessionExpiry) return
  handlingSessionExpiry = true
  const auth = useAuthStore()
  auth.logout(true)
  if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
    window.location.assign('/login?session=expired')
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const auth = useAuthStore()
    const hadBearer = Boolean(original?.headers?.Authorization || auth.accessToken)
    const messageBody = error.response?.data?.message || ''

    const url = original?.url || ''
    const isPublicDataBundle =
      url.includes('/data-bundles/offers') ||
      url.includes('/data-bundles/purchase') ||
      /\/data-bundles\/status\//.test(url)

    const sessionExpired =
      !isPublicDataBundle &&
      hadBearer &&
      (status === 401 ||
        (status === 403 &&
          (!messageBody ||
            /session expired|unauthenticated|invalid token|jwt|unauthorized/i.test(messageBody))))

    if (sessionExpired && original && !original._retry) {
      original._retry = true
      forceLogoutToLogin()
      return Promise.reject(new Error(messageBody || 'Session expired. Please sign in again.'))
    }

    const message =
      messageBody || error.message || 'Something went wrong. Please try again.'

    return Promise.reject(new Error(message))
  },
)

export default api
