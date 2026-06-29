import { apiGet, apiGetPage } from '@/lib/api'
import type {
  Account,
  DepreciationAccounts,
  FiscalPeriod,
  FiscalYear,
  FixedAsset,
} from '@/types/finance'
import type { PageResponse } from '@/types/api'
import FixedAssetsClient from './fixed-assets-client'

export const metadata = { title: '고정자산 | ERP' }

export default async function FixedAssetsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const [data, accounts, depreciationAccounts, fiscalYears] = await Promise.all([
    apiGetPage<FixedAsset>(`/api/finance/fixed-assets?page=${page}&size=${size}`),
    apiGet<Account[]>('/api/finance/accounts'),
    apiGet<DepreciationAccounts>('/api/finance/fixed-assets/depreciation-accounts'),
    apiGet<FiscalYear[]>('/api/finance/fiscal-years'),
  ])

  // 상각 실행 대상 회계기간 — 연도별 기간을 모아 OPEN만 선택지로 제공.
  const periodLists = await Promise.all(
    fiscalYears.map((fy) => apiGet<FiscalPeriod[]>(`/api/finance/fiscal-years/${fy.id}/periods`)),
  )
  const periods = periodLists.flat()

  return (
    <FixedAssetsClient
      data={data as PageResponse<FixedAsset>}
      accounts={accounts}
      depreciationAccounts={depreciationAccounts}
      periods={periods}
    />
  )
}
