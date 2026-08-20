<script setup lang="ts">
import { RouterLink } from 'vue-router'

withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: 'Ready to send SMS?',
    description: 'Create an account, top up with M-Pesa, and send from the dashboard or the API.',
  },
)
</script>

<template>
  <section class="rounded-2xl bg-brand-700 px-6 py-8 text-white sm:px-8">
    <h2 class="font-serif text-2xl font-bold">{{ title }}</h2>
    <p class="mt-2 text-sm text-teal-50">{{ description }}</p>
    <div class="mt-5 flex flex-wrap gap-3">
      <RouterLink
        to="/register"
        class="rounded-lg bg-white px-4 py-2.5 text-sm font-semibold text-brand-800 hover:bg-teal-50"
      >
        Create account
      </RouterLink>
      <RouterLink
        to="/developers"
        class="rounded-lg border border-white/40 px-4 py-2.5 text-sm font-semibold text-white hover:bg-white/10"
      >
        View documentation
      </RouterLink>
      <RouterLink
        to="/login"
        class="rounded-lg px-4 py-2.5 text-sm font-semibold text-teal-100 hover:text-white"
      >
        Sign in
      </RouterLink>
    </div>
  </section>
</template>
