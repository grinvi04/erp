import { apiGet, apiGetPage } from '@/lib/api'
import type { Employee, Department, Position, JobGrade } from '@/types/hr'
import EmployeesClient from './employees-client'

export const metadata = { title: '직원 관리 | ERP' }

export default async function EmployeesPage(props: {
  searchParams: Promise<{ page?: string; size?: string; keyword?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const keyword = sp.keyword?.trim() || ''
  const keywordQuery = keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''

  const [data, departments, positions, jobGrades, managerPool] = await Promise.all([
    apiGetPage<Employee>(`/api/hr/employees?page=${page}&size=${size}${keywordQuery}`),
    apiGet<Department[]>('/api/hr/departments'),
    apiGet<Position[]>('/api/hr/positions'),
    apiGet<JobGrade[]>('/api/hr/job-grades'),
    // 결재자(매니저) 피커용 직원 전체 목록 — 현재 페이지/검색과 무관하게 채운다.
    apiGetPage<Employee>('/api/hr/employees?page=0&size=200'),
  ])

  return (
    <EmployeesClient
      data={data}
      departments={departments}
      positions={positions}
      jobGrades={jobGrades}
      employees={managerPool.content}
      keyword={keyword}
    />
  )
}
