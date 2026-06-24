import { apiGet, apiGetPage } from '@/lib/api'
import type { Employee, LeaveRequest, LeavePolicy } from '@/types/hr'
import type { PageResponse } from '@/types/api'
import LeaveRequestsClient from './leave-requests-client'

export const metadata = { title: '휴가 신청 | ERP' }

export default async function LeaveRequestsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, employeePage, policies] = await Promise.all([
    apiGetPage<LeaveRequest>(`/api/hr/leave-requests?page=${page}&size=${size}`),
    apiGetPage<Employee>('/api/hr/employees?status=ACTIVE&page=0&size=1000'),
    apiGet<LeavePolicy[]>('/api/hr/leave-policies'),
  ])

  return (
    <LeaveRequestsClient
      data={data as PageResponse<LeaveRequest>}
      employees={(employeePage as PageResponse<Employee>).content}
      policies={policies}
    />
  )
}
