<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import Sidebar from '@/components/dashboard/Sidebar.vue'
import Navbar from '@/components/dashboard/Navbar.vue'
import { useAuthStore } from '@/stores/auth.store'
import { useWalletStore } from '@/stores/wallet.store'
import { useOrganizationStore } from '@/stores/organization.store'

const sidebarOpen = ref(false)
const auth = useAuthStore()
const wallet = useWalletStore()
const org = useOrganizationStore()

onMounted(() => {
  if (!auth.isSuperAdmin) {
    void wallet.fetchBalance()
    if (auth.user?.organizationId && !org.organizationName) {
      void org.fetchCurrentOrganization().catch(() => {
        if (auth.user?.organizationName) {
          org.setOrganizationName(auth.user.organizationName)
        }
      })
    } else if (auth.user?.organizationName && !org.organizationName) {
      org.setOrganizationName(auth.user.organizationName)
    }
  }
})
</script>

<template>
  <div class="flex min-h-screen bg-surface-50">
    <Sidebar :open="sidebarOpen" @close="sidebarOpen = false" />
    <div class="flex min-w-0 flex-1 flex-col">
      <Navbar @toggle-sidebar="sidebarOpen = !sidebarOpen" />
      <main class="flex-1 px-4 py-6 lg:px-8">
        <RouterView />
      </main>
    </div>
  </div>
</template>
