'use server'
import { apiPost, apiGetRaw } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { TaxInvoice, ChargeType } from '@/types/finance'

const PATH = '/finance/tax-invoices'

export async function issueTaxInvoice(
  arInvoiceId: number,
  data: {
    writeDate: string | null
    chargeType: ChargeType
    itemName: string | null
    note: string | null
  },
): Promise<void> {
  await apiPost<TaxInvoice>(`/api/finance/ar-invoices/${arInvoiceId}/tax-invoice`, data)
  revalidatePath(PATH)
}

export async function cancelTaxInvoice(id: number): Promise<void> {
  await apiPost<TaxInvoice>(`/api/finance/tax-invoices/${id}/cancel`, {})
  revalidatePath(PATH)
}

/** 국세청 표준 XML 본문을 BFF 인증 경유로 받아 반환. 클라이언트가 blob 다운로드한다. */
export async function getTaxInvoiceXml(id: number): Promise<string> {
  const res = await apiGetRaw(`/api/finance/tax-invoices/${id}/xml`)
  if (!res.ok) {
    throw new Error('XML 생성에 실패했습니다')
  }
  return res.text()
}
