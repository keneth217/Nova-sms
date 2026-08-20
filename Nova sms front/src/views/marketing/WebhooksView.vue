<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import RelatedLinks from '@/components/marketing/RelatedLinks.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import { RouterLink } from 'vue-router'
</script>

<template>
  <ArticleLayout
    kicker="Integrations"
    title="M-Pesa callbacks, retries, and payment status"
    description="Safaricom sends STK and C2B callbacks to Nova SMS. Your app should poll Nova SMS for wallet top-up status and SMS delivery, so a missed or delayed callback does not leave you out of sync."
    path="/webhooks"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">Why callbacks matter</h2>
      <p class="mt-2">
        M-Pesa payments are asynchronous. The customer may take time to enter a PIN. The network may
        deliver the Daraja callback late. If your backend “succeeds” at STK send time, you will
        credit SMS accidentally when the customer cancelled.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">How payment callbacks work here</h2>
      <p class="mt-2">
        Nova SMS registers STK callback URLs and Paybill C2B confirmation/validation URLs with
        Daraja. Safaricom posts to Nova SMS, not to your server. Nova SMS updates the wallet
        transaction, credits once, and exposes status through the API.
      </p>
      <p class="mt-2">
        Partner applications do not currently receive a customer-configured payment webhook. Use
        <RouterLink to="/developers/wallet" class="font-medium text-brand-700 hover:underline">
          the wallet status endpoints
        </RouterLink>
        instead.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Retry handling</h2>
      <p class="mt-2">
        If an STK is still PENDING, <code class="font-mono text-sm">POST /api/v1/wallet/topup/{id}/check</code>
        reads the database and, when needed, queries Safaricom. That covers delayed callbacks. Keep
        polling every few seconds. Do not create another STK for the same attempt while PENDING.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Idempotency</h2>
      <p class="mt-2">
        Wallet credit is applied once per successful M-Pesa receipt. A repeated callback or a later
        STK query will not double-fund the SMS wallet. SMS send APIs also support idempotency keys
        so a retried HTTP request does not send the same message twice — see
        <RouterLink to="/developers/idempotency" class="font-medium text-brand-700 hover:underline">
          API idempotency
        </RouterLink>.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Payment status synchronization</h2>
      <ul class="mt-2 list-disc space-y-1 pl-5">
        <li>PENDING — still processing. Keep polling.</li>
        <li>COMPLETED with walletCredited true — credit the UI and refresh balance.</li>
        <li>FAILED — stop polling. Do not add SMS credit in your app.</li>
      </ul>
      <p class="mt-3">
        SMS delivery is a separate path: provider delivery reports update message status, which you
        read with
        <RouterLink to="/developers/status" class="font-medium text-brand-700 hover:underline">
          GET /api/v1/sms/{id}/status
        </RouterLink>.
      </p>
    </section>

    <RelatedLinks
      :links="[
        { to: '/mpesa-stk-push', label: 'M-Pesa STK Push lifecycle' },
        { to: '/mpesa-paybill', label: 'Paybill C2B account references' },
        { to: '/developers/wallet', label: 'Wallet API documentation' },
        { to: '/developers/status', label: 'SMS status API' },
      ]"
    />
    <CtaBanner />
  </ArticleLayout>
</template>
