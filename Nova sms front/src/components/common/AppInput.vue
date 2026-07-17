<script setup lang="ts">
import { computed, ref } from 'vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    modelValue?: string | number
    type?: string
    placeholder?: string
    disabled?: boolean
    rows?: number
  }>(),
  { type: 'text', modelValue: '', rows: 4 },
)

defineEmits<{ 'update:modelValue': [value: string] }>()

const passwordVisible = ref(false)
const inputType = computed(() =>
  props.type === 'password' && passwordVisible.value ? 'text' : props.type,
)

const inputClass =
  'w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 shadow-sm transition focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 disabled:bg-slate-50 disabled:text-slate-500'
</script>

<template>
  <textarea
    v-if="type === 'textarea'"
    :value="modelValue"
    :rows="rows"
    :placeholder="placeholder"
    :disabled="disabled"
    :class="inputClass"
    v-bind="$attrs"
    @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
  <div v-else class="relative">
    <input
      :type="inputType"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :class="[inputClass, type === 'password' ? 'pr-10' : '']"
      v-bind="$attrs"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <button
      v-if="type === 'password'"
      type="button"
      class="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-slate-400 transition hover:text-slate-700 disabled:cursor-not-allowed disabled:text-slate-300"
      :disabled="disabled"
      :aria-label="passwordVisible ? 'Hide password' : 'Show password'"
      :title="passwordVisible ? 'Hide password' : 'Show password'"
      @click="passwordVisible = !passwordVisible"
    >
      <EyeSlashIcon v-if="passwordVisible" class="h-5 w-5" />
      <EyeIcon v-else class="h-5 w-5" />
    </button>
  </div>
</template>
