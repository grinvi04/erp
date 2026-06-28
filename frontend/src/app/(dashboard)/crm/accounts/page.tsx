import { apiGetPage } from '@/lib/api'
import { resolveUserNames } from '@/lib/users'
import type { CrmAccount } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import AccountsClient from './accounts-client'

export const metadata = { title: '고객사 | ERP' }

export default async function CrmAccountsPage(props: {
  searchParams: Promise<{ page?: string; size?: string; keyword?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const keyword = sp.keyword?.trim() || ''
  const keywordQuery = keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''

  const data = await apiGetPage<CrmAccount>(
    `/api/crm/accounts?page=${page}&size=${size}${keywordQuery}`,
  )
  const names = await resolveUserNames(data.content.map((a) => a.ownerId))

  return <AccountsClient data={data as PageResponse<CrmAccount>} keyword={keyword} names={names} />
}
