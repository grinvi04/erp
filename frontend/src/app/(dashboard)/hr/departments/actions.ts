'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Department } from '@/types/hr'

export async function createDepartment(data: {
  code: string
  name: string
  parentId: number | null
  sortOrder: number
}): Promise<void> {
  await apiPost<Department>('/api/hr/departments', data)
  revalidatePath('/hr/departments')
}

export async function updateDepartment(
  id: number,
  data: {
    name: string
    sortOrder: number
    parentId: number | null
    active: boolean
    version: number
  },
): Promise<void> {
  await apiPut<Department>(`/api/hr/departments/${id}`, data)
  revalidatePath('/hr/departments')
}

export async function deleteDepartment(id: number): Promise<void> {
  await apiDelete(`/api/hr/departments/${id}`)
  revalidatePath('/hr/departments')
}
