import type { AuthResponse, AuthUser } from '@/models/auth.model'

export function authUserFromResponse(auth: AuthResponse): AuthUser {
  return {
    userId: auth.userId,
    email: auth.email,
    phone: auth.phone ?? null,
    fullName: auth.fullName,
    role: auth.role,
    organizationId: auth.organizationId,
    organizationName: auth.organizationName ?? null,
    accountType: auth.accountType ?? null,
    expiresAt: auth.expiresAt ?? null,
  }
}
