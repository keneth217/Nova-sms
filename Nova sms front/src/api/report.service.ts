import api from './axios'
import type { ApiResponse } from '@/models/auth.model'
import type {
  ActivityItem,
  CampaignSummary,
  DailyVolumePoint,
  DashboardReport,
  MonthlyUsagePoint,
  UsageSummaryRow,
} from '@/models/report.model'
import type { SmsMessage } from '@/models/sms.model'
import type { WalletTransaction } from '@/models/wallet.model'
import { delay, isMockMode } from '@/utils/format'
import {
  mockActivity,
  mockCampaigns,
  mockDailyVolume,
  mockDashboard,
  mockMonthlyUsage,
  mockUsageSummary,
} from '@/mocks/data'
import { smsService } from '@/api/sms.service'
import { walletService } from '@/api/wallet.service'
import { senderIdService } from '@/api/senderid.service'

function dayKey(iso: string): string {
  return new Date(iso).toISOString().slice(0, 10)
}

function monthKey(iso: string): string {
  const d = new Date(iso)
  return `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, '0')}`
}

function lastNDates(n: number): string[] {
  const dates: string[] = []
  const today = new Date()
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    dates.push(d.toISOString().slice(0, 10))
  }
  return dates
}

function buildDailyVolume(messages: SmsMessage[], days = 14): DailyVolumePoint[] {
  const buckets = new Map<string, DailyVolumePoint>()
  for (const date of lastNDates(days)) {
    buckets.set(date, { date, sent: 0, delivered: 0, failed: 0 })
  }
  for (const msg of messages) {
    const key = dayKey(msg.createdAt)
    const bucket = buckets.get(key)
    if (!bucket) continue
    bucket.sent += 1
    if (msg.status === 'DELIVERED') bucket.delivered += 1
    if (msg.status === 'FAILED') bucket.failed += 1
  }
  return [...buckets.values()]
}

function buildMonthlyUsage(messages: SmsMessage[]): MonthlyUsagePoint[] {
  const buckets = new Map<string, MonthlyUsagePoint>()
  for (const msg of messages) {
    const key = monthKey(msg.createdAt)
    const existing = buckets.get(key) ?? { month: key, volume: 0, cost: 0 }
    existing.volume += 1
    existing.cost += Number(msg.cost) || 0
    buckets.set(key, existing)
  }
  return [...buckets.values()].sort((a, b) => a.month.localeCompare(b.month))
}

function buildCampaigns(messages: SmsMessage[]): CampaignSummary[] {
  const byBatch = new Map<string, SmsMessage[]>()
  for (const msg of messages) {
    const key = msg.batchId || `single-${msg.id}`
    const list = byBatch.get(key) ?? []
    list.push(msg)
    byBatch.set(key, list)
  }
  return [...byBatch.entries()]
    .map(([id, list]) => {
      const delivered = list.filter((m) => m.status === 'DELIVERED').length
      const failed = list.filter((m) => m.status === 'FAILED').length
      const cost = list.reduce((sum, m) => sum + (Number(m.cost) || 0), 0)
      const newest = list.reduce((a, b) => (a.createdAt > b.createdAt ? a : b))
      return {
        id,
        name: list[0]?.batchId ? `Batch ${list[0].batchId.slice(0, 8)}` : 'Single send',
        recipients: list.length,
        delivered,
        failed,
        cost,
        createdAt: newest.createdAt,
      }
    })
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, 20)
}

function buildUsageSummary(
  daily: DailyVolumePoint[],
  dashboard: DashboardReport,
): UsageSummaryRow[] {
  const today = daily[daily.length - 1]
  const week = daily.slice(-7).reduce(
    (acc, d) => ({
      sent: acc.sent + d.sent,
      delivered: acc.delivered + d.delivered,
      failed: acc.failed + d.failed,
    }),
    { sent: 0, delivered: 0, failed: 0 },
  )
  const rate = (delivered: number, sent: number, failed: number) => {
    const base = delivered + failed + Math.max(0, sent - delivered - failed)
    return base === 0 ? 0 : Number(((delivered * 100) / base).toFixed(1))
  }

  return [
    {
      period: 'Today',
      sent: today?.sent ?? dashboard.smsSentToday,
      delivered: today?.delivered ?? 0,
      failed: today?.failed ?? 0,
      cost: dashboard.costToday,
      deliveryRate: rate(today?.delivered ?? 0, today?.sent ?? 0, today?.failed ?? 0),
    },
    {
      period: 'Last 7 days',
      sent: week.sent,
      delivered: week.delivered,
      failed: week.failed,
      cost: dashboard.costThisMonth,
      deliveryRate: rate(week.delivered, week.sent, week.failed),
    },
    {
      period: 'This month',
      sent: dashboard.smsSentThisMonth,
      delivered: dashboard.deliveredCount,
      failed: dashboard.failedCount,
      cost: dashboard.costThisMonth,
      deliveryRate: dashboard.deliveryRate,
    },
  ]
}

function buildActivity(
  messages: SmsMessage[],
  transactions: WalletTransaction[],
): ActivityItem[] {
  const smsItems: ActivityItem[] = messages.slice(0, 8).map((m) => ({
    id: `sms-${m.id}`,
    type: 'sms',
    title: `SMS to ${m.recipient}`,
    description: m.content.slice(0, 80),
    timestamp: m.createdAt,
    status: m.status,
  }))

  const txItems: ActivityItem[] = transactions.slice(0, 8).map((t) => ({
    id: `tx-${t.id}`,
    type: t.type === 'TOPUP' ? 'topup' : 'sms',
    title:
      t.type === 'TOPUP'
        ? 'Wallet top-up'
        : t.type === 'SMS_DEBIT'
          ? 'SMS debit'
          : t.type,
    description: t.description || t.mpesaReceipt || t.reference || t.type,
    timestamp: t.createdAt,
    status: t.status || undefined,
  }))

  return [...smsItems, ...txItems]
    .sort((a, b) => b.timestamp.localeCompare(a.timestamp))
    .slice(0, 12)
}

class ReportService {
  async getDashboard(): Promise<DashboardReport> {
    if (isMockMode()) {
      await delay(300)
      return { ...mockDashboard }
    }
    const { data } = await api.get<ApiResponse<DashboardReport>>('/reports/dashboard')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load dashboard')

    try {
      const senders = await senderIdService.list()
      return {
        ...data.data,
        activeSenderIds: senders.filter((s) => s.status === 'APPROVED').length,
      }
    } catch {
      return data.data
    }
  }

  async getDailyVolume(): Promise<DailyVolumePoint[]> {
    if (isMockMode()) {
      await delay(200)
      return [...mockDailyVolume]
    }
    const history = await smsService.getHistory({ page: 0, size: 200 })
    return buildDailyVolume(history.content, 14)
  }

  async getMonthlyUsage(): Promise<MonthlyUsagePoint[]> {
    if (isMockMode()) {
      await delay(200)
      return [...mockMonthlyUsage]
    }
    const history = await smsService.getHistory({ page: 0, size: 200 })
    return buildMonthlyUsage(history.content)
  }

  async getCampaigns(): Promise<CampaignSummary[]> {
    if (isMockMode()) {
      await delay(200)
      return [...mockCampaigns]
    }
    const history = await smsService.getHistory({ page: 0, size: 200 })
    return buildCampaigns(history.content)
  }

  async getActivity(): Promise<ActivityItem[]> {
    if (isMockMode()) {
      await delay(200)
      return [...mockActivity]
    }
    const [history, transactions] = await Promise.all([
      smsService.getHistory({ page: 0, size: 20 }),
      walletService.getTransactions({ page: 0, size: 20 }),
    ])
    return buildActivity(history.content, transactions.content)
  }

  async getUsageSummary(): Promise<UsageSummaryRow[]> {
    if (isMockMode()) {
      await delay(200)
      return [...mockUsageSummary]
    }
    const [dashboard, daily] = await Promise.all([this.getDashboard(), this.getDailyVolume()])
    return this.buildUsageSummary(dashboard, daily)
  }

  buildUsageSummary(dashboard: DashboardReport, daily: DailyVolumePoint[]): UsageSummaryRow[] {
    return buildUsageSummary(daily, dashboard)
  }
}

export const reportService = new ReportService()
