'use server'
import { apiPost } from '@/lib/api'
import { fetchAllPages } from '@/lib/export'
import { revalidatePath } from 'next/cache'
import type { ApInvoice, TaxType } from '@/types/finance'

const PATH = '/finance/invoices'

// 전체 엑셀 내보내기 — 현재 페이지가 아닌 전체 매입계산서(전 페이지 순회). 화면이 조회조건을 재적용한다.
export async function exportAllInvoices(): Promise<{ rows: ApInvoice[]; truncated: boolean }> {
  return fetchAllPages<ApInvoice>('/api/finance/invoices')
}

export async function createInvoice(data: {
  invoiceNo: string
  vendorId: number
  invoiceDate: string
  dueDate: string
  supplyAmount: number
  taxType: TaxType
  currency: string
  note: string | null
  lines: { accountId: number; amount: number; description: string | null }[] | null
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

export async function payInvoice(
  id: number,
  amount: number,
  cashAccountId: number | null,
  paymentDate: string | null,
): Promise<void> {
  await apiPost<ApInvoice>(`/api/finance/invoices/${id}/pay`, {
    amount,
    cashAccountId,
    paymentDate,
  })
  revalidatePath(PATH)
}

export async function cancelInvoice(id: number): Promise<void> {
  await apiPost<ApInvoice>(`/api/finance/invoices/${id}/cancel`, {})
  revalidatePath(PATH)
}
