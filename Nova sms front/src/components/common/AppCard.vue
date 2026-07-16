<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    padding?: boolean
  }>(),
  { padding: true },
)
</script>

<template>
  <section class="rounded-xl border border-slate-200/80 bg-white shadow-sm shadow-slate-900/5">
    <header
      v-if="title || $slots.header || $slots.actions"
      class="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 px-5 py-4"
    >
      <div>
        <slot name="header">
          <h2 v-if="title" class="text-sm font-semibold text-slate-900">{{ title }}</h2>
          <p v-if="subtitle" class="mt-0.5 text-sm text-slate-500">{{ subtitle }}</p>
        </slot>
      </div>
      <div v-if="$slots.actions" class="flex items-center gap-2">
        <slot name="actions" />
      </div>
    </header>
    <div :class="padding ? 'p-5' : ''">
      <slot />
    </div>
  </section>
</template>
