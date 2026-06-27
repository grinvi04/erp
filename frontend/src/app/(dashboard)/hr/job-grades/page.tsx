import { apiGet } from '@/lib/api'
import type { JobGrade } from '@/types/hr'
import JobGradesClient from './job-grades-client'

export const metadata = { title: '직급 관리 | ERP' }

export default async function JobGradesPage() {
  const jobGrades = await apiGet<JobGrade[]>('/api/hr/job-grades')
  return <JobGradesClient jobGrades={Array.isArray(jobGrades) ? jobGrades : []} />
}
