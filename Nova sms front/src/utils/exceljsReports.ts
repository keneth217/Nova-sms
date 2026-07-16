import ExcelJS from 'exceljs'
import { downloadBlob, stampFilename } from '@/utils/download'
import { formatCurrency } from '@/utils/format'

export interface ReportColumn {
  header: string
  key: string
  width?: number
}

export async function exportStyledWorkbook(options: {
  filename: string
  sheetName: string
  title: string
  subtitle?: string
  columns: ReportColumn[]
  rows: Record<string, string | number | null | undefined>[]
}) {
  const workbook = new ExcelJS.Workbook()
  workbook.creator = 'Nova SMS'
  workbook.created = new Date()

  const sheet = workbook.addWorksheet(options.sheetName.slice(0, 31))
  sheet.addRow([options.title])
  sheet.getRow(1).font = { bold: true, size: 14, color: { argb: 'FF0F766E' } }
  if (options.subtitle) {
    sheet.addRow([options.subtitle])
    sheet.getRow(2).font = { size: 10, color: { argb: 'FF64748B' } }
  }
  sheet.addRow([])

  const headerRow = sheet.addRow(options.columns.map((c) => c.header))
  headerRow.eachCell((cell) => {
    cell.font = { bold: true, color: { argb: 'FFFFFFFF' } }
    cell.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF0F766E' },
    }
  })

  options.columns.forEach((col, index) => {
    sheet.getColumn(index + 1).width = col.width ?? 16
  })

  for (const row of options.rows) {
    sheet.addRow(options.columns.map((c) => row[c.key] ?? ''))
  }

  const buffer = await workbook.xlsx.writeBuffer()
  downloadBlob(
    new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
    options.filename.endsWith('.xlsx') ? options.filename : `${options.filename}.xlsx`,
  )
}

export async function exportInvoiceExcel(input: {
  invoiceNumber: string
  organizationName: string
  items: { description: string; quantity: number; unitPrice: number }[]
  currency?: string
}) {
  const currency = input.currency || 'KES'
  const rows = input.items.map((item) => ({
    description: item.description,
    quantity: item.quantity,
    unitPrice: item.unitPrice,
    amount: item.quantity * item.unitPrice,
  }))
  const total = rows.reduce((sum, r) => sum + r.amount, 0)

  await exportStyledWorkbook({
    filename: stampFilename(`nova-sms-invoice-${input.invoiceNumber}`, 'xlsx'),
    sheetName: 'Invoice',
    title: `Invoice ${input.invoiceNumber}`,
    subtitle: `${input.organizationName} · Total ${formatCurrency(total, currency)}`,
    columns: [
      { header: 'Description', key: 'description', width: 36 },
      { header: 'Qty', key: 'quantity', width: 10 },
      { header: 'Unit price', key: 'unitPrice', width: 14 },
      { header: 'Amount', key: 'amount', width: 14 },
    ],
    rows,
  })
}
