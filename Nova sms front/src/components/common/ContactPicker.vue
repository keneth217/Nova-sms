<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Contact } from '@/models/contact.model'

const props = withDefaults(
  defineProps<{
    contacts: Contact[]
    modelValue: string[]
    multiple?: boolean
    disabled?: boolean
    maxHeightClass?: string
  }>(),
  {
    multiple: true,
    disabled: false,
    maxHeightClass: 'max-h-56',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const search = ref('')

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return props.contacts
  return props.contacts.filter((c) => {
    const name = displayName(c).toLowerCase()
    return (
      c.phone.includes(q) ||
      name.includes(q) ||
      (c.email || '').toLowerCase().includes(q) ||
      c.groupNames.some((g) => g.toLowerCase().includes(q))
    )
  })
})

const selectedSet = computed(() => new Set(props.modelValue))

function displayName(contact: Contact) {
  const name = [contact.firstName, contact.lastName].filter(Boolean).join(' ').trim()
  return name || contact.phone
}

function isSelected(id: string) {
  return selectedSet.value.has(id)
}

function toggle(id: string) {
  if (props.disabled) return
  if (props.multiple) {
    const next = isSelected(id)
      ? props.modelValue.filter((x) => x !== id)
      : [...props.modelValue, id]
    emit('update:modelValue', next)
    return
  }
  emit('update:modelValue', isSelected(id) ? [] : [id])
}

function selectAllFiltered() {
  if (props.disabled || !props.multiple) return
  const ids = new Set(props.modelValue)
  for (const c of filtered.value) ids.add(c.id)
  emit('update:modelValue', [...ids])
}

function clearSelection() {
  if (props.disabled) return
  emit('update:modelValue', [])
}
</script>

<template>
  <div class="rounded-xl border border-slate-200 bg-white">
    <div class="flex flex-wrap items-center gap-2 border-b border-slate-100 px-3 py-2.5">
      <input
        v-model="search"
        type="search"
        :disabled="disabled"
        placeholder="Search contacts by name, phone, or group…"
        class="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 disabled:bg-slate-50"
      />
      <template v-if="multiple">
        <button
          type="button"
          class="rounded-lg px-2.5 py-1.5 text-xs font-medium text-brand-700 hover:bg-brand-50 disabled:opacity-50"
          :disabled="disabled || filtered.length === 0"
          @click="selectAllFiltered"
        >
          Select all
        </button>
        <button
          type="button"
          class="rounded-lg px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-50"
          :disabled="disabled || modelValue.length === 0"
          @click="clearSelection"
        >
          Clear
        </button>
      </template>
    </div>

    <div :class="['overflow-y-auto scrollbar-thin', maxHeightClass]">
      <p v-if="contacts.length === 0" class="px-4 py-8 text-center text-sm text-slate-500">
        No contacts yet. Add some under Contacts first.
      </p>
      <p v-else-if="filtered.length === 0" class="px-4 py-8 text-center text-sm text-slate-500">
        No contacts match your search.
      </p>
      <ul v-else class="divide-y divide-slate-100">
        <li v-for="contact in filtered" :key="contact.id">
          <label
            class="flex cursor-pointer items-start gap-3 px-4 py-3 transition hover:bg-slate-50"
            :class="{ 'opacity-50': disabled, 'bg-brand-50/60': isSelected(contact.id) }"
          >
            <input
              class="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
              :type="multiple ? 'checkbox' : 'radio'"
              :name="multiple ? undefined : 'contact-picker'"
              :checked="isSelected(contact.id)"
              :disabled="disabled"
              @change="toggle(contact.id)"
            />
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-medium text-slate-900">
                {{ displayName(contact) }}
              </span>
              <span class="mt-0.5 block truncate text-xs text-slate-500">
                {{ contact.phone }}
                <template v-if="contact.groupNames.length">
                  · {{ contact.groupNames.join(', ') }}
                </template>
              </span>
            </span>
          </label>
        </li>
      </ul>
    </div>

    <div
      v-if="modelValue.length"
      class="border-t border-slate-100 px-4 py-2 text-xs font-medium text-slate-600"
    >
      {{ modelValue.length }} contact{{ modelValue.length === 1 ? '' : 's' }} selected
    </div>
  </div>
</template>
