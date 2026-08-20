import { fileURLToPath, URL } from 'node:url'

import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'
import { PUBLIC_SITEMAP_PATHS, SITE_ORIGIN } from './src/seo/public-paths'

function sitemapXml(): string {
  const body = PUBLIC_SITEMAP_PATHS.map((path) => {
    const loc = path === '/' ? `${SITE_ORIGIN}/` : `${SITE_ORIGIN}${path}`
    return `  <url>\n    <loc>${loc}</loc>\n    <changefreq>weekly</changefreq>\n  </url>`
  }).join('\n')
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${body}\n</urlset>\n`
}

function sitemapPlugin(): Plugin {
  const xml = sitemapXml()
  return {
    name: 'nova-sms-sitemap',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url?.split('?')[0] === '/sitemap.xml') {
          res.setHeader('Content-Type', 'application/xml; charset=utf-8')
          res.end(xml)
          return
        }
        next()
      })
    },
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: 'sitemap.xml', source: xml })
    },
  }
}

export default defineConfig({
  plugins: [vue(), vueDevTools(), tailwindcss(), sitemapPlugin()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'https://smsapi.novastack.co.ke',
        changeOrigin: true,
      },
    },
  },
})
