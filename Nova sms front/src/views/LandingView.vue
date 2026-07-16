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
    <section class="relative min-h-[100svh] overflow-hidden">
      <div
        class="absolute inset-0 bg-[linear-gradient(165deg,#eef7f5_0%,#f7faf9_38%,#e8f1f4_100%)]"
      />
      <div
        class="pointer-events-none absolute inset-0 opacity-[0.4]"
        style="
          background-image: radial-gradient(circle at 1px 1px, rgba(15, 118, 110, 0.12) 1px, transparent 0);
          background-size: 28px 28px;
        "
      />
      <div
        class="landing-orb pointer-events-none absolute -right-20 top-16 h-[28rem] w-[28rem] rounded-full bg-brand-200/50 blur-3xl"
      />
      <div
        class="landing-orb-delayed pointer-events-none absolute -left-24 bottom-10 h-80 w-80 rounded-full bg-sky-200/40 blur-3xl"
      />

      <div
        class="relative mx-auto flex min-h-[100svh] max-w-6xl flex-col justify-center px-4 pb-20 pt-28 sm:px-6 lg:px-8"
      >
        <p
          class="landing-fade-up font-serif text-5xl font-bold tracking-tight text-slate-900 sm:text-6xl lg:text-7xl"
        >
          Nova SMS
        </p>
        <h1
          class="landing-fade-up landing-delay-1 mt-5 max-w-2xl text-2xl font-semibold tracking-tight text-slate-800 sm:text-3xl"
        >
          Send once for an event — or every day for your business.
        </h1>
        <p
          class="landing-fade-up landing-delay-2 mt-4 max-w-xl text-base leading-relaxed text-slate-600 sm:text-lg"
        >
          Families and organizers use Nova SMS for event and ceremony notices. Organizations use it for ongoing
          campaigns. Create an account, top up with M-Pesa, send your messages — event accounts stay
          active for one week.
        </p>
        <div class="landing-fade-up landing-delay-3 mt-8 flex flex-wrap items-center gap-3">
          <RouterLink
            to="/register?intent=event"
            class="rounded-lg bg-brand-600 px-5 py-3 text-sm font-semibold text-white shadow-sm shadow-brand-600/20 transition hover:bg-brand-700"
          >
            Send for an event
          </RouterLink>
          <RouterLink
            to="/register?intent=business"
            class="rounded-lg border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-800 transition hover:border-slate-400 hover:bg-slate-50"
          >
            Register business
          </RouterLink>
          <a
            href="#events"
            class="text-sm font-medium text-brand-700 underline-offset-4 transition hover:underline"
          >
            How short-term use works
          </a>
        </div>
      </div>
    </section>

    <section class="border-y border-slate-200/80 bg-white">
      <div class="mx-auto max-w-3xl px-4 py-16 text-center sm:px-6 lg:px-8">
        <h2 class="font-serif text-2xl font-bold text-slate-900 sm:text-3xl">
          Built for businesses — and for people who only need SMS once
        </h2>
        <p class="mt-4 text-base leading-relaxed text-slate-600 sm:text-lg">
          Not every sender needs a permanent operations stack. If you are organizing a wedding,
          community meeting, or short campaign, you can register, fund a small wallet, notify everyone, and
          leave. Businesses that send every day get the same reliable gateway, wallets, and reports.
        </p>
      </div>
    </section>

    <section id="events" class="scroll-mt-20 bg-[#f4f7f6]">
      <div class="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="max-w-2xl">
          <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">One-time sending</p>
          <h2 class="mt-2 font-serif text-3xl font-bold text-slate-900 sm:text-4xl">
            Perfect for events, ceremonies, and short notices
          </h2>
          <p class="mt-3 text-base text-slate-600">
            Create an account, stay active for about one week, send what you need, and you do not
            have to come back.
          </p>
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
        <div class="max-w-2xl">
          <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Features</p>
          <h2 class="mt-2 font-serif text-3xl font-bold text-slate-900 sm:text-4xl">
            Simple enough for a one-week send — strong enough for daily use
          </h2>
          <p class="mt-3 text-base text-slate-600">
            Whether you send once for an event or run monthly campaigns, the same tools apply.
          </p>
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
            to="/register?intent=event"
            class="rounded-lg bg-white px-5 py-3 text-sm font-semibold text-brand-800 transition hover:bg-teal-50"
          >
            Event account
          </RouterLink>
          <RouterLink
            to="/login"
            class="rounded-lg border border-white/40 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
          >
            Login
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
