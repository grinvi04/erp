import { apiGet } from '@/lib/api'
import { formatMoneyList } from '@/lib/money'
import type {
  PipelineDistributionResponse,
  LeadStatusCountResponse,
  MonthlyInvoiceByCurrencyResponse,
  EmployeeStatusCountResponse,
  DepartmentHeadcountResponse,
  PositionHeadcountResponse,
  EmploymentTypeCountResponse,
  MonthlyHiresTerminationsResponse,
  LeaveTypeStatResponse,
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

  const [
    pipeline,
    leads,
    monthly,
    hrStatus,
    hrByDept,
    hrByPosition,
    hrByEmploymentType,
    hrHiresTerms,
    hrLeaves,
  ] = await Promise.all([
    safeGetArray<PipelineDistributionResponse>('/api/crm/analytics/pipeline'),
    safeGetArray<LeadStatusCountResponse>('/api/crm/analytics/leads-by-status'),
    safeGetArray<MonthlyInvoiceByCurrencyResponse>(`/api/finance/analytics/monthly-invoices?year=${currentYear}`),
    safeGetArray<EmployeeStatusCountResponse>('/api/hr/analytics/status-distribution'),
    safeGetArray<DepartmentHeadcountResponse>('/api/hr/analytics/by-department'),
    safeGetArray<PositionHeadcountResponse>('/api/hr/analytics/by-position'),
    safeGetArray<EmploymentTypeCountResponse>('/api/hr/analytics/by-employment-type'),
    safeGetArray<MonthlyHiresTerminationsResponse>(`/api/hr/analytics/hires-terminations?year=${currentYear}`),
    safeGetArray<LeaveTypeStatResponse>('/api/hr/analytics/leaves-by-type'),
  ])

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
  const maxHiresTerms = Math.max(
    ...hrHiresTerms.map((r) => Math.max(r.hires, r.terminations)),
    1,
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">분석</h1>
        <p className="text-sm text-gray-500 mt-1">영업 파이프라인, 리드 현황, 매입 인보이스 추이, 인사 현황</p>
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

        {/* ===== HR ===== */}
        <div className="pt-2">
          <h2 className="text-base font-semibold text-gray-700">인사 현황</h2>
        </div>

        {/* 재직 상태 분포 */}
        <SectionCard title="재직 상태 분포">
          {hrStatus.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            hrStatus.map((r) => (
              <HorizontalBar
                key={r.status}
                label={EMPLOYEE_STATUS_LABELS[r.status] ?? r.status}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrStatus) * 100)}
                color="bg-blue-500"
              />
            ))
          )}
        </SectionCard>

        {/* 부서별 인원 */}
        <SectionCard title="부서별 인원 (재직)">
          {hrByDept.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            hrByDept.map((r) => (
              <HorizontalBar
                key={r.departmentId}
                label={r.departmentName}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrDept) * 100)}
                color="bg-sky-500"
              />
            ))
          )}
        </SectionCard>

        {/* 직위별 인원 */}
        <SectionCard title="직위별 인원 (재직)">
          {hrByPosition.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            hrByPosition.map((r) => (
              <HorizontalBar
                key={r.positionId}
                label={r.positionName}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrPosition) * 100)}
                color="bg-indigo-500"
              />
            ))
          )}
        </SectionCard>

        {/* 고용형태별 분포 */}
        <SectionCard title="고용형태별 분포">
          {hrByEmploymentType.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            hrByEmploymentType.map((r) => (
              <HorizontalBar
                key={r.employmentType}
                label={EMPLOYMENT_TYPE_LABELS[r.employmentType] ?? r.employmentType}
                subLabel={`${r.count}명`}
                pct={Math.round((r.count / maxHrEmploymentType) * 100)}
                color="bg-teal-500"
              />
            ))
          )}
        </SectionCard>

        {/* 월별 입사/퇴사 추이 — 입사(emerald)·퇴사(rose) 두 시리즈를 월별 그룹 막대로 */}
        <SectionCard title={`월별 입사/퇴사 추이 (${currentYear}년)`}>
          {hrHiresTerms.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            <>
              <div className="flex items-center gap-4 text-xs text-gray-500 mb-3">
                <span className="flex items-center gap-1"><span className="inline-block w-3 h-3 rounded bg-emerald-500" />입사</span>
                <span className="flex items-center gap-1"><span className="inline-block w-3 h-3 rounded bg-rose-500" />퇴사</span>
              </div>
              <div className="flex items-end gap-2">
                {hrHiresTerms.map((r, idx) => (
                  <div key={r.month} className="flex flex-col items-center flex-1">
                    <div className="relative group flex h-40 w-full items-end justify-center gap-0.5">
                      <div className="absolute bottom-full mb-1 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs rounded px-2 py-1 whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                        입사 {r.hires}명<br />퇴사 {r.terminations}명
                      </div>
                      <div
                        className="bg-emerald-500 rounded-t w-1/2"
                        style={{ height: `${(r.hires / maxHiresTerms) * 100}%`, minHeight: r.hires > 0 ? '4px' : '0px' }}
                      />
                      <div
                        className="bg-rose-500 rounded-t w-1/2"
                        style={{ height: `${(r.terminations / maxHiresTerms) * 100}%`, minHeight: r.terminations > 0 ? '4px' : '0px' }}
                      />
                    </div>
                    <div className="text-xs text-gray-500 mt-1">{MONTH_LABELS[idx]}</div>
                  </div>
                ))}
              </div>
            </>
          )}
        </SectionCard>

        {/* 휴가 유형별 신청 (승인 기준) */}
        <SectionCard title="휴가 유형별 신청 (승인)">
          {hrLeaves.length === 0 ? (
            <p className="text-sm text-gray-400">데이터 없음</p>
          ) : (
            hrLeaves.map((r) => (
              <HorizontalBar
                key={r.leaveType}
                label={LEAVE_TYPE_LABELS[r.leaveType] ?? r.leaveType}
                subLabel={`${r.count}건 · ${r.totalDays}일`}
                pct={Math.round((r.totalDays / maxHrLeaveDays) * 100)}
                color="bg-amber-500"
              />
            ))
          )}
        </SectionCard>
      </div>
    </div>
  )
}
