<script setup lang="ts">
import BrandLogo from '@/components/common/BrandLogo.vue'

withDefaults(
  defineProps<{
    kicker: string
    headline: string
    intro: string
    panelKicker?: string
    panelHeadline?: string
    panelBody?: string
    panelFooter?: string
  }>(),
  {
    panelKicker: 'Begin here',
    panelHeadline: 'Messaging built for Kenya.',
    panelBody:
      'Create an account to send bulk SMS, fund your wallet with M-Pesa, and keep delivery in one place.',
    panelFooter: 'Nairobi — Kenya',
  },
)
</script>

<template>
  <div class="flex min-h-svh bg-surface-50">
    <aside
      class="relative hidden w-[48%] flex-col overflow-hidden bg-brand-800 px-12 py-10 text-white lg:flex xl:px-16"
    >
      <div
        class="pointer-events-none absolute -right-24 top-24 h-80 w-80 rounded-full bg-brand-600/30 blur-2xl"
      />
      <div
        class="pointer-events-none absolute -bottom-20 left-10 h-64 w-64 rounded-full bg-brand-900/50 blur-3xl"
      />

      <BrandLogo to="/" height-class="h-11" class="relative z-10" />

      <div class="relative z-10 flex flex-1 flex-col justify-center py-16">
        <p class="text-[11px] font-semibold uppercase tracking-wider text-brand-200">
          {{ panelKicker }}
        </p>
        <h2 class="mt-5 max-w-md font-serif text-4xl leading-tight xl:text-5xl">
          {{ panelHeadline }}
        </h2>
        <p class="mt-6 max-w-sm text-sm leading-7 text-brand-100">
          {{ panelBody }}
        </p>
      </div>

      <p class="relative z-10 text-[11px] font-semibold uppercase tracking-wider text-brand-200">
        {{ panelFooter }}
      </p>
    </aside>

    <section class="flex w-full flex-1 flex-col bg-surface-50 lg:w-[52%] lg:bg-white">
      <div class="flex items-center justify-between px-6 py-5 lg:hidden">
        <BrandLogo to="/" height-class="h-10" />
      </div>

      <div class="flex flex-1 items-center justify-center px-6 py-8 sm:px-10 lg:px-16">
        <div class="w-full max-w-[420px]">
          <p class="text-[11px] font-semibold uppercase tracking-wider text-brand-700">
            {{ kicker }}
          </p>
          <h1 class="mt-3 font-serif text-4xl text-slate-900 sm:text-[2.75rem]">
            {{ headline }}
          </h1>
          <p class="mt-3 text-sm leading-6 text-slate-500">
            {{ intro }}
          </p>
          <div class="mt-8">
            <slot />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
