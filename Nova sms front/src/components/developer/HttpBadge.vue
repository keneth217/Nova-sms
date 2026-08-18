<script setup lang="ts">
defineProps<{
  method: string
}>()

const tones: Record<string, string> = {
  GET: 'bg-sky-50 text-sky-700 ring-sky-200',
  POST: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  PATCH: 'bg-amber-50 text-amber-700 ring-amber-200',
  PUT: 'bg-amber-50 text-amber-700 ring-amber-200',
  DELETE: 'bg-rose-50 text-rose-700 ring-rose-200',
}
</script>

<template>
  <span
    class="inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-bold tracking-wide ring-1 ring-inset"
    :class="tones[method] || 'bg-slate-100 text-slate-600 ring-slate-200'"
  >
    {{ method }}
  </span>
</template>
