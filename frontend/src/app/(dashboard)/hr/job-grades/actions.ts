'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { JobGrade } from '@/types/hr'

const PATH = '/hr/job-grades'

export async function createJobGrade(data: {
  code: string
  name: string
  gradeOrder: number
  minSalary: number | null
  maxSalary: number | null
}): Promise<void> {
  await apiPost<JobGrade>('/api/hr/job-grades', data)
  revalidatePath(PATH)
}

export async function updateJobGrade(
  id: number,
  data: {
    name: string
    gradeOrder: number
    minSalary: number | null
    maxSalary: number | null
    version: number
  },
): Promise<void> {
  await apiPut<JobGrade>(`/api/hr/job-grades/${id}`, data)
  revalidatePath(PATH)
}

export async function deleteJobGrade(id: number): Promise<void> {
  await apiDelete(`/api/hr/job-grades/${id}`)
  revalidatePath(PATH)
}
