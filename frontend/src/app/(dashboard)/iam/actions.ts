'use server'
import { apiPost, apiPut, apiDelete, apiGet } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { AccessProfile, DataScope, Role } from '@/types/iam'

const PATH = '/iam'

export async function createRole(data: {
  code: string
  name: string
  description: string | null
  permissions: string[]
}): Promise<void> {
  await apiPost<Role>('/api/iam/roles', data)
  revalidatePath(PATH)
}

export async function updateRole(id: number, data: {
  name: string
  description: string | null
  permissions: string[]
}): Promise<void> {
  await apiPut<Role>(`/api/iam/roles/${id}`, data)
  revalidatePath(PATH)
}

export async function deleteRole(id: number): Promise<void> {
  await apiDelete(`/api/iam/roles/${id}`)
  revalidatePath(PATH)
}

// --- 사용자 접근 관리 (동적 조회는 서버 액션이 데이터를 반환) ---

export async function getUserRoles(userId: string): Promise<Role[]> {
  return apiGet<Role[]>(`/api/iam/users/${encodeURIComponent(userId)}/roles`)
}

export async function getAccessProfile(userId: string): Promise<AccessProfile | null> {
  try {
    return await apiGet<AccessProfile>(`/api/iam/users/${encodeURIComponent(userId)}/access-profile`)
  } catch {
    return null // 미설정
  }
}

export async function assignRole(userId: string, roleId: number): Promise<void> {
  await apiPost<void>(`/api/iam/users/${encodeURIComponent(userId)}/roles/${roleId}`, {})
}

export async function unassignRole(userId: string, roleId: number): Promise<void> {
  await apiDelete(`/api/iam/users/${encodeURIComponent(userId)}/roles/${roleId}`)
}

export async function setAccessProfile(userId: string, data: {
  dataScope: DataScope
  departmentId: number | null
  approvalLimit: number | null
}): Promise<void> {
  await apiPut<AccessProfile>(`/api/iam/users/${encodeURIComponent(userId)}/access-profile`, data)
}
