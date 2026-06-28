'use server'
import { apiGet, apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Contract, ContractType } from '@/types/hr'

const PATH = '/hr/contracts'

export async function fetchContracts(employeeId: number): Promise<Contract[]> {
  const contracts = await apiGet<Contract[]>(`/api/hr/employees/${employeeId}/contracts`)
  return Array.isArray(contracts) ? contracts : []
}

export async function createContract(
  employeeId: number,
  data: {
    contractType: ContractType
    startDate: string
    endDate: string | null
    baseSalary: number | null
    positionId: number
    jobGradeId: number | null
    note: string | null
  },
): Promise<void> {
  await apiPost<Contract>(`/api/hr/employees/${employeeId}/contracts`, data)
  revalidatePath(PATH)
}
