export type AccountType = 'PROSPECT' | 'CUSTOMER' | 'PARTNER' | 'COMPETITOR';
export type LeadStatus = 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'CONVERTED' | 'DISQUALIFIED';
export type ActivityType = 'CALL' | 'EMAIL' | 'MEETING' | 'TASK' | 'NOTE';
export type ActivityStatus = 'OPEN' | 'COMPLETED' | 'CANCELLED';

export interface CrmAccount {
  id: number;
  code: string;
  name: string;
  businessNo: string | null;
  industry: string | null;
  website: string | null;
  phone: string | null;
  address: string | null;
  employeeCount: number | null;
  annualRevenue: number | null;
  accountType: AccountType;
  ownerId: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Contact {
  id: number;
  accountId: number;
  accountName: string;
  lastName: string;
  firstName: string;
  title: string | null;
  department: string | null;
  email: string | null;
  phone: string | null;
  mobile: string | null;
  isPrimary: boolean;
  createdAt: string;
  version: number;
}

export interface Lead {
  id: number;
  lastName: string;
  firstName: string;
  company: string | null;
  title: string | null;
  email: string | null;
  phone: string | null;
  source: string | null;
  status: LeadStatus;
  ownerId: string;
  convertedAccountId: number | null;
  convertedOpportunityId: number | null;
  convertedAt: string | null;
  note: string | null;
  createdAt: string;
  version: number;
}

export interface PipelineStage {
  id: number;
  name: string;
  stageOrder: number;
  probability: number;
  isClosedWon: boolean;
  isClosedLost: boolean;
}

export interface Opportunity {
  id: number;
  accountId: number;
  accountName: string;
  name: string;
  stageId: number;
  stageName: string;
  amount: number | null;
  currency: string;
  closeDate: string | null;
  probability: number;
  ownerId: string;
  source: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Activity {
  id: number;
  activityType: ActivityType;
  subject: string;
  accountId: number | null;
  accountName: string | null;
  contactId: number | null;
  contactName: string | null;
  opportunityId: number | null;
  opportunityName: string | null;
  ownerId: string;
  dueDate: string | null;
  completedAt: string | null;
  status: ActivityStatus;
  description: string | null;
  createdAt: string;
}
