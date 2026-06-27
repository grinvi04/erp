import { apiGet } from '@/lib/api'
import { formatMoneyOne } from '@/lib/money'
import { Badge } from '@/components/ui/badge'
import type {
  TrialBalanceResponse,
  IncomeStatementResponse,
  BalanceSheetResponse,
} from '@/types/finance'
import YearSelect from './year-select'

export const metadata = { title: '재무제표 | ERP' }

async function safeGet<T>(path: string): Promise<T | null> {
  try {
    return await apiGet<T>(path)
  } catch {
    return null
  }
}

function SectionCard({ title, action, children }: {
  title: string
  action?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
        {action}
      </div>
      {children}
    </div>
  )
}

function ExcludedNotice({ count }: { count: number }) {
  if (count <= 0) return null
  return (
    <p className="text-xs text-amber-600 mb-3">
      환율 미산정 {count.toLocaleString('ko-KR')}건 제외
    </p>
  )
}

const EMPTY = <p className="text-sm text-gray-400">데이터 없음</p>

export default async function FinancialReportsPage(props: {
  searchParams: Promise<{ year?: string }>
}) {
  const sp = await props.searchParams
  const currentYear = new Date().getFullYear()
  const parsed = sp.year ? Number(sp.year) : currentYear
  const year = Number.isFinite(parsed) ? parsed : currentYear

  const [trialBalance, incomeStatement, balanceSheet] = await Promise.all([
    safeGet<TrialBalanceResponse>(`/api/finance/reports/trial-balance?year=${year}`),
    safeGet<IncomeStatementResponse>(`/api/finance/reports/income-statement?year=${year}`),
    safeGet<BalanceSheetResponse>(`/api/finance/reports/balance-sheet?year=${year}`),
  ])

  // 연도 옵션: 현재연도 기준 최근 6년 + 선택 연도(밖이면 포함)
  const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear - i)
  if (!yearOptions.includes(year)) yearOptions.unshift(year)

  const tbCcy = trialBalance?.baseCurrency ?? 'KRW'
  const isCcy = incomeStatement?.baseCurrency ?? 'KRW'
  const bsCcy = balanceSheet?.baseCurrency ?? 'KRW'

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">재무제표</h1>
          <p className="text-sm text-gray-500 mt-1">시산표 · 손익계산서 · 재무상태표 ({year}년)</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">회계연도</span>
          <YearSelect year={year} years={yearOptions} />
        </div>
      </div>

      <div className="space-y-6">
        {/* ===== 시산표 ===== */}
        <SectionCard title="시산표">
          {trialBalance == null ? (
            EMPTY
          ) : (
            <>
              <ExcludedNotice count={trialBalance.excludedEntryCount} />
              {trialBalance.rows.length === 0 ? (
                EMPTY
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="text-left text-gray-500 border-b border-gray-200">
                        <th className="py-2 pr-4 font-medium">계정코드</th>
                        <th className="py-2 pr-4 font-medium">계정명</th>
                        <th className="py-2 pr-4 font-medium text-right">차변</th>
                        <th className="py-2 pr-4 font-medium text-right">대변</th>
                        <th className="py-2 font-medium text-right">잔액</th>
                      </tr>
                    </thead>
                    <tbody>
                      {trialBalance.rows.map((r) => (
                        <tr key={r.accountCode} className="border-b border-gray-100">
                          <td className="py-2 pr-4 font-mono text-gray-700">{r.accountCode}</td>
                          <td className="py-2 pr-4 text-gray-700">{r.accountName}</td>
                          <td className="py-2 pr-4 text-right text-gray-700">{formatMoneyOne(r.debit, tbCcy)}</td>
                          <td className="py-2 pr-4 text-right text-gray-700">{formatMoneyOne(r.credit, tbCcy)}</td>
                          <td className="py-2 text-right text-gray-700">{formatMoneyOne(r.balance, tbCcy)}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr className="border-t-2 border-gray-300 font-semibold text-gray-900">
                        <td className="py-2 pr-4" colSpan={2}>합계</td>
                        <td className="py-2 pr-4 text-right">{formatMoneyOne(trialBalance.totalDebit, tbCcy)}</td>
                        <td className="py-2 pr-4 text-right">{formatMoneyOne(trialBalance.totalCredit, tbCcy)}</td>
                        <td className="py-2" />
                      </tr>
                    </tfoot>
                  </table>
                </div>
              )}
            </>
          )}
        </SectionCard>

        {/* ===== 손익계산서 ===== */}
        <SectionCard title="손익계산서">
          {incomeStatement == null ? (
            EMPTY
          ) : (
            <>
              <ExcludedNotice count={incomeStatement.excludedEntryCount} />
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <tbody>
                    {/* 수익 */}
                    <tr className="bg-gray-50 text-gray-700 font-medium border-b border-gray-200">
                      <td className="py-2 pr-4" colSpan={3}>수익</td>
                    </tr>
                    {incomeStatement.revenues.length === 0 ? (
                      <tr className="border-b border-gray-100">
                        <td className="py-2 pr-4 text-gray-400" colSpan={3}>데이터 없음</td>
                      </tr>
                    ) : (
                      incomeStatement.revenues.map((r) => (
                        <tr key={`rev-${r.accountCode}`} className="border-b border-gray-100">
                          <td className="py-2 pr-4 pl-4 font-mono text-gray-600">{r.accountCode}</td>
                          <td className="py-2 pr-4 text-gray-700">{r.accountName}</td>
                          <td className="py-2 text-right text-gray-700">{formatMoneyOne(r.amount, isCcy)}</td>
                        </tr>
                      ))
                    )}
                    <tr className="border-b border-gray-200 font-medium text-gray-900">
                      <td className="py-2 pr-4" colSpan={2}>수익 합계</td>
                      <td className="py-2 text-right">{formatMoneyOne(incomeStatement.totalRevenue, isCcy)}</td>
                    </tr>

                    {/* 비용 */}
                    <tr className="bg-gray-50 text-gray-700 font-medium border-b border-gray-200">
                      <td className="py-2 pr-4" colSpan={3}>비용</td>
                    </tr>
                    {incomeStatement.expenses.length === 0 ? (
                      <tr className="border-b border-gray-100">
                        <td className="py-2 pr-4 text-gray-400" colSpan={3}>데이터 없음</td>
                      </tr>
                    ) : (
                      incomeStatement.expenses.map((r) => (
                        <tr key={`exp-${r.accountCode}`} className="border-b border-gray-100">
                          <td className="py-2 pr-4 pl-4 font-mono text-gray-600">{r.accountCode}</td>
                          <td className="py-2 pr-4 text-gray-700">{r.accountName}</td>
                          <td className="py-2 text-right text-gray-700">{formatMoneyOne(r.amount, isCcy)}</td>
                        </tr>
                      ))
                    )}
                    <tr className="border-b border-gray-200 font-medium text-gray-900">
                      <td className="py-2 pr-4" colSpan={2}>비용 합계</td>
                      <td className="py-2 text-right">{formatMoneyOne(incomeStatement.totalExpense, isCcy)}</td>
                    </tr>

                    {/* 당기순이익 */}
                    <tr className="border-t-2 border-gray-300 font-semibold text-gray-900">
                      <td className="py-2 pr-4" colSpan={2}>당기순이익</td>
                      <td className={`py-2 text-right ${incomeStatement.netIncome < 0 ? 'text-rose-600' : 'text-gray-900'}`}>
                        {formatMoneyOne(incomeStatement.netIncome, isCcy)}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </>
          )}
        </SectionCard>

        {/* ===== 재무상태표 ===== */}
        <SectionCard
          title="재무상태표"
          action={
            balanceSheet != null ? (
              balanceSheet.balanced ? (
                <Badge variant="default">균형</Badge>
              ) : (
                <Badge variant="destructive">불균형</Badge>
              )
            ) : null
          }
        >
          {balanceSheet == null ? (
            EMPTY
          ) : (
            <>
              <ExcludedNotice count={balanceSheet.excludedEntryCount} />
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <tbody>
                    {/* 자산 */}
                    <tr className="bg-gray-50 text-gray-700 font-medium border-b border-gray-200">
                      <td className="py-2 pr-4" colSpan={3}>자산</td>
                    </tr>
                    {balanceSheet.assets.length === 0 ? (
                      <tr className="border-b border-gray-100">
                        <td className="py-2 pr-4 text-gray-400" colSpan={3}>데이터 없음</td>
                      </tr>
                    ) : (
                      balanceSheet.assets.map((r) => (
                        <tr key={`ast-${r.accountCode}`} className="border-b border-gray-100">
                          <td className="py-2 pr-4 pl-4 font-mono text-gray-600">{r.accountCode}</td>
                          <td className="py-2 pr-4 text-gray-700">{r.accountName}</td>
                          <td className="py-2 text-right text-gray-700">{formatMoneyOne(r.amount, bsCcy)}</td>
                        </tr>
                      ))
                    )}
                    <tr className="border-b border-gray-200 font-medium text-gray-900">
                      <td className="py-2 pr-4" colSpan={2}>자산 합계</td>
                      <td className="py-2 text-right">{formatMoneyOne(balanceSheet.totalAssets, bsCcy)}</td>
                    </tr>

                    {/* 부채 */}
                    <tr className="bg-gray-50 text-gray-700 font-medium border-b border-gray-200">
                      <td className="py-2 pr-4" colSpan={3}>부채</td>
                    </tr>
                    {balanceSheet.liabilities.length === 0 ? (
                      <tr className="border-b border-gray-100">
                        <td className="py-2 pr-4 text-gray-400" colSpan={3}>데이터 없음</td>
                      </tr>
                    ) : (
                      balanceSheet.liabilities.map((r) => (
                        <tr key={`lia-${r.accountCode}`} className="border-b border-gray-100">
                          <td className="py-2 pr-4 pl-4 font-mono text-gray-600">{r.accountCode}</td>
                          <td className="py-2 pr-4 text-gray-700">{r.accountName}</td>
                          <td className="py-2 text-right text-gray-700">{formatMoneyOne(r.amount, bsCcy)}</td>
                        </tr>
                      ))
                    )}
                    <tr className="border-b border-gray-200 font-medium text-gray-900">
                      <td className="py-2 pr-4" colSpan={2}>부채 합계</td>
                      <td className="py-2 text-right">{formatMoneyOne(balanceSheet.totalLiabilities, bsCcy)}</td>
                    </tr>

                    {/* 자본 */}
                    <tr className="bg-gray-50 text-gray-700 font-medium border-b border-gray-200">
                      <td className="py-2 pr-4" colSpan={3}>자본</td>
                    </tr>
                    {balanceSheet.equity.length === 0 ? (
                      <tr className="border-b border-gray-100">
                        <td className="py-2 pr-4 text-gray-400" colSpan={3}>데이터 없음</td>
                      </tr>
                    ) : (
                      balanceSheet.equity.map((r) => (
                        <tr key={`eqt-${r.accountCode}`} className="border-b border-gray-100">
                          <td className="py-2 pr-4 pl-4 font-mono text-gray-600">{r.accountCode}</td>
                          <td className="py-2 pr-4 text-gray-700">{r.accountName}</td>
                          <td className="py-2 text-right text-gray-700">{formatMoneyOne(r.amount, bsCcy)}</td>
                        </tr>
                      ))
                    )}
                    <tr className="border-b border-gray-100 text-gray-700">
                      <td className="py-2 pr-4 pl-4" colSpan={2}>당기순이익(이익잉여금 가산)</td>
                      <td className={`py-2 text-right ${balanceSheet.netIncome < 0 ? 'text-rose-600' : 'text-gray-700'}`}>
                        {formatMoneyOne(balanceSheet.netIncome, bsCcy)}
                      </td>
                    </tr>
                    <tr className="border-b border-gray-200 font-medium text-gray-900">
                      <td className="py-2 pr-4" colSpan={2}>자본 합계(당기순이익 포함)</td>
                      <td className="py-2 text-right">
                        {formatMoneyOne(balanceSheet.totalEquity + balanceSheet.netIncome, bsCcy)}
                      </td>
                    </tr>

                    {/* 부채+자본 */}
                    <tr className="border-t-2 border-gray-300 font-semibold text-gray-900">
                      <td className="py-2 pr-4" colSpan={2}>부채 + 자본 합계</td>
                      <td className="py-2 text-right">
                        {formatMoneyOne(
                          balanceSheet.totalLiabilities + balanceSheet.totalEquity + balanceSheet.netIncome,
                          bsCcy,
                        )}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </>
          )}
        </SectionCard>
      </div>
    </div>
  )
}
