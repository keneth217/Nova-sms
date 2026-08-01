export type BundleStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'

export type BundleCategory = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'PROMOTIONAL' | 'OTHER'

export type BundlePaymentMode = 'airtime' | 'm-pesa'

export interface DataBundleOffer {
  offerId: string
  uniqueOfferingId?: string | null
  offerName: string
  category: BundleCategory | string
  amount: number
  validity: string | null
  description: string | null
  accountId?: string | null
  resourceAmount?: string | null
  offerSource?: string | null
  parentOfferId?: string | null
}

export interface DataBundleOffersResponse {
  success: boolean
  phoneNumber: string
  offers: DataBundleOffer[]
}

export interface DataBundlePurchaseRequest {
  phoneNumber: string
  offerId: string
  /** Fingerprint from fetch — helps server re-resolve live offeringId. */
  accountId?: string
  amount?: number
  resourceAmount?: string
  reference?: string
  paymentMode?: BundlePaymentMode
  /** Optional M-Pesa payer MSISDN when different from the bundle recipient. */
  paymentPhoneNumber?: string
}

export interface DataBundleTransaction {
  id: string
  reference: string
  phoneNumber: string
  offerId: string
  offerName: string
  category: string | null
  amount: number
  status: BundleStatus
  checkoutRequestId: string | null
  responseCode: string | null
  responseDescription: string | null
  failureReason: string | null
  createdAt: string
  updatedAt: string | null
}

export interface DataBundleMetrics {
  totalSold: number
  successful: number
  failed: number
  pending: number
  revenue: number
}
