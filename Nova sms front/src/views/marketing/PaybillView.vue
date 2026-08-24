<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import RelatedLinks from '@/components/marketing/RelatedLinks.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import { PLATFORM_PAYBILL } from '@/seo/public-paths'
</script>

<template>
  <ArticleLayout
    kicker="M-Pesa"
    title="M-Pesa Paybill C2B for wallet funding"
    description="Paybill Customer-to-Business payments credit a Nova SMS organization when the account number matches that organization’s M-Pesa reference. Nova SMS records the Safaricom confirmation and credits once per receipt."
    path="/mpesa-paybill"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">C2B payment flow</h2>
      <p class="mt-2">
        C2B is the Paybill flow: the customer opens M-Pesa, chooses Lipa na M-Pesa, Paybill, enters
        the business number, an account number, and an amount. Safaricom sends a confirmation to the
        URLs registered for that Paybill. Nova SMS uses those confirmations to fund SMS wallets.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Paybill and account references</h2>
      <p class="mt-2">
        The platform Paybill is <strong>{{ PLATFORM_PAYBILL }}</strong>. Each organization has an
        M-Pesa account reference (for example NOVAC727) shown in Settings. That value is the
        BillRefNumber. It identifies the wallet. Nova SMS does not trust an organization ID submitted
        from a browser or mobile app to decide who to credit.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">For API clients</h2>
      <p class="mt-2">
        Call <code class="font-mono text-sm">GET /api/v1/mpesa/c2b</code> for Paybill instructions and
        <code class="font-mono text-sm">GET /api/v1/mpesa/c2b/transactions</code> to list credits.
        Optional: <code class="font-mono text-sm">POST /api/v1/mpesa/c2b/verify</code> with a receipt.
        You never implement Safaricom callbacks.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Callback processing</h2>
      <p class="mt-2">
        Confirmation payloads are stored, then the wallet is credited. If wallet credit fails after
        Safaricom has already been acknowledged, the inbound record remains so the receipt can be
        recovered later. Duplicate M-Pesa receipts are not credited twice.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Transaction reconciliation</h2>
      <p class="mt-2">
        The M-Pesa receipt (TransID) is the reconciliation key. Organizations can verify a delayed
        top-up from the wallet page using that receipt, or from
        <code class="font-mono text-sm">POST /api/v1/mpesa/c2b/verify</code>. Admins can look up the
        same receipt. The stored BillRefNumber always wins over any account typed in a recovery form.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Payment confirmation</h2>
      <p class="mt-2">
        After a successful C2B credit, the organization wallet balance increases and a top-up
        transaction appears in history. Platform collection accounts (internal names used on the
        same Paybill) never credit an organization SMS wallet.
      </p>
    </section>

    <RelatedLinks
      :links="[
        { to: '/mpesa-stk-push', label: 'Learn about M-Pesa STK Push' },
        { to: '/webhooks', label: 'M-Pesa callbacks and status sync' },
        { to: '/sms-gateway', label: 'Bulk SMS gateway overview' },
        { to: '/developers', label: 'Nova SMS API documentation' },
      ]"
    />
    <CtaBanner />
  </ArticleLayout>
</template>
