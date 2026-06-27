import { apiGet, apiGetPage } from '@/lib/api'
import type { Account, ArInvoice, Customer } from '@/types/finance'
import type { PageResponse } from '@/types/api'
import ArInvoicesClient from './ar-invoices-client'

export const metadata = { title: '매출 인보이스 | ERP' }

export default async function ArInvoicesPage(props: {
  searchParams: Promise<{ page?: string; size?: string; status?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const statusFilter = sp.status ? `&status=${sp.status}` : ''

  const [data, customers, accounts] = await Promise.all([
    apiGetPage<ArInvoice>(`/api/finance/ar-invoices?page=${page}&size=${size}${statusFilter}`),
    apiGet<Customer[]>('/api/finance/customers?size=1000'),
    apiGet<Account[]>('/api/finance/accounts'),
  ])

  return (
    <ArInvoicesClient
      data={data as PageResponse<ArInvoice>}
      customers={customers}
      accounts={accounts}
    />
  )
}
