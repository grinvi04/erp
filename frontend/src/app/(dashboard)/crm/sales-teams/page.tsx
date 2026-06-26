import { apiGet } from '@/lib/api'
import type { SalesTeam } from '@/types/crm'
import SalesTeamsClient from './sales-teams-client'

export const metadata = { title: '영업팀 | ERP' }

export default async function CrmSalesTeamsPage() {
  const data = await apiGet<SalesTeam[]>('/api/crm/sales-teams')
  const teams = Array.isArray(data) ? data : []
  return <SalesTeamsClient teams={teams} />
}
