<script setup lang="ts">
import { RouterLink } from 'vue-router'

const year = new Date().getFullYear()

const productLinks = [
  { to: '/#features', label: 'Features' },
  { to: '/#events', label: 'Event sending' },
  { to: '/register?intent=event', label: 'Get started' },
  { to: '/login', label: 'Sign in' },
]

const resourceLinks = [
  { to: '/register?intent=event', label: 'Event / one-time account' },
  { to: '/register?intent=business', label: 'Business account' },
  { to: '/login', label: 'Customer login' },
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
          <RouterLink to="/" class="inline-flex items-center gap-2.5">
            <span
              class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-500 text-sm font-bold text-white"
            >
              N
            </span>
            <span class="font-serif text-lg font-bold text-white">Nova SMS</span>
          </RouterLink>
          <p class="mt-4 max-w-sm text-sm leading-relaxed text-slate-400">
            Bulk SMS for businesses — and for people who only need to send once. Event notices,
            weddings, harambees, or daily campaigns. Top up with M-Pesa and send.
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
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Account</p>
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
