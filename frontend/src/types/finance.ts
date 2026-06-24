export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';
export type JournalStatus = 'DRAFT' | 'POSTED';

export interface Account {
  id: number;
  code: string;
  name: string;
  accountType: AccountType;
  parentId: number | null;
  isSummary: boolean;
  isActive: boolean;
  createdAt: string;
}

export interface JournalEntry {
  id: number;
  entryNo: string;
  entryDate: string;
  description: string;
  status: JournalStatus;
  totalDebit: number;
  totalCredit: number;
  createdBy: string;
  createdAt: string;
}

export interface Vendor {
  id: number;
  code: string;
  name: string;
  businessNo: string | null;
  email: string | null;
  phone: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface Invoice {
  id: number;
  invoiceNo: string;
  vendorId: number;
  vendorName: string;
  invoiceDate: string;
  dueDate: string;
  totalAmount: number;
  currency: string;
  paidAmount: number;
  status: 'OPEN' | 'PARTIAL' | 'PAID' | 'VOID';
  createdAt: string;
}
