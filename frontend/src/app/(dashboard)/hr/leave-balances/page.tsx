import { apiGetPage } from '@/lib/api'
import type { Employee } from '@/types/hr'
import type { PageResponse } from '@/types/api'
import LeaveBalancesClient from './leave-balances-client'

export const metadata = { title: '휴가 잔여 | ERP' }

export default async function LeaveBalancesPage() {
  const employeePage = await apiGetPage<Employee>(
    '/api/hr/employees?status=ACTIVE&page=0&size=1000',
  )
  return <LeaveBalancesClient employees={(employeePage as PageResponse<Employee>).content} />
}
