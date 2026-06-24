'use server'
import { apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { JournalEntry, JournalEntryType } from '@/types/finance'

const PATH = '/finance/journal-entries'

export interface JournalLineInput {
  accountId: number
  debitAmount: number
  creditAmount: number
  description: string | null
  departmentId: number | null
}

export async function createJournalEntry(data: {
  entryDate: string
  fiscalPeriodId: number
  description: string
  entryType: JournalEntryType
  currency: string
  lines: JournalLineInput[]
}): Promise<void> {
  await apiPost<JournalEntry>('/api/finance/journal-entries', data)
  revalidatePath(PATH)
}

export async function postJournalEntry(id: number): Promise<void> {
  await apiPost<JournalEntry>(`/api/finance/journal-entries/${id}/post`, {})
  revalidatePath(PATH)
}
