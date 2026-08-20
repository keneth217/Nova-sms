<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import { CONTACT_EMAIL } from '@/seo/public-paths'
</script>

<template>
  <ArticleLayout
    kicker="Support"
    title="Contact Nova SMS"
    description="Questions about accounts, API access, or wallet top-ups can go to support. For sending, create an organization account and use the dashboard."
    path="/contact"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">Email</h2>
      <p class="mt-2">
        <a class="font-medium text-brand-700 hover:underline" :href="`mailto:${CONTACT_EMAIL}`">
          {{ CONTACT_EMAIL }}
        </a>
      </p>
      <p class="mt-2">
        Use this address for privacy questions, commercial rate discussions, and API client access
        for an existing organization.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Self-serve</h2>
      <ul class="mt-2 list-disc space-y-1 pl-5">
        <li>Create an account to send SMS and top up with M-Pesa.</li>
        <li>Read the public API documentation for request examples.</li>
        <li>Sign in if you already have an organization.</li>
      </ul>
    </section>

    <CtaBanner title="Get started" description="Register, fund the wallet, and send your first SMS." />
  </ArticleLayout>
</template>
