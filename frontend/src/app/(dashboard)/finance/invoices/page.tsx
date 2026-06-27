import { apiGet, apiGetPage } from '@/lib/api'
import type { Account, ApInvoice, Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'
import InvoicesClient from './invoices-client'

export const metadata = { title: '매입 인보이스 | ERP' }

export default async function InvoicesPage(props: {
  searchParams: Promise<{ page?: string; size?: string; status?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const statusFilter = sp.status ? `&status=${sp.status}` : ''

  const [data, vendors, accounts] = await Promise.all([
    apiGetPage<ApInvoice>(`/api/finance/invoices?page=${page}&size=${size}${statusFilter}`),
    apiGet<Vendor[]>('/api/finance/vendors?size=1000'),
    apiGet<Account[]>('/api/finance/accounts'),
  ])

  return (
    <InvoicesClient data={data as PageResponse<ApInvoice>} vendors={vendors} accounts={accounts} />
  )
}
