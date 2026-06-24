'use server'
import { apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { LeaveRequest } from '@/types/hr'

const PATH = '/hr/leave-requests'

export async function createLeaveRequest(data: {
  employeeId: number
  leavePolicyId: number
  startDate: string
  endDate: string
  requestedDays: number
  reason: string | null
}): Promise<void> {
  await apiPost<LeaveRequest>('/api/hr/leave-requests', data)
  revalidatePath(PATH)
}

export async function approveLeaveRequest(id: number, comment: string): Promise<void> {
  await apiPost<LeaveRequest>(`/api/hr/leave-requests/${id}/approve`, { comment })
  revalidatePath(PATH)
}

export async function rejectLeaveRequest(id: number, comment: string): Promise<void> {
  await apiPost<LeaveRequest>(`/api/hr/leave-requests/${id}/reject`, { comment })
  revalidatePath(PATH)
}
