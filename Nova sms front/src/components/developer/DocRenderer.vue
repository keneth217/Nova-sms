<script setup lang="ts">
import type { DocBlock } from '@/data/developer-docs'
import CodeBlock from '@/components/developer/CodeBlock.vue'
import HttpBadge from '@/components/developer/HttpBadge.vue'

defineProps<{
  blocks: DocBlock[]
}>()
</script>

<template>
  <div class="space-y-5 text-sm leading-6 text-slate-600">
    <template v-for="(block, index) in blocks" :key="index">
      <p v-if="block.type === 'p'">{{ block.text }}</p>
      <h2 v-else-if="block.type === 'h2'" class="pt-2 text-lg font-semibold text-slate-900">
        {{ block.text }}
      </h2>
      <h3 v-else-if="block.type === 'h3'" class="text-sm font-semibold text-slate-900">
        {{ block.text }}
      </h3>
      <ul v-else-if="block.type === 'ul'" class="list-disc space-y-1 pl-5">
        <li v-for="item in block.items" :key="item">{{ item }}</li>
      </ul>
      <ol v-else-if="block.type === 'ol'" class="list-decimal space-y-1 pl-5">
        <li v-for="item in block.items" :key="item">{{ item }}</li>
      </ol>
      <div
        v-else-if="block.type === 'warn'"
        class="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-amber-900"
      >
        {{ block.text }}
      </div>
      <div
        v-else-if="block.type === 'note'"
        class="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-slate-700"
      >
        {{ block.text }}
      </div>
      <CodeBlock v-else-if="block.type === 'code'" :code="block.code" :language="block.language" />
      <div v-else-if="block.type === 'http'" class="flex flex-wrap items-center gap-2">
        <HttpBadge :method="block.method" />
        <code class="font-mono text-xs text-slate-800">{{ block.path }}</code>
      </div>
      <pre
        v-else-if="block.type === 'pre'"
        class="overflow-x-auto rounded-xl bg-slate-900 p-4 font-mono text-xs text-slate-100"
        >{{ block.text }}</pre
      >
      <div v-else-if="block.type === 'table'" class="overflow-x-auto">
        <table class="min-w-full text-left text-xs">
          <thead>
            <tr class="border-b border-slate-200 text-slate-500">
              <th v-for="header in block.headers" :key="header" class="px-3 py-2 font-semibold">
                {{ header }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex" class="border-b border-slate-100">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex" class="px-3 py-2 font-mono text-slate-800">
                {{ cell }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>
