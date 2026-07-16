import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  ActivityItem,
  CampaignSummary,
  DailyVolumePoint,
  DashboardReport,
  MonthlyUsagePoint,
  UsageSummaryRow,
} from '@/models/report.model'
import { reportService } from '@/api/report.service'
import { useAuthStore } from '@/stores/auth.store'

export const useReportStore = defineStore('report', () => {
  const dashboard = ref<DashboardReport | null>(null)
  const dailyVolume = ref<DailyVolumePoint[]>([])
  const monthlyUsage = ref<MonthlyUsagePoint[]>([])
  const campaigns = ref<CampaignSummary[]>([])
  const activity = ref<ActivityItem[]>([])
  const usageSummary = ref<UsageSummaryRow[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchDashboard() {
    const auth = useAuthStore()
    loading.value = true
    error.value = null

    if (auth.isSuperAdmin) {
      dashboard.value = null
      dailyVolume.value = []
      monthlyUsage.value = []
      campaigns.value = []
      activity.value = []
      usageSummary.value = []
      loading.value = false
      return
    }

    try {
      const [dash, daily, monthly, camps, acts] = await Promise.all([
        reportService.getDashboard(),
        reportService.getDailyVolume(),
        reportService.getMonthlyUsage(),
        reportService.getCampaigns(),
        reportService.getActivity(),
      ])
      dashboard.value = dash
      dailyVolume.value = daily
      monthlyUsage.value = monthly
      campaigns.value = camps
      activity.value = acts
      usageSummary.value = await reportService.buildUsageSummary(dash, daily)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load reports'
    } finally {
      loading.value = false
    }
  }

  return {
    dashboard,
    dailyVolume,
    monthlyUsage,
    campaigns,
    activity,
    usageSummary,
    loading,
    error,
    fetchDashboard,
  }
})
