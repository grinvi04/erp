'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

// 백엔드 PUT은 ItemCategoryCreateRequest를 재사용(code·name @NotBlank)하므로 code를 함께 전송한다.
export async function createItemCategory(data: {
  code: string; name: string; parentId: number | null
}): Promise<void> {
  await apiPost('/api/inventory/item-categories', data)
  revalidatePath('/inventory/item-categories')
}

export async function updateItemCategory(id: number, data: {
  code: string; name: string; parentId: number | null
}): Promise<void> {
  await apiPut(`/api/inventory/item-categories/${id}`, data)
  revalidatePath('/inventory/item-categories')
}

export async function deleteItemCategory(id: number): Promise<void> {
  await apiDelete(`/api/inventory/item-categories/${id}`)
  revalidatePath('/inventory/item-categories')
}
