'use server'
import { apiPost, apiPut, apiDelete, apiPostForm, apiGetRaw } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Customer } from '@/types/finance'
import type { BulkImportResult } from '@/types/api'

const PATH = '/finance/customers'

export async function importCustomersCsv(form: FormData): Promise<BulkImportResult> {
  const result = await apiPostForm<BulkImportResult>('/api/finance/customers/import', form)
  revalidatePath(PATH)
  return result
}

export async function getCustomerTemplate(): Promise<string> {
  const res = await apiGetRaw('/api/finance/customers/import/template')
  if (!res.ok) throw new Error('템플릿을 받을 수 없습니다')
  return res.text()
}

export async function createCustomer(data: {
  code: string
  name: string
  businessNo: string | null
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  paymentTerms: number
  receivablesAccountId: number | null
  representativeName: string | null
  address: string | null
  businessType: string | null
  businessItem: string | null
}): Promise<void> {
  await apiPost<Customer>('/api/finance/customers', data)
  revalidatePath(PATH)
}

export async function updateCustomer(
  id: number,
  data: {
    name: string
    businessNo: string | null
    contactName: string | null
    contactEmail: string | null
    contactPhone: string | null
    paymentTerms: number
    receivablesAccountId: number | null
    representativeName: string | null
    address: string | null
    businessType: string | null
    businessItem: string | null
    version: number
  },
): Promise<void> {
  await apiPut<Customer>(`/api/finance/customers/${id}`, data)
  revalidatePath(PATH)
}

export async function deactivateCustomer(id: number): Promise<void> {
  await apiDelete(`/api/finance/customers/${id}`)
  revalidatePath(PATH)
}
