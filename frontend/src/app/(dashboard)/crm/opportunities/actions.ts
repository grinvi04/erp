'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { fetchAllPages } from '@/lib/export'
import { revalidatePath } from 'next/cache'
import type { Opportunity } from '@/types/crm'

// 전체 엑셀 내보내기 — 현재 페이지가 아닌 전체 영업기회(전 페이지 순회). 화면이 조회조건을 재적용한다.
export interface OpportunityExportFilter {
  account: string
  stage: string
  from: string
  to: string
}

export async function exportAllOpportunities(filter: OpportunityExportFilter): Promise<{
  rows: Opportunity[]
  truncated: boolean
  limit: number
}> {
  const params = new URLSearchParams()
  if (filter.account) params.set('accountId', filter.account)
  if (filter.stage) params.set('stageId', filter.stage)
  if (filter.from) params.set('from', filter.from)
  if (filter.to) params.set('to', filter.to)
  const query = params.toString()
  return fetchAllPages<Opportunity>(`/api/crm/opportunities${query ? `?${query}` : ''}`)
}

export interface OpportunityPayload {
  name: string
  stageId: number
  amount: number | null
  currency: string | null
  closeDate: string | null
  probability: number
  ownerId: string
  source: string | null
  description: string | null
}

export async function createOpportunity(
  data: Omit<OpportunityPayload, 'ownerId'> & { accountId: number },
): Promise<void> {
  await apiPost('/api/crm/opportunities', data)
  revalidatePath('/crm/opportunities')
}

export async function updateOpportunity(
  id: number,
  data: OpportunityPayload & { version: number },
): Promise<void> {
  await apiPut(`/api/crm/opportunities/${id}`, data)
  revalidatePath('/crm/opportunities')
}

export async function deleteOpportunity(id: number): Promise<void> {
  await apiDelete(`/api/crm/opportunities/${id}`)
  revalidatePath('/crm/opportunities')
}
