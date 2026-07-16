<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  hint?: string
  icon?: Component
  tone?: 'default' | 'brand' | 'success' | 'warning' | 'danger'
}>()

const tones: Record<string, string> = {
  default: 'bg-slate-100 text-slate-600',
  brand: 'bg-brand-50 text-brand-700',
  success: 'bg-emerald-50 text-emerald-700',
  warning: 'bg-amber-50 text-amber-700',
  danger: 'bg-rose-50 text-rose-700',
}
</script>

<template>
  <div class="rounded-xl border border-slate-200/80 bg-white p-5 shadow-sm shadow-slate-900/5">
    <div class="flex items-start justify-between gap-3">
      <div>
        <p class="text-sm font-medium text-slate-500">{{ label }}</p>
        <p class="mt-2 text-2xl font-semibold tracking-tight text-slate-900">{{ value }}</p>
        <p v-if="hint" class="mt-1 text-xs text-slate-500">{{ hint }}</p>
      </div>
      <div
        v-if="icon"
        class="flex h-10 w-10 items-center justify-center rounded-lg"
        :class="tones[tone || 'default']"
      >
        <component :is="icon" class="h-5 w-5" />
      </div>
    </div>
  </div>
</template>
