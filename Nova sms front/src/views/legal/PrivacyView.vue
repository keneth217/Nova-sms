<script setup lang="ts">
const lastUpdated = 'July 2026'
</script>

<template>
  <div class="bg-white px-4 pb-20 pt-28 sm:px-6 lg:px-8">
    <article class="mx-auto max-w-3xl">
      <header class="border-b border-slate-200 pb-8">
        <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Legal</p>
        <h1 class="mt-2 font-serif text-4xl font-bold tracking-tight text-slate-900">
          Privacy Policy
        </h1>
        <p class="mt-3 text-sm text-slate-500">Last Updated: {{ lastUpdated }}</p>
      </header>

      <div class="mt-10 space-y-9 text-sm leading-7 text-slate-600 sm:text-base">
        <p>
          NovaSMS ("we", "our", or "us") provides SMS messaging services to organizations,
          businesses, SACCOs, schools, NGOs, churches, and community groups.
        </p>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Information We Collect</h2>
          <p class="mt-2">We may collect:</p>
          <ul class="mt-2 list-disc space-y-1 pl-6">
            <li>Organization details including name, email, phone number, and billing information.</li>
            <li>User account information.</li>
            <li>SMS sender IDs.</li>
            <li>Recipient phone numbers provided by customers.</li>
            <li>SMS delivery status and logs.</li>
            <li>Payment and wallet transaction records.</li>
            <li>System usage and audit logs.</li>
          </ul>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">How We Use Information</h2>
          <p class="mt-2">We use information to:</p>
          <ul class="mt-2 list-disc space-y-1 pl-6">
            <li>Provide SMS delivery services.</li>
            <li>Process wallet top-ups and payments.</li>
            <li>Monitor service performance and security.</li>
            <li>Generate reports and analytics.</li>
            <li>Comply with legal and regulatory obligations.</li>
          </ul>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Data Security</h2>
          <p class="mt-2">
            We implement reasonable technical and organizational measures to protect customer data
            from unauthorized access, disclosure, alteration, or destruction.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Data Sharing</h2>
          <p class="mt-2">We may share information with:</p>
          <ul class="mt-2 list-disc space-y-1 pl-6">
            <li>SMS delivery partners and telecommunications providers.</li>
            <li>Payment service providers.</li>
            <li>Regulatory authorities where legally required.</li>
          </ul>
          <p class="mt-2">We do not sell customer data to third parties.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Data Retention</h2>
          <p class="mt-2">
            We retain account information, transaction records, and message logs for as long as
            necessary to provide services, comply with legal requirements, and resolve disputes.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Customer Responsibilities</h2>
          <p class="mt-2">
            Customers are responsible for ensuring they have appropriate consent and legal authority
            to send messages to recipients.
          </p>
        </section>

        <section class="rounded-xl border border-brand-100 bg-brand-50 p-5">
          <h2 class="text-xl font-semibold text-slate-900">Contact</h2>
          <p class="mt-2">
            Questions regarding this policy may be directed to
            <a class="font-medium text-brand-700 hover:underline" href="mailto:support@novastack.com">
              support@novastack.com
            </a>.
          </p>
        </section>
      </div>
    </article>
  </div>
</template>
