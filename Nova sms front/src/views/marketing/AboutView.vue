<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import RelatedLinks from '@/components/marketing/RelatedLinks.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
</script>

<template>
  <ArticleLayout
    kicker="Company"
    title="About Nova SMS"
    description="Nova SMS is Novastack’s messaging platform for Kenya: bulk SMS, prepaid wallets, and M-Pesa top-up for businesses and short-term events."
    path="/about"
  >
    <section>
      <h2 class="text-xl font-semibold text-slate-900">What we operate</h2>
      <p class="mt-2">
        Nova SMS is a web application and REST API. Organizations manage contacts, sender IDs, and
        message history. Developers send SMS from other Novastack products and third-party backends
        without talking to the upstream SMS provider directly.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Payments</h2>
      <p class="mt-2">
        Wallet funding uses Safaricom M-Pesa (STK Push and Paybill C2B) so customers can pay in KES.
        Nova SMS processes those callbacks and credits wallets once per receipt. That is wallet
        funding for messaging — not a general-purpose merchant checkout for selling arbitrary goods.
      </p>
    </section>

    <section>
      <h2 class="text-xl font-semibold text-slate-900">Security practices we actually use</h2>
      <ul class="mt-2 list-disc space-y-1 pl-5">
        <li>Dashboard sessions use JWT authentication.</li>
        <li>API keys are hashed at rest and scoped by permission.</li>
        <li>Live keys must stay on a server, not in a public frontend.</li>
        <li>Wallet credits are idempotent per M-Pesa receipt.</li>
      </ul>
      <p class="mt-3">
        We do not publish uptime percentages, certification badges, or partnership claims that are
        not part of this product’s public materials.
      </p>
    </section>

    <RelatedLinks
      :links="[
        { to: '/sms-gateway', label: 'Bulk SMS gateway' },
        { to: '/sms-api', label: 'Developer API' },
        { to: '/contact', label: 'Contact' },
        { to: '/privacy', label: 'Privacy Policy' },
      ]"
    />
    <CtaBanner />
  </ArticleLayout>
</template>
