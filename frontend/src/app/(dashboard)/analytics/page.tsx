import { safeGet, safeGetArray } from '@/lib/api'
import { formatMoneyList, formatMoneyOne } from '@/lib/money'
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

const MONTH_LABELS = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']

function HorizontalBar({
  label,
  subLabel,
  pct,
  color = 'bg-chart-1',
}: {
  label: string
  subLabel: string
  pct: number
  color?: string
}) {
  return (
    <div className="mb-3">
      <div className="flex justify-between text-sm text-foreground mb-1">
        <span className="font-medium">{label}</span>
        <span className="text-muted-foreground">{subLabel}</span>
      </div>
      <div className="w-full bg-muted rounded h-6">
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
    <div className="bg-card rounded-lg border border-border p-6">
      <h2 className="text-lg font-semibold text-foreground mb-4">{title}</h2>
      {children}
    </div>
  )
}

// 월별 세로 막대차트. 단일 시리즈(w-full 막대 1개) 또는 다중 시리즈(그룹 막대) 지원.
// 막대 높이는 모든 시리즈 값을 통틀어 계산한 최대값으로 스케일한다(≥1 보장).
function MonthlyBarChart<T extends { month: number }>({
  rows,
  bars,
  renderTooltip,
  renderFooter,
}: {
  rows: T[]
  bars: { value: (r: T) => number; color: string; width: string }[]
  renderTooltip: (r: T) => React.ReactNode
  renderFooter?: (r: T) => React.ReactNode
}) {
  const max = Math.max(...rows.flatMap((r) => bars.map((b) => b.value(r))), 1)
  const groupGap = bars.length > 1 ? ' gap-0.5' : ''
  return (
    <div className="flex items-end gap-2">
      {rows.map((r, idx) => (
        <div key={r.month} className="flex flex-col items-center flex-1">
          <div className={`relative group flex h-40 w-full items-end justify-center${groupGap}`}>
            <div className="absolute bottom-full mb-1 left-1/2 -translate-x-1/2 bg-popover text-popover-foreground border border-border shadow-md text-xs rounded px-2 py-1 whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
              {renderTooltip(r)}
            </div>
            {bars.map((b, bi) => {
              const v = b.value(r)
              return (
                <div
                  key={bi}
                  className={`${b.color} rounded-t ${b.width}`}
                  style={{ height: `${(v / max) * 100}%`, minHeight: v > 0 ? '4px' : '0px' }}
                />
              )
            })}
          </div>
          <div className="text-xs text-muted-foreground mt-1">{MONTH_LABELS[idx]}</div>
          {renderFooter?.(r)}
        </div>
      ))}
    </div>
  )
}

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
    safeGet<MonthlyInvoiceAnalyticsResponse>(`/api/finance/analytics/monthly-invoices?year=${currentYear}`),
    safeGetArray<EmployeeStatusCountResponse>('/api/hr/analytics/status-distribution'),
    safeGetArray<DepartmentHeadcountResponse>('/api/hr/analytics/by-department'),
    safeGetArray<PositionHeadcountResponse>('/api/hr/analytics/by-position'),
    safeGetArray<EmploymentTypeCountResponse>('/api/hr/analytics/by-employment-type'),
    safeGetArray<MonthlyHiresTerminationsResponse>(`/api/hr/analytics/hires-terminations?year=${currentYear}`),
    safeGetArray<LeaveTypeStatResponse>('/api/hr/analytics/leaves-by-type'),
    safeGetArray<CategoryItemCountResponse>('/api/inventory/analytics/by-category'),
    safeGetArray<WarehouseStockResponse>('/api/inventory/analytics/by-warehouse'),
    safeGetArray<MovementTypeCountResponse>('/api/inventory/analytics/movements-by-type'),
    safeGetArray<MonthlyMovementByTypeResponse>(`/api/inventory/analytics/monthly-movements?year=${currentYear}`),
    safeGetArray<LowStockItemResponse>('/api/inventory/analytics/low-stock'),
  ])

  // 통화별 분리는 유지하고 기준통화 합계를 추가 표시한다(래퍼에서 시리즈를 꺼낸다).
  const pipeline = pipelineData?.stages ?? []
  const pipelineBaseCurrency = pipelineData?.baseCurrency ?? 'KRW'
  const monthly = monthlyData?.byCurrency ?? []
  const monthlyBaseTotals = monthlyData?.baseMonthlyTotals ?? []
  const monthlyBaseCurrency = monthlyData?.baseCurrency ?? 'KRW'

  // Pipeline: scale by count
  const maxPipelineCount = Math.max(...pipeline.map((r) => r.count), 1)

  // Leads: scale by count
  const maxLeadCount = Math.max(...leads.map((r) => r.count), 1)

  // HR scaling
  const maxHrStatus = Math.max(...hrStatus.map((r) => r.count), 1)
  const maxHrDept = Math.max(...hrByDept.map((r) => r.count), 1)
  const maxHrPosition = Math.max(...hrByPosition.map((r) => r.count), 1)
  const maxHrEmploymentType = Math.max(...hrByEmploymentType.map((r) => r.count), 1)
  const maxHrLeaveDays = Math.max(...hrLeaves.map((r) => r.totalDays), 1)

  // Inventory scaling
  const maxInvCategory = Math.max(...invByCategory.map((r) => r.count), 1)
  const maxInvWhValue = Math.max(...invByWarehouse.map((r) => r.totalValue), 1)
  const maxInvWhQty = Math.max(...invByWarehouse.map((r) => r.totalQty), 1)
  const maxInvMovementType = Math.max(...invMovementsByType.map((r) => r.count), 1)

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-foreground">분석</h1>
        <p className="text-sm text-muted-foreground mt-1">영업 파이프라인, 리드 현황, 매입 인보이스 추이, 인사 현황, 재고 현황</p>
      </div>

      <div className="space-y-6">
        {/* Pipeline Distribution */}
        <SectionCard title="영업 파이프라인 분포">
          {pipeline.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            pipeline.map((r) => (
              <HorizontalBar
                key={r.stageId}
                label={r.stageName}
                subLabel={`${r.count}건${r.amounts.length ? ' · ' + formatMoneyList(r.amounts) : ''}`
                  + (r.baseTotal != null ? ` · ≈ ${formatMoneyOne(r.baseTotal, pipelineBaseCurrency)}` : '')}
                pct={Math.round((r.count / maxPipelineCount) * 100)}
                color="bg-chart-1"
              />
            ))
          )}
        </SectionCard>

        {/* Leads by Status */}
        <SectionCard title="리드 상태 분포">
          {leads.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            leads.map((r) => (
              <HorizontalBar
                key={r.status}
                label={LEAD_STATUS_LABELS[r.status] ?? r.status}
                subLabel={`${r.count}건`}
                pct={Math.round((r.count / maxLeadCount) * 100)}
                color="bg-chart-2"
              />
            ))
          )}
        </SectionCard>

        {/* Monthly Invoice Trend — 통화별 카드로 분리(막대 비교는 같은 통화 안에서만 의미) */}
        {monthly.length === 0 ? (
          <SectionCard title={`월별 매입 인보이스 추이 (${currentYear}년)`}>
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          </SectionCard>
        ) : (
          monthly.map((series) => (
            <SectionCard
              key={series.currency}
              title={`월별 매입 인보이스 추이 (${currentYear}년) · ${series.currency}`}
            >
              {/* 막대 높이는 그 통화 내 최대 금액으로 스케일 */}
              <MonthlyBarChart
                rows={series.months}
                bars={[{ value: (r) => r.totalAmount, color: 'bg-chart-5', width: 'w-full' }]}
                renderTooltip={(r) => (
                  <>
                    {r.count}건<br />{formatMoneyList([{ currency: series.currency, amount: r.totalAmount }])}
                  </>
                )}
                renderFooter={(r) => <div className="text-xs text-muted-foreground font-medium">{r.count}</div>}
              />
            </SectionCard>
          ))
        )}

        {/* 월별 기준통화 합계 추이 — 모든 통화를 기준통화로 환산해 합산(산정분만). 통화별 카드와 별개로 추가. */}
        {monthlyBaseTotals.length > 0 && (
          <SectionCard
            title={`월별 매입 인보이스 추이 (${currentYear}년) · 기준통화 합계(${monthlyBaseCurrency})`}
          >
            <MonthlyBarChart
              rows={monthlyBaseTotals}
              bars={[{ value: (r) => r.totalAmount, color: 'bg-chart-1', width: 'w-full' }]}
              renderTooltip={(r) => <>≈ {formatMoneyOne(r.totalAmount, monthlyBaseCurrency)}</>}
            />
          </SectionCard>
        )}

        {/* ===== HR ===== */}
        <div className="pt-2">
          <h2 className="text-base font-semibold text-foreground">인사 현황</h2>
        </div>

        {/* 재직 상태 분포 */}
        <SectionCard title="재직 상태 분포">
          {hrStatus.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            hrStatus.map((r) => (
              <HorizontalBar
                key={r.status}
                label={EMPLOYEE_STATUS_LABELS[r.status] ?? r.status}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrStatus) * 100)}
                color="bg-chart-1"
              />
            ))
          )}
        </SectionCard>

        {/* 부서별 인원 */}
        <SectionCard title="부서별 인원 (재직)">
          {hrByDept.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            hrByDept.map((r) => (
              <HorizontalBar
                key={r.departmentId}
                label={r.departmentName}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrDept) * 100)}
                color="bg-chart-1"
              />
            ))
          )}
        </SectionCard>

        {/* 직위별 인원 */}
        <SectionCard title="직위별 인원 (재직)">
          {hrByPosition.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            hrByPosition.map((r) => (
              <HorizontalBar
                key={r.positionId}
                label={r.positionName}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrPosition) * 100)}
                color="bg-chart-1"
              />
            ))
          )}
        </SectionCard>

        {/* 고용형태별 분포 */}
        <SectionCard title="고용형태별 분포">
          {hrByEmploymentType.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            hrByEmploymentType.map((r) => (
              <HorizontalBar
                key={r.employmentType}
                label={EMPLOYMENT_TYPE_LABELS[r.employmentType] ?? r.employmentType}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrEmploymentType) * 100)}
                color="bg-chart-2"
              />
            ))
          )}
        </SectionCard>

        {/* 월별 입사/퇴사 추이 — 입사(emerald)·퇴사(rose) 두 시리즈를 월별 그룹 막대로 */}
        <SectionCard title={`월별 입사/퇴사 추이 (${currentYear}년)`}>
          {hrHiresTerms.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            <>
              <div className="flex items-center gap-4 text-xs text-muted-foreground mb-3">
                <span className="flex items-center gap-1"><span className="inline-block w-3 h-3 rounded bg-chart-2" />입사</span>
                <span className="flex items-center gap-1"><span className="inline-block w-3 h-3 rounded bg-chart-4" />퇴사</span>
              </div>
              <MonthlyBarChart
                rows={hrHiresTerms}
                bars={[
                  { value: (r) => r.hires, color: 'bg-chart-2', width: 'w-1/2' },
                  { value: (r) => r.terminations, color: 'bg-chart-4', width: 'w-1/2' },
                ]}
                renderTooltip={(r) => (
                  <>
                    입사 {r.hires}명<br />퇴사 {r.terminations}명
                  </>
                )}
              />
            </>
          )}
        </SectionCard>

        {/* 휴가 유형별 신청 (승인 기준) */}
        <SectionCard title="휴가 유형별 신청 (승인)">
          {hrLeaves.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            hrLeaves.map((r) => (
              <HorizontalBar
                key={r.leaveType}
                label={LEAVE_TYPE_LABELS[r.leaveType] ?? r.leaveType}
                subLabel={`${r.count}건 · ${r.totalDays}일`}
                pct={Math.round((r.totalDays / maxHrLeaveDays) * 100)}
                color="bg-chart-3"
              />
            ))
          )}
        </SectionCard>

        {/* ===== Inventory ===== */}
        <div className="pt-2">
          <h2 className="text-base font-semibold text-foreground">재고 현황</h2>
        </div>

        {/* 카테고리별 활성 품목 수 */}
        <SectionCard title="카테고리별 활성 품목 수">
          {invByCategory.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            invByCategory.map((r) => (
              <HorizontalBar
                key={r.categoryId}
                label={r.categoryName}
                subLabel={`${r.count}개`}
                pct={Math.round((r.count / maxInvCategory) * 100)}
                color="bg-chart-2"
              />
            ))
          )}
        </SectionCard>

        {/* 창고별 재고 가치 (₩ 단일 기준통화) — 수량은 sublabel */}
        <SectionCard title="창고별 재고 가치">
          {invByWarehouse.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            invByWarehouse.map((r) => (
              <HorizontalBar
                key={r.warehouseId}
                label={r.warehouseName}
                subLabel={`${formatMoneyOne(r.totalValue, 'KRW')} · ${r.totalQty.toLocaleString('ko-KR')}개`}
                pct={Math.round((r.totalValue / maxInvWhValue) * 100)}
                color="bg-chart-5"
              />
            ))
          )}
        </SectionCard>

        {/* 창고별 재고 수량 */}
        <SectionCard title="창고별 재고 수량">
          {invByWarehouse.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            invByWarehouse.map((r) => (
              <HorizontalBar
                key={r.warehouseId}
                label={r.warehouseName}
                subLabel={`${r.totalQty.toLocaleString('ko-KR')}개`}
                pct={Math.round((r.totalQty / maxInvWhQty) * 100)}
                color="bg-chart-5"
              />
            ))
          )}
        </SectionCard>

        {/* 이동유형별 건수 (확정) */}
        <SectionCard title="이동유형별 건수 (확정)">
          {invMovementsByType.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          ) : (
            invMovementsByType.map((r) => (
              <HorizontalBar
                key={r.movementType}
                label={MOVEMENT_TYPE_LABELS[r.movementType] ?? r.movementType}
                subLabel={`${r.count}건`}
                pct={Math.round((r.count / maxInvMovementType) * 100)}
                color="bg-chart-3"
              />
            ))
          )}
        </SectionCard>

        {/* 월별 입출고 추이 — 이동유형별 카드(수량 기준 막대) */}
        {invMonthlyMovements.length === 0 ? (
          <SectionCard title={`월별 입출고 추이 (${currentYear}년)`}>
            <p className="text-sm text-muted-foreground">데이터 없음</p>
          </SectionCard>
        ) : (
          invMonthlyMovements.map((series) => (
            <SectionCard
              key={series.movementType}
              title={`월별 입출고 추이 (${currentYear}년) · ${MOVEMENT_TYPE_LABELS[series.movementType] ?? series.movementType}`}
            >
              <MonthlyBarChart
                rows={series.months}
                bars={[{ value: (r) => r.totalQty, color: 'bg-chart-3', width: 'w-full' }]}
                renderTooltip={(r) => (
                  <>
                    {r.count}건<br />수량 {r.totalQty.toLocaleString('ko-KR')}
                  </>
                )}
                renderFooter={(r) => <div className="text-xs text-muted-foreground font-medium">{r.count}</div>}
              />
            </SectionCard>
          ))
        )}

        {/* 저재고 품목 목록 (Σ현재고 ≤ 재주문점) */}
        <SectionCard title="저재고 품목">
          {invLowStock.length === 0 ? (
            <p className="text-sm text-muted-foreground">데이터 없음</p>
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
                      <td className="py-2 text-right text-muted-foreground">{r.reorderPoint.toLocaleString('ko-KR')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  )
}
