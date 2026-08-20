<script setup lang="ts">
import { RouterLink } from 'vue-router'

const links = [
  { to: '/', label: 'Home' },
  { to: '/sms-gateway', label: 'Bulk SMS gateway' },
  { to: '/developers', label: 'API documentation' },
  { to: '/contact', label: 'Contact' },
]
</script>

<template>
  <div class="bg-white px-4 pb-24 pt-28 sm:px-6 lg:px-8">
    <div class="mx-auto max-w-xl text-center">
      <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">404</p>
      <h1 class="mt-3 font-serif text-3xl font-bold text-slate-900">Page not found</h1>
      <p class="mt-3 text-slate-600">
        That URL is not a public Nova SMS page. Check the address or use one of the links below.
      </p>
      <ul class="mt-8 space-y-2">
        <li v-for="link in links" :key="link.to">
          <RouterLink :to="link.to" class="font-medium text-brand-700 hover:underline">
            {{ link.label }}
          </RouterLink>
        </li>
      </ul>
    </div>
  </div>
</template>
