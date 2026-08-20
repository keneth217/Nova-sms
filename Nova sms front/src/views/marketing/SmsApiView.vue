<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import RelatedLinks from '@/components/marketing/RelatedLinks.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import { RouterLink } from 'vue-router'
</script>

<template>
  <ArticleLayout
    kicker="Developers"
    title="Nova SMS API for developers"
    description="A REST API to send SMS, read delivery status, check wallet balance, and start M-Pesa STK top-ups from your backend — without calling the upstream SMS provider or Daraja yourself."
    path="/sms-api"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">Authentication</h2>
      <p class="mt-2">
        API clients belong to an organization. You send
        <code class="rounded bg-slate-100 px-1 py-0.5 font-mono text-sm">X-API-Key: nova_live_…</code>.
        Dashboard users keep using email and password; they do not need a key. Nova SMS stores a
        hash of the key. The full value is shown only at create or rotate. Never ship it in a
        browser bundle.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Creating payment and SMS requests</h2>
      <p class="mt-2">
        Send one message with <code class="font-mono text-sm">POST /api/v1/sms/send</code>. Bulk and
        scheduled sends use <code class="font-mono text-sm">/sms/bulk</code> and
        <code class="font-mono text-sm">/sms/schedule</code>. To fund the same organization wallet
        from your product, grant WALLET_TOPUP and call
        <code class="font-mono text-sm">POST /api/v1/wallet/topup</code>.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Checking payment and message status</h2>
      <p class="mt-2">
        Poll STK with <code class="font-mono text-sm">POST /api/v1/wallet/topup/{id}/check</code>.
        Read SMS with <code class="font-mono text-sm">GET /api/v1/sms/{id}/status</code>. Nova SMS
        is the source of truth for both.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Callbacks and errors</h2>
      <p class="mt-2">
        Safaricom callbacks terminate at Nova SMS. Your error handling should read HTTP status and
        the JSON <code class="font-mono text-sm">message</code> field. Typical cases are an invalid
        key, missing permission, insufficient wallet balance, or a validation error on the recipient.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Documentation</h2>
      <p class="mt-2">
        Full request and response examples, including cURL and language guides, are on
        <RouterLink to="/developers" class="font-medium text-brand-700 hover:underline">
          the public API documentation
        </RouterLink>.
        OpenAPI is published on the API host at <code class="font-mono text-sm">/swagger-ui.html</code>.
      </p>
    </section>

    <RelatedLinks
      :links="[
        { to: '/developers/quick-start', label: 'Quick start: send your first SMS' },
        { to: '/developers/authentication', label: 'API authentication' },
        { to: '/developers/wallet', label: 'M-Pesa STK Push API example' },
        { to: '/webhooks', label: 'Payment callbacks and polling' },
      ]"
    />
    <CtaBanner title="Get API access" description="Register an organization, create an API client, and send from your backend." />
  </ArticleLayout>
</template>
