<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const form = reactive({ password: '', confirmPassword: '' })
const success = ref('')
const localError = ref('')

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))

async function onSubmit() {
  success.value = ''
  localError.value = ''
  if (!token.value) {
    localError.value = 'This reset link is missing its security token.'
    return
  }
  if (form.password.length < 8) {
    localError.value = 'Password must contain at least 8 characters.'
    return
  }
  if (form.password !== form.confirmPassword) {
    localError.value = 'Passwords do not match.'
    return
  }

  try {
    success.value = await auth.resetPassword({
      token: token.value,
      newPassword: form.password,
    })
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Unable to reset password'
  }
}

async function goToLogin() {
  await router.push({ name: 'login' })
}
</script>

<template>
  <div
    class="relative min-h-[calc(100svh-4rem)] overflow-hidden px-4 pb-16 pt-28 sm:px-6 lg:px-8"
  >
    <div
      class="pointer-events-none absolute inset-0 bg-[linear-gradient(165deg,#eef7f5_0%,#f7faf9_38%,#e8f1f4_100%)]"
    />
    <div class="relative mx-auto w-full max-w-md">
      <div class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm sm:p-8">
        <h1 class="text-xl font-semibold text-slate-900">Create a new password</h1>
        <p class="mt-1 text-sm text-slate-500">
          Choose a strong password that you have not used before.
        </p>

        <div
          v-if="success"
          class="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-sm text-emerald-800"
        >
          <p class="font-semibold">{{ success }}</p>
          <AppButton class="mt-4" block @click="goToLogin">Continue to sign in</AppButton>
        </div>

        <form v-else class="mt-6 space-y-4" @submit.prevent="onSubmit">
          <FormField label="New password" required hint="At least 8 characters">
            <AppInput
              v-model="form.password"
              type="password"
              autocomplete="new-password"
              placeholder="••••••••"
            />
          </FormField>
          <FormField label="Confirm password" required>
            <AppInput
              v-model="form.confirmPassword"
              type="password"
              autocomplete="new-password"
              placeholder="••••••••"
            />
          </FormField>
          <p v-if="localError || auth.error" class="text-sm text-rose-600">
            {{ localError || auth.error }}
          </p>
          <AppButton
            type="submit"
            block
            :loading="auth.loading"
            :disabled="!form.password || !form.confirmPassword || !token"
          >
            Reset password
          </AppButton>
        </form>

        <p class="mt-6 text-center text-sm text-slate-500">
          <RouterLink to="/login" class="font-medium text-brand-700 hover:text-brand-800">
            Back to sign in
          </RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
