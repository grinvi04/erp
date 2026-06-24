import { apiGetPage, getCurrentUserId } from '@/lib/api'
import type { CrmAccount } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import AccountsClient from './accounts-client'

export const metadata = { title: '고객사 | ERP' }

export default async function CrmAccountsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, currentUserId] = await Promise.all([
    apiGetPage<CrmAccount>(`/api/crm/accounts?page=${page}&size=${size}`),
    getCurrentUserId(),
  ])

  return <AccountsClient data={data as PageResponse<CrmAccount>} currentUserId={currentUserId} />
}
