import { apiGet, apiGetPage } from '@/lib/api'
import type { LeaveRequest, Employee, LeavePolicy } from '@/types/hr'
import type { PageResponse } from '@/types/api'
import LeaveRequestsClient from './leave-requests-client'

export const metadata = { title: '휴가 신청 | ERP' }

export default async function LeaveRequestsPage() {
  const [requests, employeePage, policies] = await Promise.all([
    apiGet<LeaveRequest[]>('/api/hr/leave-requests'),
    apiGetPage<Employee>('/api/hr/employees?page=0&size=500'),
    apiGet<LeavePolicy[]>('/api/hr/leave-policies'),
  ])

  return (
    <LeaveRequestsClient
      requests={requests}
      employees={(employeePage as PageResponse<Employee>).content}
      policies={policies}
    />
  )
}
