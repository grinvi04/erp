'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Position } from '@/types/hr'

const PATH = '/hr/positions'

export async function createPosition(data: {
  code: string
  name: string
  levelOrder: number
}): Promise<void> {
  await apiPost<Position>('/api/hr/positions', data)
  revalidatePath(PATH)
}

export async function updatePosition(
  id: number,
  data: { name: string; levelOrder: number; version: number }
): Promise<void> {
  await apiPut<Position>(`/api/hr/positions/${id}`, data)
  revalidatePath(PATH)
}

export async function deletePosition(id: number): Promise<void> {
  await apiDelete(`/api/hr/positions/${id}`)
  revalidatePath(PATH)
}
