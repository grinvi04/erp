'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

// 백엔드 UpdateRequest는 code·name @NotBlank이므로 code를 함께 전송한다. 수정은 version도 동봉(낙관적 잠금).
export async function createItemCategory(data: {
  code: string
  name: string
  parentId: number | null
}): Promise<void> {
  await apiPost('/api/inventory/item-categories', data)
  revalidatePath('/inventory/item-categories')
}

export async function updateItemCategory(
  id: number,
  data: {
    code: string
    name: string
    parentId: number | null
    version: number
  },
): Promise<void> {
  await apiPut(`/api/inventory/item-categories/${id}`, data)
  revalidatePath('/inventory/item-categories')
}

export async function deleteItemCategory(id: number): Promise<void> {
  await apiDelete(`/api/inventory/item-categories/${id}`)
  revalidatePath('/inventory/item-categories')
}
