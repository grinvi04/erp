import { safeGet, safeGetArray } from '@/lib/api'
import { formatMoneyList, formatMoneyOne } from '@/lib/money'
import { PageHeader } from '@/components/ui/page-header'
import { ChartCard } from '@/components/ui/chart-card'
import { EmptyState } from '@/components/ui/empty-state'
import { DonutChart } from '@/components/charts/donut-chart'
import { CategoryBarChart } from '@/components/charts/category-bar-chart'
import { MonthlyBarChart } from '@/components/charts/monthly-bar-chart'
import { PackageX } from 'lucide-react'
import type {
  PipelineAnalyticsResponse,
  LeadStatusCountResponse,
  MonthlyInvoiceAnalyticsResponse,
  EmployeeStatusCountResponse,
  DepartmentHeadcountResponse,
  PositionHeadcountResponse,
  EmploymentTypeCountResponse,
  MonthlyHiresTerminationsResponse,
  LeaveTypeStatResponse,
  CategoryItemCountResponse,
  WarehouseStockResponse,
  MovementTypeCountResponse,
  MonthlyMovementByTypeResponse,
  LowStockItemResponse,
} from '@/types/analytics'

export const metadata = { title: '분석 | ERP' }

const LEAD_STATUS_LABELS: Record<string, string> = {
  NEW: '신규',
  CONTACTED: '접촉',
  QUALIFIED: '적격',
  CONVERTED: '전환',
  DISQUALIFIED: '불량',
}

const EMPLOYEE_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '재직',
  ON_LEAVE: '휴직',
  SUSPENDED: '정직',
  TERMINATED: '퇴직',
}

const EMPLOYMENT_TYPE_LABELS: Record<string, string> = {
  REGULAR: '정규직',
  CONTRACT: '계약직',
  PART_TIME: '파트타임',
  INTERN: '인턴',
  DISPATCH: '파견직',
}

const LEAVE_TYPE_LABELS: Record<string, string> = {
  ANNUAL: '연차',
  SICK: '병가',
  PARENTAL: '육아휴직',
  BEREAVEMENT: '경조사',
  UNPAID: '무급',
  COMPENSATORY: '보상휴가',
}

const MOVEMENT_TYPE_LABELS: Record<string, string> = {
  RECEIPT: '입고',
  ISSUE: '출고',
  TRANSFER: '이동',
  ADJUSTMENT: '조정',
  RETURN: '반품',
}

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

export default async function AnalyticsPage() {
  const currentYear = new Date().getFullYear()

  const [
    pipelineData,
    leads,
    monthlyData,
    hrStatus,
    hrByDept,
    hrByPosition,
    hrByEmploymentType,
    hrHiresTerms,
    hrLeaves,
    invByCategory,
    invByWarehouse,
    invMovementsByType,
    invMonthlyMovements,
    invLowStock,
  ] = await Promise.all([
    safeGet<PipelineAnalyticsResponse>('/api/crm/analytics/pipeline'),
    safeGetArray<LeadStatusCountResponse>('/api/crm/analytics/leads-by-status'),
    safeGet<MonthlyInvoiceAnalyticsResponse>(
      `/api/finance/analytics/monthly-invoices?year=${currentYear}`,
    ),
    safeGetArray<EmployeeStatusCountResponse>('/api/hr/analytics/status-distribution'),
    safeGetArray<DepartmentHeadcountResponse>('/api/hr/analytics/by-department'),
    safeGetArray<PositionHeadcountResponse>('/api/hr/analytics/by-position'),
    safeGetArray<EmploymentTypeCountResponse>('/api/hr/analytics/by-employment-type'),
    safeGetArray<MonthlyHiresTerminationsResponse>(
      `/api/hr/analytics/hires-terminations?year=${currentYear}`,
    ),
    safeGetArray<LeaveTypeStatResponse>('/api/hr/analytics/leaves-by-type'),
    safeGetArray<CategoryItemCountResponse>('/api/inventory/analytics/by-category'),
    safeGetArray<WarehouseStockResponse>('/api/inventory/analytics/by-warehouse'),
    safeGetArray<MovementTypeCountResponse>('/api/inventory/analytics/movements-by-type'),
    safeGetArray<MonthlyMovementByTypeResponse>(
      `/api/inventory/analytics/monthly-movements?year=${currentYear}`,
    ),
    safeGetArray<LowStockItemResponse>('/api/inventory/analytics/low-stock'),
  ])

  // 통화별 분리는 유지하고 기준통화 합계를 추가 표시한다(래퍼에서 시리즈를 꺼낸다).
  const pipeline = pipelineData?.stages ?? []
  const monthly = monthlyData?.byCurrency ?? []
  const monthlyBaseTotals = monthlyData?.baseMonthlyTotals ?? []
  const monthlyBaseCurrency = monthlyData?.baseCurrency ?? 'KRW'

  return (
    <div className="space-y-8 p-6">
      <PageHeader title="분석" description="영업·재무·인사·재고 핵심 지표" />

      {/* ===== 영업 (CRM) ===== */}
      <section>
        <h2 className="mb-3 text-base font-semibold text-foreground">영업</h2>
        <div className="grid gap-4 lg:grid-cols-2">
          <ChartCard title="영업 파이프라인 분포" description="단계별 진행중 기회">
            {pipeline.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <CategoryBarChart
                data={pipeline.map((s) => ({
                  label: s.stageName,
                  value: s.count,
                  display: `${s.count}건${s.amounts.length ? ` · ${formatMoneyList(s.amounts)}` : ''}`,
                }))}
                color="var(--chart-1)"
              />
            )}
          </ChartCard>
          <ChartCard title="리드 상태 분포" description="상태별 리드 수">
            {leads.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <DonutChart
                data={leads.map((r) => ({
                  label: LEAD_STATUS_LABELS[r.status] ?? r.status,
                  value: r.count,
                }))}
                valueFormat={{ kind: 'suffix', suffix: '건' }}
                centerLabel="리드"
              />
            )}
          </ChartCard>
        </div>
      </section>

      {/* ===== 재무 ===== */}
      <section>
        <h2 className="mb-3 text-base font-semibold text-foreground">재무</h2>
        <div className="grid gap-4 lg:grid-cols-2">
          {monthly.length === 0 ? (
            <ChartCard title={`월별 매입 인보이스 (${currentYear}년)`}>
              <EmptyState title="데이터가 없습니다" className="py-10" />
            </ChartCard>
          ) : (
            monthly.map((series) => (
              <ChartCard
                key={series.currency}
                title={`월별 매입 인보이스 · ${series.currency}`}
                description={`${currentYear}년`}
              >
                <MonthlyBarChart
                  data={series.months.map((r, i) => ({
                    month: MONTH_LABELS[i],
                    amount: r.totalAmount,
                  }))}
                  series={[{ key: 'amount', label: '매입액', color: 'var(--chart-5)' }]}
                  valueFormat={{ kind: 'money', currency: series.currency }}
                />
              </ChartCard>
            ))
          )}
          {monthlyBaseTotals.length > 0 && (
            <ChartCard
              title="월별 매입 · 기준통화 합계"
              description={`${currentYear}년 · ${monthlyBaseCurrency} 환산`}
            >
              <MonthlyBarChart
                data={monthlyBaseTotals.map((r, i) => ({
                  month: MONTH_LABELS[i],
                  amount: r.totalAmount,
                }))}
                series={[
                  {
                    key: 'amount',
                    label: `기준통화(${monthlyBaseCurrency})`,
                    color: 'var(--chart-1)',
                  },
                ]}
                valueFormat={{ kind: 'money', currency: monthlyBaseCurrency }}
              />
            </ChartCard>
          )}
        </div>
      </section>

      {/* ===== 인사 (HR) ===== */}
      <section>
        <h2 className="mb-3 text-base font-semibold text-foreground">인사</h2>
        <div className="grid gap-4 lg:grid-cols-2">
          <ChartCard title="재직 상태 분포" description="상태별 직원 수">
            {hrStatus.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <DonutChart
                data={hrStatus.map((r) => ({
                  label: EMPLOYEE_STATUS_LABELS[r.status] ?? r.status,
                  value: r.count,
                }))}
                valueFormat={{ kind: 'suffix', suffix: '명' }}
                centerLabel="직원"
              />
            )}
          </ChartCard>
          <ChartCard title="고용형태별 분포" description="형태별 직원 수">
            {hrByEmploymentType.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <DonutChart
                data={hrByEmploymentType.map((r) => ({
                  label: EMPLOYMENT_TYPE_LABELS[r.employmentType] ?? r.employmentType,
                  value: r.count,
                }))}
                valueFormat={{ kind: 'suffix', suffix: '명' }}
              />
            )}
          </ChartCard>
          <ChartCard title="부서별 인원" description="재직 기준">
            {hrByDept.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <CategoryBarChart
                data={hrByDept.map((r) => ({
                  label: r.departmentName,
                  value: r.count,
                  display: `${r.count}명`,
                }))}
                color="var(--chart-1)"
              />
            )}
          </ChartCard>
          <ChartCard title="직위별 인원" description="재직 기준">
            {hrByPosition.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <CategoryBarChart
                data={hrByPosition.map((r) => ({
                  label: r.positionName,
                  value: r.count,
                  display: `${r.count}명`,
                }))}
                color="var(--chart-2)"
              />
            )}
          </ChartCard>
          <ChartCard
            title="월별 입사/퇴사"
            description={`${currentYear}년 인력 변동`}
            className="lg:col-span-2"
          >
            {hrHiresTerms.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <MonthlyBarChart
                data={hrHiresTerms.map((r, i) => ({
                  month: MONTH_LABELS[i],
                  hires: r.hires,
                  terminations: r.terminations,
                }))}
                series={[
                  { key: 'hires', label: '입사', color: 'var(--chart-2)' },
                  { key: 'terminations', label: '퇴사', color: 'var(--chart-4)' },
                ]}
                valueFormat={{ kind: 'suffix', suffix: '명' }}
              />
            )}
          </ChartCard>
          <ChartCard title="휴가 유형별 신청(승인)" description="유형별 신청 건·일수">
            {hrLeaves.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <CategoryBarChart
                data={hrLeaves.map((r) => ({
                  label: LEAVE_TYPE_LABELS[r.leaveType] ?? r.leaveType,
                  value: r.totalDays,
                  display: `${r.count}건 · ${r.totalDays}일`,
                }))}
                color="var(--chart-3)"
              />
            )}
          </ChartCard>
        </div>
      </section>

      {/* ===== 재고 (Inventory) ===== */}
      <section>
        <h2 className="mb-3 text-base font-semibold text-foreground">재고</h2>
        <div className="grid gap-4 lg:grid-cols-2">
          <ChartCard title="카테고리별 활성 품목" description="카테고리별 품목 수">
            {invByCategory.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <CategoryBarChart
                data={invByCategory.map((r) => ({
                  label: r.categoryName,
                  value: r.count,
                  display: `${r.count}개`,
                }))}
                color="var(--chart-2)"
              />
            )}
          </ChartCard>
          <ChartCard title="이동유형별 건수(확정)" description="유형별 이동 건수">
            {invMovementsByType.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <DonutChart
                data={invMovementsByType.map((r) => ({
                  label: MOVEMENT_TYPE_LABELS[r.movementType] ?? r.movementType,
                  value: r.count,
                }))}
                valueFormat={{ kind: 'suffix', suffix: '건' }}
              />
            )}
          </ChartCard>
          <ChartCard title="창고별 재고 가치" description="기준통화(KRW)">
            {invByWarehouse.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <CategoryBarChart
                data={invByWarehouse.map((r) => ({
                  label: r.warehouseName,
                  value: r.totalValue,
                  display: formatMoneyOne(r.totalValue, 'KRW'),
                }))}
                color="var(--chart-5)"
              />
            )}
          </ChartCard>
          <ChartCard title="창고별 재고 수량" description="창고별 보유 수량">
            {invByWarehouse.length === 0 ? (
              <EmptyState title="데이터가 없습니다" className="py-10" />
            ) : (
              <CategoryBarChart
                data={invByWarehouse.map((r) => ({
                  label: r.warehouseName,
                  value: r.totalQty,
                  display: `${r.totalQty.toLocaleString('ko-KR')}개`,
                }))}
                color="var(--chart-5)"
              />
            )}
          </ChartCard>
          {invMonthlyMovements.length === 0 ? (
            <ChartCard title={`월별 입출고 (${currentYear}년)`}>
              <EmptyState title="데이터가 없습니다" className="py-10" />
            </ChartCard>
          ) : (
            invMonthlyMovements.map((series) => (
              <ChartCard
                key={series.movementType}
                title={`월별 입출고 · ${MOVEMENT_TYPE_LABELS[series.movementType] ?? series.movementType}`}
                description={`${currentYear}년`}
              >
                <MonthlyBarChart
                  data={series.months.map((r, i) => ({ month: MONTH_LABELS[i], qty: r.totalQty }))}
                  series={[{ key: 'qty', label: '수량', color: 'var(--chart-3)' }]}
                  valueFormat={{ kind: 'suffix', suffix: '개' }}
                />
              </ChartCard>
            ))
          )}
          <ChartCard
            title="저재고 품목"
            description="Σ현재고 ≤ 재주문점"
            className="lg:col-span-2"
            action={
              invLowStock.length > 0 ? (
                <span className="text-sm font-medium text-warning">{invLowStock.length}건</span>
              ) : undefined
            }
          >
            {invLowStock.length === 0 ? (
              <EmptyState icon={PackageX} title="저재고 품목이 없습니다" className="py-10" />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-muted-foreground border-b border-border">
                      <th className="py-2 pr-4 font-medium">SKU</th>
                      <th className="py-2 pr-4 font-medium">품목명</th>
                      <th className="py-2 pr-4 font-medium">카테고리</th>
                      <th className="py-2 pr-4 font-medium text-right">현재고</th>
                      <th className="py-2 font-medium text-right">재주문점</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invLowStock.map((r) => (
                      <tr key={r.sku} className="border-b border-border">
                        <td className="py-2 pr-4 font-mono text-foreground">{r.sku}</td>
                        <td className="py-2 pr-4 text-foreground">{r.name}</td>
                        <td className="py-2 pr-4 text-muted-foreground">{r.categoryName ?? '-'}</td>
                        <td className="py-2 pr-4 text-right text-destructive font-medium">
                          {r.currentQty.toLocaleString('ko-KR')}
                        </td>
                        <td className="py-2 text-right text-muted-foreground">
                          {r.reorderPoint.toLocaleString('ko-KR')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </ChartCard>
        </div>
      </section>
    </div>
  )
}
