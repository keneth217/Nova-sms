<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  HomeIcon,
  WalletIcon,
  PaperAirplaneIcon,
  QueueListIcon,
  UserGroupIcon,
  IdentificationIcon,
  ClockIcon,
  ChartBarIcon,
  Cog6ToothIcon,
  BuildingOffice2Icon,
  BanknotesIcon,
  SignalIcon,
  PresentationChartLineIcon,
  UserCircleIcon,
  DevicePhoneMobileIcon,
  XMarkIcon,
} from '@heroicons/vue/24/outline'
import { useAuthStore } from '@/stores/auth.store'

defineProps<{ open: boolean }>()
defineEmits<{ close: [] }>()

const route = useRoute()
const auth = useAuthStore()

const orgLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: HomeIcon },
  { to: '/wallet', label: 'Wallet', icon: WalletIcon },
  { to: '/data-bundles/history', label: 'Bundle History', icon: DevicePhoneMobileIcon },
  { to: '/send-sms', label: 'Send SMS', icon: PaperAirplaneIcon },
  { to: '/bulk-sms', label: 'Bulk SMS', icon: QueueListIcon },
  { to: '/contacts', label: 'Contacts', icon: UserGroupIcon },
  { to: '/sender-ids', label: 'Sender IDs', icon: IdentificationIcon },
  { to: '/sms-history', label: 'SMS History', icon: ClockIcon },
  { to: '/reports', label: 'Reports', icon: ChartBarIcon },
  { to: '/profile', label: 'Profile', icon: UserCircleIcon },
  { to: '/settings', label: 'Settings', icon: Cog6ToothIcon },
]

const adminLinks = [
  { to: '/admin/system-reports', label: 'Overview', icon: PresentationChartLineIcon },
  { to: '/admin/organizations', label: 'Organizations', icon: BuildingOffice2Icon },
  { to: '/admin/topups', label: 'Wallet Funding', icon: BanknotesIcon },
  { to: '/admin/sms-monitoring', label: 'SMS Monitoring', icon: SignalIcon },
  { to: '/sender-ids', label: 'Sender IDs', icon: IdentificationIcon },
  { to: '/profile', label: 'Profile', icon: UserCircleIcon },
  { to: '/settings', label: 'Settings', icon: Cog6ToothIcon },
]

const links = computed(() => (auth.isSuperAdmin ? adminLinks : orgLinks))

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-40 bg-slate-900/40 lg:hidden" @click="$emit('close')" />

  <aside
    class="fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-slate-200 bg-white transition-transform duration-200 lg:static lg:translate-x-0"
    :class="open ? 'translate-x-0' : '-translate-x-full'"
  >
    <div class="flex h-16 items-center justify-between border-b border-slate-100 px-5">
      <RouterLink to="/dashboard" class="flex items-center gap-2.5" @click="$emit('close')">
        <span
          class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white"
        >
          N
        </span>
        <div>
          <p class="text-sm font-semibold text-slate-900">Nova SMS</p>
          <p class="text-[11px] text-slate-500">Bulk Gateway</p>
        </div>
      </RouterLink>
      <button
        type="button"
        class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 lg:hidden"
        @click="$emit('close')"
      >
        <XMarkIcon class="h-5 w-5" />
      </button>
    </div>

    <nav class="flex-1 space-y-1 overflow-y-auto p-3 scrollbar-thin">
      <p class="px-3 pb-2 pt-1 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
        {{ auth.isSuperAdmin ? 'Platform' : 'Workspace' }}
      </p>
      <RouterLink
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition"
        :class="
          isActive(link.to)
            ? 'bg-brand-50 text-brand-700'
            : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
        "
        @click="$emit('close')"
      >
        <component :is="link.icon" class="h-5 w-5 shrink-0" />
        {{ link.label }}
      </RouterLink>
    </nav>

    <div class="border-t border-slate-100 p-4">
      <div class="rounded-lg bg-slate-50 px-3 py-2.5">
        <p class="text-xs font-medium text-slate-500">Signed in as</p>
        <p class="truncate text-sm font-semibold text-slate-800">{{ auth.displayName }}</p>
        <p class="truncate text-xs text-slate-500">{{ auth.user?.role?.replace('_', ' ') }}</p>
      </div>
    </div>
  </aside>
</template>
