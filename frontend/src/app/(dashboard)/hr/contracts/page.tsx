import { apiGet, apiGetPage } from '@/lib/api'
import type { Employee, Position, JobGrade } from '@/types/hr'
import type { PageResponse } from '@/types/api'
import ContractsClient from './contracts-client'

export const metadata = { title: '근로 계약 | ERP' }

export default async function ContractsPage() {
  const [employeePage, positions, jobGrades] = await Promise.all([
    apiGetPage<Employee>('/api/hr/employees?status=ACTIVE&page=0&size=1000'),
    apiGet<Position[]>('/api/hr/positions'),
    apiGet<JobGrade[]>('/api/hr/job-grades'),
  ])

  return (
    <ContractsClient
      employees={(employeePage as PageResponse<Employee>).content}
      positions={Array.isArray(positions) ? positions : []}
      jobGrades={Array.isArray(jobGrades) ? jobGrades : []}
    />
  )
}
