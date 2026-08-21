<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import AuthSplitLayout from '@/components/auth/AuthSplitLayout.vue'
import FormField from '@/components/common/FormField.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppButton from '@/components/common/AppButton.vue'

type Intent = 'event' | 'business'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const intent = ref<Intent>(
  route.query.intent === 'business' ? 'business' : route.query.intent === 'event' ? 'event' : 'event',
)

const form = reactive({
  name: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  adminFullName: '',
})
const termsAccepted = ref(false)
const success = ref(false)
const apiKey = ref('')
const localError = ref('')

const isEvent = computed(() => intent.value === 'event')

const copy = computed(() =>
  isEvent.value
    ? {
        kicker: 'Create account',
        headline: 'Get started.',
        intro: 'Create an event account to send SMS for one week.',
        panelHeadline: 'Join a platform built to deliver.',
        panelBody:
          'Create your account to send SMS, manage contacts, and stay on top of delivery from one dashboard.',
        nameLabel: 'Account name',
        namePlaceholder: 'Kamau Family Gathering',
        personLabel: 'Full name',
        emailLabel: 'Email',
        emailPlaceholder: 'you@example.com',
        submit: 'Create account',
      }
    : {
        kicker: 'Create account',
        headline: 'Get started.',
        intro: 'Enter your details to create your Nova SMS organization.',
        panelHeadline: 'Join a platform built to deliver.',
        panelBody:
          'Create your account to send bulk SMS, fund the wallet with M-Pesa, and keep your team connected.',
        nameLabel: 'Organization name',
        namePlaceholder: 'Acme Logistics Ltd',
        personLabel: 'Full name',
        emailLabel: 'Email',
        emailPlaceholder: 'you@example.com',
        submit: 'Create account',
      },
)

watch(
  () => route.query.intent,
  (value) => {
    if (value === 'business' || value === 'event') intent.value = value
  },
)

function setIntent(next: Intent) {
  intent.value = next
  void router.replace({ query: { ...route.query, intent: next } })
}

async function onSubmit() {
  localError.value = ''
  if (form.password !== form.confirmPassword) {
    localError.value = 'Passwords do not match.'
    return
  }
  if (!termsAccepted.value) {
    localError.value = 'Please accept the Terms of Service, Privacy Policy, and Acceptable Use Policy.'
    return
  }
  try {
    const org = await auth.register({
      name: form.name,
      email: form.email,
      phone: form.phone,
      password: form.password,
      adminFullName: form.adminFullName,
      accountType: isEvent.value ? 'EVENT' : 'BUSINESS',
      termsAccepted: true,
    })
    apiKey.value = org.apiKey || ''
    success.value = true
    localStorage.setItem(
      'nova_sms_account_intent',
      JSON.stringify({
        intent: isEvent.value ? 'event' : 'business',
        accountType: org.accountType,
        activatedAt: org.createdAt,
        expiresAt: org.expiresAt,
        activeDays: org.activeDays ?? (isEvent.value ? 7 : null),
        organizationName: form.name,
      }),
    )
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Registration failed'
  }
}

async function goLogin() {
  await router.push({ name: 'login' })
}
</script>

<template>
  <AuthSplitLayout
    :kicker="copy.kicker"
    :headline="copy.headline"
    :intro="copy.intro"
    panel-kicker="Begin here"
    :panel-headline="copy.panelHeadline"
    :panel-body="copy.panelBody"
  >
    <template v-if="!success">
      <div class="mb-6 grid grid-cols-2 gap-1 rounded-xl bg-slate-100/80 p-1">
        <button
          type="button"
          class="rounded-lg px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em] transition"
          :class="isEvent ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
          @click="setIntent('event')"
        >
          Event
        </button>
        <button
          type="button"
          class="rounded-lg px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em] transition"
          :class="!isEvent ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
          @click="setIntent('business')"
        >
          Business
        </button>
      </div>

      <form class="space-y-5" @submit.prevent="onSubmit">
        <FormField variant="auth" :label="copy.nameLabel" required>
          <AppInput v-model="form.name" :placeholder="copy.namePlaceholder" />
        </FormField>
        <FormField variant="auth" :label="copy.personLabel" required>
          <AppInput v-model="form.adminFullName" placeholder="Your full name" />
        </FormField>
        <FormField variant="auth" :label="copy.emailLabel" required>
          <AppInput v-model="form.email" type="email" :placeholder="copy.emailPlaceholder" />
        </FormField>
        <FormField variant="auth" label="Phone number" required>
          <AppInput v-model="form.phone" type="tel" placeholder="0712345678" />
        </FormField>
        <FormField variant="auth" label="Password" required>
          <AppInput v-model="form.password" type="password" placeholder="••••••••" />
        </FormField>
        <FormField variant="auth" label="Confirm password" required>
          <AppInput v-model="form.confirmPassword" type="password" placeholder="••••••••" />
        </FormField>

        <label class="flex items-start gap-3 text-sm text-slate-600">
          <input
            v-model="termsAccepted"
            type="checkbox"
            class="mt-1 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
          />
          <span>
            I have read and agree to the
            <RouterLink to="/terms" class="font-medium text-brand-700 hover:underline" target="_blank">
              Terms of Service
            </RouterLink>,
            <RouterLink to="/privacy" class="font-medium text-brand-700 hover:underline" target="_blank">
              Privacy Policy
            </RouterLink>,
            and
            <RouterLink
              to="/acceptable-use"
              class="font-medium text-brand-700 hover:underline"
              target="_blank"
            >
              Acceptable Use Policy
            </RouterLink>.
          </span>
        </label>

        <p v-if="localError || auth.error" class="text-sm text-rose-600">
          {{ localError || auth.error }}
        </p>

        <AppButton
          type="submit"
          block
          size="lg"
          class="rounded-xl uppercase tracking-[0.16em]"
          :loading="auth.loading"
          :disabled="!termsAccepted"
        >
          {{ copy.submit }}
        </AppButton>
      </form>

      <p class="mt-8 text-center text-sm text-slate-500">
        Already have an account?
        <RouterLink to="/login" class="font-medium text-brand-700 hover:text-brand-800">
          Sign in
        </RouterLink>
      </p>
    </template>

    <template v-else>
      <h2 class="font-serif text-2xl text-slate-900">
        {{ isEvent ? 'Event account created' : 'Organization created' }}
      </h2>
      <p class="mt-2 text-sm text-slate-500">
        <template v-if="isEvent">
          Your account is active for
          {{ auth.registeredOrg?.activeDays || 7 }} days
          <template v-if="auth.registeredOrg?.expiresAt">
            (until {{ new Date(auth.registeredOrg.expiresAt).toLocaleString() }})
          </template>
          .
        </template>
        <template v-else>
          Save your API key securely — it will not be shown again.
        </template>
      </p>

      <div class="mt-5 rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
        <p class="font-semibold">Prepaid wallet ready</p>
        <p class="mt-1 text-emerald-800/90">
          Balance
          {{
            Number(auth.registeredOrg?.walletBalance ?? 0).toLocaleString('en-KE', {
              style: 'currency',
              currency: auth.registeredOrg?.walletCurrency || 'KES',
            })
          }}.
          Sign in, open Wallet to top up with M-Pesa, then send SMS.
        </p>
      </div>

      <div
        v-if="!isEvent"
        class="mt-4 rounded-xl bg-slate-900 px-4 py-3 font-mono text-xs text-brand-200 break-all"
      >
        {{ apiKey }}
      </div>
      <div
        v-else
        class="mt-4 rounded-xl border border-brand-100 bg-brand-50 px-4 py-3 text-sm text-brand-900"
      >
        Next: sign in → Wallet → top up → Bulk SMS.
      </div>
      <AppButton class="mt-6 rounded-xl uppercase tracking-[0.16em]" block @click="goLogin">
        Continue to sign in
      </AppButton>
    </template>
  </AuthSplitLayout>
</template>
