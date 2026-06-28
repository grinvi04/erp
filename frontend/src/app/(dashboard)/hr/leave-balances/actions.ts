'use server'
import { apiGet } from '@/lib/api'
import type { LeaveBalance } from '@/types/hr'

export async function fetchLeaveBalances(
  employeeId: number,
  year: number,
): Promise<LeaveBalance[]> {
  const balances = await apiGet<LeaveBalance[]>(
    `/api/hr/leave-balances?employeeId=${employeeId}&year=${year}`,
  )
  return Array.isArray(balances) ? balances : []
}
