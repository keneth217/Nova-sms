<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bars3Icon,
  BellIcon,
  ChevronDownIcon,
  ArrowRightOnRectangleIcon,
  UserCircleIcon,
  Cog6ToothIcon,
} from '@heroicons/vue/24/outline'
import { onClickOutside } from '@vueuse/core'
import { useAuthStore } from '@/stores/auth.store'
import { useOrganizationStore } from '@/stores/organization.store'
import { useWalletStore } from '@/stores/wallet.store'
import { formatCurrency, initials } from '@/utils/format'

defineEmits<{ toggleSidebar: [] }>()

const router = useRouter()
const auth = useAuthStore()
const org = useOrganizationStore()
const wallet = useWalletStore()

const menuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)
onClickOutside(menuRef, () => {
  menuOpen.value = false
})

const orgLabel = computed(() => (auth.isSuperAdmin ? 'Platform Admin' : org.organizationName))

const balanceLabel = computed(() =>
  auth.isSuperAdmin ? 'System' : formatCurrency(wallet.formattedBalance, wallet.currency),
)

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header
    class="sticky top-0 z-30 flex h-16 items-center justify-between gap-4 border-b border-slate-200/80 bg-white/90 px-4 backdrop-blur lg:px-6"
  >
    <div class="flex items-center gap-3">
      <button
        type="button"
        class="rounded-lg p-2 text-slate-500 hover:bg-slate-100 lg:hidden"
        @click="$emit('toggleSidebar')"
      >
        <Bars3Icon class="h-5 w-5" />
      </button>
      <div>
        <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Organization</p>
        <p class="text-sm font-semibold text-slate-900">{{ orgLabel }}</p>
      </div>
    </div>

    <div class="flex items-center gap-2 sm:gap-3">
      <div
        v-if="!auth.isSuperAdmin"
        class="hidden rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 sm:block"
      >
        <p class="text-[11px] font-medium uppercase tracking-wide text-slate-400">Balance</p>
        <p class="text-sm font-semibold text-brand-700">{{ balanceLabel }}</p>
      </div>

      <button
        type="button"
        class="relative rounded-lg p-2 text-slate-500 hover:bg-slate-100"
        aria-label="Notifications"
      >
        <BellIcon class="h-5 w-5" />
        <span
          class="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-brand-500 ring-2 ring-white"
        />
      </button>

      <div ref="menuRef" class="relative">
        <button
          type="button"
          class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-slate-50"
          @click="menuOpen = !menuOpen"
        >
          <span
            class="flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-xs font-semibold text-white"
          >
            {{ initials(auth.displayName) }}
          </span>
          <span class="hidden text-left sm:block">
            <span class="block text-sm font-medium text-slate-800">{{ auth.displayName }}</span>
            <span class="block text-xs text-slate-500">{{ auth.user?.email }}</span>
          </span>
          <ChevronDownIcon class="hidden h-4 w-4 text-slate-400 sm:block" />
        </button>

        <div
          v-if="menuOpen"
          class="absolute right-0 mt-2 w-52 overflow-hidden rounded-xl border border-slate-200 bg-white py-1 shadow-lg shadow-slate-900/10"
        >
          <RouterLink
            to="/profile"
            class="flex items-center gap-2 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"
            @click="menuOpen = false"
          >
            <UserCircleIcon class="h-4 w-4" />
            Profile
          </RouterLink>
          <RouterLink
            to="/settings"
            class="flex items-center gap-2 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"
            @click="menuOpen = false"
          >
            <Cog6ToothIcon class="h-4 w-4" />
            Settings
          </RouterLink>
          <button
            type="button"
            class="flex w-full items-center gap-2 px-3 py-2 text-sm text-rose-600 hover:bg-rose-50"
            @click="logout"
          >
            <ArrowRightOnRectangleIcon class="h-4 w-4" />
            Sign out
          </button>
        </div>
      </div>
    </div>
  </header>
</template>
