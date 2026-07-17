<script setup lang="ts">
const lastUpdated = 'July 2026'
</script>

<template>
  <div class="bg-white px-4 pb-20 pt-28 sm:px-6 lg:px-8">
    <article class="mx-auto max-w-3xl">
      <header class="border-b border-slate-200 pb-8">
        <p class="text-sm font-semibold uppercase tracking-wider text-brand-700">Legal</p>
        <h1 class="mt-2 font-serif text-4xl font-bold tracking-tight text-slate-900">
          Acceptable Use Policy
        </h1>
        <p class="mt-3 text-sm text-slate-500">Last Updated: {{ lastUpdated }}</p>
      </header>

      <div class="mt-10 space-y-9 text-sm leading-7 text-slate-600 sm:text-base">
        <section>
          <h2 class="text-xl font-semibold text-slate-900">Prohibited Use</h2>
          <p class="mt-2">NovaSMS prohibits the use of its platform for:</p>
          <ul class="mt-2 list-disc space-y-1 pl-6">
            <li>Spam or unsolicited bulk messaging.</li>
            <li>Fraudulent, deceptive, or misleading communications.</li>
            <li>Phishing attempts.</li>
            <li>Distribution of malware or harmful content.</li>
            <li>Harassment, threats, or abuse.</li>
            <li>Illegal gambling activities.</li>
            <li>Promotion of illegal products or services.</li>
            <li>Content that violates applicable laws or regulations.</li>
          </ul>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-slate-900">Customer Obligations</h2>
          <p class="mt-2">Customers must:</p>
          <ul class="mt-2 list-disc space-y-1 pl-6">
            <li>Obtain consent where required.</li>
            <li>Maintain accurate recipient records.</li>
            <li>Honor opt-out requests where applicable.</li>
            <li>Use sender IDs only for authorized purposes.</li>
          </ul>
        </section>

        <section class="rounded-xl border border-brand-100 bg-brand-50 p-5">
          <h2 class="text-xl font-semibold text-slate-900">Enforcement</h2>
          <p class="mt-2">
            NovaSMS reserves the right to suspend or terminate any account found in violation of
            this policy without prior notice.
          </p>
        </section>
      </div>
    </article>
  </div>
</template>
