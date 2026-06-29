'use server'
import { apiPost } from '@/lib/api'
import { fetchAllPages } from '@/lib/export'
import { revalidatePath } from 'next/cache'
import type { ArInvoice, TaxType } from '@/types/finance'

const PATH = '/finance/ar-invoices'

// 전체 엑셀 내보내기 — 현재 페이지가 아닌 전체 매출계산서(전 페이지 순회). 화면이 조회조건을 재적용한다.
export async function exportAllArInvoices(): Promise<{
  rows: ArInvoice[]
  truncated: boolean
  limit: number
}> {
  return fetchAllPages<ArInvoice>('/api/finance/ar-invoices')
}

export async function createArInvoice(data: {
  invoiceNo: string
  customerId: number
  invoiceDate: string
  dueDate: string
  supplyAmount: number
  taxType: TaxType
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

export async function collectArInvoice(
  id: number,
  amount: number,
  cashAccountId: number | null,
  paymentDate: string | null,
): Promise<void> {
  await apiPost<ArInvoice>(`/api/finance/ar-invoices/${id}/pay`, {
    amount,
    cashAccountId,
    paymentDate,
  })
  revalidatePath(PATH)
}

export async function cancelArInvoice(id: number): Promise<void> {
  await apiPost<ArInvoice>(`/api/finance/ar-invoices/${id}/cancel`, {})
  revalidatePath(PATH)
}
