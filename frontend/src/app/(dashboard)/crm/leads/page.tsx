import { apiGet, apiGetPage } from '@/lib/api'
import type { CrmAccount, Lead } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import LeadsClient from './leads-client'

export const metadata = { title: '리드 | ERP' }

export default async function LeadsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, accounts] = await Promise.all([
    apiGetPage<Lead>(`/api/crm/leads?page=${page}&size=${size}`),
    apiGet<PageResponse<CrmAccount>>('/api/crm/accounts?isActive=true&size=1000'),
  ])

  return <LeadsClient data={data as PageResponse<Lead>} accounts={accounts.content} />
}
