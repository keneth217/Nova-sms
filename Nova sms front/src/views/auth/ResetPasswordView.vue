<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import AuthSplitLayout from '@/components/auth/AuthSplitLayout.vue'
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
  <AuthSplitLayout
    kicker="Password"
    headline="Set a new one."
    intro="Choose a strong password that you have not used before."
    panel-kicker="Account recovery"
    panel-headline="Get back to sending."
    panel-body="Set a new password and return to your Nova SMS dashboard."
  >
    <div
      v-if="success"
      class="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-sm text-emerald-800"
    >
      <p class="font-semibold">{{ success }}</p>
      <AppButton class="mt-4 rounded-xl uppercase tracking-[0.16em]" block @click="goToLogin">
        Continue to sign in
      </AppButton>
    </div>

    <form v-else class="space-y-5" @submit.prevent="onSubmit">
      <FormField variant="auth" label="Password" required>
        <AppInput
          v-model="form.password"
          type="password"
          autocomplete="new-password"
          placeholder="••••••••"
        />
      </FormField>
      <FormField variant="auth" label="Confirm password" required>
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
        class="rounded-xl uppercase tracking-[0.16em]"
        :loading="auth.loading"
        :disabled="!form.password || !form.confirmPassword || !token"
      >
        Reset password
      </AppButton>
    </form>

    <p class="mt-8 text-center text-sm text-slate-500">
      Already have an account?
      <RouterLink to="/login" class="font-medium text-brand-700 hover:text-brand-800">
        Sign in
      </RouterLink>
    </p>
  </AuthSplitLayout>
</template>
