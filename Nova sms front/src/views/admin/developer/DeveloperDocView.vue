<script setup lang="ts">
import { computed, inject, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { buildDeveloperPages, originFromVite } from '@/data/developer-docs'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import DocRenderer from '@/components/developer/DocRenderer.vue'

const route = useRoute()
const injectedOrigin = inject<Ref<string>>('developerApiOrigin')
const origin = computed(() => injectedOrigin?.value || originFromVite())
const docId = computed(() => String(route.meta.docId || 'overview'))
const page = computed(() => buildDeveloperPages(origin.value)[docId.value])
</script>

<template>
  <div v-if="page">
    <PageHeader :title="page.title" :description="page.description" />
    <AppCard>
      <DocRenderer :blocks="page.blocks" />
    </AppCard>
  </div>
  <p v-else class="text-sm text-slate-500">Documentation page not found.</p>
</template>
