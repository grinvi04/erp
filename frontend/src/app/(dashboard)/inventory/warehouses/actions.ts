'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

export async function createWarehouse(data: {
  code: string; name: string; address: string | null
}): Promise<void> {
  await apiPost('/api/inventory/warehouses', data)
  revalidatePath('/inventory/warehouses')
}

export async function updateWarehouse(id: number, data: {
  name: string; address: string | null
}): Promise<void> {
  await apiPut(`/api/inventory/warehouses/${id}`, data)
  revalidatePath('/inventory/warehouses')
}

export async function deactivateWarehouse(id: number): Promise<void> {
  await apiDelete(`/api/inventory/warehouses/${id}`)
  revalidatePath('/inventory/warehouses')
}
