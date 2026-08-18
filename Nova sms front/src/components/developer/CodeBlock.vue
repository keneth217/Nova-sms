<script setup lang="ts">
import { ref } from 'vue'
import { ClipboardDocumentIcon, CheckIcon } from '@heroicons/vue/24/outline'

const props = defineProps<{
  code: string
  language?: string
}>()

const copied = ref(false)

async function copy() {
  try {
    await navigator.clipboard.writeText(props.code)
    copied.value = true
    window.setTimeout(() => {
      copied.value = false
    }, 1600)
  } catch {
    copied.value = false
  }
}
</script>

<template>
  <div class="overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
    <div class="flex items-center justify-between border-b border-slate-800 px-3 py-1.5">
      <span class="text-[11px] font-medium uppercase tracking-wider text-slate-400">{{
        language || 'code'
      }}</span>
      <button
        type="button"
        class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[11px] font-medium text-slate-300 hover:bg-slate-800"
        @click="copy"
      >
        <CheckIcon v-if="copied" class="h-3.5 w-3.5 text-emerald-400" />
        <ClipboardDocumentIcon v-else class="h-3.5 w-3.5" />
        {{ copied ? 'Copied' : 'Copy' }}
      </button>
    </div>
    <pre class="overflow-x-auto p-4 text-xs leading-5 text-slate-100"><code>{{ code }}</code></pre>
  </div>
</template>
