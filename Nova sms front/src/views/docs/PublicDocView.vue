<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { buildDeveloperPages, originFromVite, publicDeveloperNav } from '@/data/developer-docs'
import DocRenderer from '@/components/developer/DocRenderer.vue'
import JsonLd from '@/components/seo/JsonLd.vue'
import { breadcrumbSchema, webPageSchema } from '@/seo/schema'
import { useSeo } from '@/composables/useSeo'
import { publicDocSeo } from '@/seo/doc-seo'
import type { PublicDocSlug } from '@/seo/public-paths'

const route = useRoute()
const origin = originFromVite()
const slug = computed(() => String(route.params.slug || ''))
const page = computed(() => buildDeveloperPages(origin)[slug.value])
const seo = computed(() => publicDocSeo[slug.value as PublicDocSlug])

useSeo(() => {
  if (!page.value) {
    return {
      title: 'Documentation not found · Nova SMS',
      description: 'That Nova SMS API page does not exist.',
      path: route.path,
      robots: 'noindex,nofollow',
    }
  }
  return {
    title: seo.value?.title || `${page.value.title} · Nova SMS`,
    description: seo.value?.description || page.value.description || '',
    path: route.path,
  }
})
</script>

<template>
  <div class="bg-white px-4 pb-20 pt-28 sm:px-6 lg:px-8">
    <template v-if="page">
      <JsonLd
        id="doc-webpage"
        :data="webPageSchema({
          name: seo?.title || page.title,
          description: seo?.description || page.description,
          path: route.path,
        })"
      />
      <JsonLd
        id="doc-crumbs"
        :data="breadcrumbSchema([
          { name: 'Home', path: '/' },
          { name: 'API documentation', path: '/developers' },
          { name: page.title, path: route.path },
        ])"
      />
      <div class="mx-auto grid max-w-6xl gap-10 lg:grid-cols-[16rem_minmax(0,1fr)]">
        <aside class="lg:pt-2">
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Documentation</p>
          <nav class="mt-3 space-y-5 text-sm">
            <div v-for="group in publicDeveloperNav" :key="group.label">
              <p class="font-semibold text-slate-800">{{ group.label }}</p>
              <ul class="mt-2 space-y-1">
                <li v-for="item in group.items" :key="item.id">
                  <RouterLink
                    :to="item.to"
                    class="block rounded-md px-2 py-1 text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                    active-class="!bg-brand-50 !text-brand-800"
                  >
                    {{ item.label }}
                  </RouterLink>
                </li>
              </ul>
            </div>
          </nav>
        </aside>
        <article>
          <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Nova SMS API</p>
          <h1 class="mt-2 font-serif text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
            {{ page.title }}
          </h1>
          <p class="mt-3 text-base text-slate-600">{{ page.description }}</p>
          <div class="mt-8">
            <DocRenderer :blocks="page.blocks" />
          </div>
        </article>
      </div>
    </template>
    <p v-else class="mx-auto max-w-3xl text-slate-600">This documentation page was not found.</p>
  </div>
</template>
