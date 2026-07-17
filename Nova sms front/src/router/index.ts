import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import type { UserRole } from '@/models/auth.model'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guestOnly?: boolean
    roles?: UserRole[]
    title?: string
    layout?: 'dashboard' | 'marketing'
  }
}

function defaultHome(auth: ReturnType<typeof useAuthStore>) {
  return auth.isSuperAdmin ? { name: 'admin-system-reports' as const } : { name: 'dashboard' as const }
}

function matchedMetaFlag(
  to: RouteLocationNormalized,
  key: 'requiresAuth' | 'guestOnly',
): boolean {
  return to.matched.some((record) => Boolean(record.meta[key]))
}

function matchedRoles(to: RouteLocationNormalized): UserRole[] | undefined {
  for (let i = to.matched.length - 1; i >= 0; i--) {
    const roles = to.matched[i]?.meta.roles
    if (roles?.length) return roles
  }
  return undefined
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MarketingLayout.vue'),
    meta: { layout: 'marketing' },
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/LandingView.vue'),
        meta: { title: 'Home', guestOnly: true },
      },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { guestOnly: true, title: 'Sign in' },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: { guestOnly: true, title: 'Create account' },
      },
      {
        path: 'forgot-password',
        name: 'forgot-password',
        component: () => import('@/views/auth/ForgotPasswordView.vue'),
        meta: { guestOnly: true, title: 'Forgot password' },
      },
      {
        path: 'reset-password',
        name: 'reset-password',
        component: () => import('@/views/auth/ResetPasswordView.vue'),
        meta: { guestOnly: true, title: 'Reset password' },
      },
      {
        path: 'terms',
        name: 'terms',
        component: () => import('@/views/legal/TermsView.vue'),
        meta: { title: 'Terms of Service' },
      },
      {
        path: 'privacy',
        name: 'privacy',
        component: () => import('@/views/legal/PrivacyView.vue'),
        meta: { title: 'Privacy Policy' },
      },
      {
        path: 'acceptable-use',
        name: 'acceptable-use',
        component: () => import('@/views/legal/AcceptableUseView.vue'),
        meta: { title: 'Acceptable Use Policy' },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/DashboardLayout.vue'),
    meta: { requiresAuth: true, layout: 'dashboard' },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: {
          requiresAuth: true,
          title: 'Dashboard',
          roles: ['ORGANIZATION_ADMIN', 'SUPER_ADMIN'],
        },
      },
      {
        path: 'wallet',
        name: 'wallet',
        component: () => import('@/views/dashboard/WalletView.vue'),
        meta: { requiresAuth: true, title: 'Wallet', roles: ['ORGANIZATION_ADMIN'] },
      },
      {
        path: 'send-sms',
        name: 'send-sms',
        component: () => import('@/views/dashboard/SendSmsView.vue'),
        meta: { requiresAuth: true, title: 'Send SMS', roles: ['ORGANIZATION_ADMIN'] },
      },
      {
        path: 'bulk-sms',
        name: 'bulk-sms',
        component: () => import('@/views/dashboard/BulkSmsView.vue'),
        meta: { requiresAuth: true, title: 'Bulk SMS', roles: ['ORGANIZATION_ADMIN'] },
      },
      {
        path: 'contacts',
        name: 'contacts',
        component: () => import('@/views/dashboard/ContactsView.vue'),
        meta: { requiresAuth: true, title: 'Contacts', roles: ['ORGANIZATION_ADMIN'] },
      },
      {
        path: 'sender-ids',
        name: 'sender-ids',
        component: () => import('@/views/dashboard/SenderIdsView.vue'),
        meta: {
          requiresAuth: true,
          title: 'Sender IDs',
          roles: ['ORGANIZATION_ADMIN', 'SUPER_ADMIN'],
        },
      },
      {
        path: 'sms-history',
        name: 'sms-history',
        component: () => import('@/views/dashboard/SmsHistoryView.vue'),
        meta: { requiresAuth: true, title: 'SMS History', roles: ['ORGANIZATION_ADMIN'] },
      },
      {
        path: 'reports',
        name: 'reports',
        component: () => import('@/views/dashboard/ReportsView.vue'),
        meta: { requiresAuth: true, title: 'Reports', roles: ['ORGANIZATION_ADMIN'] },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/dashboard/ProfileView.vue'),
        meta: {
          requiresAuth: true,
          title: 'Profile',
          roles: ['ORGANIZATION_ADMIN', 'SUPER_ADMIN'],
        },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/dashboard/SettingsView.vue'),
        meta: {
          requiresAuth: true,
          title: 'Settings',
          roles: ['ORGANIZATION_ADMIN', 'SUPER_ADMIN'],
        },
      },
      {
        path: 'admin/organizations',
        name: 'admin-organizations',
        component: () => import('@/views/admin/OrganizationsView.vue'),
        meta: { requiresAuth: true, title: 'Organizations', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'admin/topups',
        name: 'admin-topups',
        component: () => import('@/views/admin/TopupsView.vue'),
        meta: { requiresAuth: true, title: 'Wallet Funding', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'admin/sms-monitoring',
        name: 'admin-sms-monitoring',
        component: () => import('@/views/admin/SmsMonitoringView.vue'),
        meta: { requiresAuth: true, title: 'SMS Monitoring', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'admin/system-reports',
        name: 'admin-system-reports',
        component: () => import('@/views/admin/SystemReportsView.vue'),
        meta: { requiresAuth: true, title: 'System Reports', roles: ['SUPER_ADMIN'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    redirect: (to) => {
      const auth = useAuthStore()
      if (auth.isAuthenticated) return defaultHome(auth)
      return { name: 'home', query: to.query, hash: to.hash }
    },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const title = to.meta.title ? `${to.meta.title} · Nova SMS` : 'Nova SMS'
  document.title = title

  if ((auth.accessToken && !auth.user) || (!auth.accessToken && auth.user)) {
    auth.logout(true)
  }

  const requiresAuth = matchedMetaFlag(to, 'requiresAuth')
  const guestOnly = matchedMetaFlag(to, 'guestOnly')
  const loggedIn = auth.isAuthenticated

  if (requiresAuth && !loggedIn) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (guestOnly && loggedIn) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : null
    if (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) {
      return redirect
    }
    return defaultHome(auth)
  }

  const roles = matchedRoles(to)
  if (requiresAuth && roles?.length && auth.user && !roles.includes(auth.user.role)) {
    return defaultHome(auth)
  }

  if (to.name === 'dashboard' && auth.isSuperAdmin) {
    return { name: 'admin-system-reports' }
  }

  return true
})

export default router
