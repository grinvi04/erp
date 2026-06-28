import { apiGet, apiGetPage } from '@/lib/api'
import type { FiscalYear, FiscalPeriod, JournalEntry, Account } from '@/types/finance'
import type { PageResponse } from '@/types/api'
import JournalEntriesClient from './journal-entries-client'

export const metadata = { title: '전표 | ERP' }

export default async function JournalEntriesPage(props: {
  searchParams: Promise<{
    fiscalYearId?: string
    fiscalPeriodId?: string
    page?: string
    size?: string
  }>
}) {
  const sp = await props.searchParams
  const fiscalYearId = sp.fiscalYearId ? Number(sp.fiscalYearId) : null
  const fiscalPeriodId = sp.fiscalPeriodId ? Number(sp.fiscalPeriodId) : null
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [fiscalYears, periods, entries, accounts] = await Promise.all([
    apiGet<FiscalYear[]>('/api/finance/fiscal-years'),
    fiscalYearId != null
      ? apiGet<FiscalPeriod[]>(`/api/finance/fiscal-years/${fiscalYearId}/periods`)
      : Promise.resolve([] as FiscalPeriod[]),
    fiscalPeriodId != null
      ? apiGetPage<JournalEntry>(
          `/api/finance/journal-entries?fiscalPeriodId=${fiscalPeriodId}&page=${page}&size=${size}`,
        )
      : Promise.resolve(null),
    fiscalPeriodId != null
      ? apiGet<Account[]>('/api/finance/accounts')
      : Promise.resolve([] as Account[]),
  ])

  return (
    <JournalEntriesClient
      fiscalYears={fiscalYears}
      selectedYearId={fiscalYearId}
      periods={periods}
      selectedPeriodId={fiscalPeriodId}
      entries={entries as PageResponse<JournalEntry> | null}
      accounts={accounts}
    />
  )
}
