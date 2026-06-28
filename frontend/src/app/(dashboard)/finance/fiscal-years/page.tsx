import { apiGet, safeGetArray } from '@/lib/api'
import type { FiscalYear, FiscalPeriod } from '@/types/finance'
import FiscalYearsClient from './fiscal-years-client'

export const metadata = { title: '회계기간 | ERP' }

export default async function FiscalYearsPage() {
  const years = await apiGet<FiscalYear[]>('/api/finance/fiscal-years')
  const periodLists = await Promise.all(
    years.map((y) => safeGetArray<FiscalPeriod>(`/api/finance/fiscal-years/${y.id}/periods`)),
  )
  const periods = periodLists.flat()
  return <FiscalYearsClient years={years} periods={periods} />
}
