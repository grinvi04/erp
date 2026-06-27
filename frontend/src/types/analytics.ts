import type { CurrencyAmount } from '@/types/money'

export interface PipelineDistributionResponse {
  stageId: number
  stageName: string
  stageOrder: number
  count: number
  amounts: CurrencyAmount[]
  // 단계별 기준통화 환산 합계(base_amount 산정분만). 산정분 없으면 null.
  baseTotal: number | null
}

// 파이프라인 분포 — 단계별 통화 분리 + 기준통화 합계
export interface PipelineAnalyticsResponse {
  baseCurrency: string
  stages: PipelineDistributionResponse[]
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

// 월별 매입 인보이스 추이 — 통화별 시리즈 + 기준통화 환산 합계 시리즈
export interface MonthlyInvoiceAnalyticsResponse {
  baseCurrency: string
  byCurrency: MonthlyInvoiceByCurrencyResponse[]
  // 모든 통화를 기준통화로 합산한 월별 시리즈(산정분만). 산정분 없으면 빈 배열.
  baseMonthlyTotals: MonthlyInvoiceResponse[]
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

// Inventory analytics
export interface CategoryItemCountResponse {
  categoryId: number
  categoryName: string
  count: number
}

export interface WarehouseStockResponse {
  warehouseId: number
  warehouseName: string
  totalQty: number
  totalValue: number
}

export type MovementType = 'RECEIPT' | 'ISSUE' | 'TRANSFER' | 'ADJUSTMENT' | 'RETURN'

export interface MovementTypeCountResponse {
  movementType: MovementType
  count: number
}

export interface MonthlyMovementResponse {
  month: number
  count: number
  totalQty: number
}

export interface MonthlyMovementByTypeResponse {
  movementType: MovementType
  months: MonthlyMovementResponse[]
}

export interface LowStockItemResponse {
  sku: string
  name: string
  categoryName: string | null
  currentQty: number
  reorderPoint: number
}
