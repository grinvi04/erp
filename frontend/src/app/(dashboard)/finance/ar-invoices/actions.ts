'use server'
import { apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { ArInvoice } from '@/types/finance'

const PATH = '/finance/ar-invoices'

export async function createArInvoice(data: {
  invoiceNo: string
  customerId: number
  invoiceDate: string
  dueDate: string
  totalAmount: number
  currency: string
  note: string | null
  lines: { accountId: number; amount: number; description: string | null }[] | null
}): Promise<void> {
  await apiPost<ArInvoice>('/api/finance/ar-invoices', data)
  revalidatePath(PATH)
}

export async function submitArInvoice(id: number): Promise<void> {
  await apiPost<ArInvoice>(`/api/finance/ar-invoices/${id}/submit`, {})
  revalidatePath(PATH)
}

export async function approveArInvoice(id: number): Promise<void> {
  await apiPost<ArInvoice>(`/api/finance/ar-invoices/${id}/approve`, {})
  revalidatePath(PATH)
}

export async function collectArInvoice(id: number, amount: number, cashAccountId: number | null, paymentDate: string | null): Promise<void> {
  await apiPost<ArInvoice>(`/api/finance/ar-invoices/${id}/pay`, { amount, cashAccountId, paymentDate })
  revalidatePath(PATH)
}

export async function cancelArInvoice(id: number): Promise<void> {
  await apiPost<ArInvoice>(`/api/finance/ar-invoices/${id}/cancel`, {})
  revalidatePath(PATH)
}
