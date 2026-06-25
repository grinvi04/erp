'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

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
  data: OpportunityPayload & { accountId: number }
): Promise<void> {
  await apiPost('/api/crm/opportunities', data)
  revalidatePath('/crm/opportunities')
}

export async function updateOpportunity(id: number, data: OpportunityPayload): Promise<void> {
  await apiPut(`/api/crm/opportunities/${id}`, data)
  revalidatePath('/crm/opportunities')
}

export async function deleteOpportunity(id: number): Promise<void> {
  await apiDelete(`/api/crm/opportunities/${id}`)
  revalidatePath('/crm/opportunities')
}
