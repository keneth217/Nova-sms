export interface PaybillCollection {
  id: string
  billRef: string
  amount: number
  mpesaReceipt: string
  phoneNumber: string | null
  mpesaTransactionDate: string | null
  payerName: string | null
  createdAt: string
}

export interface PaybillCollectionAccountStat {
  billRef: string
  count: number
  amount: number
}

export interface PaybillCollectionDashboard {
  paybill: string
  accounts: string[]
  totalAmount: number
  totalCount: number
  todayAmount: number
  todayCount: number
  monthAmount: number
  monthCount: number
  byAccount: PaybillCollectionAccountStat[]
  recent: {
    content: PaybillCollection[]
    totalElements: number
    totalPages: number
    size: number
    number: number
    empty: boolean
  }
}
