import * as XLSX from 'xlsx'
import type { Contact } from '@/models/contact.model'
import { downloadBlob, stampFilename } from '@/utils/download'
import { formatDate } from '@/utils/format'

export const CONTACT_EXPORT_HEADERS = [
  'phone',
  'firstName',
  'lastName',
  'email',
  'groups',
  'createdAt',
] as const

export interface ExcelSheetExport {
  sheetName: string
  rows: Record<string, string | number | null | undefined>[]
  columns?: { key: string; header: string; width?: number }[]
}

export function exportToXlsx(filename: string, sheets: ExcelSheetExport[]) {
  const workbook = XLSX.utils.book_new()

  for (const sheet of sheets) {
    const keys = sheet.columns?.map((c) => c.key) ?? Object.keys(sheet.rows[0] ?? {})
    const headers = sheet.columns?.map((c) => c.header) ?? keys
    const data = sheet.rows.map((row) => {
      const out: Record<string, string | number> = {}
      keys.forEach((key, i) => {
        const value = row[key]
        out[headers[i] ?? key] = value == null ? '' : value
      })
      return out
    })

    const worksheet = XLSX.utils.json_to_sheet(data, { header: headers })
    if (sheet.columns) {
      worksheet['!cols'] = sheet.columns.map((c) => ({ wch: c.width ?? 16 }))
    }
    XLSX.utils.book_append_sheet(workbook, worksheet, sheet.sheetName.slice(0, 31))
  }

  const array = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer
  downloadBlob(
    new Blob([array], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
    filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`,
  )
}

export function exportContactsToExcel(
  contacts: Contact[],
  options?: { organizationName?: string; groupFilter?: string },
) {
  const rows = contacts.map((c) => ({
    phone: c.phone,
    firstName: c.firstName ?? '',
    lastName: c.lastName ?? '',
    email: c.email ?? '',
    groups: c.groupNames.join(', '),
    createdAt: formatDate(c.createdAt, false),
  }))

  const title = options?.groupFilter
    ? `contacts-${options.groupFilter}`
    : 'contacts'

  exportToXlsx(stampFilename(`nova-sms-${title}`, 'xlsx'), [
    {
      sheetName: 'Contacts',
      rows,
      columns: [
        { key: 'phone', header: 'Phone', width: 16 },
        { key: 'firstName', header: 'First name', width: 14 },
        { key: 'lastName', header: 'Last name', width: 14 },
        { key: 'email', header: 'Email', width: 26 },
        { key: 'groups', header: 'Groups', width: 24 },
        { key: 'createdAt', header: 'Added', width: 14 },
      ],
    },
  ])
}
