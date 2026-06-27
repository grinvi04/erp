export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE'
export type NormalBalance = 'DEBIT' | 'CREDIT'
export type JournalEntryType = 'MANUAL' | 'AP' | 'AR' | 'PAYROLL' | 'ADJUSTMENT'
export type JournalEntryStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'POSTED' | 'REVERSED'
export type ApInvoiceStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'PAID' | 'CANCELLED'
export type ArInvoiceStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'PAID' | 'CANCELLED'
export type FiscalYearStatus = 'OPEN' | 'CLOSED'
export type FiscalPeriodStatus = 'OPEN' | 'CLOSED'

export interface Account {
  id: number
  code: string
  name: string
  accountType: AccountType
  normalBalance: NormalBalance
  parentId: number | null
  parentCode: string | null
  isSummary: boolean
  isActive: boolean
  version: number
}

export interface Vendor {
  id: number
  code: string
  name: string
  businessNo: string | null
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  paymentTerms: number
  isActive: boolean
  payablesAccountId: number | null
  version: number
}

export interface JournalLine {
  id: number
  lineNo: number
  accountId: number
  accountCode: string
  accountName: string
  debitAmount: number
  creditAmount: number
  description: string | null
  departmentId: number | null
}

export interface JournalEntry {
  id: number
  entryNo: string
  entryDate: string
  fiscalPeriodId: number
  fiscalPeriodNumber: number
  description: string
  entryType: JournalEntryType
  status: JournalEntryStatus
  totalDebit: number
  totalCredit: number
  currency: string
  referenceType: string | null
  referenceId: number | null
  postedAt: string | null
  postedBy: string | null
}

export interface ApInvoice {
  id: number
  invoiceNo: string
  vendorId: number
  vendorName: string
  invoiceDate: string
  dueDate: string
  totalAmount: number
  paidAmount: number
  outstandingAmount: number
  currency: string
  status: ApInvoiceStatus
  journalEntryId: number | null
  approvalRequestId: number | null
  note: string | null
}

export interface Customer {
  id: number
  code: string
  name: string
  businessNo: string | null
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  paymentTerms: number
  isActive: boolean
  receivablesAccountId: number | null
  version: number
}

export interface ArInvoice {
  id: number
  invoiceNo: string
  customerId: number
  customerName: string
  invoiceDate: string
  dueDate: string
  totalAmount: number
  paidAmount: number
  outstandingAmount: number
  currency: string
  status: ArInvoiceStatus
  journalEntryId: number | null
  approvalRequestId: number | null
  note: string | null
}

export interface FiscalYear {
  id: number
  year: number
  startDate: string
  endDate: string
  status: FiscalYearStatus
}

export interface FiscalPeriod {
  id: number
  fiscalYearId: number
  periodNumber: number
  startDate: string
  endDate: string
  status: FiscalPeriodStatus
}

export interface BaseCurrency {
  baseCurrency: string
}

export interface ExchangeRate {
  id: number
  fromCurrency: string
  toCurrency: string
  effectiveDate: string
  rate: number
}
