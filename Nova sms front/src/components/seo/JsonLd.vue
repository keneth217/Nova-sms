<script setup lang="ts">
import { onUnmounted, watch } from 'vue'

const props = defineProps<{
  id?: string
  data: Record<string, unknown> | Record<string, unknown>[]
}>()

const scriptId = props.id || `jsonld-${Math.random().toString(36).slice(2, 8)}`
let el: HTMLScriptElement | null = null

function sync() {
  if (!el) {
    el = document.createElement('script')
    el.type = 'application/ld+json'
    el.id = scriptId
    document.head.appendChild(el)
  }
  el.textContent = JSON.stringify(props.data)
}

watch(() => props.data, sync, { immediate: true, deep: true })

onUnmounted(() => {
  el?.remove()
  el = null
})
</script>

<template>
  <!-- JSON-LD is injected into document.head -->
</template>
