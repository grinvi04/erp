import { apiGet, apiGetPage } from '@/lib/api'
import type { Employee, Department, Position, JobGrade } from '@/types/hr'
import type { PageResponse } from '@/types/api'
import EmployeesClient from './employees-client'

export const metadata = { title: '직원 관리 | ERP' }

export default async function EmployeesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, departments, positions, jobGrades] = await Promise.all([
    apiGetPage<Employee>(`/api/hr/employees?page=${page}&size=${size}`),
    apiGet<Department[]>('/api/hr/departments'),
    apiGet<Position[]>('/api/hr/positions'),
    apiGet<JobGrade[]>('/api/hr/job-grades'),
  ])

  return (
    <EmployeesClient
      data={data as PageResponse<Employee>}
      departments={departments}
      positions={positions}
      jobGrades={jobGrades}
    />
  )
}
