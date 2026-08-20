<script setup lang="ts">
import { nextTick, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  PaperAirplaneIcon,
  WalletIcon,
  UserGroupIcon,
  ChartBarIcon,
  IdentificationIcon,
  CalendarDaysIcon,
  BoltIcon,
  HeartIcon,
} from '@heroicons/vue/24/outline'
import JsonLd from '@/components/seo/JsonLd.vue'
import { publicFaqs } from '@/data/public-faq'
import { faqSchema, softwareApplicationSchema } from '@/seo/schema'

const route = useRoute()

const features = [
  {
    icon: PaperAirplaneIcon,
    title: 'Bulk & single SMS',
    description:
      'Send event notices, wedding updates, OTPs, or business alerts with cost previews before you hit send.',
  },
  {
    icon: CalendarDaysIcon,
    title: 'Event & short-term use',
    description:
      'Create an account for a wedding, harambee, or community event, send for about a week, then walk away — no long contract.',
  },
  {
    icon: WalletIcon,
    title: 'Pay only for what you send',
    description:
      'Top up with M-Pesa, send your messages, and stop. Unused balance stays in your wallet if you return later.',
  },
  {
    icon: UserGroupIcon,
    title: 'Contacts & groups',
    description:
      'Paste numbers or import a CSV of family, friends, or customers — then send to the whole list at once.',
  },
  {
    icon: IdentificationIcon,
    title: 'Sender ID management',
    description:
      'Use a shared platform sender ID immediately, or request a branded name for ongoing business use.',
  },
  {
    icon: ChartBarIcon,
    title: 'Delivery reports',
    description:
      'See who received the message and who failed — useful for both one-off events and daily operations.',
  },
]

const eventUses = [
  {
    icon: HeartIcon,
    title: 'Family & social notices',
    description:
      'Inform relatives and friends about arrangements, venue, and timing in one send.',
  },
  {
    icon: CalendarDaysIcon,
    title: 'Weddings & ceremonies',
    description:
      'Share date changes, venue details, or thank-you messages without a permanent business account.',
  },
  {
    icon: BoltIcon,
    title: 'Quick community alerts',
    description:
      'Church meetings, harambees, school notices — register, top up, send, and you are done.',
  },
]

onMounted(async () => {
  await nextTick()
  if (route.hash === '#features') {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })
  }
  if (route.hash === '#events') {
    document.getElementById('events')?.scrollIntoView({ behavior: 'smooth' })
  }
})
</script>

<template>
  <div>
    <JsonLd id="home-software" :data="softwareApplicationSchema()" />
    <JsonLd id="home-faq" :data="faqSchema(publicFaqs)" />
    <section class="relative min-h-[78svh] overflow-hidden bg-[#fff7ed]">
      <picture>
        <source srcset="/girls.webp" type="image/webp" />
        <img
          src="/girls.png"
          alt="People reading an SMS together on a phone, illustrating Nova SMS messaging in Kenya"
          width="1672"
          height="941"
          fetchpriority="high"
          decoding="async"
          class="absolute inset-0 h-full w-full object-cover object-center"
        />
      </picture>
      <div
        class="absolute inset-0 bg-[linear-gradient(90deg,rgba(255,250,242,0.98)_0%,rgba(255,248,238,0.94)_36%,rgba(255,247,237,0.38)_66%,rgba(255,247,237,0.04)_100%)]"
      />
      <div
        class="pointer-events-none absolute inset-0 opacity-[0.4]"
        style="
          background-image: radial-gradient(circle at 1px 1px, rgba(15, 118, 110, 0.12) 1px, transparent 0);
          background-size: 28px 28px;
        "
      />
      <div
        class="landing-orb-delayed pointer-events-none absolute -left-24 bottom-10 h-80 w-80 rounded-full bg-orange-200/40 blur-3xl"
      />

      <div
        class="relative mx-auto flex min-h-[78svh] max-w-6xl flex-col justify-center px-4 pb-14 pt-24 sm:px-6 lg:px-8"
      >
        <h1
          class="landing-fade-up font-serif text-5xl font-bold tracking-tight text-slate-900 sm:text-6xl lg:text-7xl"
        >
          Nova SMS
        </h1>
        <p
          class="landing-fade-up landing-delay-1 mt-5 max-w-2xl text-2xl font-semibold tracking-tight text-slate-800 sm:text-3xl"
        >
          Bulk SMS gateway for Kenya — send once for an event, or every day for your business.
        </p>
        <p
          class="landing-fade-up landing-delay-2 mt-4 max-w-xl text-base leading-relaxed text-slate-600 sm:text-lg"
        >
          Organizations, SACCOs, schools, and families use Nova SMS for campaigns and ceremony notices.
          Create an account, top up with M-Pesa STK Push or Paybill, then send from the dashboard or the
          REST API. Event accounts stay active for one week.
        </p>
        <div class="landing-fade-up landing-delay-3 mt-8 flex flex-wrap items-center gap-3">
          <RouterLink
            to="/register"
            class="rounded-lg bg-brand-600 px-5 py-3 text-sm font-semibold text-white shadow-sm shadow-brand-600/20 transition hover:bg-brand-700"
          >
            Get started
          </RouterLink>
          <RouterLink
            to="/developers"
            class="rounded-lg border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-800 transition hover:border-slate-400 hover:bg-slate-50"
          >
            View documentation
          </RouterLink>
          <RouterLink
            to="/register?intent=business"
            class="text-sm font-medium text-brand-700 underline-offset-4 transition hover:underline"
          >
            Create account
          </RouterLink>
        </div>
      </div>
    </section>

    <section class="border-y border-slate-200/80 bg-white">
      <div
        class="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:px-8"
      >
        <div>
          <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">
            Messaging made human
          </p>
          <h2 class="mt-2 font-serif text-2xl font-bold text-slate-900 sm:text-3xl">
            Built for businesses — and for people who only need SMS once
          </h2>
          <p class="mt-4 text-base leading-relaxed text-slate-600 sm:text-lg">
            Not every sender needs a permanent operations stack. If you are organizing a wedding,
            community meeting, or short campaign, you can register, fund a small wallet, notify
            everyone, and leave. Businesses that send every day get the same reliable gateway,
            wallets, and reports.
          </p>
        </div>
        <div class="relative">
          <div class="absolute -inset-3 rounded-[2rem] bg-brand-100/60 blur-2xl" />
          <picture>
            <source srcset="/girls2.webp" type="image/webp" />
            <img
              src="/girls2.png"
              alt="People sharing an SMS update on a phone after sending with Nova SMS"
              width="1536"
              height="1024"
              loading="lazy"
              decoding="async"
              class="relative aspect-[4/3] w-full rounded-3xl object-cover shadow-xl shadow-slate-900/10"
            />
          </picture>
        </div>
      </div>
    </section>

    <section id="events" class="scroll-mt-20 bg-[#f4f7f6]">
      <div class="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="grid items-center gap-10 lg:grid-cols-[0.8fr_1fr]">
          <div class="max-w-2xl lg:order-2">
            <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">
              One-time sending
            </p>
            <h2 class="mt-2 font-serif text-3xl font-bold text-slate-900 sm:text-4xl">
              Perfect for events, ceremonies, and short notices
            </h2>
            <p class="mt-3 text-base text-slate-600">
              Create an account, stay active for about one week, send what you need, and you do not
              have to come back.
            </p>
          </div>
          <picture>
            <source srcset="/girls3.webp" type="image/webp" />
            <img
              src="/girls3.png"
              alt="Event organizer celebrating after sending SMS updates with Nova SMS"
              width="1536"
              height="1024"
              loading="lazy"
              decoding="async"
              class="aspect-[16/10] w-full rounded-3xl object-cover shadow-lg shadow-slate-900/10 lg:order-1"
            />
          </picture>
        </div>

        <div class="mt-12 grid gap-10 sm:grid-cols-3">
          <article v-for="item in eventUses" :key="item.title">
            <div
              class="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-100 text-brand-700"
            >
              <component :is="item.icon" class="h-5 w-5" />
            </div>
            <h3 class="mt-4 text-lg font-semibold text-slate-900">{{ item.title }}</h3>
            <p class="mt-2 text-sm leading-relaxed text-slate-600">{{ item.description }}</p>
          </article>
        </div>

        <ol class="mt-14 grid gap-4 sm:grid-cols-4">
          <li
            v-for="(step, i) in [
              'Create a free account',
              'Top up with M-Pesa',
              'Paste contacts & send',
              'Active for 1 week',
            ]"
            :key="step"
            class="border-l-2 border-brand-500 pl-4"
          >
            <p class="text-xs font-semibold uppercase tracking-wider text-brand-700">
              Step {{ i + 1 }}
            </p>
            <p class="mt-1 text-sm font-medium text-slate-900">{{ step }}</p>
          </li>
        </ol>

        <div class="mt-10">
          <RouterLink
            to="/register?intent=event"
            class="inline-flex rounded-lg bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-700"
          >
            Start event account
          </RouterLink>
        </div>
      </div>
    </section>

    <section id="features" class="scroll-mt-20 border-t border-slate-200/80 bg-white">
      <div class="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="grid items-end gap-10 lg:grid-cols-[1fr_0.72fr]">
          <div class="max-w-2xl">
            <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Features</p>
            <h2 class="mt-2 font-serif text-3xl font-bold text-slate-900 sm:text-4xl">
              Simple enough for a one-week send — strong enough for daily use
            </h2>
            <p class="mt-3 text-base text-slate-600">
              Whether you send once for an event or run monthly campaigns, the same tools apply.
            </p>
          </div>
          <picture>
            <source srcset="/girls4.webp" type="image/webp" />
            <img
              src="/girls4.png"
              alt="A sender checking a successful Nova SMS delivery on her phone"
              width="1536"
              height="1024"
              loading="lazy"
              decoding="async"
              class="aspect-[16/9] w-full rounded-3xl object-cover object-center shadow-lg shadow-slate-900/10"
            />
          </picture>
        </div>

        <div class="mt-12 grid gap-x-10 gap-y-12 sm:grid-cols-2 lg:grid-cols-3">
          <article v-for="feature in features" :key="feature.title" class="group">
            <div
              class="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-700 transition group-hover:bg-brand-100"
            >
              <component :is="feature.icon" class="h-5 w-5" />
            </div>
            <h3 class="mt-4 text-lg font-semibold text-slate-900">{{ feature.title }}</h3>
            <p class="mt-2 text-sm leading-relaxed text-slate-600">{{ feature.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="border-t border-slate-200 bg-[#f4f7f6]">
      <div class="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
        <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Platform</p>
        <h2 class="mt-2 font-serif text-3xl font-bold text-slate-900 sm:text-4xl">
          M-Pesa top-up, APIs, and prepaid sending
        </h2>
        <p class="mt-3 max-w-2xl text-slate-600">
          Nova SMS is a messaging platform. M-Pesa STK Push and Paybill C2B fund the SMS wallet.
          Developers send SMS and start top-ups through the REST API.
        </p>
        <div class="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <article class="rounded-2xl border border-slate-200 bg-white p-6">
            <h3 class="text-lg font-semibold text-slate-900">M-Pesa STK Push</h3>
            <p class="mt-2 text-sm text-slate-600">
              Send a Lipa na M-Pesa prompt to a phone and credit the organization wallet once the PIN
              succeeds.
            </p>
            <RouterLink to="/mpesa-stk-push" class="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
              Learn about M-Pesa STK Push
            </RouterLink>
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-6">
            <h3 class="text-lg font-semibold text-slate-900">Paybill C2B</h3>
            <p class="mt-2 text-sm text-slate-600">
              Pay the platform Paybill using your organization account reference. Nova SMS credits
              that wallet from the Safaricom confirmation.
            </p>
            <RouterLink to="/mpesa-paybill" class="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
              Learn about M-Pesa Paybill C2B
            </RouterLink>
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-6">
            <h3 class="text-lg font-semibold text-slate-900">Developer API</h3>
            <p class="mt-2 text-sm text-slate-600">
              Authenticate with an API key from your backend. Send SMS, read delivery status, and
              start STK top-ups.
            </p>
            <RouterLink to="/sms-api" class="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
              Nova SMS API for developers
            </RouterLink>
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-6">
            <h3 class="text-lg font-semibold text-slate-900">Callbacks and status</h3>
            <p class="mt-2 text-sm text-slate-600">
              Safaricom callbacks land on Nova SMS. Poll wallet status so delayed PIN entry does not
              desync your app.
            </p>
            <RouterLink to="/webhooks" class="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
              How payment callbacks work
            </RouterLink>
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-6">
            <h3 class="text-lg font-semibold text-slate-900">Security</h3>
            <p class="mt-2 text-sm text-slate-600">
              Dashboard JWT sessions, hashed API keys, permission scopes, and once-only wallet
              credits per M-Pesa receipt.
            </p>
            <RouterLink to="/about" class="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
              About Nova SMS
            </RouterLink>
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-6">
            <h3 class="text-lg font-semibold text-slate-900">Prepaid pricing</h3>
            <p class="mt-2 text-sm text-slate-600">
              Top up in KES, then each SMS is deducted from the wallet at the organization rate. No
              post-pay invoice in the current product.
            </p>
            <RouterLink to="/pricing" class="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
              How prepaid pricing works
            </RouterLink>
          </article>
        </div>
      </div>
    </section>

    <section id="faq" class="scroll-mt-20 border-t border-slate-200 bg-white">
      <div class="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
        <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">FAQ</p>
        <h2 class="mt-2 font-serif text-3xl font-bold text-slate-900">Common questions</h2>
        <dl class="mt-10 space-y-8">
          <div v-for="item in publicFaqs" :key="item.question">
            <dt class="text-lg font-semibold text-slate-900">{{ item.question }}</dt>
            <dd class="mt-2 text-sm leading-relaxed text-slate-600">{{ item.answer }}</dd>
          </div>
        </dl>
        <p class="mt-8 text-sm">
          <RouterLink to="/faq" class="font-medium text-brand-700 hover:underline">
            Open the full FAQ page
          </RouterLink>
        </p>
      </div>
    </section>

    <section class="relative overflow-hidden border-t border-slate-200">
      <div
        class="absolute inset-0 bg-[linear-gradient(120deg,#0f766e_0%,#0d9488_45%,#0e7490_100%)]"
      />
      <div
        class="relative mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:flex lg:items-center lg:justify-between lg:px-8"
      >
        <div>
          <h2 class="font-serif text-2xl font-bold text-white sm:text-3xl">
            Need to notify people this week?
          </h2>
          <p class="mt-2 max-w-xl text-teal-50">
            Register in minutes, fund with M-Pesa, and send event or business messages
            today. Event accounts remain active for one week.
          </p>
        </div>
        <div class="mt-6 flex flex-wrap gap-3 lg:mt-0">
          <RouterLink
            to="/register"
            class="rounded-lg bg-white px-5 py-3 text-sm font-semibold text-brand-800 transition hover:bg-teal-50"
          >
            Get started
          </RouterLink>
          <RouterLink
            to="/developers"
            class="rounded-lg border border-white/40 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
          >
            View documentation
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.landing-fade-up {
  animation: landing-fade-up 0.8s ease-out both;
}

.landing-delay-1 {
  animation-delay: 0.12s;
}
.landing-delay-2 {
  animation-delay: 0.24s;
}
.landing-delay-3 {
  animation-delay: 0.36s;
}

.landing-orb {
  animation: landing-float 8s ease-in-out infinite;
}

.landing-orb-delayed {
  animation: landing-float 10s ease-in-out infinite reverse;
}

@keyframes landing-fade-up {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes landing-float {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(-12px, 18px);
  }
}
</style>
