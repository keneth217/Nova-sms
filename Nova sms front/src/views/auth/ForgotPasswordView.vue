<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'

const auth = useAuthStore()
const form = reactive({ email: '' })
const success = ref('')
const localError = ref('')

async function onSubmit() {
  success.value = ''
  localError.value = ''
  try {
    success.value = await auth.forgotPassword({ email: form.email.trim().toLowerCase() })
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Unable to request a reset link'
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
    <div class="relative mx-auto w-full max-w-md">
      <div class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm sm:p-8">
        <h1 class="text-xl font-semibold text-slate-900">Forgot your password?</h1>
        <p class="mt-1 text-sm leading-relaxed text-slate-500">
          Enter your account email and we will send a secure reset link.
        </p>

        <div
          v-if="success"
          class="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
        >
          {{ success }}
        </div>

        <form v-else class="mt-6 space-y-4" @submit.prevent="onSubmit">
          <FormField label="Email address" required>
            <AppInput
              v-model="form.email"
              type="email"
              autocomplete="email"
              placeholder="you@example.com"
            />
          </FormField>
          <p v-if="localError || auth.error" class="text-sm text-rose-600">
            {{ localError || auth.error }}
          </p>
          <AppButton type="submit" block :loading="auth.loading" :disabled="!form.email.trim()">
            Send reset link
          </AppButton>
        </form>

        <p class="mt-6 text-center text-sm text-slate-500">
          Remembered your password?
          <RouterLink to="/login" class="font-medium text-brand-700 hover:text-brand-800">
            Back to sign in
          </RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
