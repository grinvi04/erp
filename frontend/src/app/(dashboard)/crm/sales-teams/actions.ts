'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

export async function createSalesTeam(data: { code: string; name: string }): Promise<void> {
  await apiPost('/api/crm/sales-teams', data)
  revalidatePath('/crm/sales-teams')
}

export async function updateSalesTeam(
  id: number,
  data: { name: string; version: number },
): Promise<void> {
  await apiPut(`/api/crm/sales-teams/${id}`, data)
  revalidatePath('/crm/sales-teams')
}

export async function deleteSalesTeam(id: number): Promise<void> {
  await apiDelete(`/api/crm/sales-teams/${id}`)
  revalidatePath('/crm/sales-teams')
}

export async function addSalesTeamMember(id: number, userId: string): Promise<void> {
  await apiPost(`/api/crm/sales-teams/${id}/members`, { userId })
  revalidatePath('/crm/sales-teams')
}

export async function removeSalesTeamMember(id: number, userId: string): Promise<void> {
  await apiDelete(`/api/crm/sales-teams/${id}/members/${encodeURIComponent(userId)}`)
  revalidatePath('/crm/sales-teams')
}
