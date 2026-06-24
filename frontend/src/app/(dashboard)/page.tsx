import Link from 'next/link'
import { apiGet } from '@/lib/api'
import { Card } from '@/components/ui/card'
import {
  Users, BarChart3, Package, TrendingUp, ChevronRight, AlertTriangle,
} from 'lucide-react'
import type {
  HrSummary, FinanceSummary, InventorySummary, CrmSummary,
} from '@/types/dashboard'

export const metadata = { title: '대시보드 | ERP' }

function fmtNum(n: number) {
  return n.toLocaleString('ko-KR')
}
function fmtMoney(n: number) {
  return `₩${n.toLocaleString('ko-KR')}`
}

// 한 모듈의 요약 호출이 실패해도 나머지 대시보드는 정상 렌더되도록 개별적으로 처리한다.
async function safeGet<T>(path: string): Promise<T | null> {
  try {
    return await apiGet<T>(path)
  } catch {
    return null
  }
}

interface Metric {
  label: string
  value: string
  alert?: boolean
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
    <Card className="p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Icon className="h-5 w-5 text-gray-700" />
          <h2 className="font-semibold text-gray-900">{title}</h2>
        </div>
        <Link href={href} className="text-sm text-blue-600 hover:underline flex items-center">
          바로가기<ChevronRight className="h-3 w-3" />
        </Link>
      </div>
      {failed ? (
        <p className="text-sm text-gray-400 py-4">요약 정보를 불러오지 못했습니다</p>
      ) : (
        <div className="grid grid-cols-3 gap-3">
          {metrics.map((m) => (
            <div key={m.label}>
              <div className={`text-2xl font-semibold tabular-nums ${m.alert ? 'text-amber-600' : 'text-gray-900'}`}>
                {m.alert && <AlertTriangle className="inline h-4 w-4 mr-1 -mt-1" />}
                {m.value}
              </div>
              <div className="text-xs text-gray-500 mt-1">{m.label}</div>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}

export default async function DashboardPage() {
  const [hr, finance, inventory, crm] = await Promise.all([
    safeGet<HrSummary>('/api/hr/summary'),
    safeGet<FinanceSummary>('/api/finance/summary'),
    safeGet<InventorySummary>('/api/inventory/summary'),
    safeGet<CrmSummary>('/api/crm/summary'),
  ])

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">대시보드</h1>
        <p className="text-sm text-gray-500 mt-1">모듈별 핵심 지표 요약</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
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
            { label: '미지급 금액', value: fmtMoney(finance?.unpaidAmount ?? 0) },
            { label: '임시 전표', value: fmtNum(finance?.draftJournalEntries ?? 0) },
          ]}
        />
        <ModuleCard
          title="재고(Inventory)" href="/inventory/items" icon={Package} failed={inventory === null}
          metrics={[
            { label: '활성 품목', value: fmtNum(inventory?.activeItems ?? 0) },
            { label: '재고 부족', value: fmtNum(inventory?.lowStockItems ?? 0), alert: (inventory?.lowStockItems ?? 0) > 0 },
            { label: '임시 이동', value: fmtNum(inventory?.draftMovements ?? 0) },
          ]}
        />
        <ModuleCard
          title="CRM" href="/crm/opportunities" icon={TrendingUp} failed={crm === null}
          metrics={[
            { label: '진행중 기회', value: fmtNum(crm?.openOpportunities ?? 0) },
            { label: '파이프라인 금액', value: fmtMoney(crm?.openOpportunityAmount ?? 0) },
            { label: '미완료 활동', value: fmtNum(crm?.openActivities ?? 0) },
          ]}
        />
      </div>
    </div>
  )
}
