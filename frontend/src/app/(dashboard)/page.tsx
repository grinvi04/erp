import { safeGet, safeGetArray } from '@/lib/api'
import { formatMoneyList, formatMoneyOne } from '@/lib/money'
import { PageHeader } from '@/components/ui/page-header'
import { StatCard } from '@/components/ui/stat-card'
import { ChartCard } from '@/components/ui/chart-card'
import { EmptyState } from '@/components/ui/empty-state'
import { MonthlyBarChart } from '@/components/charts/monthly-bar-chart'
import { DonutChart } from '@/components/charts/donut-chart'
import { Users, TrendingUp, TriangleAlert, Wallet, PackageX } from 'lucide-react'
import type { HrSummary, FinanceSummary, InventorySummary, CrmSummary } from '@/types/dashboard'
import type {
  MonthlyInvoiceAnalyticsResponse,
  PipelineAnalyticsResponse,
  MonthlyHiresTerminationsResponse,
  LowStockItemResponse,
} from '@/types/analytics'

export const metadata = { title: '대시보드 | ERP' }

const MONTH_LABELS = [
  '1월',
  '2월',
  '3월',
  '4월',
  '5월',
  '6월',
  '7월',
  '8월',
  '9월',
  '10월',
  '11월',
  '12월',
]

function fmtNum(n: number) {
  return n.toLocaleString('ko-KR')
}
// 금액 KPI — 기준통화 환산 합계를 헤드라인으로(균일한 한 줄), 통화가 여럿이면 내역을 sub로.
function moneyKpi(
  amounts: Parameters<typeof formatMoneyList>[0] | undefined,
  baseTotal: number | null | undefined,
  baseCurrency: string | undefined,
  hasAny: boolean,
): { value: string; sub?: string } {
  const list = amounts ?? []
  if (!hasAny || list.length === 0) return { value: '₩0' }
  if (baseTotal != null && baseCurrency) {
    return {
      value: formatMoneyOne(baseTotal, baseCurrency),
      sub: list.length > 1 ? formatMoneyList(list) : undefined,
    }
  }
  return { value: formatMoneyList(list) }
}

export default async function DashboardPage() {
  const year = new Date().getFullYear()
  const [hr, finance, inventory, crm, monthlyInv, pipeline, hiresTerms, lowStock] =
    await Promise.all([
      safeGet<HrSummary>('/api/hr/summary'),
      safeGet<FinanceSummary>('/api/finance/summary'),
      safeGet<InventorySummary>('/api/inventory/summary'),
      safeGet<CrmSummary>('/api/crm/summary'),
      safeGet<MonthlyInvoiceAnalyticsResponse>(
        `/api/finance/analytics/monthly-invoices?year=${year}`,
      ),
      safeGet<PipelineAnalyticsResponse>('/api/crm/analytics/pipeline'),
      safeGetArray<MonthlyHiresTerminationsResponse>(
        `/api/hr/analytics/hires-terminations?year=${year}`,
      ),
      safeGetArray<LowStockItemResponse>('/api/inventory/analytics/low-stock'),
    ])

  const lowStockCount = inventory?.lowStockItems ?? 0
  const invBase = monthlyInv?.baseMonthlyTotals ?? []
  const invBaseCurrency = monthlyInv?.baseCurrency ?? 'KRW'
  const stages = pipeline?.stages ?? []
  const unpaidKpi = moneyKpi(
    finance?.unpaidAmounts,
    finance?.unpaidBaseTotal,
    finance?.baseCurrency,
    !!finance?.unpaidInvoices,
  )
  const pipelineKpi = moneyKpi(
    crm?.openOpportunityAmounts,
    crm?.openOpportunityBaseTotal,
    crm?.baseCurrency,
    !!crm?.openOpportunities,
  )

  return (
    <div className="space-y-6 p-6">
      <PageHeader title="대시보드" description={`${year}년 핵심 지표 · 모듈별 요약`} />

      {/* 핵심 KPI */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="재직 직원"
          value={fmtNum(hr?.activeEmployees ?? 0)}
          icon={Users}
          tone="primary"
          href="/hr/employees"
        />
        <StatCard
          label="미지급 금액"
          value={unpaidKpi.value}
          sub={unpaidKpi.sub}
          icon={Wallet}
          tone="default"
          href="/finance/invoices"
        />
        <StatCard
          label="파이프라인 금액"
          value={pipelineKpi.value}
          sub={pipelineKpi.sub}
          icon={TrendingUp}
          tone="success"
          href="/crm/opportunities"
        />
        <StatCard
          label="재고 부족"
          value={fmtNum(lowStockCount)}
          icon={TriangleAlert}
          tone={lowStockCount > 0 ? 'warning' : 'default'}
          href="/inventory/stocks"
        />
      </div>

      {/* 차트 행 1 — 매입 추이 + 파이프라인 분포 */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <ChartCard
          title="월별 매입계산서 추이"
          description={`${year}년 · 기준통화(${invBaseCurrency}) 환산 합계`}
          href="/finance/invoices"
          className="lg:col-span-2"
        >
          {invBase.length === 0 ? (
            <EmptyState title="데이터가 없습니다" className="py-10" />
          ) : (
            <MonthlyBarChart
              data={invBase.map((r, i) => ({
                month: MONTH_LABELS[r.month - 1] ?? MONTH_LABELS[i],
                amount: r.totalAmount,
              }))}
              series={[{ key: 'amount', label: '매입액', color: 'var(--chart-1)' }]}
              valueFormat={{ kind: 'money', currency: invBaseCurrency }}
              height={260}
            />
          )}
        </ChartCard>
        <ChartCard
          title="영업 파이프라인"
          description="단계별 진행중 기회"
          href="/crm/opportunities"
        >
          {stages.length === 0 ? (
            <EmptyState title="데이터가 없습니다" className="py-10" />
          ) : (
            <DonutChart
              data={stages.map((s) => ({ label: s.stageName, value: s.count }))}
              valueFormat={{ kind: 'suffix', suffix: '건' }}
              centerLabel="진행중"
            />
          )}
        </ChartCard>
      </div>

      {/* 차트 행 2 — 입사/퇴사 + 저재고 */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ChartCard title="월별 입사/퇴사" description={`${year}년 인력 변동`} href="/hr/employees">
          {hiresTerms.length === 0 ? (
            <EmptyState title="데이터가 없습니다" className="py-10" />
          ) : (
            <MonthlyBarChart
              data={hiresTerms.map((r, i) => ({
                month: MONTH_LABELS[r.month - 1] ?? MONTH_LABELS[i],
                hires: r.hires,
                terminations: r.terminations,
              }))}
              series={[
                { key: 'hires', label: '입사', color: 'var(--chart-2)' },
                { key: 'terminations', label: '퇴사', color: 'var(--chart-4)' },
              ]}
              valueFormat={{ kind: 'suffix', suffix: '명' }}
              height={240}
            />
          )}
        </ChartCard>
        <ChartCard
          title="저재고 품목"
          description={
            lowStock.length > 0 ? `재주문점 이하 · ${lowStock.length}건` : '재주문점 이하'
          }
          href="/inventory/stocks"
        >
          {lowStock.length === 0 ? (
            <EmptyState icon={PackageX} title="저재고 품목이 없습니다" className="py-10" />
          ) : (
            <ul className="divide-y divide-border">
              {lowStock.slice(0, 6).map((it) => (
                <li key={it.sku} className="flex items-center justify-between gap-3 py-2.5">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-foreground">{it.name}</p>
                    <p className="font-mono text-xs text-muted-foreground">
                      {it.sku}
                      {it.categoryName ? ` · ${it.categoryName}` : ''}
                    </p>
                  </div>
                  <div className="shrink-0 text-right tabular-nums">
                    <span className="text-sm font-semibold text-destructive">
                      {fmtNum(it.currentQty)}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {' '}
                      / {fmtNum(it.reorderPoint)}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </ChartCard>
      </div>
    </div>
  )
}
