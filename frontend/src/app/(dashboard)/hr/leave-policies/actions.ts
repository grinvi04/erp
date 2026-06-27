'use server'
import { apiPost, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { LeavePolicy, LeaveType } from '@/types/hr'

const PATH = '/hr/leave-policies'

export async function createLeavePolicy(data: {
  code: string
  name: string
  leaveType: LeaveType
  annualDays: number
  carryOverDays: number
  requiresApproval: boolean
  minNoticeDays: number
}): Promise<void> {
  await apiPost<LeavePolicy>('/api/hr/leave-policies', data)
  revalidatePath(PATH)
}

export async function deleteLeavePolicy(id: number): Promise<void> {
  await apiDelete(`/api/hr/leave-policies/${id}`)
  revalidatePath(PATH)
}
