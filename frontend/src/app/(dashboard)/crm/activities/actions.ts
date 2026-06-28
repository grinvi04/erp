'use server'
import { apiGet, apiGetPage, apiPost, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { ActivityType, Contact, Opportunity } from '@/types/crm'

export interface ActivityPayload {
  activityType: ActivityType
  subject: string
  accountId: number | null
  contactId: number | null
  opportunityId: number | null
  dueDate: string | null
  description: string | null
}

export async function createActivity(data: ActivityPayload): Promise<void> {
  await apiPost('/api/crm/activities', data)
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

/** 선택한 고객사의 담당자 목록 — 활동 등록 폼에서 on-demand 조회 */
export async function fetchContactsByAccount(accountId: number): Promise<Contact[]> {
  return apiGet<Contact[]>(`/api/crm/contacts?accountId=${accountId}`)
}

/** 선택한 고객사의 영업기회 목록 — 활동 등록 폼에서 on-demand 조회 */
export async function fetchOpportunitiesByAccount(accountId: number): Promise<Opportunity[]> {
  const page = await apiGetPage<Opportunity>(
    `/api/crm/opportunities?accountId=${accountId}&size=1000`,
  )
  return page.content
}
