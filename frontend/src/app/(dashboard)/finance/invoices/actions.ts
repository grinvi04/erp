'use server'
import { apiPost } from '@/lib/api'
import { fetchAllPages } from '@/lib/export'
import { revalidatePath } from 'next/cache'
import type { ApInvoice, TaxType } from '@/types/finance'

const PATH = '/finance/invoices'

export interface InvoiceExportFilter {
  vendor: string
  status: string
  from: string
  to: string
}

// 전체 엑셀 내보내기 — 조회 조건을 백엔드에 전달해 상한 이후의 일치 행도 빠뜨리지 않는다.
export async function exportAllInvoices(filter: InvoiceExportFilter): Promise<{
  rows: ApInvoice[]
  truncated: boolean
  limit: number
}> {
  const params = new URLSearchParams()
  if (filter.vendor) params.set('vendorId', filter.vendor)
  if (filter.status) params.set('status', filter.status)
  if (filter.from) params.set('from', filter.from)
  if (filter.to) params.set('to', filter.to)
  const query = params.toString()
  return fetchAllPages<ApInvoice>(`/api/finance/invoices${query ? `?${query}` : ''}`)
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
