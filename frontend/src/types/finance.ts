export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE'
export type NormalBalance = 'DEBIT' | 'CREDIT'
export type JournalEntryType = 'MANUAL' | 'AP' | 'AR' | 'PAYROLL' | 'ADJUSTMENT'
export type JournalEntryStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'POSTED' | 'REVERSED'
export type ApInvoiceStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'PAID' | 'CANCELLED'
export type TaxType = 'TAXABLE' | 'ZERO_RATED' | 'EXEMPT'
export type ArInvoiceStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'PAID' | 'CANCELLED'
export type FiscalYearStatus = 'OPEN' | 'CLOSED'
export type FiscalPeriodStatus = 'OPEN' | 'CLOSED' | 'LOCKED'

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
  supplyAmount: number
  vatAmount: number
  taxType: TaxType
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
  representativeName: string | null
  address: string | null
  businessType: string | null
  businessItem: string | null
  version: number
}

export interface CompanyProfile {
  companyName: string | null
  businessNo: string | null
  representative: string | null
  address: string | null
  businessType: string | null
  businessItem: string | null
}

export interface ArInvoice {
  id: number
  invoiceNo: string
  customerId: number
  customerName: string
  invoiceDate: string
  dueDate: string
  supplyAmount: number
  vatAmount: number
  taxType: TaxType
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

export interface FxGainLossAccounts {
  fxGainAccountId: number | null
  fxLossAccountId: number | null
}

export interface VatAccounts {
  vatReceivableAccountId: number | null
  vatPayableAccountId: number | null
}

export interface ExchangeRate {
  id: number
  fromCurrency: string
  toCurrency: string
  effectiveDate: string
  rate: number
}

// 재무제표 — 백엔드 TrialBalanceResponse / IncomeStatementResponse / BalanceSheetResponse record와 1:1
export interface TrialBalanceRow {
  accountCode: string
  accountName: string
  debit: number
  credit: number
  balance: number
}

export interface TrialBalanceResponse {
  baseCurrency: string
  rows: TrialBalanceRow[]
  totalDebit: number
  totalCredit: number
  excludedEntryCount: number
}

export interface IncomeStatementLine {
  accountCode: string
  accountName: string
  amount: number
}

export interface IncomeStatementResponse {
  baseCurrency: string
  revenues: IncomeStatementLine[]
  totalRevenue: number
  expenses: IncomeStatementLine[]
  totalExpense: number
  netIncome: number
  excludedEntryCount: number
}

export interface BalanceSheetLine {
  accountCode: string
  accountName: string
  amount: number
}

export interface BalanceSheetResponse {
  baseCurrency: string
  assets: BalanceSheetLine[]
  totalAssets: number
  liabilities: BalanceSheetLine[]
  totalLiabilities: number
  equity: BalanceSheetLine[]
  totalEquity: number
  netIncome: number
  balanced: boolean
  excludedEntryCount: number
}
