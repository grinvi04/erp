'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Customer } from '@/types/finance'

const PATH = '/finance/customers'

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
