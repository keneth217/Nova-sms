<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import JsonLd from '@/components/seo/JsonLd.vue'
import { breadcrumbSchema, webPageSchema } from '@/seo/schema'

const props = defineProps<{
  kicker?: string
  title: string
  description: string
  path: string
}>()

const crumbs = [
  { name: 'Home', path: '/' },
  { name: props.title, path: props.path },
]

const schemaId = computed(() => props.path.replace(/[^\w]+/g, '-') || 'home')
</script>

<template>
  <div class="bg-white px-4 pb-20 pt-28 sm:px-6 lg:px-8">
    <JsonLd :id="`webpage${schemaId}`" :data="webPageSchema({ name: title, description, path })" />
    <JsonLd :id="`crumbs${schemaId}`" :data="breadcrumbSchema(crumbs)" />
    <article class="mx-auto max-w-3xl">
      <nav class="text-xs text-slate-500" aria-label="Breadcrumb">
        <ol class="flex flex-wrap items-center gap-1">
          <li>
            <RouterLink to="/" class="hover:text-slate-800">Home</RouterLink>
          </li>
          <li aria-hidden="true">/</li>
          <li class="text-slate-700">{{ title }}</li>
        </ol>
      </nav>
      <header class="mt-6 border-b border-slate-200 pb-8">
        <p v-if="kicker" class="text-sm font-semibold uppercase tracking-wider text-brand-700">
          {{ kicker }}
        </p>
        <h1 class="mt-2 font-serif text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
          {{ title }}
        </h1>
        <p class="mt-3 text-base leading-relaxed text-slate-600">{{ description }}</p>
      </header>
      <div class="mt-10 space-y-8 text-base leading-7 text-slate-600">
        <slot />
      </div>
    </article>
  </div>
</template>
