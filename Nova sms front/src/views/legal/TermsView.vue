<script setup lang="ts">
const lastUpdated = 'July 2026'
</script>

<template>
  <div class="bg-white px-4 pb-20 pt-28 sm:px-6 lg:px-8">
    <article class="mx-auto max-w-3xl">
      <header class="border-b border-slate-200 pb-8">
        <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Legal</p>
        <h1 class="mt-2 font-serif text-4xl font-bold tracking-tight text-slate-900">
          Terms of Service
        </h1>
        <p class="mt-3 text-sm text-slate-500">Last Updated: {{ lastUpdated }}</p>
      </header>

      <div class="mt-10 space-y-9 text-sm leading-7 text-slate-600 sm:text-base">
        <p>By creating an account or using NovaSMS, you agree to these Terms of Service.</p>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Services</h2>
          <p class="mt-2">
            NovaSMS provides a platform that allows registered organizations to purchase SMS
            credits, manage sender IDs, and send SMS messages through supported telecommunications
            providers.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Account Registration</h2>
          <p class="mt-2">
            Users must provide accurate and complete information when registering.
          </p>
          <p class="mt-2">
            You are responsible for maintaining the security of your account credentials and API
            keys.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Wallet and Payments</h2>
          <ul class="mt-2 list-disc space-y-1 pl-6">
            <li>SMS services are prepaid.</li>
            <li>Wallet balances are non-transferable unless otherwise approved.</li>
            <li>SMS charges are deducted based on usage.</li>
            <li>Top-up transactions are subject to payment provider processing.</li>
          </ul>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Sender IDs</h2>
          <p class="mt-2">Customers may use approved sender IDs.</p>
          <p class="mt-2">
            NovaSMS reserves the right to reject, suspend, or remove sender IDs that violate
            applicable laws, regulations, or provider requirements.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Service Availability</h2>
          <p class="mt-2">
            We strive to provide reliable services but do not guarantee uninterrupted availability.
          </p>
          <p class="mt-2">
            SMS delivery depends on telecommunications providers and external networks.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Limitation of Liability</h2>
          <p class="mt-2">
            NovaSMS shall not be liable for indirect, incidental, special, or consequential damages
            arising from use of the service.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Suspension and Termination</h2>
          <p class="mt-2">We may suspend or terminate accounts that:</p>
          <ul class="mt-2 list-disc space-y-1 pl-6">
            <li>Violate these Terms.</li>
            <li>Engage in spam or abusive messaging.</li>
            <li>Attempt fraud or illegal activity.</li>
            <li>Threaten platform security.</li>
          </ul>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Changes</h2>
          <p class="mt-2">
            We may update these Terms from time to time. Continued use of the platform constitutes
            acceptance of updated terms.
          </p>
        </section>
      </div>
    </article>
  </div>
</template>
