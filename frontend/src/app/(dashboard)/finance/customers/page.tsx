import { apiGet, apiGetPage } from '@/lib/api'
import type { Account, Customer } from '@/types/finance'
import type { PageResponse } from '@/types/api'
import CustomersClient from './customers-client'

export const metadata = { title: '고객 | ERP' }

export default async function CustomersPage(props: {
  searchParams: Promise<{ page?: string; size?: string; keyword?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const keyword = sp.keyword?.trim() || ''
  const keywordQuery = keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''

  const [data, accounts] = await Promise.all([
    apiGetPage<Customer>(`/api/finance/customers?page=${page}&size=${size}${keywordQuery}`),
    apiGet<Account[]>('/api/finance/accounts'),
  ])
  return <CustomersClient data={data as PageResponse<Customer>} accounts={accounts} keyword={keyword} />
}
