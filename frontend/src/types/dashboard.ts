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
}
