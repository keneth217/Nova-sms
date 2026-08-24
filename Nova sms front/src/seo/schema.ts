import { CONTACT_EMAIL, SITE_NAME, SITE_ORIGIN } from '@/seo/public-paths'
import { absoluteUrl, ogImageUrl } from '@/composables/useSeo'

export function organizationSchema() {
  return {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: SITE_NAME,
    url: `${SITE_ORIGIN}/`,
    email: CONTACT_EMAIL,
    logo: ogImageUrl('/novasmslogo.png'),
    areaServed: {
      '@type': 'Country',
      name: 'Kenya',
    },
    parentOrganization: {
      '@type': 'Organization',
      name: 'Novastack',
    },
  }
}

export function websiteSchema() {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: SITE_NAME,
    url: `${SITE_ORIGIN}/`,
    inLanguage: 'en-KE',
    publisher: {
      '@type': 'Organization',
      name: SITE_NAME,
    },
  }
}

export function softwareApplicationSchema() {
  return {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: SITE_NAME,
    applicationCategory: 'BusinessApplication',
    operatingSystem: 'Web',
    url: `${SITE_ORIGIN}/`,
    description:
      'Bulk SMS platform for Kenya with a prepaid wallet, M-Pesa STK Push and Paybill top-up, and a REST API.',
  }
}

export function webPageSchema(opts: {
  name: string
  description: string
  path: string
}) {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebPage',
    name: opts.name,
    description: opts.description,
    url: absoluteUrl(opts.path),
    isPartOf: {
      '@type': 'WebSite',
      name: SITE_NAME,
      url: `${SITE_ORIGIN}/`,
    },
    inLanguage: 'en-KE',
  }
}

export function breadcrumbSchema(items: { name: string; path: string }[]) {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: item.name,
      item: absoluteUrl(item.path),
    })),
  }
}

export function faqSchema(items: { question: string; answer: string }[]) {
  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: items.map((item) => ({
      '@type': 'Question',
      name: item.question,
      acceptedAnswer: {
        '@type': 'Answer',
        text: item.answer,
      },
    })),
  }
}
