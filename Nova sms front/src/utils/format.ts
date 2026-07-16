export function formatCurrency(amount: number, currency = 'KES'): string {
  return new Intl.NumberFormat('en-KE', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount)
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat('en-KE').format(value)
}

export function formatDate(value: string | null | undefined, withTime = true): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('en-KE', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  }).format(date)
}

export function formatPercent(value: number, digits = 1): string {
  return `${value.toFixed(digits)}%`
}

export function smsPageCount(message: string): number {
  const length = message.length
  if (length === 0) return 0
  if (length <= 160) return 1
  return Math.ceil(length / 153)
}

export function estimateSmsCost(message: string, recipients: number, unitCost: number): number {
  return smsPageCount(message) * recipients * unitCost
}

export function parsePhoneList(raw: string): string[] {
  return raw
    .split(/[\n,;]+/)
    .map((p) => p.trim())
    .filter(Boolean)
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function isMockMode(): boolean {
  return import.meta.env.VITE_USE_MOCK === 'true'
}

export function looksLikeEmail(value: string): boolean {
  return value.includes('@')
}

export function looksLikePhone(value: string): boolean {
  const cleaned = value.replace(/[\s\-()+]/g, '')
  return !value.includes('@') && /^\d{9,15}$/.test(cleaned)
}

export function normalizePhone(phone: string): string {
  let cleaned = phone.trim().replace(/[\s\-()]/g, '')
  if (cleaned.startsWith('+')) cleaned = cleaned.slice(1)
  if (cleaned.startsWith('00')) cleaned = cleaned.slice(2)
  if (cleaned.startsWith('0') && cleaned.length === 10) {
    cleaned = `254${cleaned.slice(1)}`
  } else if (/^[17]\d{8}$/.test(cleaned)) {
    cleaned = `254${cleaned}`
  }
  return cleaned
}

export function normalizeLoginIdentifier(value: string): string {
  const trimmed = value.trim()
  if (looksLikePhone(trimmed)) return normalizePhone(trimmed)
  return trimmed.toLowerCase()
}

