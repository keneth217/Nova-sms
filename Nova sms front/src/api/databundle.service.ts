import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  BundleStatus,
  DataBundleMetrics,
  DataBundleOffersResponse,
  DataBundlePurchaseRequest,
  DataBundleTransaction,
} from '@/models/databundle.model'
import { normalizePhone } from '@/utils/format'

class DataBundleService {
  async fetchOffers(phoneNumber: string): Promise<DataBundleOffersResponse> {
    const phone = normalizePhone(phoneNumber)
    const { data } = await api.post<ApiResponse<DataBundleOffersResponse>>('/data-bundles/offers', {
      phoneNumber: phone,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load offers')
    return data.data
  }

  async purchase(payload: DataBundlePurchaseRequest): Promise<DataBundleTransaction> {
    const body = {
      ...payload,
      phoneNumber: normalizePhone(payload.phoneNumber),
      paymentPhoneNumber: payload.paymentPhoneNumber
        ? normalizePhone(payload.paymentPhoneNumber)
        : undefined,
    }
    const { data } = await api.post<ApiResponse<DataBundleTransaction>>('/data-bundles/purchase', body)
    if (!data.success || !data.data) throw new Error(data.message || 'Purchase failed')
    return data.data
  }

  async getStatus(reference: string): Promise<DataBundleTransaction> {
    const { data } = await api.get<ApiResponse<DataBundleTransaction>>(
      `/data-bundles/status/${encodeURIComponent(reference)}`,
    )
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load status')
    return data.data
  }

  async history(
    params: PageRequest & { status?: BundleStatus | ''; phone?: string } = {},
  ): Promise<Page<DataBundleTransaction>> {
    const { data } = await api.get<ApiResponse<Page<DataBundleTransaction>>>('/data-bundles/history', {
      params,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load history')
    return data.data
  }

  async metrics(): Promise<DataBundleMetrics> {
    const { data } = await api.get<ApiResponse<DataBundleMetrics>>('/data-bundles/metrics')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load metrics')
    return data.data
  }
}

export const dataBundleService = new DataBundleService()
