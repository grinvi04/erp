'use server'
import { apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { ApInvoice } from '@/types/finance'

const PATH = '/finance/invoices'

export async function createInvoice(data: {
  invoiceNo: string
  vendorId: number
  invoiceDate: string
  dueDate: string
  totalAmount: number
  currency: string
  note: string | null
}): Promise<void> {
  await apiPost<ApInvoice>('/api/finance/invoices', data)
  revalidatePath(PATH)
}

export async function submitInvoice(id: number): Promise<void> {
  await apiPost<ApInvoice>(`/api/finance/invoices/${id}/submit`, {})
  revalidatePath(PATH)
}

export async function approveInvoice(id: number): Promise<void> {
  await apiPost<ApInvoice>(`/api/finance/invoices/${id}/approve`, {})
  revalidatePath(PATH)
}

export async function payInvoice(id: number, amount: number): Promise<void> {
  await apiPost<ApInvoice>(`/api/finance/invoices/${id}/pay`, { amount })
  revalidatePath(PATH)
}

export async function cancelInvoice(id: number): Promise<void> {
  await apiPost<ApInvoice>(`/api/finance/invoices/${id}/cancel`, {})
  revalidatePath(PATH)
}
