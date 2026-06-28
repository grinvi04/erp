'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { LocationType } from '@/types/inventory'

export async function createLocation(data: {
  warehouseId: number
  code: string
  name: string
  parentId: number | null
  locationType: LocationType
}): Promise<void> {
  await apiPost('/api/inventory/locations', data)
  revalidatePath('/inventory/locations')
}

// LocationUpdateRequest는 version(@NotNull)을 요구한다.
export async function updateLocation(
  id: number,
  data: {
    version: number
    name: string
    parentId: number | null
    locationType: LocationType
  },
): Promise<void> {
  await apiPut(`/api/inventory/locations/${id}`, data)
  revalidatePath('/inventory/locations')
}

export async function deactivateLocation(id: number): Promise<void> {
  await apiDelete(`/api/inventory/locations/${id}`)
  revalidatePath('/inventory/locations')
}
