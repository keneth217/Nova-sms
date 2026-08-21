<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import AuthSplitLayout from '@/components/auth/AuthSplitLayout.vue'
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
  <AuthSplitLayout
    kicker="Password"
    headline="Reset access."
    intro="Enter your account email and we will send a secure reset link."
    panel-kicker="Account recovery"
    panel-headline="Get back to sending."
    panel-body="We’ll email a reset link so you can choose a new password and return to your dashboard."
  >
    <div
      v-if="success"
      class="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
    >
      {{ success }}
    </div>

    <form v-else class="space-y-5" @submit.prevent="onSubmit">
      <FormField variant="auth" label="Email" required>
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
      <AppButton
        type="submit"
        block
        class="rounded-xl uppercase tracking-[0.16em]"
        :loading="auth.loading"
        :disabled="!form.email.trim()"
      >
        Send reset link
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
