<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import AuthSplitLayout from '@/components/auth/AuthSplitLayout.vue'
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
  <AuthSplitLayout
    kicker="Sign in"
    headline="Welcome back."
    intro="Enter your details to access your Nova SMS dashboard."
    panel-kicker="Welcome back"
    panel-headline="Your messages, already in motion."
    panel-body="Sign in to send SMS, check delivery, and top up the organization wallet with M-Pesa."
  >
    <div
      v-if="route.query.session === 'expired'"
      class="mb-5 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
    >
      Your session expired. Please sign in again.
    </div>
    <div
      v-if="route.query.password === 'changed'"
      class="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800"
    >
      Password updated. Sign in with your new password.
    </div>

    <form class="space-y-5" @submit.prevent="onSubmit">
      <FormField variant="auth" label="Email or phone" required>
        <AppInput
          v-model="form.email"
          type="text"
          autocomplete="username"
          placeholder="you@example.com"
        />
      </FormField>
      <FormField variant="auth" label="Password" required>
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

      <AppButton
        type="submit"
        block
        size="lg"
        class="rounded-xl uppercase tracking-[0.16em]"
        :loading="auth.loading"
      >
        Sign in
      </AppButton>
    </form>

    <p class="mt-8 text-center text-sm text-slate-500">
      New organization?
      <RouterLink to="/register" class="font-medium text-brand-700 hover:text-brand-800">
        Create an account
      </RouterLink>
    </p>
  </AuthSplitLayout>
</template>
