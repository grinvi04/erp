import { apiGet } from '@/lib/api'
import type { VatReturn } from '@/types/finance'
import VatReturnClient from './vat-return-client'

export const metadata = { title: '부가세신고 | ERP' }

/** 지정 분기(없으면 현재 분기)의 신고기간 from~to(YYYY-MM-DD)를 산출. */
function resolvePeriod(spFrom?: string, spTo?: string): { from: string; to: string } {
  if (spFrom && spTo) return { from: spFrom, to: spTo }
  const now = new Date()
  const year = now.getFullYear()
  const q = Math.floor(now.getMonth() / 3) // 0~3
  const startMonth = q * 3 + 1
  const endMonth = startMonth + 2
  const pad = (n: number) => String(n).padStart(2, '0')
  const lastDay = new Date(year, endMonth, 0).getDate()
  return { from: `${year}-${pad(startMonth)}-01`, to: `${year}-${pad(endMonth)}-${pad(lastDay)}` }
}

export default async function VatReturnPage(props: {
  searchParams: Promise<{ from?: string; to?: string }>
}) {
  const sp = await props.searchParams
  const { from, to } = resolvePeriod(sp.from?.trim(), sp.to?.trim())
  const data = await apiGet<VatReturn>(
    `/api/finance/vat-return?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
  )
  return <VatReturnClient data={data} from={from} to={to} />
}
