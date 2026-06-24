'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Account, AccountType, NormalBalance } from '@/types/finance'

const PATH = '/finance/accounts'

export async function createAccount(data: {
  code: string
  name: string
  accountType: AccountType
  normalBalance: NormalBalance
  parentId: number | null
  isSummary: boolean
}): Promise<void> {
  await apiPost<Account>('/api/finance/accounts', data)
  revalidatePath(PATH)
}

export async function updateAccount(id: number, data: {
  name: string
  isSummary: boolean
}): Promise<void> {
  await apiPut<Account>(`/api/finance/accounts/${id}`, data)
  revalidatePath(PATH)
}

export async function deactivateAccount(id: number): Promise<void> {
  await apiDelete(`/api/finance/accounts/${id}`)
  revalidatePath(PATH)
}
