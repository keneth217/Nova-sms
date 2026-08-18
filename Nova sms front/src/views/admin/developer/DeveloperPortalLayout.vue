<script setup lang="ts">
import { computed, onMounted, provide, ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { apiClientService } from '@/api/api-client.service'
import { developerNav, originFromVite } from '@/data/developer-docs'

const route = useRoute()
const origin = ref(originFromVite())
provide('developerApiOrigin', origin)

onMounted(async () => {
  try {
    const config = await apiClientService.developerConfig()
    if (config.publicBaseUrl) origin.value = config.publicBaseUrl.replace(/\/$/, '')
  } catch {
  }
})

const groups = developerNav

function isActive(path: string) {
  if (path === '/admin/developer') {
    return route.path === '/admin/developer'
  }
  return route.path === path || route.path.startsWith(path + '/')
}

const swaggerHref = computed(() => `${origin.value}/swagger-ui.html`)
</script>

<template>
  <div class="flex min-h-[calc(100vh-8rem)] gap-6">
    <nav
      class="hidden w-56 shrink-0 overflow-y-auto rounded-xl border border-slate-200/80 bg-white p-3 lg:block"
    >
      <p class="px-2 pb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
        Developer
      </p>
      <div v-for="group in groups" :key="group.label" class="mb-4">
        <p class="px-2 pb-1 text-[10px] font-semibold uppercase tracking-wider text-slate-400">
          {{ group.label }}
        </p>
        <RouterLink
          v-for="item in group.items"
          :key="item.id"
          :to="item.to"
          class="block rounded-lg px-2 py-1.5 text-sm transition"
          :class="
            isActive(item.to)
              ? 'bg-brand-50 font-medium text-brand-700'
              : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
          "
        >
          {{ item.label }}
        </RouterLink>
      </div>
      <a
        :href="swaggerHref"
        target="_blank"
        rel="noreferrer"
        class="mt-2 block px-2 text-xs text-brand-700 hover:underline"
      >
        OpenAPI / Swagger
      </a>
    </nav>

    <div class="min-w-0 flex-1">
      <div class="mb-4 flex flex-wrap gap-2 lg:hidden">
        <RouterLink
          v-for="item in groups.flatMap((g) => g.items)"
          :key="item.id"
          :to="item.to"
          class="rounded-full px-3 py-1 text-xs ring-1 ring-inset"
          :class="
            isActive(item.to)
              ? 'bg-brand-50 text-brand-700 ring-brand-200'
              : 'text-slate-600 ring-slate-200'
          "
        >
          {{ item.label }}
        </RouterLink>
      </div>
      <RouterView />
    </div>
  </div>
</template>
