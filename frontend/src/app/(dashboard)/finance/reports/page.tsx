import { safeGet } from '@/lib/api'
import { formatMoneyOne } from '@/lib/money'
import { Badge } from '@/components/ui/badge'
import type {
  TrialBalanceResponse,
  IncomeStatementResponse,
  BalanceSheetResponse,
} from '@/types/finance'
import YearSelect from './year-select'

export const metadata = { title: '재무제표 | ERP' }

function SectionCard({
  title,
  action,
  children,
}: {
  title: string
  action?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <div className="bg-card rounded-lg border border-border p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-foreground">{title}</h2>
        {action}
      </div>
      {children}
    </div>
  )
}

type StatementLine = { accountCode: string; accountName: string; amount: number }

// 손익·재무상태표의 한 그룹(그룹헤더 + 빈 안내/라인 + 합계행)을 tbody 안에 렌더.
// totalLabel 미지정 시 `${title} 합계`. children은 합계행 직전에 끼워 넣는다(예: 당기순이익 가산).
function StatementGroup({
  title,
  keyPrefix,
  rows,
  total,
  currency,
  totalLabel,
  children,
}: {
  title: string
  keyPrefix: string
  rows: StatementLine[]
  total: number
  currency: string
  totalLabel?: string
  children?: React.ReactNode
}) {
  return (
    <>
      <tr className="bg-muted/40 text-foreground font-medium border-b border-border">
        <td className="py-2 pr-4" colSpan={3}>
          {title}
        </td>
      </tr>
      {rows.length === 0 ? (
        <tr className="border-b border-border">
          <td className="py-2 pr-4 text-muted-foreground" colSpan={3}>
            데이터 없음
          </td>
        </tr>
      ) : (
        rows.map((r) => (
          <tr key={`${keyPrefix}-${r.accountCode}`} className="border-b border-border">
            <td className="py-2 pr-4 pl-4 font-mono text-muted-foreground">{r.accountCode}</td>
            <td className="py-2 pr-4 text-foreground">{r.accountName}</td>
            <td className="py-2 text-right text-foreground">
              {formatMoneyOne(r.amount, currency)}
            </td>
          </tr>
        ))
      )}
      {children}
      <tr className="border-b border-border font-medium text-foreground">
        <td className="py-2 pr-4" colSpan={2}>
          {totalLabel ?? `${title} 합계`}
        </td>
        <td className="py-2 text-right">{formatMoneyOne(total, currency)}</td>
      </tr>
    </>
  )
}

function ExcludedNotice({ count }: { count: number }) {
  if (count <= 0) return null
  return (
    <p className="text-xs text-warning mb-3">환율 미산정 {count.toLocaleString('ko-KR')}건 제외</p>
  )
}

const EMPTY = <p className="text-sm text-muted-foreground">데이터 없음</p>

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

  const trialCcy = trialBalance?.baseCurrency ?? 'KRW'
  const incomeCcy = incomeStatement?.baseCurrency ?? 'KRW'
  const balanceCcy = balanceSheet?.baseCurrency ?? 'KRW'

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">재무제표</h1>
          <p className="text-sm text-muted-foreground mt-1">
            시산표 · 손익계산서 · 재무상태표 ({year}년)
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">회계연도</span>
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
                      <tr className="text-left text-muted-foreground border-b border-border">
                        <th className="py-2 pr-4 font-medium">계정코드</th>
                        <th className="py-2 pr-4 font-medium">계정명</th>
                        <th className="py-2 pr-4 font-medium text-right">차변</th>
                        <th className="py-2 pr-4 font-medium text-right">대변</th>
                        <th className="py-2 font-medium text-right">잔액</th>
                      </tr>
                    </thead>
                    <tbody>
                      {trialBalance.rows.map((r) => (
                        <tr key={r.accountCode} className="border-b border-border">
                          <td className="py-2 pr-4 font-mono text-foreground">{r.accountCode}</td>
                          <td className="py-2 pr-4 text-foreground">{r.accountName}</td>
                          <td className="py-2 pr-4 text-right text-foreground">
                            {formatMoneyOne(r.debit, trialCcy)}
                          </td>
                          <td className="py-2 pr-4 text-right text-foreground">
                            {formatMoneyOne(r.credit, trialCcy)}
                          </td>
                          <td className="py-2 text-right text-foreground">
                            {formatMoneyOne(r.balance, trialCcy)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr className="border-t-2 border-border font-semibold text-foreground">
                        <td className="py-2 pr-4" colSpan={2}>
                          합계
                        </td>
                        <td className="py-2 pr-4 text-right">
                          {formatMoneyOne(trialBalance.totalDebit, trialCcy)}
                        </td>
                        <td className="py-2 pr-4 text-right">
                          {formatMoneyOne(trialBalance.totalCredit, trialCcy)}
                        </td>
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
                    <StatementGroup
                      title="수익"
                      keyPrefix="rev"
                      rows={incomeStatement.revenues}
                      total={incomeStatement.totalRevenue}
                      currency={incomeCcy}
                    />
                    <StatementGroup
                      title="비용"
                      keyPrefix="exp"
                      rows={incomeStatement.expenses}
                      total={incomeStatement.totalExpense}
                      currency={incomeCcy}
                    />

                    {/* 당기순이익 */}
                    <tr className="border-t-2 border-border font-semibold text-foreground">
                      <td className="py-2 pr-4" colSpan={2}>
                        당기순이익
                      </td>
                      <td
                        className={`py-2 text-right ${incomeStatement.netIncome < 0 ? 'text-destructive' : 'text-foreground'}`}
                      >
                        {formatMoneyOne(incomeStatement.netIncome, incomeCcy)}
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
                    <StatementGroup
                      title="자산"
                      keyPrefix="ast"
                      rows={balanceSheet.assets}
                      total={balanceSheet.totalAssets}
                      currency={balanceCcy}
                    />
                    <StatementGroup
                      title="부채"
                      keyPrefix="lia"
                      rows={balanceSheet.liabilities}
                      total={balanceSheet.totalLiabilities}
                      currency={balanceCcy}
                    />
                    <StatementGroup
                      title="자본"
                      keyPrefix="eqt"
                      rows={balanceSheet.equity}
                      total={balanceSheet.totalEquity + balanceSheet.netIncome}
                      currency={balanceCcy}
                      totalLabel="자본 합계(당기순이익 포함)"
                    >
                      <tr className="border-b border-border text-foreground">
                        <td className="py-2 pr-4 pl-4" colSpan={2}>
                          당기순이익(이익잉여금 가산)
                        </td>
                        <td
                          className={`py-2 text-right ${balanceSheet.netIncome < 0 ? 'text-destructive' : 'text-foreground'}`}
                        >
                          {formatMoneyOne(balanceSheet.netIncome, balanceCcy)}
                        </td>
                      </tr>
                    </StatementGroup>

                    {/* 부채+자본 */}
                    <tr className="border-t-2 border-border font-semibold text-foreground">
                      <td className="py-2 pr-4" colSpan={2}>
                        부채 + 자본 합계
                      </td>
                      <td className="py-2 text-right">
                        {formatMoneyOne(
                          balanceSheet.totalLiabilities +
                            balanceSheet.totalEquity +
                            balanceSheet.netIncome,
                          balanceCcy,
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
