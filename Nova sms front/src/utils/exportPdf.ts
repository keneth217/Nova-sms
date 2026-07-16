import { jsPDF } from 'jspdf'
import autoTable from 'jspdf-autotable'
import type { Contact } from '@/models/contact.model'
import { downloadBlob, stampFilename } from '@/utils/download'
import { formatCurrency, formatDate } from '@/utils/format'

export interface PdfTableColumn {
  header: string
  dataKey: string
  width?: number
  align?: 'left' | 'center' | 'right'
}

export interface PdfReportOptions {
  title: string
  subtitle?: string
  filename: string
  columns: PdfTableColumn[]
  rows: Record<string, string | number>[]
  orientation?: 'portrait' | 'landscape'
  footerNote?: string
}

type Rgb = [number, number, number]

const BRAND = {
  primary: [13, 148, 136] as Rgb,
  primaryDark: [15, 118, 110] as Rgb,
  primarySoft: [204, 251, 241] as Rgb,
  ink: [15, 23, 42] as Rgb,
  muted: [100, 116, 139] as Rgb,
  line: [226, 232, 240] as Rgb,
  soft: [236, 253, 245] as Rgb,
  softAlt: [248, 250, 252] as Rgb,
  white: [255, 255, 255] as Rgb,
  footerBg: [248, 250, 252] as Rgb,
}

function centerX(doc: jsPDF) {
  return doc.internal.pageSize.getWidth() / 2
}

function centerText(
  doc: jsPDF,
  text: string,
  y: number,
  options?: {
    fontSize?: number
    fontStyle?: 'normal' | 'bold' | 'italic'
    color?: Rgb
    maxWidth?: number
  },
) {
  doc.setFont('helvetica', options?.fontStyle ?? 'normal')
  doc.setFontSize(options?.fontSize ?? 11)
  if (options?.color) doc.setTextColor(...options.color)
  doc.text(text, centerX(doc), y, {
    align: 'center',
    ...(options?.maxWidth ? { maxWidth: options.maxWidth } : {}),
  })
}

function drawBrandMark(doc: jsPDF, cx: number, cy: number, size = 7) {
  doc.setFillColor(...BRAND.primary)
  doc.circle(cx, cy, size, 'F')
  doc.setTextColor(...BRAND.white)
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(8)
  doc.text('N', cx, cy + 1.1, { align: 'center' })
}

function drawPageChrome(
  doc: jsPDF,
  options: Pick<PdfReportOptions, 'title' | 'subtitle' | 'footerNote'>,
  pageNumber: number,
  pageCount: number,
) {
  const pageWidth = doc.internal.pageSize.getWidth()
  const pageHeight = doc.internal.pageSize.getHeight()
  const marginX = 16
  const contentWidth = pageWidth - marginX * 2

  doc.setFillColor(...BRAND.primaryDark)
  doc.rect(0, 0, pageWidth, 3.2, 'F')
  doc.setFillColor(...BRAND.primary)
  doc.rect(0, 3.2, pageWidth, 1.2, 'F')

  doc.setFillColor(...BRAND.soft)
  doc.roundedRect(marginX, 10, contentWidth, 34, 2.5, 2.5, 'F')
  doc.setDrawColor(...BRAND.primarySoft)
  doc.setLineWidth(0.35)
  doc.roundedRect(marginX, 10, contentWidth, 34, 2.5, 2.5, 'S')

  drawBrandMark(doc, centerX(doc), 18.5, 5.5)

  centerText(doc, 'NOVA SMS', 27, {
    fontSize: 8,
    fontStyle: 'bold',
    color: BRAND.primaryDark,
  })
  centerText(doc, options.title, 34.5, {
    fontSize: 15,
    fontStyle: 'bold',
    color: BRAND.ink,
  })

  if (options.subtitle) {
    centerText(doc, options.subtitle, 40.5, {
      fontSize: 8.5,
      color: BRAND.muted,
      maxWidth: contentWidth - 12,
    })
  }

  const ruleW = 28
  doc.setDrawColor(...BRAND.primary)
  doc.setLineWidth(0.7)
  doc.line(centerX(doc) - ruleW / 2, 47.5, centerX(doc) + ruleW / 2, 47.5)
  doc.setFillColor(...BRAND.primary)
  doc.circle(centerX(doc), 47.5, 0.9, 'F')

  doc.setFillColor(...BRAND.footerBg)
  doc.rect(0, pageHeight - 16, pageWidth, 16, 'F')
  doc.setDrawColor(...BRAND.line)
  doc.setLineWidth(0.3)
  doc.line(marginX, pageHeight - 16, pageWidth - marginX, pageHeight - 16)

  const note = options.footerNote ?? `Generated ${formatDate(new Date().toISOString())}`
  centerText(doc, note, pageHeight - 10, {
    fontSize: 7.5,
    color: BRAND.muted,
  })
  centerText(doc, `Page ${pageNumber} of ${pageCount}`, pageHeight - 5.5, {
    fontSize: 7.5,
    fontStyle: 'bold',
    color: BRAND.primaryDark,
  })

  doc.setFillColor(...BRAND.primary)
  doc.rect(0, pageHeight - 2.2, pageWidth, 2.2, 'F')
}

export function exportPdfReport(options: PdfReportOptions) {
  const orientation = options.orientation ?? 'portrait'
  const doc = new jsPDF({ orientation, unit: 'mm', format: 'a4' })
  const pageWidth = doc.internal.pageSize.getWidth()
  const marginX = orientation === 'landscape' ? 18 : 16
  const contentWidth = pageWidth - marginX * 2

  const columnStyles: Record<
    number,
    { halign?: 'left' | 'center' | 'right'; cellWidth?: number; fontStyle?: 'normal' | 'bold' }
  > = {}
  options.columns.forEach((col, index) => {
    columnStyles[index] = {
      halign: col.align ?? 'center',
      ...(col.width ? { cellWidth: col.width } : {}),
    }
  })

  autoTable(doc, {
    startY: 52,
    head: [options.columns.map((c) => c.header)],
    body: options.rows.map((row) =>
      options.columns.map((c) => {
        const value = row[c.dataKey]
        return value == null || value === '' ? '—' : String(value)
      }),
    ),
    theme: 'plain',
    tableWidth: contentWidth,
    margin: { top: 52, left: marginX, right: marginX, bottom: 20 },
    styles: {
      font: 'helvetica',
      fontSize: 8.5,
      cellPadding: { top: 3.2, right: 3, bottom: 3.2, left: 3 },
      textColor: BRAND.ink,
      lineColor: BRAND.line,
      lineWidth: 0,
      valign: 'middle',
      halign: 'center',
      overflow: 'linebreak',
      minCellHeight: 8,
    },
    headStyles: {
      fillColor: BRAND.primaryDark,
      textColor: BRAND.white,
      fontStyle: 'bold',
      fontSize: 8.5,
      halign: 'center',
      cellPadding: { top: 4, right: 3, bottom: 4, left: 3 },
    },
    bodyStyles: {
      halign: 'center',
      fillColor: BRAND.white,
    },
    alternateRowStyles: {
      fillColor: BRAND.softAlt,
    },
    columnStyles,
    didParseCell: (data) => {
      if (data.section === 'body') {
        data.cell.styles.lineWidth = { top: 0, right: 0, bottom: 0.2, left: 0 }
        data.cell.styles.lineColor = BRAND.line
      }
    },
    didDrawPage: (data) => {
      const pageCount = doc.getNumberOfPages()
      drawPageChrome(doc, options, data.pageNumber, pageCount)
    },
  })

  if (!options.rows.length) {
    centerText(doc, 'No records to display', 70, {
      fontSize: 11,
      color: BRAND.muted,
    })
  }

  if (doc.getNumberOfPages() === 1 && options.rows.length === 0) {
    drawPageChrome(doc, options, 1, 1)
  }

  const blob = doc.output('blob')
  downloadBlob(
    blob,
    options.filename.endsWith('.pdf') ? options.filename : `${options.filename}.pdf`,
  )
}

export function exportContactsToPdf(
  contacts: Contact[],
  options?: { organizationName?: string; groupFilter?: string },
) {
  const org = options?.organizationName || 'Organization'
  const groupLabel = options?.groupFilter ? ` · ${options.groupFilter}` : ''
  const countLabel = `${contacts.length} contact${contacts.length === 1 ? '' : 's'}`
  const generated = formatDate(new Date().toISOString())

  exportPdfReport({
    title: 'Contacts List',
    subtitle: `${org}${groupLabel}  ·  ${countLabel}  ·  ${generated}`,
    filename: stampFilename('nova-sms-contacts', 'pdf'),
    orientation: contacts.length > 18 ? 'landscape' : 'portrait',
    columns: [
      { header: 'Phone', dataKey: 'phone', align: 'center' },
      { header: 'First name', dataKey: 'firstName', align: 'center' },
      { header: 'Last name', dataKey: 'lastName', align: 'center' },
      { header: 'Email', dataKey: 'email', align: 'center' },
      { header: 'Groups', dataKey: 'groups', align: 'center' },
      { header: 'Added', dataKey: 'createdAt', align: 'center' },
    ],
    rows: contacts.map((c) => ({
      phone: c.phone,
      firstName: c.firstName || '—',
      lastName: c.lastName || '—',
      email: c.email || '—',
      groups: c.groupNames.join(', ') || '—',
      createdAt: formatDate(c.createdAt, false),
    })),
    footerNote: 'Nova SMS  ·  Confidential contacts report',
  })
}

export function exportInvoicePdf(input: {
  invoiceNumber: string
  organizationName: string
  issuedAt?: string
  items: { description: string; quantity: number; unitPrice: number }[]
  currency?: string
}) {
  const currency = input.currency || 'KES'
  const issuedAt = input.issuedAt || new Date().toISOString()
  const rows = input.items.map((item) => {
    const total = item.quantity * item.unitPrice
    return {
      description: item.description,
      quantity: item.quantity,
      unitPrice: formatCurrency(item.unitPrice, currency),
      total: formatCurrency(total, currency),
      _total: total,
    }
  })
  const grand = rows.reduce((sum, r) => sum + r._total, 0)

  exportPdfReport({
    title: `Invoice ${input.invoiceNumber}`,
    subtitle: `${input.organizationName}  ·  Issued ${formatDate(issuedAt, false)}  ·  Total ${formatCurrency(grand, currency)}`,
    filename: stampFilename(`nova-sms-invoice-${input.invoiceNumber}`, 'pdf'),
    columns: [
      { header: 'Description', dataKey: 'description', align: 'left' },
      { header: 'Qty', dataKey: 'quantity', align: 'center' },
      { header: 'Unit price', dataKey: 'unitPrice', align: 'center' },
      { header: 'Amount', dataKey: 'total', align: 'center' },
    ],
    rows: rows.map(({ description, quantity, unitPrice, total }) => ({
      description,
      quantity,
      unitPrice,
      total,
    })),
    footerNote: `Nova SMS invoice  ·  ${input.invoiceNumber}`,
  })
}
