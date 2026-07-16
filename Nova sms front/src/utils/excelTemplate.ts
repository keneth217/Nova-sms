import * as XLSX from 'xlsx'
import { downloadBlob } from '@/utils/download'

export const CONTACT_EXCEL_HEADERS = ['phone', 'firstName', 'lastName', 'email'] as const

export const CONTACT_EXCEL_SAMPLE_ROWS = [
  {
    phone: '0712345678',
    firstName: 'Jane',
    lastName: 'Wanjiku',
    email: 'jane@example.com',
  },
  {
    phone: '0722334455',
    firstName: 'Peter',
    lastName: 'Otieno',
    email: 'peter@example.com',
  },
  {
    phone: '254700112233',
    firstName: 'Amina',
    lastName: 'Hassan',
    email: '',
  },
]

export function buildContactsExcelTemplateBlob(): Blob {
  const worksheet = XLSX.utils.json_to_sheet(CONTACT_EXCEL_SAMPLE_ROWS, {
    header: [...CONTACT_EXCEL_HEADERS],
  })
  worksheet['!cols'] = [{ wch: 16 }, { wch: 14 }, { wch: 14 }, { wch: 24 }]

  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Contacts')

  const array = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer
  return new Blob([array], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
}

export { downloadBlob }

export function downloadContactsExcelTemplate() {
  downloadBlob(buildContactsExcelTemplateBlob(), 'nova-sms-contacts-template.xlsx')
}
