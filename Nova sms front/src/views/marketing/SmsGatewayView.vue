<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import RelatedLinks from '@/components/marketing/RelatedLinks.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import { RouterLink } from 'vue-router'
</script>

<template>
  <ArticleLayout
    kicker="Product"
    title="Bulk SMS gateway for Kenya"
    description="Nova SMS is a prepaid messaging platform for Kenyan businesses, SACCOs, schools, NGOs, and event organizers. Send from the dashboard or the REST API, and fund the wallet with M-Pesa."
    path="/sms-gateway"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">What Nova SMS is</h2>
      <p class="mt-2">
        Nova SMS sits between your organization and the SMS network. You register, receive a prepaid
        wallet in Kenyan shillings, and send single or bulk messages. Each send is billed from that
        wallet. You can import contacts, request a branded sender ID, and read delivery reports.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Who it is for</h2>
      <ul class="mt-2 list-disc space-y-1 pl-5">
        <li>Businesses that send OTPs, alerts, and campaigns every day.</li>
        <li>Event organizers who need a one-week account for a wedding, harambee, or ceremony.</li>
        <li>Developers integrating SMS into Mwalimu, Chamaplus, Nova POS, or any backend.</li>
      </ul>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">How sending works</h2>
      <ol class="mt-2 list-decimal space-y-1 pl-5">
        <li>Create an organization account.</li>
        <li>Top up the wallet with
          <RouterLink to="/mpesa-stk-push" class="font-medium text-brand-700 hover:underline">M-Pesa STK Push</RouterLink>
          or
          <RouterLink to="/mpesa-paybill" class="font-medium text-brand-700 hover:underline">Paybill C2B</RouterLink>.
        </li>
        <li>Send from Send SMS, Bulk SMS, or
          <RouterLink to="/sms-api" class="font-medium text-brand-700 hover:underline">the SMS API</RouterLink>.
        </li>
        <li>Track delivery in SMS History and reports.</li>
      </ol>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Wallet, not post-pay invoices</h2>
      <p class="mt-2">
        SMS is prepaid. If the wallet cannot cover the message, the send is rejected. Unused balance
        stays on the organization. Event accounts remain active for about one week; business
        accounts stay available for ongoing use.
      </p>
    </section>

    <RelatedLinks
      :links="[
        { to: '/mpesa-stk-push', label: 'Learn about M-Pesa STK Push' },
        { to: '/mpesa-paybill', label: 'Learn about M-Pesa Paybill C2B' },
        { to: '/sms-api', label: 'Nova SMS API for developers' },
        { to: '/pricing', label: 'How prepaid pricing works' },
      ]"
    />
    <CtaBanner />
  </ArticleLayout>
</template>
