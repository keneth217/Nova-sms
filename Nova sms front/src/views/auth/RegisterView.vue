<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
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
        title: 'Create event account',
        subtitle: 'For events, ceremonies, and one-week sending',
        nameLabel: 'Account / event name',
        namePlaceholder: 'e.g. Kamau Family Gathering',
        personLabel: 'Your full name',
        emailLabel: 'Email',
        emailPlaceholder: 'you@gmail.com',
        note: 'Event accounts stay active for 1 week. A prepaid wallet is created so you can top up and send SMS.',
        submit: 'Create event account',
      }
    : {
        title: 'Create organization',
        subtitle: 'For ongoing SMS campaigns and business alerts',
        nameLabel: 'Organization name',
        namePlaceholder: 'Acme Logistics Ltd',
        personLabel: 'Admin full name',
        emailLabel: 'Work email',
        emailPlaceholder: 'admin@company.com',
        note: 'We create a prepaid wallet for your org — top up with M-Pesa, then send SMS from the balance.',
        submit: 'Create business account',
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
  if (!termsAccepted.value) {
    localError.value = 'Please accept the Terms of Service, Privacy Policy, and Acceptable Use Policy.'
    return
  }
  try {
    const org = await auth.register({
      ...form,
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
        <template v-if="!success">
          <h2 class="text-xl font-semibold text-slate-900">{{ copy.title }}</h2>
          <p class="mt-1 text-sm text-slate-500">{{ copy.subtitle }}</p>

          <div class="mt-4 grid grid-cols-2 gap-2 rounded-lg bg-slate-100 p-1">
            <button
              type="button"
              class="rounded-md px-3 py-2 text-xs font-semibold transition sm:text-sm"
              :class="
                isEvent
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'
              "
              @click="setIntent('event')"
            >
              Event / one-time
            </button>
            <button
              type="button"
              class="rounded-md px-3 py-2 text-xs font-semibold transition sm:text-sm"
              :class="
                !isEvent
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'
              "
              @click="setIntent('business')"
            >
              Business
            </button>
          </div>

          <p
            class="mt-3 rounded-lg border border-brand-100 bg-brand-50 px-3 py-2 text-xs leading-relaxed text-brand-800"
          >
            {{ copy.note }}
          </p>

          <form class="mt-5 space-y-4" @submit.prevent="onSubmit">
            <FormField :label="copy.nameLabel" required>
              <AppInput v-model="form.name" :placeholder="copy.namePlaceholder" />
            </FormField>
            <FormField :label="copy.personLabel" required>
              <AppInput v-model="form.adminFullName" placeholder="Jane Wanjiku" />
            </FormField>
            <FormField :label="copy.emailLabel" required>
              <AppInput v-model="form.email" type="email" :placeholder="copy.emailPlaceholder" />
            </FormField>
            <FormField label="Phone" required hint="Use 07…, 01…, or 254… — we’ll format it for you">
              <AppInput v-model="form.phone" type="tel" placeholder="0712345678" />
            </FormField>
            <FormField label="Password" required hint="At least 6 characters">
              <AppInput v-model="form.password" type="password" placeholder="••••••••" />
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

            <AppButton type="submit" block :loading="auth.loading" :disabled="!termsAccepted">
              {{ copy.submit }}
            </AppButton>
          </form>

          <p class="mt-6 text-center text-sm text-slate-500">
            Already registered?
            <RouterLink to="/login" class="font-medium text-brand-700 hover:text-brand-800">
              Sign in
            </RouterLink>
          </p>
        </template>

        <template v-else>
          <h2 class="text-xl font-semibold text-slate-900">
            {{ isEvent ? 'Event account created' : 'Organization created' }}
          </h2>
          <p class="mt-1 text-sm text-slate-500">
            <template v-if="isEvent">
              Your account is active for
              {{ auth.registeredOrg?.activeDays || 7 }} days
              <template v-if="auth.registeredOrg?.expiresAt">
                (until {{ new Date(auth.registeredOrg.expiresAt).toLocaleString() }})
              </template>
              .
            </template>
            <template v-else>
              Save your API key securely — it won’t be shown again in production.
            </template>
          </p>

          <div
            class="mt-4 rounded-lg border border-emerald-100 bg-emerald-50 px-3 py-3 text-sm text-emerald-900"
          >
            <p class="font-semibold">Prepaid wallet ready</p>
            <p class="mt-1 text-emerald-800/90">
              Balance
              {{
                Number(auth.registeredOrg?.walletBalance ?? 0).toLocaleString('en-KE', {
                  style: 'currency',
                  currency: auth.registeredOrg?.walletCurrency || 'KES',
                })
              }}.
              Sign in, open <strong>Wallet</strong> to top up with M-Pesa, then send SMS.
            </p>
          </div>

          <div
            v-if="!isEvent"
            class="mt-4 rounded-lg bg-slate-900 px-3 py-3 font-mono text-xs text-brand-200 break-all"
          >
            {{ apiKey }}
          </div>
          <div
            v-else
            class="mt-4 rounded-lg border border-brand-100 bg-brand-50 px-3 py-3 text-sm text-brand-900"
          >
            Next: sign in → Wallet → top up → Bulk SMS.
          </div>
          <AppButton class="mt-6" block @click="goLogin">Continue to sign in</AppButton>
        </template>
      </div>
    </div>
  </div>
</template>
