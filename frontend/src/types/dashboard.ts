import type { CurrencyAmount } from '@/types/money'

export type { CurrencyAmount }

export interface HrSummary {
  activeEmployees: number
  onLeaveEmployees: number
  pendingLeaveRequests: number
}

export interface FinanceSummary {
  unpaidInvoices: number
  unpaidAmounts: CurrencyAmount[]
  draftJournalEntries: number
  baseCurrency: string
  // 미지급 금액의 기준통화 환산 합계(base_amount 산정분만). 산정분 없으면 null.
  unpaidBaseTotal: number | null
}

export interface InventorySummary {
  activeItems: number
  lowStockItems: number
  draftMovements: number
}

export interface CrmSummary {
  openOpportunities: number
  openOpportunityAmounts: CurrencyAmount[]
  newLeads: number
  openActivities: number
  baseCurrency: string
  // 진행중 파이프라인 금액의 기준통화 환산 합계(base_amount 산정분만). 산정분 없으면 null.
  openOpportunityBaseTotal: number | null
}
