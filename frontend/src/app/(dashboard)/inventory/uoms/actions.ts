'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

// 백엔드 UpdateRequest는 code·name 모두 @NotBlank이므로 code를 함께 전송한다. 수정은 version도 동봉(낙관적 잠금).
export async function createUom(data: { code: string; name: string }): Promise<void> {
  await apiPost('/api/inventory/uoms', data)
  revalidatePath('/inventory/uoms')
}

export async function updateUom(
  id: number,
  data: { code: string; name: string; version: number },
): Promise<void> {
  await apiPut(`/api/inventory/uoms/${id}`, data)
  revalidatePath('/inventory/uoms')
}

export async function deleteUom(id: number): Promise<void> {
  await apiDelete(`/api/inventory/uoms/${id}`)
  revalidatePath('/inventory/uoms')
}
