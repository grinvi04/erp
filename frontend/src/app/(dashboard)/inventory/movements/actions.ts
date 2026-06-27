'use server'
import { apiPost, apiGet } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Location, MovementType } from '@/types/inventory'

export interface MovementLineInput {
  itemId: number
  fromLocationId: number | null
  toLocationId: number | null
  lotNo: string | null
  serialNo: string | null
  qty: number
  unitCost: number
}

export async function createMovement(data: {
  movementType: MovementType
  movementDate: string
  referenceType: string | null
  note: string | null
  lines: MovementLineInput[]
}): Promise<void> {
  await apiPost('/api/inventory/movements', data)
  revalidatePath('/inventory/movements')
}

export async function confirmMovement(id: number): Promise<void> {
  await apiPost(`/api/inventory/movements/${id}/confirm`, {})
  revalidatePath('/inventory/movements')
}

export async function submitMovement(id: number): Promise<void> {
  await apiPost(`/api/inventory/movements/${id}/submit`, {})
  revalidatePath('/inventory/movements')
}

export async function approveMovement(id: number): Promise<void> {
  await apiPost(`/api/inventory/movements/${id}/approve`, {})
  revalidatePath('/inventory/movements')
}

export async function cancelMovement(id: number): Promise<void> {
  await apiPost(`/api/inventory/movements/${id}/cancel`, {})
  revalidatePath('/inventory/movements')
}

// 철회: 상신자 본인이 결재 대기 조정 이동을 DRAFT로 되돌린다(본인 검증은 서버가 최종 수행).
export async function withdrawMovement(id: number): Promise<void> {
  await apiPost(`/api/inventory/movements/${id}/withdraw`, {})
  revalidatePath('/inventory/movements')
}

export async function getLocationsByWarehouse(warehouseId: number): Promise<Location[]> {
  return apiGet<Location[]>(`/api/inventory/locations?warehouseId=${warehouseId}`)
}
