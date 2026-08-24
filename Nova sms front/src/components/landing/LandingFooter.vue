<script setup lang="ts">
import { RouterLink } from 'vue-router'
import BrandLogo from '@/components/common/BrandLogo.vue'

const year = new Date().getFullYear()

const productLinks = [
  { to: '/mpesa-stk-push', label: 'M-Pesa STK Push' },
  { to: '/mpesa-paybill', label: 'Paybill C2B' },
  { to: '/webhooks', label: 'Callbacks and status' },
  { to: '/developers', label: 'API documentation' },
  { to: '/pricing', label: 'Pricing' },
]

const resourceLinks = [
  { to: '/about', label: 'About' },
  { to: '/faq', label: 'FAQ' },
  { to: '/contact', label: 'Contact' },
  { to: '/register', label: 'Create account' },
  { to: '/data-bundles', label: 'Data bundles' },
]

const legalLinks = [
  { to: '/terms', label: 'Terms of Service' },
  { to: '/privacy', label: 'Privacy Policy' },
  { to: '/acceptable-use', label: 'Acceptable Use Policy' },
]
</script>

<template>
  <footer class="border-t border-slate-200 bg-slate-900 text-slate-400">
    <div class="mx-auto max-w-6xl px-4 py-14 sm:px-6 lg:px-8">
      <div class="grid gap-10 sm:grid-cols-2 lg:grid-cols-5">
        <div class="sm:col-span-2 lg:col-span-2">
          <BrandLogo to="/" height-class="h-12" />
          <p class="mt-4 max-w-sm text-sm leading-relaxed text-slate-400">
            Bulk SMS for Kenyan businesses and events. Prepaid wallet, M-Pesa STK Push and Paybill
            top-up, delivery reports, and a REST API.
          </p>
        </div>

        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Product</p>
          <ul class="mt-4 space-y-2.5">
            <li v-for="link in productLinks" :key="link.label">
              <RouterLink :to="link.to" class="text-sm transition hover:text-white">
                {{ link.label }}
              </RouterLink>
            </li>
          </ul>
        </div>

        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Resources</p>
          <ul class="mt-4 space-y-2.5">
            <li v-for="link in resourceLinks" :key="link.label">
              <RouterLink :to="link.to" class="text-sm transition hover:text-white">
                {{ link.label }}
              </RouterLink>
            </li>
          </ul>
        </div>

        <div>
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Legal</p>
          <ul class="mt-4 space-y-2.5">
            <li v-for="link in legalLinks" :key="link.label">
              <RouterLink :to="link.to" class="text-sm transition hover:text-white">
                {{ link.label }}
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>

      <div
        class="mt-12 flex flex-col gap-3 border-t border-slate-800 pt-6 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between"
      >
        <p>© {{ year }} Nova SMS. All rights reserved.</p>
        <p>Built for Kenyan businesses · M-Pesa ready</p>
      </div>
    </div>
  </footer>
</template>
