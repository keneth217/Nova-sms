<script setup lang="ts">
import { RouterView } from 'vue-router'
import LandingNavbar from '@/components/landing/LandingNavbar.vue'
import LandingFooter from '@/components/landing/LandingFooter.vue'
import JsonLd from '@/components/seo/JsonLd.vue'
import { organizationSchema, websiteSchema } from '@/seo/schema'
</script>

<template>
  <div class="flex min-h-screen flex-col bg-[#f4f7f6] text-slate-900">
    <JsonLd id="org-schema" :data="organizationSchema()" />
    <JsonLd id="website-schema" :data="websiteSchema()" />
    <LandingNavbar />
    <main class="flex-1">
      <RouterView />
    </main>
    <LandingFooter />
  </div>
</template>
