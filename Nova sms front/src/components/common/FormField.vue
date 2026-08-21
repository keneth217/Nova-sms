<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    hint?: string
    error?: string
    required?: boolean
    variant?: 'default' | 'auth'
  }>(),
  { variant: 'default' },
)
</script>

<template>
  <label class="block space-y-1.5">
    <span
      v-if="label"
      :class="
        variant === 'auth'
          ? 'text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-700'
          : 'text-sm font-medium text-slate-700'
      "
    >
      {{ label }}
      <span v-if="required && variant !== 'auth'" class="text-rose-500">*</span>
    </span>
    <slot />
    <span v-if="error" class="block text-xs text-rose-600">{{ error }}</span>
    <span v-else-if="hint" class="block text-xs text-slate-500">{{ hint }}</span>
  </label>
</template>
