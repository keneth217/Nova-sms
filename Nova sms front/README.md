# Nova SMS Frontend

Vue 3 + TypeScript SaaS dashboard for the Nova SMS multi-tenant bulk SMS gateway.

## Stack

- Vue 3 (Composition API) · TypeScript · Vite
- Vue Router · Pinia · Axios
- Tailwind CSS v4 · Heroicons · Chart.js · VueUse

## Quick start

```bash
npm install
npm run dev
```

App runs at [http://localhost:5173](http://localhost:5173).

Mock mode is enabled by default (`VITE_USE_MOCK=true` in `.env`) so the UI works without the backend.

### Demo logins (mock mode)

| Role | Email | Password |
|------|-------|----------|
| Organization admin | `admin@acme.co.ke` | `password123` |
| Super admin | `admin@novastack.com` | `ChangeMe123!` |

## Live API

1. Start the Spring Boot backend on port `8092`.
2. Copy `.env.example` → `.env` and set:

```env
VITE_API_BASE_URL=/api/v1
VITE_USE_MOCK=false
```

Vite proxies `/api` to `http://localhost:8092`.

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Dev server |
| `npm run build` | Type-check + production build |
| `npm run preview` | Preview production build |

## Architecture

```
src/
  api/          Axios client + service classes
  models/       TypeScript domain types
  stores/       Pinia stores
  router/       Routes + auth/role guards
  layouts/      Auth + dashboard shells
  views/        Auth, org dashboard, super-admin screens
  components/   Reusable UI (cards, tables, forms, charts)
  mocks/        Demo data used when VITE_USE_MOCK=true
```
