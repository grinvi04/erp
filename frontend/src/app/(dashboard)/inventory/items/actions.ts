'use server'
import { apiPost, apiPut, apiDelete, apiPostForm, apiGetRaw } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { CostMethod } from '@/types/inventory'
import type { BulkImportResult } from '@/types/api'

const ITEMS_PATH = '/inventory/items'

export async function importItemsCsv(form: FormData): Promise<BulkImportResult> {
  const result = await apiPostForm<BulkImportResult>('/api/inventory/items/import', form)
  revalidatePath(ITEMS_PATH)
  return result
}

export async function getItemTemplate(): Promise<string> {
  const res = await apiGetRaw('/api/inventory/items/import/template')
  if (!res.ok) throw new Error('템플릿을 받을 수 없습니다')
  return res.text()
}

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

export async function updateItem(
  id: number,
  data: {
    version: number
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
  },
): Promise<void> {
  await apiPut(`/api/inventory/items/${id}`, data)
  revalidatePath('/inventory/items')
}

export async function deactivateItem(id: number): Promise<void> {
  await apiDelete(`/api/inventory/items/${id}`)
  revalidatePath('/inventory/items')
}
