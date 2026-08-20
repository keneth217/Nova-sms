<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { buildDeveloperPages, originFromVite, publicDeveloperNav } from '@/data/developer-docs'
import JsonLd from '@/components/seo/JsonLd.vue'
import { breadcrumbSchema, webPageSchema } from '@/seo/schema'
import CtaBanner from '@/components/marketing/CtaBanner.vue'

const origin = originFromVite()
const overview = computed(() => buildDeveloperPages(origin).overview)

const pageTitle = 'Nova SMS API documentation'
const pageDescription =
  'REST API to send SMS, check delivery, read wallet balance, and start M-Pesa STK Push top-ups from your backend.'
</script>

<template>
  <div class="bg-white px-4 pb-20 pt-28 sm:px-6 lg:px-8">
    <JsonLd
      id="docs-webpage"
      :data="webPageSchema({ name: pageTitle, description: pageDescription, path: '/developers' })"
    />
    <JsonLd
      id="docs-crumbs"
      :data="breadcrumbSchema([
        { name: 'Home', path: '/' },
        { name: 'API documentation', path: '/developers' },
      ])"
    />
    <article class="mx-auto max-w-3xl">
      <header class="border-b border-slate-200 pb-8">
        <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Developers</p>
        <h1 class="mt-2 font-serif text-4xl font-bold tracking-tight text-slate-900">
          Nova SMS API documentation
        </h1>
        <p class="mt-3 text-base leading-relaxed text-slate-600">
          Send SMS from Mwalimu, Chamaplus, Nova POS, or any backend through Nova SMS. You never talk
          to the upstream SMS provider. Authenticate with an API key from your server.
        </p>
      </header>

      <p class="mt-8 text-slate-600">{{ overview?.description }}</p>

      <nav class="mt-10 space-y-8" aria-label="API documentation">
        <section v-for="group in publicDeveloperNav" :key="group.label">
          <h2 class="text-lg font-semibold text-slate-900">{{ group.label }}</h2>
          <ul class="mt-3 divide-y divide-slate-100 rounded-xl border border-slate-200">
            <li v-for="item in group.items" :key="item.id">
              <RouterLink
                :to="item.to"
                class="flex items-center justify-between px-4 py-3 text-sm font-medium text-brand-700 hover:bg-slate-50"
              >
                {{ item.label }}
                <span class="text-slate-400" aria-hidden="true">→</span>
              </RouterLink>
            </li>
          </ul>
        </section>
      </nav>

      <div class="mt-10">
        <CtaBanner
          title="Get started"
          description="Create an account, issue an API client, and send your first SMS from your backend."
        />
      </div>
    </article>
  </div>
</template>
