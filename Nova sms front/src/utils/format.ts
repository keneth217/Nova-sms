import { isBillableFailure } from '@/models/sms.model'

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

export function formatRelativeTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const deltaSec = Math.round((Date.now() - date.getTime()) / 1000)
  if (deltaSec < 10) return 'just now'
  if (deltaSec < 60) return `${deltaSec} seconds ago`
  const minutes = Math.round(deltaSec / 60)
  if (minutes < 60) return `${minutes} minute${minutes === 1 ? '' : 's'} ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`
  const days = Math.round(hours / 24)
  return `${days} day${days === 1 ? '' : 's'} ago`
}

export function formatPercent(value: number, digits = 1): string {
  return `${value.toFixed(digits)}%`
}

export function smsPageCount(message: string): number {
  return analyzeSms(message).units
}

export function analyzeSms(message: string): { encoding: 'GSM-7' | 'Unicode'; characters: number; units: number } {
  if (!message) return { encoding: 'GSM-7', characters: 0, units: 0 }
  if (isGsm7(message)) {
    const septets = gsm7SeptetCount(message)
    const units = septets <= 160 ? 1 : Math.ceil(septets / 153)
    return { encoding: 'GSM-7', characters: message.length, units }
  }
  const units = message.length <= 70 ? 1 : Math.ceil(message.length / 67)
  return { encoding: 'Unicode', characters: message.length, units }
}

const GSM7_BASIC =
  '@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !"#¤%&\'()*+,-./0123456789:;<=>?' +
  '¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà'
const GSM7_EXTENDED = new Set('|€{}[]~\\^')

function isGsm7(message: string): boolean {
  for (const char of message) {
    if (!GSM7_BASIC.includes(char) && !GSM7_EXTENDED.has(char)) return false
  }
  return true
}

function gsm7SeptetCount(message: string): number {
  let count = 0
  for (const char of message) {
    count += GSM7_EXTENDED.has(char) ? 2 : 1
  }
  return count
}

export function estimateSmsCost(message: string, recipients: number, unitCost: number): number {
  return smsPageCount(message) * recipients * unitCost
}

export function estimateWhatsAppCost(message: string, recipients: number, unitCost: number): number {
  if (!message || recipients <= 0) return 0
  return recipients * unitCost
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
    // 07XXXXXXXX → 2547…, 011XXXXXXX → 25411…
    cleaned = `254${cleaned.slice(1)}`
  } else if (/^7\d{8}$/.test(cleaned) || /^11\d{7}$/.test(cleaned)) {
    cleaned = `254${cleaned}`
  }
  return cleaned
}

/** Safaricom: 2547XXXXXXXX (07…) or 25411XXXXXXX (011…). */
export function isSafaricomMsisdn(phone: string): boolean {
  return /^254(7\d{8}|11\d{7})$/.test(normalizePhone(phone))
}

export function normalizeLoginIdentifier(value: string): string {
  const trimmed = value.trim()
  if (looksLikePhone(trimmed)) return normalizePhone(trimmed)
  return trimmed.toLowerCase()
}

/** Clean Africa's Talking / provider error text for UI display. */
export function formatProviderError(reason: string | null | undefined): string {
  if (!reason) return 'Provider rejected the message.'
  return reason
    .replace(/^\d{3}\s+[A-Za-z ]+:\s*/i, '')
    .replace(/^"|"$/g, '')
    .trim() || 'Provider rejected the message.'
}

export function summarizeSingleSmsResult(message: {
  status: string
  recipient: string
  failureReason?: string | null
}): { ok: boolean; text: string } {
  if (message.status === 'FAILED') {
    return {
      ok: false,
      text: `Send failed for ${message.recipient}: ${formatProviderError(message.failureReason)}`,
    }
  }
  if (message.status === 'DELIVERED') {
    return {
      ok: true,
      text: `Message delivered to ${message.recipient}.`,
    }
  }
  return {
    ok: true,
    text: `Message accepted for ${message.recipient}. Delivery confirmation pending.`,
  }
}

export function summarizeBulkSmsResult(result: {
  queuedCount: number
  batchId: string
  messages: Array<{ status: string; recipient: string; failureReason?: string | null }>
}): { ok: boolean; text: string } {
  const failed = result.messages.filter((m) => isBillableFailure(m.status))
  const sent = result.messages.filter((m) => !isBillableFailure(m.status))

  if (failed.length === 0) {
    return {
      ok: true,
      text: `Campaign sent: ${result.queuedCount} message${result.queuedCount === 1 ? '' : 's'} (batch ${result.batchId}).`,
    }
  }

  const samples = failed
    .slice(0, 3)
    .map((m) => `${m.recipient}: ${formatProviderError(m.failureReason)}`)
    .join(' · ')
  const extra = failed.length > 3 ? ` (+${failed.length - 3} more)` : ''

  if (sent.length === 0) {
    return {
      ok: false,
      text: `All ${failed.length} message${failed.length === 1 ? '' : 's'} failed. ${samples}${extra}`,
    }
  }

  return {
    ok: false,
    text: `${sent.length} sent, ${failed.length} failed. ${samples}${extra}`,
  }
}


