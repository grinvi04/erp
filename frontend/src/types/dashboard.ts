export interface HrSummary {
  activeEmployees: number
  onLeaveEmployees: number
  pendingLeaveRequests: number
}

export interface FinanceSummary {
  unpaidInvoices: number
  unpaidAmount: number
  draftJournalEntries: number
}

export interface InventorySummary {
  activeItems: number
  lowStockItems: number
  draftMovements: number
}

export interface CrmSummary {
  openOpportunities: number
  openOpportunityAmount: number
  newLeads: number
  openActivities: number
}
