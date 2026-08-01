import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type {
  AuthResponse,
  AuthUser,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  OrganizationRegisterRequest,
  ResetPasswordRequest,
  UserRole,
} from '@/models/auth.model'
import type { Organization } from '@/models/organization.model'
import type { User } from '@/models/user.model'
import { authService } from '@/api/auth.service'
import { authUserFromResponse } from '@/utils/auth'
import { useOrganizationStore } from '@/stores/organization.store'

const TOKEN_KEY = 'nova_sms_token'
const USER_KEY = 'nova_sms_user'

function loadStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<AuthUser | null>(loadStoredUser())
  const loading = ref(false)
  const error = ref<string | null>(null)
  const registeredOrg = ref<Organization | null>(null)
  const profile = ref<User | null>(null)

  const isAuthenticated = computed(() => Boolean(accessToken.value && user.value))
  const isSuperAdmin = computed(() => user.value?.role === 'SUPER_ADMIN')
  const isOrgAdmin = computed(() => user.value?.role === 'ORGANIZATION_ADMIN')
  const role = computed<UserRole | null>(() => user.value?.role ?? null)
  const displayName = computed(() => user.value?.fullName ?? 'User')

  function persist(auth: AuthResponse) {
    accessToken.value = auth.accessToken
    user.value = authUserFromResponse(auth)
    localStorage.setItem(TOKEN_KEY, auth.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    if (auth.organizationName) {
      useOrganizationStore().setOrganizationName(auth.organizationName)
    }
  }

  async function login(payload: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      const auth = await authService.login(payload)
      persist(auth)
      if (auth.organizationId && !auth.organizationName) {
        try {
          await useOrganizationStore().fetchCurrentOrganization()
        } catch {
          void 0
        }
      }
      return auth
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Login failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function register(payload: OrganizationRegisterRequest) {
    loading.value = true
    error.value = null
    try {
      registeredOrg.value = await authService.register(payload)
      return registeredOrg.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Registration failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function tryRefreshToken(): Promise<boolean> {
    if (!accessToken.value) return false
    try {
      const refreshed = await authService.refreshToken(accessToken.value)
      if (!refreshed) return false
      persist(refreshed)
      return true
    } catch {
      return false
    }
  }

  async function fetchProfile() {
    loading.value = true
    error.value = null
    try {
      profile.value = await authService.getProfile()
      if (user.value) {
        user.value = {
          ...user.value,
          userId: profile.value.id,
          email: profile.value.email,
          fullName: profile.value.fullName,
          role: profile.value.role,
          organizationId: profile.value.organizationId,
          organizationName: profile.value.organizationName,
        }
        localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      }
      return profile.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load profile'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function changePassword(payload: ChangePasswordRequest) {
    loading.value = true
    error.value = null
    try {
      return await authService.changePassword(payload)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to change password'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function forgotPassword(payload: ForgotPasswordRequest) {
    loading.value = true
    error.value = null
    try {
      return await authService.forgotPassword(payload)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to request password reset'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function resetPassword(payload: ResetPasswordRequest) {
    loading.value = true
    error.value = null
    try {
      return await authService.resetPassword(payload)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to reset password'
      throw e
    } finally {
      loading.value = false
    }
  }

  function logout(silent = false) {
    accessToken.value = null
    user.value = null
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    useOrganizationStore().clearCurrentOrganization()
    if (!silent) error.value = null
  }

  function hasRole(...roles: UserRole[]) {
    return user.value ? roles.includes(user.value.role) : false
  }

  return {
    accessToken,
    user,
    loading,
    error,
    registeredOrg,
    profile,
    isAuthenticated,
    isSuperAdmin,
    isOrgAdmin,
    role,
    displayName,
    login,
    register,
    tryRefreshToken,
    fetchProfile,
    changePassword,
    forgotPassword,
    resetPassword,
    logout,
    hasRole,
  }
})
