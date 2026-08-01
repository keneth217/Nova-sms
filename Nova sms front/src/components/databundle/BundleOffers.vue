<script setup lang="ts">
import type { DataBundleOffer } from '@/models/databundle.model'
import AppButton from '@/components/common/AppButton.vue'
import { formatCurrency } from '@/utils/format'

defineProps<{
  groups: Record<string, DataBundleOffer[]>
  purchasing?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  buy: [offer: DataBundleOffer]
}>()

const categoryOrder = ['DAILY', 'WEEKLY', 'MONTHLY', 'PROMOTIONAL', 'OTHER']

const categoryLabels: Record<string, string> = {
  DAILY: 'Daily bundles',
  WEEKLY: 'Weekly bundles',
  MONTHLY: 'Monthly bundles',
  PROMOTIONAL: 'Promotional offers',
  OTHER: 'Other offers',
}
</script>

<template>
  <div class="space-y-8">
    <template v-for="category in categoryOrder" :key="category">
      <section v-if="(groups[category] || []).length">
        <h3 class="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
          {{ categoryLabels[category] || category }}
        </h3>
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <article
            v-for="offer in groups[category]"
            :key="offer.offerId"
            class="flex flex-col rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <h4 class="text-base font-semibold text-slate-900">{{ offer.offerName }}</h4>
                <p class="mt-1 text-xs text-slate-500">{{ offer.validity || 'Validity varies' }}</p>
              </div>
              <p class="text-lg font-semibold text-brand-700">
                {{ formatCurrency(Number(offer.amount)) }}
              </p>
            </div>
            <p class="mt-3 flex-1 text-sm text-slate-600">
              {{ offer.description || 'Safaricom mobile data bundle' }}
            </p>
            <AppButton
              class="mt-4"
              :loading="purchasing"
              :disabled="disabled || purchasing"
              @click="emit('buy', offer)"
            >
              Buy
            </AppButton>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>
