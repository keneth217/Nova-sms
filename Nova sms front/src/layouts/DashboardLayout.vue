<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import Sidebar from '@/components/dashboard/Sidebar.vue'
import Navbar from '@/components/dashboard/Navbar.vue'
import AnnouncementBanner from '@/components/dashboard/AnnouncementBanner.vue'
import { useAuthStore } from '@/stores/auth.store'
import { useWalletStore } from '@/stores/wallet.store'
import { useOrganizationStore } from '@/stores/organization.store'
import { announcementService } from '@/api/announcement.service'
import type { Announcement } from '@/models/announcement.model'

const DISMISS_KEY = 'nova_sms_announcement_dismissed'

const sidebarOpen = ref(false)
const auth = useAuthStore()
const wallet = useWalletStore()
const org = useOrganizationStore()
const announcement = ref<Announcement | null>(null)
const dismissed = ref(false)

function dismissKey(item: Announcement) {
  return `${DISMISS_KEY}:${item.updatedAt || item.body}`
}

function isDismissed(item: Announcement) {
  try {
    return localStorage.getItem(dismissKey(item)) === '1'
  } catch {
    return false
  }
}

function dismissAnnouncement() {
  if (!announcement.value) return
  try {
    localStorage.setItem(dismissKey(announcement.value), '1')
  } catch {
    void 0
  }
  dismissed.value = true
}

onMounted(() => {
  if (!auth.isSuperAdmin) {
    void wallet.fetchBalance()
    if (auth.user?.organizationId) {
      void org.fetchCurrentOrganization().catch(() => {
        if (auth.user?.organizationName) {
          org.setOrganizationName(auth.user.organizationName)
        }
      })
    } else if (auth.user?.organizationName && !org.organizationName) {
      org.setOrganizationName(auth.user.organizationName)
    }
  }
  void announcementService
    .getPublic()
    .then((item) => {
      announcement.value = item
      dismissed.value = isDismissed(item)
    })
    .catch(() => {
      announcement.value = null
    })
})
</script>

<template>
  <div class="flex min-h-screen bg-surface-50">
    <Sidebar :open="sidebarOpen" @close="sidebarOpen = false" />
    <div class="flex min-w-0 flex-1 flex-col">
      <Navbar @toggle-sidebar="sidebarOpen = !sidebarOpen" />
      <main class="flex-1 px-4 py-6 lg:px-8">
        <div
          v-if="announcement?.enabled && announcement.body && !dismissed"
          class="mb-6"
        >
          <AnnouncementBanner :announcement="announcement" @dismiss="dismissAnnouncement" />
        </div>
        <RouterView />
      </main>
    </div>
  </div>
</template>
