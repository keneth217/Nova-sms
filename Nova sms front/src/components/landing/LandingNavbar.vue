<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Bars3Icon, XMarkIcon } from '@heroicons/vue/24/outline'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const auth = useAuthStore()
const open = ref(false)
const scrolled = ref(false)

const isAuthPage = computed(
  () => route.name === 'login' || route.name === 'register',
)

const navSolid = computed(() => scrolled.value || open.value || isAuthPage.value)

function onScroll() {
  scrolled.value = window.scrollY > 12
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))

function goFeatures() {
  open.value = false
  if (route.path === '/') {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })
  }
}

function goEvents() {
  open.value = false
  if (route.path === '/') {
    document.getElementById('events')?.scrollIntoView({ behavior: 'smooth' })
  }
}
</script>

<template>
  <header
    class="fixed inset-x-0 top-0 z-40 transition-all duration-300"
    :class="
      navSolid
        ? 'border-b border-slate-200/80 bg-white/90 shadow-sm shadow-slate-900/5 backdrop-blur-md'
        : 'bg-transparent'
    "
  >
    <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
      <RouterLink to="/" class="flex items-center gap-2.5" @click="open = false">
        <span
          class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white"
        >
          N
        </span>
        <span class="font-serif text-lg font-bold tracking-tight text-slate-900">Nova SMS</span>
      </RouterLink>

      <nav class="hidden items-center gap-8 md:flex">
        <RouterLink
          to="/"
          class="text-sm font-medium text-slate-600 transition hover:text-slate-900"
          active-class="!text-slate-900"
        >
          Home
        </RouterLink>
        <RouterLink
          to="/#events"
          class="text-sm font-medium text-slate-600 transition hover:text-slate-900"
          @click="goEvents"
        >
          Events
        </RouterLink>
        <RouterLink
          to="/#features"
          class="text-sm font-medium text-slate-600 transition hover:text-slate-900"
          @click="goFeatures"
        >
          Features
        </RouterLink>
      </nav>

      <div class="hidden items-center gap-3 md:flex">
        <template v-if="auth.isAuthenticated">
          <RouterLink
            :to="auth.isSuperAdmin ? '/admin/system-reports' : '/dashboard'"
            class="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-brand-700"
          >
            Go to dashboard
          </RouterLink>
        </template>
        <template v-else>
          <RouterLink
            to="/login"
            class="text-sm font-medium transition"
            :class="
              route.name === 'login'
                ? 'text-slate-900'
                : 'text-slate-600 hover:text-slate-900'
            "
          >
            Login
          </RouterLink>
          <RouterLink
            to="/register"
            class="rounded-lg px-4 py-2 text-sm font-semibold transition"
            :class="
              route.name === 'register'
                ? 'bg-brand-700 text-white'
                : 'bg-brand-600 text-white hover:bg-brand-700'
            "
          >
            Register
          </RouterLink>
        </template>
      </div>

      <button
        type="button"
        class="rounded-lg p-2 text-slate-600 hover:bg-slate-100 md:hidden"
        aria-label="Toggle menu"
        @click="open = !open"
      >
        <Bars3Icon v-if="!open" class="h-6 w-6" />
        <XMarkIcon v-else class="h-6 w-6" />
      </button>
    </div>

    <div
      v-if="open"
      class="border-t border-slate-200 bg-white px-4 py-4 md:hidden"
    >
      <div class="flex flex-col gap-3">
        <RouterLink to="/" class="text-sm font-medium text-slate-900" @click="open = false">
          Home
        </RouterLink>
        <RouterLink
          to="/#events"
          class="text-sm font-medium text-slate-600"
          @click="goEvents"
        >
          Events
        </RouterLink>
        <RouterLink
          to="/#features"
          class="text-sm font-medium text-slate-600"
          @click="goFeatures"
        >
          Features
        </RouterLink>
        <hr class="border-slate-200" />
        <template v-if="auth.isAuthenticated">
          <RouterLink
            :to="auth.isSuperAdmin ? '/admin/system-reports' : '/dashboard'"
            class="text-sm font-semibold text-brand-700"
            @click="open = false"
          >
            Go to dashboard
          </RouterLink>
        </template>
        <template v-else>
          <RouterLink to="/login" class="text-sm font-medium text-slate-600" @click="open = false">
            Login
          </RouterLink>
          <RouterLink
            to="/register"
            class="rounded-lg bg-brand-600 px-4 py-2.5 text-center text-sm font-semibold text-white"
            @click="open = false"
          >
            Register
          </RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>
