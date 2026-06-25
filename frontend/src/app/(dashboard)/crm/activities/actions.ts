'use server'
import { apiPost, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { ActivityType } from '@/types/crm'

export interface ActivityPayload {
  activityType: ActivityType
  subject: string
  accountId: number | null
  ownerId: string
  dueDate: string | null
  description: string | null
}

export async function createActivity(data: ActivityPayload): Promise<void> {
  await apiPost('/api/crm/activities', { ...data, contactId: null, opportunityId: null })
  revalidatePath('/crm/activities')
}

export async function completeActivity(id: number): Promise<void> {
  await apiPost(`/api/crm/activities/${id}/complete`, {})
  revalidatePath('/crm/activities')
}

export async function cancelActivity(id: number): Promise<void> {
  await apiPost(`/api/crm/activities/${id}/cancel`, {})
  revalidatePath('/crm/activities')
}

export async function deleteActivity(id: number): Promise<void> {
  await apiDelete(`/api/crm/activities/${id}`)
  revalidatePath('/crm/activities')
}
