<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const router = useRouter()
const auth = useAuthStore()
const profileError = ref('')
const passwordError = ref('')
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

onMounted(async () => {
  try {
    await auth.fetchProfile()
  } catch (error) {
    profileError.value = error instanceof Error ? error.message : 'Unable to load profile'
  }
})

async function changePassword() {
  passwordError.value = ''
  if (passwordForm.newPassword.length < 8) {
    passwordError.value = 'New password must contain at least 8 characters.'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = 'New passwords do not match.'
    return
  }

  try {
    await auth.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    auth.logout(true)
    await router.replace({ name: 'login', query: { password: 'changed' } })
  } catch (error) {
    passwordError.value = error instanceof Error ? error.message : 'Unable to change password'
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Profile"
      description="View your account details and manage your password."
    />

    <p v-if="profileError" class="mb-4 text-sm text-rose-600">{{ profileError }}</p>

    <div class="grid gap-6 lg:grid-cols-2">
      <AppCard title="Account details">
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Full name</dt>
            <dd class="font-medium text-slate-900">
              {{ auth.profile?.fullName || auth.user?.fullName }}
            </dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Email</dt>
            <dd class="font-medium text-slate-900">
              {{ auth.profile?.email || auth.user?.email }}
            </dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Role</dt>
            <dd>
              <StatusBadge variant="brand">
                {{ auth.profile?.role || auth.user?.role }}
              </StatusBadge>
            </dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">User ID</dt>
            <dd class="font-mono text-xs text-slate-600">
              {{ auth.profile?.id || auth.user?.userId }}
            </dd>
          </div>
          <div v-if="auth.profile?.createdAt" class="flex justify-between gap-4">
            <dt class="text-slate-500">Member since</dt>
            <dd class="font-medium text-slate-900">
              {{ formatDate(auth.profile.createdAt, false) }}
            </dd>
          </div>
        </dl>
      </AppCard>

      <AppCard title="Change password">
        <form class="space-y-4" @submit.prevent="changePassword">
          <FormField label="Current password" required>
            <AppInput
              v-model="passwordForm.currentPassword"
              type="password"
              autocomplete="current-password"
            />
          </FormField>
          <FormField label="New password" required hint="At least 8 characters">
            <AppInput
              v-model="passwordForm.newPassword"
              type="password"
              autocomplete="new-password"
            />
          </FormField>
          <FormField label="Confirm new password" required>
            <AppInput
              v-model="passwordForm.confirmPassword"
              type="password"
              autocomplete="new-password"
            />
          </FormField>
          <p v-if="passwordError" class="text-sm text-rose-600">{{ passwordError }}</p>
          <AppButton
            type="submit"
            :loading="auth.loading"
            :disabled="
              !passwordForm.currentPassword ||
              !passwordForm.newPassword ||
              !passwordForm.confirmPassword
            "
          >
            Update password
          </AppButton>
        </form>
      </AppCard>
    </div>
  </div>
</template>
