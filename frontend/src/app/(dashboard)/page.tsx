import Link from 'next/link'
import { safeGet } from '@/lib/api'
import { formatMoneyList, formatMoneyOne } from '@/lib/money'
import { cn } from '@/lib/utils'
import { PageHeader } from '@/components/ui/page-header'
import { StatCard } from '@/components/ui/stat-card'
import { ErrorState } from '@/components/ui/empty-state'
import {
  Users, BarChart3, Package, TrendingUp, ChevronRight, TriangleAlert, Wallet,
} from 'lucide-react'
import type {
  HrSummary, FinanceSummary, InventorySummary, CrmSummary,
} from '@/types/dashboard'

export const metadata = { title: '대시보드 | ERP' }

function fmtNum(n: number) {
  return n.toLocaleString('ko-KR')
}

interface Metric {
  label: string
  value: string
  sub?: string
  alert?: boolean
}

function baseTotalLabel(baseTotal: number | null | undefined, baseCurrency: string | undefined): string | undefined {
  if (baseTotal == null || !baseCurrency) return undefined
  return `≈ ${formatMoneyOne(baseTotal, baseCurrency)}`
}

function ModuleCard({
  title, href, icon: Icon, metrics, failed,
}: {
  title: string
  href: string
  icon: React.ElementType
  metrics: Metric[]
  failed: boolean
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-xs">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Icon className="h-[18px] w-[18px]" />
          </span>
          <h2 className="font-semibold text-foreground">{title}</h2>
        </div>
        <Link href={href} className="inline-flex items-center gap-0.5 text-sm font-medium text-primary hover:underline">
          바로가기<ChevronRight className="h-3.5 w-3.5" />
        </Link>
      </div>
      {failed ? (
        <ErrorState title="요약을 불러오지 못했습니다" description="백엔드 연결을 확인해 주세요." className="py-6" />
      ) : (
        <div className="grid grid-cols-3 gap-4">
          {metrics.map((m) => (
            <div key={m.label} className="min-w-0">
              <div className={cn('truncate text-lg font-semibold tabular-nums', m.alert ? 'text-warning' : 'text-foreground')}>
                {m.alert && <TriangleAlert className="mr-1 -mt-0.5 inline h-4 w-4" />}
                {m.value}
              </div>
              {m.sub && <div className="mt-0.5 truncate text-xs text-muted-foreground tabular-nums">{m.sub}</div>}
              <div className="mt-1 text-xs text-muted-foreground">{m.label}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default async function DashboardPage() {
  const [hr, finance, inventory, crm] = await Promise.all([
    safeGet<HrSummary>('/api/hr/summary'),
    safeGet<FinanceSummary>('/api/finance/summary'),
    safeGet<InventorySummary>('/api/inventory/summary'),
    safeGet<CrmSummary>('/api/crm/summary'),
  ])

  const lowStock = inventory?.lowStockItems ?? 0

  return (
    <div className="space-y-6 p-6">
      <PageHeader title="대시보드" description="모듈별 핵심 지표 요약" />

      {/* 핵심 KPI */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="재직 직원" value={fmtNum(hr?.activeEmployees ?? 0)}
          icon={Users} tone="primary" href="/hr/employees"
        />
        <StatCard
          label="미지급 금액"
          value={finance?.unpaidInvoices ? formatMoneyList(finance?.unpaidAmounts ?? []) : '₩0'}
          sub={baseTotalLabel(finance?.unpaidBaseTotal, finance?.baseCurrency)}
          icon={Wallet} tone="default" href="/finance/invoices"
        />
        <StatCard
          label="파이프라인 금액"
          value={crm?.openOpportunities ? formatMoneyList(crm?.openOpportunityAmounts ?? []) : '₩0'}
          sub={baseTotalLabel(crm?.openOpportunityBaseTotal, crm?.baseCurrency)}
          icon={TrendingUp} tone="success" href="/crm/opportunities"
        />
        <StatCard
          label="재고 부족" value={fmtNum(lowStock)}
          icon={TriangleAlert} tone={lowStock > 0 ? 'warning' : 'default'} href="/inventory/stocks"
        />
      </div>

      {/* 모듈 요약 */}
      <div>
        <h2 className="mb-3 text-sm font-semibold text-muted-foreground">모듈 요약</h2>
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <ModuleCard
            title="인사(HR)" href="/hr/employees" icon={Users} failed={hr === null}
            metrics={[
              { label: '재직 직원', value: fmtNum(hr?.activeEmployees ?? 0) },
              { label: '휴직', value: fmtNum(hr?.onLeaveEmployees ?? 0) },
              { label: '대기 휴가 신청', value: fmtNum(hr?.pendingLeaveRequests ?? 0), alert: (hr?.pendingLeaveRequests ?? 0) > 0 },
            ]}
          />
          <ModuleCard
            title="재무(Finance)" href="/finance/invoices" icon={BarChart3} failed={finance === null}
            metrics={[
              { label: '미지급 인보이스', value: fmtNum(finance?.unpaidInvoices ?? 0) },
              { label: '미지급 금액', value: formatMoneyList(finance?.unpaidAmounts ?? []), sub: baseTotalLabel(finance?.unpaidBaseTotal, finance?.baseCurrency) },
              { label: '임시 전표', value: fmtNum(finance?.draftJournalEntries ?? 0) },
            ]}
          />
          <ModuleCard
            title="재고(Inventory)" href="/inventory/items" icon={Package} failed={inventory === null}
            metrics={[
              { label: '활성 품목', value: fmtNum(inventory?.activeItems ?? 0) },
              { label: '재고 부족', value: fmtNum(inventory?.lowStockItems ?? 0), alert: lowStock > 0 },
              { label: '임시 이동', value: fmtNum(inventory?.draftMovements ?? 0) },
            ]}
          />
          <ModuleCard
            title="CRM" href="/crm/opportunities" icon={TrendingUp} failed={crm === null}
            metrics={[
              { label: '진행중 기회', value: fmtNum(crm?.openOpportunities ?? 0) },
              { label: '파이프라인 금액', value: formatMoneyList(crm?.openOpportunityAmounts ?? []), sub: baseTotalLabel(crm?.openOpportunityBaseTotal, crm?.baseCurrency) },
              { label: '미완료 활동', value: fmtNum(crm?.openActivities ?? 0) },
            ]}
          />
        </div>
      </div>
    </div>
  )
}
