import { apiGet, apiGetPage, getCurrentUserId } from '@/lib/api'
import type { Activity, CrmAccount } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import ActivitiesClient from './activities-client'

export const metadata = { title: '활동 | ERP' }

export default async function ActivitiesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, accountsPage, currentUserId] = await Promise.all([
    apiGetPage<Activity>(`/api/crm/activities?page=${page}&size=${size}`),
    apiGet<PageResponse<CrmAccount>>('/api/crm/accounts?size=1000'),
    getCurrentUserId(),
  ])

  return (
    <ActivitiesClient
      data={data as PageResponse<Activity>}
      accounts={accountsPage.content}
      currentUserId={currentUserId}
    />
  )
}
