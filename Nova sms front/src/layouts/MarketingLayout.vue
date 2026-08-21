<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import LandingNavbar from '@/components/landing/LandingNavbar.vue'
import LandingFooter from '@/components/landing/LandingFooter.vue'
import JsonLd from '@/components/seo/JsonLd.vue'
import { organizationSchema, websiteSchema } from '@/seo/schema'

const route = useRoute()
const hideChrome = computed(() => Boolean(route.meta.hideMarketingChrome))
</script>

<template>
  <div
    class="flex min-h-screen flex-col text-slate-900"
    :class="hideChrome ? 'bg-surface-50' : 'bg-[#f4f7f6]'"
  >
    <JsonLd id="org-schema" :data="organizationSchema()" />
    <JsonLd id="website-schema" :data="websiteSchema()" />
    <LandingNavbar v-if="!hideChrome" />
    <main class="flex-1">
      <RouterView />
    </main>
    <LandingFooter v-if="!hideChrome" />
  </div>
</template>
