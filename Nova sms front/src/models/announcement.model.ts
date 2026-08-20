export type AnnouncementTone = 'INFO' | 'WARNING' | 'DANGER'

export interface Announcement {
  enabled: boolean
  label: string
  title: string
  body: string
  tone: AnnouncementTone
  updatedAt?: string | null
}
