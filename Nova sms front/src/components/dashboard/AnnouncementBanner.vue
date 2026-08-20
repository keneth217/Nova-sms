<script setup lang="ts">
import { computed } from 'vue'
import { BellIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import type { Announcement, AnnouncementTone } from '@/models/announcement.model'

const props = withDefaults(
  defineProps<{
    announcement: Announcement
    dismissible?: boolean
  }>(),
  { dismissible: true },
)

const emit = defineEmits<{ dismiss: [] }>()

const headerClass = computed(() => {
  const tones: Record<AnnouncementTone, string> = {
    INFO: 'bg-violet-500',
    WARNING: 'bg-amber-500',
    DANGER: 'bg-rose-600',
  }
  return tones[props.announcement.tone] || tones.INFO
})
</script>

<template>
  <section class="overflow-hidden rounded-xl shadow-sm shadow-slate-900/10">
    <div
      class="flex items-center justify-between px-4 py-2.5 text-white"
      :class="headerClass"
    >
      <div class="flex items-center gap-2 text-sm font-semibold">
        <BellIcon class="h-4 w-4" />
        <span>{{ announcement.label || 'Announcement' }}</span>
      </div>
      <button
        v-if="dismissible"
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-full bg-white/20 text-white hover:bg-white/30"
        aria-label="Dismiss announcement"
        @click="emit('dismiss')"
      >
        <XMarkIcon class="h-4 w-4" />
      </button>
    </div>
    <div class="bg-white px-4 py-4">
      <p class="text-sm font-medium text-slate-400">{{ announcement.title }}</p>
      <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700">{{ announcement.body }}</p>
    </div>
  </section>
</template>
