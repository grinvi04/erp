'use server'
import { apiGet, apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Contact } from '@/types/crm'

export interface ContactPayload {
  lastName: string
  firstName: string
  title: string | null
  department: string | null
  email: string | null
  phone: string | null
  mobile: string | null
  isPrimary: boolean
}

export async function getContactsByAccount(accountId: number): Promise<Contact[]> {
  return apiGet<Contact[]>(`/api/crm/contacts?accountId=${accountId}`)
}

export async function createContact(data: ContactPayload & { accountId: number }): Promise<void> {
  await apiPost('/api/crm/contacts', data)
  revalidatePath('/crm/contacts')
}

export async function updateContact(
  id: number,
  data: ContactPayload & { version: number },
): Promise<void> {
  await apiPut(`/api/crm/contacts/${id}`, data)
  revalidatePath('/crm/contacts')
}

export async function deleteContact(id: number): Promise<void> {
  await apiDelete(`/api/crm/contacts/${id}`)
  revalidatePath('/crm/contacts')
}
