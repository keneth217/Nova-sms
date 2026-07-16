<script setup lang="ts">
defineProps<{
  columns: { key: string; label: string; class?: string }[]
  emptyTitle?: string
  emptyHint?: string
}>()
</script>

<template>
  <div
    class="overflow-hidden rounded-xl border border-slate-200/80 bg-white shadow-sm shadow-slate-900/5"
  >
    <div class="overflow-x-auto">
      <table class="min-w-full divide-y divide-slate-100 text-left text-sm">
        <thead class="bg-slate-50/80">
          <tr>
            <th
              v-for="col in columns"
              :key="col.key"
              class="whitespace-nowrap px-4 py-3 text-xs font-semibold uppercase tracking-wide text-slate-500"
              :class="col.class"
            >
              {{ col.label }}
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <slot />
          <tr v-if="!$slots.default">
            <td :colspan="columns.length" class="px-4 py-12 text-center">
              <p class="text-sm font-medium text-slate-700">
                {{ emptyTitle || 'No records found' }}
              </p>
              <p v-if="emptyHint" class="mt-1 text-sm text-slate-500">{{ emptyHint }}</p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
