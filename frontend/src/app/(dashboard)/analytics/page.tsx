import { apiGet } from '@/lib/api'
import { formatMoneyList } from '@/lib/money'
import type {
  PipelineDistributionResponse,
  LeadStatusCountResponse,
  MonthlyInvoiceByCurrencyResponse,
} from '@/types/analytics'

export const metadata = { title: '분석 | ERP' }

async function safeGetArray<T>(path: string): Promise<T[]> {
  try {
    const data = await apiGet<T[]>(path)
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

const LEAD_STATUS_LABELS: Record<string, string> = {
  NEW: '신규',
  CONTACTED: '접촉',
  QUALIFIED: '적격',
  CONVERTED: '전환',
  DISQUALIFIED: '불량',
}

const MONTH_LABELS = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']

function HorizontalBar({
  label,
  subLabel,
  pct,
  color = 'bg-blue-500',
}: {
  label: string
  subLabel: string
  pct: number
  color?: string
}) {
  return (
    <div className="mb-3">
      <div className="flex justify-between text-sm text-gray-700 mb-1">
        <span className="font-medium">{label}</span>
        <span className="text-gray-500">{subLabel}</span>
      </div>
      <div className="w-full bg-gray-100 rounded h-6">
        <div
          className={`${color} h-6 rounded transition-all`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">{title}</h2>
      {children}
    </div>
  )
}

export default async function AnalyticsPage() {
  const currentYear = new Date().getFullYear()

  const [pipeline, leads, monthly] = await Promise.all([
    safeGetArray<PipelineDistributionResponse>('/api/crm/analytics/pipeline'),
    safeGetArray<LeadStatusCountResponse>('/api/crm/analytics/leads-by-status'),
    safeGetArray<MonthlyInvoiceByCurrencyResponse>(`/api/finance/analytics/monthly-invoices?year=${currentYear}`),
  ])

  // Pipeline: scale by count
  const maxPipelineCount = Math.max(...pipeline.map((r) => r.count), 1)

  // Leads: scale by count
  const maxLeadCount = Math.max(...leads.map((r) => r.count), 1)

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">분석</h1>
        <p className="text-sm text-gray-500 mt-1">영업 파이프라인, 리드 현황, 매입 인보이스 추이</p>
      </div>

      <div className="space-y-6">
        {/* Pipeline Distribution */}
        <SectionCard title="영업 파이프라인 분포">
          {pipeline.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            pipeline.map((r) => (
              <HorizontalBar
                key={r.stageId}
                label={r.stageName}
                subLabel={`${r.count}건${r.amounts.length ? ' · ' + formatMoneyList(r.amounts) : ''}`}
                pct={maxPipelineCount === 0 ? 0 : Math.round((r.count / maxPipelineCount) * 100)}
                color="bg-blue-500"
              />
            ))
          )}
        </SectionCard>

        {/* Leads by Status */}
        <SectionCard title="리드 상태 분포">
          {leads.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            leads.map((r) => (
              <HorizontalBar
                key={r.status}
                label={LEAD_STATUS_LABELS[r.status] ?? r.status}
                subLabel={`${r.count}건`}
                pct={maxLeadCount === 0 ? 0 : Math.round((r.count / maxLeadCount) * 100)}
                color="bg-emerald-500"
              />
            ))
          )}
        </SectionCard>

        {/* Monthly Invoice Trend — 통화별 카드로 분리(막대 비교는 같은 통화 안에서만 의미) */}
        {monthly.length === 0 ? (
          <SectionCard title={`월별 매입 인보이스 추이 (${currentYear}년)`}>
            <p className="text-sm text-gray-400">데이터 없음</p>
          </SectionCard>
        ) : (
          monthly.map((series) => {
            // 막대 높이는 그 통화 내 최대 금액으로 스케일
            const maxAmount = Math.max(...series.months.map((m) => m.totalAmount), 1)
            return (
              <SectionCard
                key={series.currency}
                title={`월별 매입 인보이스 추이 (${currentYear}년) · ${series.currency}`}
              >
                <div className="flex items-end gap-2">
                  {series.months.map((r, idx) => {
                    const heightPct = maxAmount === 0 ? 0 : (r.totalAmount / maxAmount) * 100
                    return (
                      <div key={r.month} className="flex flex-col items-center flex-1">
                        {/* 막대 영역: 고정 높이(h-40)를 기준으로 % 높이가 해석되도록 한다. */}
                        <div className="relative group flex h-40 w-full items-end justify-center">
                          <div className="absolute bottom-full mb-1 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs rounded px-2 py-1 whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                            {r.count}건<br />{formatMoneyList([{ currency: series.currency, amount: r.totalAmount }])}
                          </div>
                          <div
                            className="bg-violet-500 rounded-t w-full"
                            style={{ height: `${heightPct}%`, minHeight: r.totalAmount > 0 ? '4px' : '0px' }}
                          />
                        </div>
                        <div className="text-xs text-gray-500 mt-1">{MONTH_LABELS[idx]}</div>
                        <div className="text-xs text-gray-600 font-medium">{r.count}</div>
                      </div>
                    )
                  })}
                </div>
              </SectionCard>
            )
          })
        )}
      </div>
    </div>
  )
}
