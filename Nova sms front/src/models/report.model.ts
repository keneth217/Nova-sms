export interface DashboardReport {
  smsSentToday: number
  smsSentThisMonth: number
  deliveredCount: number
  failedCount: number
  deliveryRate: number
  walletBalance: number
  walletUsageToday: number
  walletUsageThisMonth: number
  costToday: number
  costThisMonth: number
  activeSenderIds?: number
}

export interface DailyVolumePoint {
  date: string
  sent: number
  delivered: number
  failed: number
}

export interface MonthlyUsagePoint {
  month: string
  volume: number
  cost: number
}

export interface CampaignSummary {
  id: string
  name: string
  recipients: number
  delivered: number
  failed: number
  cost: number
  createdAt: string
}

export interface ActivityItem {
  id: string
  type: 'sms' | 'topup' | 'sender_id' | 'contact'
  title: string
  description: string
  timestamp: string
  status?: string
}

export interface UsageSummaryRow {
  period: string
  sent: number
  delivered: number
  failed: number
  cost: number
  deliveryRate: number
}
