import { toValue, watch, type MaybeRefOrGetter } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import {
  DEFAULT_DESCRIPTION,
  SITE_NAME,
  SITE_ORIGIN,
} from '@/seo/public-paths'

export interface SeoInput {
  title?: string
  description?: string
  path?: string
  image?: string
  robots?: string
  ogType?: string
}

function siteOrigin(): string {
  const env = import.meta.env.VITE_SITE_URL as string | undefined
  return (env || SITE_ORIGIN).replace(/\/$/, '')
}

export function absoluteUrl(path = '/'): string {
  const origin = siteOrigin()
  if (!path || path === '/') return `${origin}/`
  const normalized = path.startsWith('/') ? path : `/${path}`
  return `${origin}${normalized}`
}

export function ogImageUrl(image?: string): string {
  if (image && /^https?:\/\//i.test(image)) return image
  return `${siteOrigin()}${image || '/novasmslogo.png'}`
}

function upsertMeta(attr: 'name' | 'property', key: string, content: string) {
  let el = document.head.querySelector(`meta[${attr}="${key}"]`) as HTMLMetaElement | null
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function upsertLink(rel: string, href: string) {
  let el = document.head.querySelector(`link[rel="${rel}"]`) as HTMLLinkElement | null
  if (!el) {
    el = document.createElement('link')
    el.rel = rel
    document.head.appendChild(el)
  }
  el.href = href
}

export function applySeo(input: SeoInput) {
  const title = input.title?.trim() || SITE_NAME
  const description = input.description?.trim() || DEFAULT_DESCRIPTION
  const url = absoluteUrl(input.path || '/')
  const image = ogImageUrl(input.image)
  const robots = input.robots || 'index,follow'
  const ogType = input.ogType || 'website'

  document.title = title
  document.documentElement.lang = 'en-KE'

  upsertMeta('name', 'description', description)
  upsertMeta('name', 'robots', robots)
  upsertMeta('name', 'googlebot', robots)
  upsertMeta('name', 'theme-color', '#0f766e')
  upsertLink('canonical', url)

  upsertMeta('property', 'og:site_name', SITE_NAME)
  upsertMeta('property', 'og:locale', 'en_KE')
  upsertMeta('property', 'og:type', ogType)
  upsertMeta('property', 'og:title', title)
  upsertMeta('property', 'og:description', description)
  upsertMeta('property', 'og:url', url)
  upsertMeta('property', 'og:image', image)
  upsertMeta('property', 'og:image:alt', `${SITE_NAME} — SMS & M-Pesa APIs`)

  upsertMeta('name', 'twitter:card', 'summary_large_image')
  upsertMeta('name', 'twitter:title', title)
  upsertMeta('name', 'twitter:description', description)
  upsertMeta('name', 'twitter:image', image)
}

export function seoFromRoute(route: RouteLocationNormalizedLoaded): SeoInput {
  const title = route.meta.seoTitle
    ? route.meta.seoTitle
    : route.meta.title
      ? `${route.meta.title} · ${SITE_NAME}`
      : SITE_NAME
  const path = (route.meta.canonicalPath as string | undefined) || route.path
  return {
    title,
    description: route.meta.description || DEFAULT_DESCRIPTION,
    path,
    image: route.meta.ogImage,
    robots: route.meta.robots || 'index,follow',
    ogType: route.meta.ogType || 'website',
  }
}

export function applySeoFromRoute(route: RouteLocationNormalizedLoaded) {
  applySeo(seoFromRoute(route))
}

/** Call from a view to override route-level SEO for dynamically titled pages. */
export function useSeo(input: MaybeRefOrGetter<SeoInput>) {
  watch(
    () => toValue(input),
    (value) => applySeo(value),
    { immediate: true, deep: true },
  )
}
