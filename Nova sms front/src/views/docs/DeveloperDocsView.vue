<script setup lang="ts">
import { computed } from 'vue'
import { buildDeveloperPages, originFromVite } from '@/data/developer-docs'
import DocRenderer from '@/components/developer/DocRenderer.vue'

const origin = originFromVite()
const pages = computed(() => buildDeveloperPages(origin))
const publicIds = [
  'overview',
  'quick-start',
  'authentication',
  'send-sms',
  'bulk-sms',
  'status',
  'history',
  'errors',
  'idempotency',
  'rate-limits',
  'spring-boot',
  'nodejs',
  'php',
  'python',
  'generic-http',
] as const
</script>

<template>
  <div class="bg-white px-4 pb-20 pt-28 sm:px-6 lg:px-8">
    <article class="mx-auto max-w-3xl">
      <header class="border-b border-slate-200 pb-8">
        <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Developers</p>
        <h1 class="mt-2 font-serif text-4xl font-bold tracking-tight text-slate-900">Nova SMS API</h1>
        <p class="mt-3 text-sm text-slate-500">
          Send SMS from Mwalimu, Chamaplus, Nova POS, or any other backend through Nova SMS. You never
          talk to the upstream provider. Super Admin users also have an in-app portal under Developer.
        </p>
        <nav class="mt-6 flex flex-wrap gap-2">
          <a
            v-for="id in publicIds"
            :key="id"
            :href="`#${id}`"
            class="rounded-full bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600 ring-1 ring-slate-200 hover:bg-slate-100"
          >
            {{ pages[id]?.title }}
          </a>
        </nav>
      </header>

      <div class="mt-10 space-y-16">
        <section v-for="id in publicIds" :id="id" :key="id">
          <h2 class="text-xl font-semibold text-slate-900">{{ pages[id]?.title }}</h2>
          <p v-if="pages[id]?.description" class="mt-1 text-sm text-slate-500">
            {{ pages[id]?.description }}
          </p>
          <div class="mt-4">
            <DocRenderer v-if="pages[id]" :blocks="pages[id].blocks" />
          </div>
        </section>
      </div>
    </article>
  </div>
</template>
