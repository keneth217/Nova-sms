<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import RelatedLinks from '@/components/marketing/RelatedLinks.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import { RouterLink } from 'vue-router'
</script>

<template>
  <ArticleLayout
    kicker="M-Pesa"
    title="M-Pesa STK Push for SMS wallet top-up"
    description="Nova SMS uses Safaricom STK Push so a customer can authorize a Lipa na M-Pesa prompt on their phone and credit an organization SMS wallet in Kenyan shillings."
    path="/mpesa-stk-push"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">What STK Push is</h2>
      <p class="mt-2">
        STK Push (Lipa na M-Pesa online) is a Safaricom Daraja API that sends a payment prompt to an
        M-Pesa registered phone. The customer enters their PIN on the handset. Nova SMS uses this
        flow to fund SMS wallets — from the dashboard and from partner backends that hold a wallet
        top-up API key.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">How Nova SMS simplifies integration</h2>
      <p class="mt-2">
        Your application does not call Daraja directly for this product. You call Nova SMS
        <code class="rounded bg-slate-100 px-1 py-0.5 font-mono text-sm">POST /api/v1/wallet/topup</code>
        with an amount in KES and the phone that should receive the prompt. Nova SMS talks to
        Safaricom, stores the pending transaction, and later credits the organization wallet once.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Payment request lifecycle</h2>
      <ol class="mt-2 list-decimal space-y-2 pl-5">
        <li>Nova SMS sends the STK request and returns a <code class="font-mono text-sm">transactionId</code> with status PENDING.</li>
        <li>The customer enters their M-Pesa PIN, cancels, or lets the prompt expire.</li>
        <li>Safaricom may send a callback to Nova SMS. You should still poll Nova SMS rather than trusting the PIN screen.</li>
        <li>Call <code class="font-mono text-sm">POST /api/v1/wallet/topup/{id}/check</code> every few seconds while status is PENDING.</li>
        <li>Stop when status is COMPLETED and <code class="font-mono text-sm">walletCredited</code> is true, or when status is FAILED.</li>
      </ol>
      <p class="mt-3">
        A successful Safaricom query can credit the wallet before the callback arrives, so
        <code class="font-mono text-sm">callbackReceived</code> can still be false when
        <code class="font-mono text-sm">walletCredited</code> is true. Treat walletCredited as the success flag.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Callback handling and payment status</h2>
      <p class="mt-2">
        Daraja callbacks are processed by Nova SMS. Credits are applied once per successful payment.
        Status never moves backwards: COMPLETED is not overwritten with PENDING or FAILED. Messages
        such as “the transaction is still under processing” stay PENDING — keep polling.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Common integration issues</h2>
      <ul class="mt-2 list-disc space-y-1 pl-5">
        <li>Marking the payment successful from the phone PIN screen instead of Nova SMS status.</li>
        <li>Starting a second STK while the first transaction is still PENDING.</li>
        <li>Using GET <code class="font-mono text-sm">/topup/{id}</code> as a Safaricom query — that endpoint only reads the stored row. POST <code class="font-mono text-sm">/check</code> queries Safaricom when needed.</li>
        <li>Putting the Nova SMS API key in frontend JavaScript.</li>
      </ul>
      <p class="mt-3">
        Request and response examples live in
        <RouterLink to="/developers/wallet" class="font-medium text-brand-700 hover:underline">
          the wallet API documentation
        </RouterLink>.
      </p>
    </section>

    <RelatedLinks
      :links="[
        { to: '/mpesa-paybill', label: 'M-Pesa Paybill C2B top-up' },
        { to: '/webhooks', label: 'How M-Pesa callbacks are processed' },
        { to: '/developers/wallet', label: 'Wallet API examples' },
        { to: '/sms-api', label: 'SMS API overview' },
      ]"
    />
    <CtaBanner title="Top up and send" description="Create an account, trigger STK Push from the wallet page, and start sending SMS." />
  </ArticleLayout>
</template>
