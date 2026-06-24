import { apiGet } from '@/lib/api'
import type { Department } from '@/types/hr'
import DepartmentsClient from './departments-client'

export const metadata = { title: '부서 관리 | ERP' }

export default async function DepartmentsPage() {
  const departments = await apiGet<Department[]>('/api/hr/departments')
  return <DepartmentsClient departments={departments} />
}
