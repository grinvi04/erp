'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { fetchAllPages } from '@/lib/export'
import { revalidatePath } from 'next/cache'
import type { Lead } from '@/types/crm'

// 전체 엑셀 내보내기 — 현재 페이지가 아닌 전체 리드(전 페이지 순회). 화면이 조회조건을 재적용한다.
export async function exportAllLeads(): Promise<{
  rows: Lead[]
  truncated: boolean
  limit: number
}> {
  return fetchAllPages<Lead>('/api/crm/leads')
}

export interface LeadPayload {
  lastName: string
  firstName: string
  company: string | null
  title: string | null
  email: string | null
  phone: string | null
  source: string | null
  ownerId: string
  note: string | null
}

export async function createLead(data: Omit<LeadPayload, 'ownerId'>): Promise<void> {
  await apiPost('/api/crm/leads', data)
  revalidatePath('/crm/leads')
}

export async function updateLead(
  id: number,
  data: LeadPayload & { version: number },
): Promise<void> {
  await apiPut(`/api/crm/leads/${id}`, data)
  revalidatePath('/crm/leads')
}

export interface ConvertPayload {
  accountId: number | null
  createOpportunity: boolean
  opportunityName: string | null
  stageId: number | null
  opportunityAmount: number | null
  opportunityCurrency: string | null
  opportunityCloseDate: string | null
}

export async function convertLead(id: number, data: ConvertPayload): Promise<void> {
  await apiPost(`/api/crm/leads/${id}/convert`, data)
  revalidatePath('/crm/leads')
}

export async function deleteLead(id: number): Promise<void> {
  await apiDelete(`/api/crm/leads/${id}`)
  revalidatePath('/crm/leads')
}
