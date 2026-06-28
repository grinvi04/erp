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

export async function submitJournalEntry(id: number): Promise<void> {
  await apiPost<JournalEntry>(`/api/finance/journal-entries/${id}/submit`, {})
  revalidatePath(PATH)
}

export async function approveJournalEntry(id: number): Promise<void> {
  await apiPost<JournalEntry>(`/api/finance/journal-entries/${id}/approve`, {})
  revalidatePath(PATH)
}

// 철회: 상신자 본인이 결재 대기 전표를 DRAFT로 되돌린다(본인 검증은 서버가 최종 수행).
export async function withdrawJournalEntry(id: number): Promise<void> {
  await apiPost<JournalEntry>(`/api/finance/journal-entries/${id}/withdraw`, {})
  revalidatePath(PATH)
}

// 반려: 승인권자가 결재 대기 전표를 사유와 함께 DRAFT로 되돌린다(권한·직무분리는 서버가 최종 수행).
export async function rejectJournalEntry(id: number, comment: string): Promise<void> {
  await apiPost<JournalEntry>(`/api/finance/journal-entries/${id}/reject`, { comment })
  revalidatePath(PATH)
}

// 역분개: 승인권자가 전기 완료 전표를 차/대 반대인 새 전표로 상쇄한다(원 전표는 REVERSED 표시).
export async function reverseJournalEntry(id: number): Promise<void> {
  await apiPost<JournalEntry>(`/api/finance/journal-entries/${id}/reverse`, {})
  revalidatePath(PATH)
}
