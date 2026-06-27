'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Vendor } from '@/types/finance'

const PATH = '/finance/vendors'

export async function createVendor(data: {
  code: string
  name: string
  businessNo: string | null
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  paymentTerms: number
  payablesAccountId: number | null
}): Promise<void> {
  await apiPost<Vendor>('/api/finance/vendors', data)
  revalidatePath(PATH)
}

export async function updateVendor(id: number, data: {
  name: string
  businessNo: string | null
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  paymentTerms: number
  payablesAccountId: number | null
  version: number
}): Promise<void> {
  await apiPut<Vendor>(`/api/finance/vendors/${id}`, data)
  revalidatePath(PATH)
}

export async function deactivateVendor(id: number): Promise<void> {
  await apiDelete(`/api/finance/vendors/${id}`)
  revalidatePath(PATH)
}
