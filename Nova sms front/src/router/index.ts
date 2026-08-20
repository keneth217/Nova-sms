import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import type { UserRole } from '@/models/auth.model'
import { applySeo, applySeoFromRoute } from '@/composables/useSeo'
import { publicDocSeo } from '@/seo/doc-seo'
import type { PublicDocSlug } from '@/seo/public-paths'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guestOnly?: boolean
    roles?: UserRole[]
    title?: string
    seoTitle?: string
    description?: string
    robots?: string
    ogImage?: string
    ogType?: string
    canonicalPath?: string
    layout?: 'dashboard' | 'marketing'
    channel?: 'SMS' | 'WHATSAPP'
    docId?: string
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
        meta: {
          title: 'Home',
          guestOnly: true,
          seoTitle: 'Nova SMS — Bulk SMS Gateway & M-Pesa API for Kenya',
          description:
            'Send bulk SMS in Kenya with Nova SMS. Integrate M-Pesa STK Push and Paybill top-up, track delivery, and call a REST API from your backend.',
        },
      },
      {
        path: 'sms-gateway',
        name: 'sms-gateway',
        component: () => import('@/views/marketing/SmsGatewayView.vue'),
        meta: {
          title: 'Bulk SMS gateway',
          seoTitle: 'Bulk SMS Gateway for Kenya | Nova SMS',
          description:
            'Prepaid bulk SMS for Kenyan businesses and events. Contacts, sender IDs, delivery reports, and M-Pesa wallet top-up.',
        },
      },
      {
        path: 'mpesa-stk-push',
        name: 'mpesa-stk-push',
        component: () => import('@/views/marketing/StkPushView.vue'),
        meta: {
          title: 'M-Pesa STK Push',
          seoTitle: 'M-Pesa STK Push API for SMS wallet top-up | Nova SMS',
          description:
            'How Nova SMS uses Safaricom STK Push to fund SMS wallets, including payment lifecycle, callbacks, and polling.',
        },
      },
      {
        path: 'mpesa-paybill',
        name: 'mpesa-paybill',
        component: () => import('@/views/marketing/PaybillView.vue'),
        meta: {
          title: 'M-Pesa Paybill C2B',
          seoTitle: 'M-Pesa Paybill C2B payments for SMS wallets | Nova SMS',
          description:
            'Fund a Nova SMS wallet with Paybill C2B. Account number is the organization M-Pesa reference. Receipts are credited once.',
        },
      },
      {
        path: 'webhooks',
        name: 'webhooks',
        component: () => import('@/views/marketing/WebhooksView.vue'),
        meta: {
          title: 'M-Pesa callbacks',
          seoTitle: 'M-Pesa callbacks, retries, and payment status | Nova SMS',
          description:
            'Safaricom STK and C2B callbacks are processed by Nova SMS. Poll wallet status for idempotent credits and SMS delivery.',
        },
      },
      {
        path: 'sms-api',
        name: 'sms-api',
        component: () => import('@/views/marketing/SmsApiView.vue'),
        meta: {
          title: 'SMS API',
          seoTitle: 'Nova SMS API — send SMS and M-Pesa wallet top-up',
          description:
            'REST API authentication, send SMS, check status, handle errors, and start M-Pesa STK Push from your backend.',
        },
      },
      {
        path: 'pricing',
        name: 'pricing',
        component: () => import('@/views/marketing/PricingView.vue'),
        meta: {
          title: 'Pricing',
          seoTitle: 'Nova SMS pricing — prepaid SMS wallet in Kenya',
          description:
            'Nova SMS is prepaid in KES. Top up with M-Pesa and pay per SMS from the organization wallet. Event and business accounts.',
        },
      },
      {
        path: 'about',
        name: 'about',
        component: () => import('@/views/marketing/AboutView.vue'),
        meta: {
          title: 'About',
          seoTitle: 'About Nova SMS',
          description:
            'Nova SMS is Novastack’s bulk SMS platform for Kenya, with prepaid wallets and M-Pesa top-up.',
        },
      },
      {
        path: 'contact',
        name: 'contact',
        component: () => import('@/views/marketing/ContactView.vue'),
        meta: {
          title: 'Contact',
          seoTitle: 'Contact Nova SMS',
          description: 'Email Nova SMS support or create an account to send SMS and top up with M-Pesa.',
        },
      },
      {
        path: 'faq',
        name: 'faq',
        component: () => import('@/views/marketing/FaqView.vue'),
        meta: {
          title: 'FAQ',
          seoTitle: 'Nova SMS FAQ — STK Push, Paybill, API, and wallets',
          description:
            'Answers about Nova SMS, M-Pesa STK Push, C2B Paybill funding, API keys, and payment status.',
        },
      },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: {
          guestOnly: true,
          title: 'Sign in',
          robots: 'noindex,nofollow',
          description: 'Sign in to your Nova SMS organization dashboard.',
        },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: { guestOnly: true, title: 'Create account', robots: 'noindex,nofollow' },
      },
      {
        path: 'forgot-password',
        name: 'forgot-password',
        component: () => import('@/views/auth/ForgotPasswordView.vue'),
        meta: { guestOnly: true, title: 'Forgot password', robots: 'noindex,nofollow' },
      },
      {
        path: 'reset-password',
        name: 'reset-password',
        component: () => import('@/views/auth/ResetPasswordView.vue'),
        meta: { guestOnly: true, title: 'Reset password', robots: 'noindex,nofollow' },
      },
      {
        path: 'terms',
        name: 'terms',
        component: () => import('@/views/legal/TermsView.vue'),
        meta: {
          title: 'Terms of Service',
          seoTitle: 'Terms of Service | Nova SMS',
          description: 'Terms of Service for the Nova SMS bulk messaging platform.',
        },
      },
      {
        path: 'privacy',
        name: 'privacy',
        component: () => import('@/views/legal/PrivacyView.vue'),
        meta: {
          title: 'Privacy Policy',
          seoTitle: 'Privacy Policy | Nova SMS',
          description: 'How Nova SMS collects and uses organization, message, and payment data.',
        },
      },
      {
        path: 'acceptable-use',
        name: 'acceptable-use',
        component: () => import('@/views/legal/AcceptableUseView.vue'),
        meta: {
          title: 'Acceptable Use Policy',
          seoTitle: 'Acceptable Use Policy | Nova SMS',
          description: 'Rules for sending SMS on Nova SMS, including prohibited content and consent.',
        },
      },
      {
        path: 'data-bundles',
        name: 'data-bundles',
        component: () => import('@/views/dashboard/DataBundlesView.vue'),
        meta: {
          title: 'Data Bundles',
          seoTitle: 'Safaricom data bundles | Nova SMS',
          description:
            'Browse Safaricom data offers by phone number and purchase through Nova SMS. No account required to look up offers.',
        },
      },
      {
        path: 'developers',
        name: 'developers',
        component: () => import('@/views/docs/DeveloperDocsView.vue'),
        meta: {
          title: 'API documentation',
          seoTitle: 'Nova SMS API documentation',
          description:
            'Nova SMS REST API for sending SMS, checking delivery, wallet balance, and M-Pesa STK Push top-up from your backend.',
        },
      },
      {
        path: 'developers/:slug',
        name: 'developer-doc',
        component: () => import('@/views/docs/PublicDocView.vue'),
        meta: {
          title: 'API documentation',
          seoTitle: 'Nova SMS API documentation',
          description:
            'Nova SMS REST API reference for SMS, wallet top-up, authentication, errors, and language guides.',
        },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/DashboardLayout.vue'),
    meta: { requiresAuth: true, layout: 'dashboard', robots: 'noindex,nofollow' },
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
        meta: { requiresAuth: true, title: 'Send SMS', roles: ['ORGANIZATION_ADMIN'], channel: 'SMS' },
      },
      {
        path: 'send-whatsapp',
        name: 'send-whatsapp',
        component: () => import('@/views/dashboard/SendSmsView.vue'),
        meta: {
          requiresAuth: true,
          title: 'Send WhatsApp',
          roles: ['ORGANIZATION_ADMIN'],
          channel: 'WHATSAPP',
        },
      },
      {
        path: 'bulk-sms',
        name: 'bulk-sms',
        component: () => import('@/views/dashboard/BulkSmsView.vue'),
        meta: { requiresAuth: true, title: 'Bulk SMS', roles: ['ORGANIZATION_ADMIN'], channel: 'SMS' },
      },
      {
        path: 'bulk-whatsapp',
        name: 'bulk-whatsapp',
        component: () => import('@/views/dashboard/BulkSmsView.vue'),
        meta: {
          requiresAuth: true,
          title: 'Bulk WhatsApp',
          roles: ['ORGANIZATION_ADMIN'],
          channel: 'WHATSAPP',
        },
      },
      {
        path: 'data-bundles/history',
        name: 'data-bundle-history',
        component: () => import('@/views/dashboard/BundleHistoryView.vue'),
        meta: { requiresAuth: true, title: 'Bundle History', roles: ['ORGANIZATION_ADMIN'] },
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
        path: 'api-clients',
        name: 'api-clients',
        component: () => import('@/views/dashboard/ApiClientsView.vue'),
        meta: { requiresAuth: true, title: 'API clients', roles: ['ORGANIZATION_ADMIN'] },
      },
      {
        path: 'sms-history',
        name: 'sms-history',
        component: () => import('@/views/dashboard/SmsHistoryView.vue'),
        meta: { requiresAuth: true, title: 'SMS History', roles: ['ORGANIZATION_ADMIN'], channel: 'SMS' },
      },
      {
        path: 'whatsapp-history',
        name: 'whatsapp-history',
        component: () => import('@/views/dashboard/SmsHistoryView.vue'),
        meta: {
          requiresAuth: true,
          title: 'WhatsApp History',
          roles: ['ORGANIZATION_ADMIN'],
          channel: 'WHATSAPP',
        },
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
        path: 'admin/collections',
        name: 'admin-collections',
        component: () => import('@/views/admin/CollectionsView.vue'),
        meta: { requiresAuth: true, title: 'Paybill collections', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'admin/sms-monitoring',
        name: 'admin-sms-monitoring',
        component: () => import('@/views/admin/SmsMonitoringView.vue'),
        meta: { requiresAuth: true, title: 'SMS Monitoring', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'admin/sms-settings',
        name: 'admin-sms-settings',
        component: () => import('@/views/admin/SmsSettingsView.vue'),
        meta: { requiresAuth: true, title: 'SMS settings', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'admin/api-clients',
        redirect: { name: 'admin-developer-clients' },
      },
      {
        path: 'admin/developer',
        component: () => import('@/views/admin/developer/DeveloperPortalLayout.vue'),
        meta: { requiresAuth: true, roles: ['SUPER_ADMIN'] },
        redirect: { name: 'admin-developer-overview' },
        children: [
          {
            path: '',
            name: 'admin-developer-overview',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'API Overview', docId: 'overview' },
          },
          {
            path: 'quick-start',
            name: 'admin-developer-quick-start',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Quick Start', docId: 'quick-start' },
          },
          {
            path: 'authentication',
            name: 'admin-developer-authentication',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Authentication', docId: 'authentication' },
          },
          {
            path: 'send-sms',
            name: 'admin-developer-send-sms',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Send SMS', docId: 'send-sms' },
          },
          {
            path: 'bulk-sms',
            name: 'admin-developer-bulk-sms',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Bulk SMS', docId: 'bulk-sms' },
          },
          {
            path: 'retry-failed',
            name: 'admin-developer-retry-failed',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Retry failed SMS', docId: 'retry-failed' },
          },
          {
            path: 'status',
            name: 'admin-developer-status',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'SMS Status', docId: 'status' },
          },
          {
            path: 'history',
            name: 'admin-developer-history',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'SMS History', docId: 'history' },
          },
          {
            path: 'wallet',
            name: 'admin-developer-wallet',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Wallet', docId: 'wallet' },
          },
          {
            path: 'errors',
            name: 'admin-developer-errors',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'API Errors', docId: 'errors' },
          },
          {
            path: 'idempotency',
            name: 'admin-developer-idempotency',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Idempotency', docId: 'idempotency' },
          },
          {
            path: 'rate-limits',
            name: 'admin-developer-rate-limits',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Rate Limits', docId: 'rate-limits' },
          },
          {
            path: 'integration/spring-boot',
            name: 'admin-developer-spring',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Spring Boot', docId: 'spring-boot' },
          },
          {
            path: 'integration/nodejs',
            name: 'admin-developer-nodejs',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Node.js', docId: 'nodejs' },
          },
          {
            path: 'integration/php',
            name: 'admin-developer-php',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'PHP', docId: 'php' },
          },
          {
            path: 'integration/python',
            name: 'admin-developer-python',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Python', docId: 'python' },
          },
          {
            path: 'integration/generic-http',
            name: 'admin-developer-http',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'Generic HTTP', docId: 'generic-http' },
          },
          {
            path: 'clients',
            name: 'admin-developer-clients',
            component: () => import('@/views/dashboard/ApiClientsView.vue'),
            meta: { title: 'API Clients' },
          },
          {
            path: 'usage',
            name: 'admin-developer-usage',
            component: () => import('@/views/admin/developer/DeveloperUsageView.vue'),
            meta: { title: 'API Usage' },
          },
          {
            path: 'console',
            name: 'admin-developer-console',
            component: () => import('@/views/admin/developer/DeveloperConsoleView.vue'),
            meta: { title: 'API Test Console' },
          },
          {
            path: 'provider',
            name: 'admin-developer-provider',
            component: () => import('@/views/admin/developer/DeveloperDocView.vue'),
            meta: { title: 'TalkSasa (internal)', docId: 'provider' },
          },
        ],
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
    component: () => import('@/layouts/MarketingLayout.vue'),
    meta: {
      title: 'Page not found',
      robots: 'noindex,nofollow',
      layout: 'marketing',
      description: 'That Nova SMS page does not exist.',
    },
    children: [
      {
        path: '',
        name: 'not-found-page',
        component: () => import('@/views/NotFoundView.vue'),
        meta: { title: 'Page not found', robots: 'noindex,nofollow' },
      },
    ],
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

router.afterEach((to) => {
  if (to.name === 'developer-doc') {
    const slug = String(to.params.slug || '') as PublicDocSlug
    const seo = publicDocSeo[slug]
    if (seo) {
      applySeo({
        title: seo.title,
        description: seo.description,
        path: to.path,
      })
      return
    }
    applySeo({
      title: 'Documentation not found · Nova SMS',
      description: 'That Nova SMS API page does not exist.',
      path: to.path,
      robots: 'noindex,nofollow',
    })
    return
  }
  applySeoFromRoute(to)
})

export default router
