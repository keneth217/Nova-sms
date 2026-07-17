import type { AuthResponse, AuthUser } from '@/models/auth.model'
import type { AdminOrganization, PlatformOverview } from '@/models/organization.model'
import type { WalletBalance, WalletTransaction, PaymentInstructions } from '@/models/wallet.model'
import type { SmsMessage } from '@/models/sms.model'
import type { SenderId } from '@/models/senderid.model'
import type { Contact, ContactGroup } from '@/models/contact.model'
import type {
  ActivityItem,
  CampaignSummary,
  DailyVolumePoint,
  DashboardReport,
  MonthlyUsagePoint,
  UsageSummaryRow,
} from '@/models/report.model'
import type { Page } from '@/models/auth.model'

const now = Date.now()
const hoursAgo = (h: number) => new Date(now - h * 3600_000).toISOString()
const daysAgo = (d: number) => new Date(now - d * 86_400_000).toISOString()

export const mockOrgAdmin: AuthResponse = {
  accessToken: 'mock-org-token',
  tokenType: 'Bearer',
  userId: 'u-org-1',
  email: 'admin@acme.co.ke',
  fullName: 'Jane Wanjiku',
  role: 'ORGANIZATION_ADMIN',
  organizationId: 'org-acme',
  organizationName: 'Acme Logistics Ltd',
  accountType: 'BUSINESS',
  expiresAt: null,
}

export const mockSuperAdmin: AuthResponse = {
  accessToken: 'mock-super-token',
  tokenType: 'Bearer',
  userId: 'u-super-1',
  email: 'admin@novastack.com',
  fullName: 'Novastack Super Admin',
  role: 'SUPER_ADMIN',
  organizationId: null,
  organizationName: null,
  accountType: null,
  expiresAt: null,
}

export const mockEventAdmin: AuthResponse = {
  accessToken: 'mock-event-token',
  tokenType: 'Bearer',
  userId: 'u-event-1',
  email: 'family@example.com',
  fullName: 'Kamau Otieno',
  role: 'ORGANIZATION_ADMIN',
  organizationId: 'org-event',
  organizationName: 'Kamau Family Gathering',
  accountType: 'EVENT',
  expiresAt: new Date(now + 5 * 86_400_000).toISOString(),
}

export const mockOrganizationName = 'Acme Logistics Ltd'

export const mockWalletBalance: WalletBalance = {
  walletId: 'wal-1',
  organizationId: 'org-acme',
  balance: 24850.5,
  currency: 'KES',
  smsCost: 1,
}

export const mockPaymentInstructions: PaymentInstructions = {
  paybill: '522522',
  accountNumber: 'ACME-78421',
  businessName: 'Nova SMS Gateway',
  notes: [
    'Use your organization account reference as the M-Pesa account number.',
    'Top-ups usually reflect within 1–2 minutes after confirmation.',
    'For bank transfers, contact support with your transaction reference.',
  ],
}

export const mockTransactions: WalletTransaction[] = [
  {
    id: 'tx-1',
    organizationId: 'org-acme',
    type: 'TOPUP',
    amount: 10000,
    balanceBefore: 14850.5,
    balanceAfter: 24850.5,
    reference: 'MPESA-QWE123',
    description: 'M-Pesa STK top-up',
    mpesaReceipt: 'QWE123XYZ',
    phoneNumber: '254712345678',
    checkoutRequestId: 'ws_CO_1',
    status: 'COMPLETED',
    resultCode: '0',
    resultDesc: 'Success',
    createdAt: hoursAgo(5),
  },
  {
    id: 'tx-2',
    organizationId: 'org-acme',
    type: 'SMS_DEBIT',
    amount: -128,
    balanceBefore: 14978.5,
    balanceAfter: 14850.5,
    reference: 'BATCH-8821',
    description: 'Bulk SMS campaign debit',
    mpesaReceipt: null,
    phoneNumber: null,
    checkoutRequestId: null,
    status: 'COMPLETED',
    resultCode: null,
    resultDesc: null,
    createdAt: hoursAgo(8),
  },
  {
    id: 'tx-3',
    organizationId: 'org-acme',
    type: 'TOPUP',
    amount: 5000,
    balanceBefore: 9978.5,
    balanceAfter: 14978.5,
    reference: 'MPESA-ABC456',
    description: 'Paybill top-up',
    mpesaReceipt: 'ABC456DEF',
    phoneNumber: '254700112233',
    checkoutRequestId: null,
    status: 'COMPLETED',
    resultCode: '0',
    resultDesc: 'Success',
    createdAt: daysAgo(2),
  },
  {
    id: 'tx-4',
    organizationId: 'org-acme',
    type: 'SMS_DEBIT',
    amount: -42.4,
    balanceBefore: 10020.9,
    balanceAfter: 9978.5,
    reference: 'SMS-9910',
    description: 'Single SMS sends',
    mpesaReceipt: null,
    phoneNumber: null,
    checkoutRequestId: null,
    status: 'COMPLETED',
    resultCode: null,
    resultDesc: null,
    createdAt: daysAgo(3),
  },
]

export const mockSenderIds: SenderId[] = [
  {
    id: 'sid-1',
    senderName: 'ACME',
    status: 'APPROVED',
    platformDefault: false,
    reason: null,
    createdAt: daysAgo(30),
    updatedAt: daysAgo(28),
  },
  {
    id: 'sid-2',
    senderName: 'ACMEALERT',
    status: 'PENDING',
    platformDefault: false,
    reason: null,
    createdAt: daysAgo(1),
    updatedAt: daysAgo(1),
  },
  {
    id: 'sid-3',
    senderName: 'NOVASMS',
    status: 'APPROVED',
    platformDefault: true,
    reason: null,
    createdAt: daysAgo(90),
    updatedAt: daysAgo(90),
  },
  {
    id: 'sid-4',
    senderName: 'ACMEPROMO',
    status: 'REJECTED',
    platformDefault: false,
    reason: 'Sender ID conflicts with an existing brand.',
    createdAt: daysAgo(10),
    updatedAt: daysAgo(8),
  },
]

export const mockSmsHistory: SmsMessage[] = [
  {
    id: 'sms-1',
    recipient: '254712345678',
    content: 'Your delivery #A4821 is out for delivery today.',
    senderId: 'ACME',
    status: 'DELIVERED',
    cost: 1,
    batchId: null,
    scheduledAt: null,
    createdAt: hoursAgo(1),
    sentAt: hoursAgo(1),
    deliveredAt: hoursAgo(0.9),
    failureReason: null,
  },
  {
    id: 'sms-2',
    recipient: '254722334455',
    content: 'OTP: 482910. Valid for 5 minutes.',
    senderId: 'ACME',
    status: 'DELIVERED',
    cost: 1,
    batchId: null,
    scheduledAt: null,
    createdAt: hoursAgo(2),
    sentAt: hoursAgo(2),
    deliveredAt: hoursAgo(1.95),
    failureReason: null,
  },
  {
    id: 'sms-3',
    recipient: '254733221100',
    content: 'Flash sale: 20% off logistics fees this weekend.',
    senderId: 'ACME',
    status: 'FAILED',
    cost: 1,
    batchId: 'BATCH-8821',
    scheduledAt: null,
    createdAt: hoursAgo(8),
    sentAt: hoursAgo(8),
    deliveredAt: null,
    failureReason: 'Invalid destination number',
  },
  {
    id: 'sms-4',
    recipient: '254700998877',
    content: 'Reminder: Invoice INV-204 is due tomorrow.',
    senderId: 'NOVASMS',
    status: 'SENT',
    cost: 1,
    batchId: null,
    scheduledAt: null,
    createdAt: hoursAgo(3),
    sentAt: hoursAgo(3),
    deliveredAt: null,
    failureReason: null,
  },
  {
    id: 'sms-5',
    recipient: '254711223344',
    content: 'Campaign kickoff starts at 09:00. Reply STOP to opt out.',
    senderId: 'ACME',
    status: 'QUEUED',
    cost: 1,
    batchId: 'BATCH-9001',
    scheduledAt: null,
    createdAt: hoursAgo(0.2),
    sentAt: null,
    deliveredAt: null,
    failureReason: null,
  },
  {
    id: 'sms-6',
    recipient: '254755667788',
    content: 'Scheduled maintenance notice for Saturday 02:00–04:00.',
    senderId: 'ACME',
    status: 'SCHEDULED',
    cost: 1,
    batchId: 'BATCH-9100',
    scheduledAt: daysAgo(-1),
    createdAt: hoursAgo(12),
    sentAt: null,
    deliveredAt: null,
    failureReason: null,
  },
]

export const mockGroups: ContactGroup[] = [
  {
    id: 'grp-1',
    name: 'Customers',
    description: 'Active retail customers',
    contactCount: 1240,
    createdAt: daysAgo(40),
  },
  {
    id: 'grp-2',
    name: 'Drivers',
    description: 'Fleet drivers and partners',
    contactCount: 86,
    createdAt: daysAgo(20),
  },
  {
    id: 'grp-3',
    name: 'VIP Clients',
    description: 'Priority account holders',
    contactCount: 42,
    createdAt: daysAgo(12),
  },
]

export const mockContacts: Contact[] = [
  {
    id: 'c-1',
    phone: '254712345678',
    firstName: 'Peter',
    lastName: 'Otieno',
    email: 'peter@example.com',
    groupIds: ['grp-1'],
    groupNames: ['Customers'],
    createdAt: daysAgo(15),
  },
  {
    id: 'c-2',
    phone: '254722334455',
    firstName: 'Mary',
    lastName: 'Njeri',
    email: 'mary@example.com',
    groupIds: ['grp-1', 'grp-3'],
    groupNames: ['Customers', 'VIP Clients'],
    createdAt: daysAgo(12),
  },
  {
    id: 'c-3',
    phone: '254700112233',
    firstName: 'Samuel',
    lastName: 'Kariuki',
    email: null,
    groupIds: ['grp-2'],
    groupNames: ['Drivers'],
    createdAt: daysAgo(8),
  },
  {
    id: 'c-4',
    phone: '254733221100',
    firstName: 'Amina',
    lastName: 'Hassan',
    email: 'amina@example.com',
    groupIds: ['grp-3'],
    groupNames: ['VIP Clients'],
    createdAt: daysAgo(5),
  },
]

export const mockDashboard: DashboardReport = {
  smsSentToday: 1842,
  smsSentThisMonth: 42890,
  deliveredCount: 40120,
  failedCount: 980,
  deliveryRate: 97.6,
  walletBalance: 24850.5,
  walletUsageToday: 1473.6,
  walletUsageThisMonth: 34312,
  costToday: 1473.6,
  costThisMonth: 34312,
  activeSenderIds: 2,
}

export const mockDailyVolume: DailyVolumePoint[] = Array.from({ length: 14 }, (_, i) => {
  const date = new Date(now - (13 - i) * 86_400_000)
  const sent = 900 + Math.round(Math.sin(i / 2) * 200) + i * 40
  const failed = Math.round(sent * 0.03)
  return {
    date: date.toISOString().slice(0, 10),
    sent,
    delivered: sent - failed,
    failed,
  }
})

export const mockMonthlyUsage: MonthlyUsagePoint[] = [
  { month: 'Feb', volume: 28100, cost: 22480 },
  { month: 'Mar', volume: 31240, cost: 24992 },
  { month: 'Apr', volume: 29880, cost: 23904 },
  { month: 'May', volume: 35620, cost: 28496 },
  { month: 'Jun', volume: 40110, cost: 32088 },
  { month: 'Jul', volume: 42890, cost: 34312 },
]

export const mockCampaigns: CampaignSummary[] = [
  {
    id: 'camp-1',
    name: 'Weekend promo blast',
    recipients: 1600,
    delivered: 1562,
    failed: 38,
    cost: 1280,
    createdAt: daysAgo(1),
  },
  {
    id: 'camp-2',
    name: 'Invoice reminders',
    recipients: 420,
    delivered: 411,
    failed: 9,
    cost: 336,
    createdAt: daysAgo(3),
  },
  {
    id: 'camp-3',
    name: 'Driver route updates',
    recipients: 86,
    delivered: 86,
    failed: 0,
    cost: 68.8,
    createdAt: daysAgo(4),
  },
]

export const mockActivity: ActivityItem[] = [
  {
    id: 'act-1',
    type: 'sms',
    title: 'Bulk campaign queued',
    description: '1,600 recipients · ACME',
    timestamp: hoursAgo(0.5),
    status: 'QUEUED',
  },
  {
    id: 'act-2',
    type: 'topup',
    title: 'Wallet topped up',
    description: 'KES 10,000 via M-Pesa',
    timestamp: hoursAgo(5),
    status: 'COMPLETED',
  },
  {
    id: 'act-3',
    type: 'sender_id',
    title: 'Sender ID requested',
    description: 'ACMEALERT awaiting review',
    timestamp: daysAgo(1),
    status: 'PENDING',
  },
  {
    id: 'act-4',
    type: 'contact',
    title: 'Contacts imported',
    description: '84 contacts added to Drivers',
    timestamp: daysAgo(2),
    status: 'COMPLETED',
  },
]

export const mockUsageSummary: UsageSummaryRow[] = [
  { period: 'Today', sent: 1842, delivered: 1798, failed: 44, cost: 1473.6, deliveryRate: 97.6 },
  {
    period: 'This week',
    sent: 11240,
    delivered: 10980,
    failed: 260,
    cost: 8992,
    deliveryRate: 97.7,
  },
  {
    period: 'This month',
    sent: 42890,
    delivered: 40120,
    failed: 980,
    cost: 34312,
    deliveryRate: 97.6,
  },
]

export const mockOrganizations: AdminOrganization[] = [
  {
    id: 'org-acme',
    name: 'Acme Logistics Ltd',
    email: 'admin@acme.co.ke',
    phone: '254712345678',
    apiKey: 'nsk_acme_demo',
    mpesaAccountRef: 'ACME-78421',
    status: 'ACTIVE',
    accountType: 'BUSINESS',
    expiresAt: null,
    createdAt: daysAgo(60),
    smsCost: 1,
    walletBalance: 24850.5,
    currency: 'KES',
    userCount: 3,
  },
  {
    id: 'org-2',
    name: 'Safari Retail',
    email: 'ops@safariretail.co.ke',
    phone: '254722000111',
    apiKey: 'nsk_safari_demo',
    mpesaAccountRef: 'SAFARI-12001',
    status: 'ACTIVE',
    accountType: 'BUSINESS',
    expiresAt: null,
    createdAt: daysAgo(45),
    smsCost: 0.75,
    walletBalance: 8120,
    currency: 'KES',
    userCount: 2,
  },
  {
    id: 'org-event',
    name: 'Kamau Family Gathering',
    email: 'family@example.com',
    phone: '254711223344',
    apiKey: 'nsk_event_demo',
    mpesaAccountRef: 'EVENT-9901',
    status: 'ACTIVE',
    accountType: 'EVENT',
    expiresAt: new Date(now + 5 * 86_400_000).toISOString(),
    createdAt: daysAgo(2),
    smsCost: 1,
    walletBalance: 500,
    currency: 'KES',
    userCount: 1,
  },
  {
    id: 'org-3',
    name: 'Coastal Clinics',
    email: 'it@coastalclinics.ke',
    phone: '254733445566',
    apiKey: 'nsk_coastal_demo',
    mpesaAccountRef: 'COASTAL-3302',
    status: 'PENDING',
    accountType: 'BUSINESS',
    expiresAt: null,
    createdAt: daysAgo(2),
    smsCost: 1,
    walletBalance: 0,
    currency: 'KES',
    userCount: 1,
  },
  {
    id: 'org-4',
    name: 'Metro School Board',
    email: 'comms@metroschools.ke',
    phone: '254700998877',
    apiKey: 'nsk_metro_demo',
    mpesaAccountRef: 'METRO-4410',
    status: 'SUSPENDED',
    accountType: 'BUSINESS',
    expiresAt: null,
    createdAt: daysAgo(90),
    smsCost: 0.7,
    walletBalance: 320,
    currency: 'KES',
    userCount: 4,
  },
]

export const mockPlatformOverview: PlatformOverview = {
  organizations: 48,
  users: 126,
  superAdmins: 2,
  totalSmsSent: 1_284_500,
  revenue: 942_180,
  pendingSenderIds: 7,
  pendingTopups: 3,
}

export function toPage<T>(items: T[], page = 0, size = 20): Page<T> {
  const start = page * size
  const content = items.slice(start, start + size)
  const totalElements = items.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  return {
    content,
    totalElements,
    totalPages,
    size,
    number: page,
    first: page === 0,
    last: page >= totalPages - 1,
    empty: content.length === 0,
    numberOfElements: content.length,
  }
}

export function authUserFromResponse(auth: AuthResponse): AuthUser {
  return {
    userId: auth.userId,
    email: auth.email,
    fullName: auth.fullName,
    role: auth.role,
    organizationId: auth.organizationId,
    organizationName: auth.organizationName ?? null,
    accountType: auth.accountType ?? null,
    expiresAt: auth.expiresAt ?? null,
  }
}
