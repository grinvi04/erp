'use server'
import { apiPost, apiPut } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { Employee, EmploymentType, Gender } from '@/types/hr'

const PATH = '/hr/employees'

export async function createEmployee(data: {
  employeeNo: string
  lastName: string
  firstName: string
  dateOfBirth: string | null
  gender: Gender | null
  nationalId: string | null
  phone: string | null
  personalEmail: string | null
  departmentId: number
  positionId: number
  jobGradeId: number | null
  hireDate: string
  employmentType: EmploymentType
  workEmail: string
  baseSalary: number | null
  managerId: number | null
}): Promise<void> {
  await apiPost<Employee>('/api/hr/employees', data)
  revalidatePath(PATH)
}

export async function updateEmployee(
  id: number,
  data: {
    lastName: string | null
    firstName: string | null
    phone: string | null
    personalEmail: string | null
    workEmail: string | null
    baseSalary: number | null
    managerId: number | null
    userId: string | null
    version: number
  },
): Promise<void> {
  await apiPut<Employee>(`/api/hr/employees/${id}`, data)
  revalidatePath(PATH)
}

export async function transferEmployee(
  id: number,
  data: { departmentId: number; positionId: number },
): Promise<void> {
  await apiPost<Employee>(`/api/hr/employees/${id}/transfer`, data)
  revalidatePath(PATH)
}

export async function promoteEmployee(
  id: number,
  data: { positionId: number; jobGradeId: number | null; baseSalary: number | null },
): Promise<void> {
  await apiPost<Employee>(`/api/hr/employees/${id}/promote`, data)
  revalidatePath(PATH)
}

export async function terminateEmployee(
  id: number,
  data: { terminationDate: string },
): Promise<void> {
  await apiPost<Employee>(`/api/hr/employees/${id}/terminate`, data)
  revalidatePath(PATH)
}

export async function setEmployeeOnLeave(id: number): Promise<void> {
  await apiPost<Employee>(`/api/hr/employees/${id}/on-leave`, {})
  revalidatePath(PATH)
}

export async function returnEmployeeFromLeave(id: number): Promise<void> {
  await apiPost<Employee>(`/api/hr/employees/${id}/return-from-leave`, {})
  revalidatePath(PATH)
}
