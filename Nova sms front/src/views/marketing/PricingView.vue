<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import RelatedLinks from '@/components/marketing/RelatedLinks.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import { RouterLink } from 'vue-router'
</script>

<template>
  <ArticleLayout
    kicker="Pricing"
    title="Prepaid SMS wallet pricing"
    description="Nova SMS is prepaid in Kenyan shillings. You top up with M-Pesa, then each SMS is deducted from the organization wallet at that organization’s rate."
    path="/pricing"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">How billing works</h2>
      <p class="mt-2">
        There is no post-pay invoice in the current product. The wallet must cover the message
        (including concatenated segments) before Nova SMS queues it. Unused balance remains on the
        organization until it is used.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">What you pay for</h2>
      <ul class="mt-2 list-disc space-y-1 pl-5">
        <li>Outbound SMS billed per segment from the wallet.</li>
        <li>Top-ups via
          <RouterLink to="/mpesa-stk-push" class="font-medium text-brand-700 hover:underline">STK Push</RouterLink>
          or
          <RouterLink to="/mpesa-paybill" class="font-medium text-brand-700 hover:underline">Paybill C2B</RouterLink>.
        </li>
      </ul>
      <p class="mt-3">
        Platform notification SMS to your organization phone (welcome, top-up receipt, low balance)
        are not billed to the wallet. Your per-SMS rate is set on the organization; contact us if
        you need a commercial discussion rather than assuming a public list price.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Account types</h2>
      <p class="mt-2">
        Event accounts are meant for a short send and stay active for about one week. Business
        accounts are for ongoing operations. Both use the same wallet and M-Pesa top-up paths.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Get started</h2>
      <ol class="mt-2 list-decimal space-y-1 pl-5">
        <li>Create an account.</li>
        <li>Fund the wallet.</li>
        <li>Send a test SMS from the dashboard or the API.</li>
      </ol>
    </section>

    <RelatedLinks
      :links="[
        { to: '/sms-gateway', label: 'Bulk SMS gateway features' },
        { to: '/mpesa-stk-push', label: 'Top up with M-Pesa STK Push' },
        { to: '/contact', label: 'Contact Nova SMS' },
        { to: '/register', label: 'Create a Nova SMS account' },
      ]"
    />
    <CtaBanner />
  </ArticleLayout>
</template>
