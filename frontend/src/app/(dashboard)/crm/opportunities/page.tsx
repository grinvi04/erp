import { apiGet, apiGetPage, getCurrentUserId } from '@/lib/api'
import type { Opportunity, CrmAccount, PipelineStage } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import OpportunitiesClient from './opportunities-client'

export const metadata = { title: '영업 기회 | ERP' }

export default async function OpportunitiesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, accountsPage, stages, currentUserId] = await Promise.all([
    apiGetPage<Opportunity>(`/api/crm/opportunities?page=${page}&size=${size}`),
    apiGet<PageResponse<CrmAccount>>('/api/crm/accounts?size=1000'),
    apiGet<PipelineStage[]>('/api/crm/pipeline-stages'),
    getCurrentUserId(),
  ])

  return (
    <OpportunitiesClient
      data={data as PageResponse<Opportunity>}
      accounts={accountsPage.content}
      stages={stages}
      currentUserId={currentUserId}
    />
  )
}
