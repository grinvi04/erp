import { apiGet, apiGetPage } from '@/lib/api'
import type { Account, Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'
import VendorsClient from './vendors-client'

export const metadata = { title: '공급업체 | ERP' }

export default async function VendorsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, accounts] = await Promise.all([
    apiGetPage<Vendor>(`/api/finance/vendors?page=${page}&size=${size}`),
    apiGet<Account[]>('/api/finance/accounts'),
  ])
  return <VendorsClient data={data as PageResponse<Vendor>} accounts={accounts} />
}
