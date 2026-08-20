<script setup lang="ts">
import ArticleLayout from '@/components/marketing/ArticleLayout.vue'
import CtaBanner from '@/components/marketing/CtaBanner.vue'
import JsonLd from '@/components/seo/JsonLd.vue'
import { publicFaqs } from '@/data/public-faq'
import { faqSchema } from '@/seo/schema'
</script>

<template>
  <ArticleLayout
    kicker="Help"
    title="Nova SMS frequently asked questions"
    description="Straight answers about the bulk SMS platform, M-Pesa STK Push and Paybill funding, API keys, and how payment status is confirmed."
    path="/faq"
  >
    <JsonLd id="faq-page" :data="faqSchema(publicFaqs)" />
    <section v-for="item in publicFaqs" :key="item.question">
      <h2 class="text-xl font-semibold text-slate-900">{{ item.question }}</h2>
      <p class="mt-2">{{ item.answer }}</p>
    </section>
    <CtaBanner />
  </ArticleLayout>
</template>
