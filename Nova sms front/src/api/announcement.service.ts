import api from './axios'
import type { ApiResponse } from '@/models/auth.model'
import type { Announcement } from '@/models/announcement.model'

class AnnouncementService {
  async getPublic(): Promise<Announcement> {
    const { data } = await api.get<ApiResponse<Announcement>>('/announcement')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load announcement')
    return data.data
  }

  async getAdmin(): Promise<Announcement> {
    const { data } = await api.get<ApiResponse<Announcement>>('/admin/announcement')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load announcement')
    return data.data
  }

  async update(payload: Announcement): Promise<Announcement> {
    const { data } = await api.put<ApiResponse<Announcement>>('/admin/announcement', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to save announcement')
    return data.data
  }
}

export const announcementService = new AnnouncementService()
