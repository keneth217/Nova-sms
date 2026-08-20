<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import FormField from '@/components/common/FormField.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppButton from '@/components/common/AppButton.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({
  email: '',
  password: '',
})
const localError = ref('')

async function onSubmit() {
  localError.value = ''
  if (!form.email.trim()) {
    localError.value = 'Enter your email or phone number'
    return
  }
  try {
    await auth.login({ email: form.email.trim(), password: form.password })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    if (redirect) {
      await router.push(redirect)
    } else if (auth.isSuperAdmin) {
      await router.push({ name: 'admin-system-reports' })
    } else {
      await router.push({ name: 'dashboard' })
    }
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Login failed'
  }
}
</script>

<template>
  <div
    class="relative min-h-[calc(100svh-4rem)] overflow-hidden px-4 pb-16 pt-28 sm:px-6 lg:px-8"
  >
    <div
      class="pointer-events-none absolute inset-0 bg-[linear-gradient(165deg,#eef7f5_0%,#f7faf9_38%,#e8f1f4_100%)]"
    />
    <div
      class="pointer-events-none absolute -right-20 top-10 h-72 w-72 rounded-full bg-brand-200/40 blur-3xl"
    />
    <div
      class="pointer-events-none absolute -left-16 bottom-0 h-64 w-64 rounded-full bg-sky-200/30 blur-3xl"
    />

    <div class="relative mx-auto w-full max-w-md">
      <div
        class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm shadow-slate-900/5 sm:p-8"
      >
        <h1 class="text-xl font-semibold text-slate-900">Sign in</h1>
        <p class="mt-1 text-sm text-slate-500">Use your email or phone number</p>

        <div
          v-if="route.query.session === 'expired'"
          class="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
        >
          Your session expired. Please sign in again.
        </div>
        <div
          v-if="route.query.password === 'changed'"
          class="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800"
        >
          Password updated. Sign in with your new password.
        </div>

        <form class="mt-6 space-y-4" @submit.prevent="onSubmit">
          <FormField
            label="Email or phone"
            required
            hint="Email, or phone as 07… / 01… / 254…"
          >
            <AppInput
              v-model="form.email"
              type="text"
              autocomplete="username"
              placeholder="you@email.com or 0712345678"
            />
          </FormField>
          <FormField label="Password" required>
            <AppInput
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••"
            />
          </FormField>
          <div class="-mt-2 text-right">
            <RouterLink
              to="/forgot-password"
              class="text-sm font-medium text-brand-700 hover:text-brand-800"
            >
              Forgot password?
            </RouterLink>
          </div>

          <p v-if="localError || auth.error" class="text-sm text-rose-600">
            {{ localError || auth.error }}
          </p>

          <AppButton type="submit" block :loading="auth.loading">Sign in</AppButton>
        </form>

        <p class="mt-6 text-center text-sm text-slate-500">
          New organization?
          <RouterLink to="/register" class="font-medium text-brand-700 hover:text-brand-800">
            Create an account
          </RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
