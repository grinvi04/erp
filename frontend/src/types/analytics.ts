import type { CurrencyAmount } from '@/types/money'

export interface PipelineDistributionResponse {
  stageId: number
  stageName: string
  stageOrder: number
  count: number
  amounts: CurrencyAmount[]
}

export interface LeadStatusCountResponse {
  status: 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'CONVERTED' | 'DISQUALIFIED'
  count: number
}

export interface MonthlyInvoiceResponse {
  month: number
  count: number
  totalAmount: number
}

export interface MonthlyInvoiceByCurrencyResponse {
  currency: string
  months: MonthlyInvoiceResponse[]
}

// HR analytics
export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED'

export interface EmployeeStatusCountResponse {
  status: EmployeeStatus
  count: number
}

export interface DepartmentHeadcountResponse {
  departmentId: number
  departmentName: string
  count: number
}

export interface PositionHeadcountResponse {
  positionId: number
  positionName: string
  count: number
}

export type EmploymentType = 'REGULAR' | 'CONTRACT' | 'PART_TIME' | 'INTERN' | 'DISPATCH'

export interface EmploymentTypeCountResponse {
  employmentType: EmploymentType
  count: number
}

export interface MonthlyHiresTerminationsResponse {
  month: number
  hires: number
  terminations: number
}

export type LeaveType = 'ANNUAL' | 'SICK' | 'PARENTAL' | 'BEREAVEMENT' | 'UNPAID' | 'COMPENSATORY'

export interface LeaveTypeStatResponse {
  leaveType: LeaveType
  count: number
  totalDays: number
}
