'use server'
import { apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'
import type { FiscalYear, FiscalPeriod } from '@/types/finance'

const PATH = '/finance/fiscal-years'

const pad = (n: number) => String(n).padStart(2, '0')

/**
 * 회계연도 기간(달력 월) 자동 생성용 12개월 분할.
 * 백엔드는 기간을 자동 생성하지 않으므로, 연도 생성 후 월 단위로 createPeriod를 호출한다.
 * 각 기간은 [startDate, endDate] 범위 안의 달력 월에 정렬되며, 첫·마지막 기간은 연도 경계로 클램프한다.
 * (백엔드 검증: 기간 시작 >= 연도 시작, 기간 종료 <= 연도 종료)
 */
function monthlyPeriods(
  startDate: string,
  endDate: string,
): { periodNumber: number; startDate: string; endDate: string }[] {
  const [sy, sm] = startDate.split('-').map(Number)
  const [ey, em] = endDate.split('-').map(Number)
  const periods: { periodNumber: number; startDate: string; endDate: string }[] = []
  let y = sy
  let m = sm
  let n = 1
  while ((y < ey || (y === ey && m <= em)) && n <= 12) {
    const lastDay = new Date(Date.UTC(y, m, 0)).getUTCDate()
    const pStart = n === 1 ? startDate : `${y}-${pad(m)}-01`
    let pEnd = `${y}-${pad(m)}-${pad(lastDay)}`
    if (pEnd > endDate) pEnd = endDate
    periods.push({ periodNumber: n, startDate: pStart, endDate: pEnd })
    n += 1
    m += 1
    if (m > 12) {
      m = 1
      y += 1
    }
  }
  return periods
}

export async function createFiscalYear(data: {
  year: number
  startDate: string
  endDate: string
}): Promise<void> {
  const fy = await apiPost<FiscalYear>('/api/finance/fiscal-years', data)
  for (const p of monthlyPeriods(data.startDate, data.endDate)) {
    await apiPost<FiscalPeriod>(`/api/finance/fiscal-years/${fy.id}/periods`, p)
  }
  revalidatePath(PATH)
}

export async function closeFiscalYear(id: number): Promise<void> {
  await apiPost<FiscalYear>(`/api/finance/fiscal-years/${id}/close`, {})
  revalidatePath(PATH)
}

export async function closeFiscalPeriod(periodId: number): Promise<void> {
  await apiPost<FiscalPeriod>(`/api/finance/fiscal-years/periods/${periodId}/close`, {})
  revalidatePath(PATH)
}
