'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

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

export async function createLead(data: LeadPayload): Promise<void> {
  await apiPost('/api/crm/leads', data)
  revalidatePath('/crm/leads')
}

export async function updateLead(id: number, data: LeadPayload & { version: number }): Promise<void> {
  await apiPut(`/api/crm/leads/${id}`, data)
  revalidatePath('/crm/leads')
}

export async function convertLead(
  id: number,
  data: { accountId: number; opportunityId: number | null },
): Promise<void> {
  await apiPost(`/api/crm/leads/${id}/convert`, data)
  revalidatePath('/crm/leads')
}

export async function deleteLead(id: number): Promise<void> {
  await apiDelete(`/api/crm/leads/${id}`)
  revalidatePath('/crm/leads')
}
