'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { AccountType } from '@/types/crm'

export interface AccountPayload {
  name: string
  businessNo: string | null
  industry: string | null
  website: string | null
  phone: string | null
  address: string | null
  employeeCount: number | null
  annualRevenue: number | null
  accountType: AccountType
  ownerId: string
}

export async function createAccount(data: AccountPayload & { code: string }): Promise<void> {
  await apiPost('/api/crm/accounts', data)
  revalidatePath('/crm/accounts')
}

export async function updateAccount(id: number, data: AccountPayload): Promise<void> {
  await apiPut(`/api/crm/accounts/${id}`, data)
  revalidatePath('/crm/accounts')
}

export async function deactivateAccount(id: number): Promise<void> {
  await apiDelete(`/api/crm/accounts/${id}`)
  revalidatePath('/crm/accounts')
}
