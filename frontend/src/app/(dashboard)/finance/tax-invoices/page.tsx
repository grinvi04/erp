import { apiGetPage } from '@/lib/api'
import type { TaxInvoice, TaxInvoiceStatus, ArInvoice } from '@/types/finance'
import type { PageResponse } from '@/types/api'
import TaxInvoicesClient from './tax-invoices-client'

export const metadata = { title: '세금계산서 | ERP' }

export default async function TaxInvoicesPage(props: {
  searchParams: Promise<{ page?: string; size?: string; status?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const status = sp.status?.trim() || ''
  const statusQuery = status ? `&status=${status}` : ''

  // 발행 대상 후보: 승인·완납 AR 인보이스(이미 발행된 건은 발행 시 409로 걸러진다).
  const [data, arInvoices] = await Promise.all([
    apiGetPage<TaxInvoice>(`/api/finance/tax-invoices?page=${page}&size=${size}${statusQuery}`),
    apiGetPage<ArInvoice>('/api/finance/ar-invoices?size=200'),
  ])
  const issuableArInvoices = (arInvoices as PageResponse<ArInvoice>).content.filter(
    (inv) => inv.status === 'APPROVED' || inv.status === 'PAID',
  )

  return (
    <TaxInvoicesClient
      data={data as PageResponse<TaxInvoice>}
      issuableArInvoices={issuableArInvoices}
      status={status as TaxInvoiceStatus | ''}
    />
  )
}
