<script setup lang="ts">
import type { MessageStatus } from '@/models/sms.model'
import type { SenderIdStatus } from '@/models/senderid.model'
import type { OrganizationStatus } from '@/models/organization.model'
import type { TopupStatus, WalletTransactionType } from '@/models/wallet.model'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{
  status:
    | MessageStatus
    | SenderIdStatus
    | OrganizationStatus
    | TopupStatus
    | WalletTransactionType
    | string
}>()

type Variant = 'neutral' | 'success' | 'warning' | 'danger' | 'info' | 'brand'

function variantFor(status: string): Variant {
  switch (status) {
    case 'DELIVERED':
    case 'SUCCESS':
    case 'APPROVED':
    case 'ACTIVE':
    case 'COMPLETED':
    case 'TOPUP':
    case 'BUSINESS':
      return 'success'
    case 'PENDING':
    case 'SCHEDULED':
    case 'EVENT':
      return 'warning'
    case 'FAILED':
    case 'CANCELLED':
    case 'REJECTED':
    case 'SUSPENDED':
    case 'EXPIRED':
      return 'danger'
    case 'SMS_DEBIT':
      return 'info'
    case 'REFUND':
    case 'ADJUSTMENT':
      return 'brand'
    default:
      return 'neutral'
  }
}
</script>

<template>
  <StatusBadge :variant="variantFor(props.status)">{{ status }}</StatusBadge>
</template>
