'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { CostMethod } from '@/types/inventory'

export async function createItem(data: {
  sku: string
  name: string
  description: string | null
  categoryId: number | null
  uomId: number
  costMethod: CostMethod
  standardCost: number
  reorderPoint: number
  reorderQty: number
  minStock: number
  maxStock: number
  lotTracked: boolean
  serialTracked: boolean
}): Promise<void> {
  await apiPost('/api/inventory/items', data)
  revalidatePath('/inventory/items')
}

export async function updateItem(id: number, data: {
  name: string
  description: string | null
  categoryId: number | null
  uomId: number
  costMethod: CostMethod
  standardCost: number
  reorderPoint: number
  reorderQty: number
  minStock: number
  maxStock: number
  lotTracked: boolean
  serialTracked: boolean
}): Promise<void> {
  await apiPut(`/api/inventory/items/${id}`, data)
  revalidatePath('/inventory/items')
}

export async function deactivateItem(id: number): Promise<void> {
  await apiDelete(`/api/inventory/items/${id}`)
  revalidatePath('/inventory/items')
}
