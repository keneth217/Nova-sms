<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  links: { to: string; label: string }[]
}>()
</script>

<template>
  <aside class="rounded-xl border border-slate-200 bg-slate-50 p-5">
    <p class="text-sm font-semibold text-slate-900">Continue reading</p>
    <ul class="mt-3 space-y-2">
      <li v-for="link in links" :key="link.to">
        <RouterLink :to="link.to" class="text-sm font-medium text-brand-700 hover:underline">
          {{ link.label }}
        </RouterLink>
      </li>
    </ul>
  </aside>
</template>
