'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { DownloadIcon, ReceiptTextIcon, TrendingUpIcon, TrendingDownIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/components/ui/page-header'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { StatCard } from '@/components/ui/stat-card'
import { DataTable, type Column } from '@/components/ui/data-table'
import { EmptyState } from '@/components/ui/empty-state'
import { downloadCsv } from '@/lib/csv'
import type { VatReturn, VatPartyLine } from '@/types/finance'

const won = (n: number) => `₩${n.toLocaleString('ko-KR')}`

function quarterRange(year: number, q: 1 | 2 | 3 | 4): { from: string; to: string } {
  const startMonth = (q - 1) * 3 + 1
  const endMonth = startMonth + 2
  const pad = (n: number) => String(n).padStart(2, '0')
  const lastDay = new Date(year, endMonth, 0).getDate()
  return { from: `${year}-${pad(startMonth)}-01`, to: `${year}-${pad(endMonth)}-${pad(lastDay)}` }
}

export default function VatReturnClient({
  data,
  from,
  to,
}: {
  data: VatReturn
  from: string
  to: string
}) {
  const router = useRouter()
  const [qFrom, setQFrom] = useState(from)
  const [qTo, setQTo] = useState(to)

  const search = (f: string, t: string) =>
    router.push(`/finance/vat-return?from=${encodeURIComponent(f)}&to=${encodeURIComponent(t)}`)

  const onSearch = () => search(qFrom, qTo)
  const onReset = () => {
    setQFrom(from)
    setQTo(to)
  }
  const applyQuarter = (q: 1 | 2 | 3 | 4) => {
    const year = Number(qFrom.slice(0, 4)) || new Date().getFullYear()
    const { from: f, to: t } = quarterRange(year, q)
    setQFrom(f)
    setQTo(t)
    search(f, t)
  }

  const isRefund = data.payableTax < 0

  const partyColumns = (): Column<VatPartyLine>[] => [
    {
      key: 'businessNo',
      header: '사업자번호',
      cell: (r) => <span className="font-mono text-sm">{r.businessNo ?? '미상'}</span>,
    },
    {
      key: 'name',
      header: '거래처',
      sortable: true,
      sortValue: (r) => r.name,
      cell: (r) => <span className="font-medium">{r.name}</span>,
    },
    {
      key: 'count',
      header: '매수',
      align: 'right',
      sortable: true,
      sortValue: (r) => r.count,
      cell: (r) => <span className="font-mono text-sm">{r.count}</span>,
      footer: (rows) => <span className="font-mono">{rows.reduce((s, r) => s + r.count, 0)}</span>,
    },
    {
      key: 'supplyTotal',
      header: '공급가액',
      align: 'right',
      sortable: true,
      sortValue: (r) => r.supplyTotal,
      cell: (r) => (
        <span className="font-mono text-sm">{r.supplyTotal.toLocaleString('ko-KR')}</span>
      ),
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + r.supplyTotal, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'vatTotal',
      header: '세액',
      align: 'right',
      sortable: true,
      sortValue: (r) => r.vatTotal,
      cell: (r) => <span className="font-mono text-sm">{r.vatTotal.toLocaleString('ko-KR')}</span>,
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + r.vatTotal, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
  ]

  const exportExcel = (rows: VatPartyLine[], kind: string) => {
    downloadCsv(
      `부가세신고_${kind}_${from}_${to}`,
      ['사업자번호', '거래처', '매수', '공급가액', '세액'],
      rows.map((r) => [r.businessNo ?? '미상', r.name, r.count, r.supplyTotal, r.vatTotal]),
    )
  }

  return (
    <div className="p-5">
      <PageHeader
        title="부가세 신고"
        description="신고기간의 매출세액·매입세액·납부(환급)세액과 매출처별·매입처별 합계표"
        className="mb-4"
      />

      <div className="space-y-4">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="신고기간">
            <Input
              type="date"
              value={qFrom}
              onChange={(e) => setQFrom(e.target.value)}
              className="h-8 w-36"
            />
            <span className="text-muted-foreground">~</span>
            <Input
              type="date"
              value={qTo}
              onChange={(e) => setQTo(e.target.value)}
              className="h-8 w-36"
            />
          </FilterField>
          <FilterField label="분기">
            <div className="flex gap-1">
              {([1, 2, 3, 4] as const).map((q) => (
                <Button
                  key={q}
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => applyQuarter(q)}
                >
                  {q}분기
                </Button>
              ))}
            </div>
          </FilterField>
        </FilterBar>

        {/* 요약 */}
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatCard
            label="매출 과세표준"
            value={won(data.sales.taxableSupply)}
            sub={`영세율 ${won(data.sales.zeroRatedSupply)} · 면세 ${won(data.sales.exemptSupply)}`}
            icon={ReceiptTextIcon}
          />
          <StatCard
            label="매출세액"
            value={won(data.sales.totalVat)}
            icon={TrendingUpIcon}
            tone="primary"
          />
          <StatCard label="매입세액" value={won(data.purchases.vat)} icon={TrendingDownIcon} />
          <StatCard
            label={isRefund ? '환급세액' : '납부세액'}
            value={won(Math.abs(data.payableTax))}
            sub={`${data.from} ~ ${data.to}`}
            icon={ReceiptTextIcon}
            tone={isRefund ? 'success' : 'warning'}
          />
        </div>

        {/* 매출처별 합계표 */}
        <section className="space-y-2">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-foreground">매출처별 세금계산서 합계표</h2>
            <Button
              variant="outline"
              size="sm"
              onClick={() => exportExcel(data.salesByBuyer, '매출')}
            >
              <DownloadIcon />
              엑셀
            </Button>
          </div>
          <DataTable
            data={data.salesByBuyer}
            columns={partyColumns()}
            getRowId={(r) => `${r.businessNo ?? 'na'}-${r.name}`}
            showTotals
            totalLabel={`총 ${data.salesByBuyer.length}곳`}
            empty={<EmptyState icon={ReceiptTextIcon} title="매출 내역이 없습니다" />}
          />
        </section>

        {/* 매입처별 합계표 */}
        <section className="space-y-2">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-foreground">매입처별 세금계산서 합계표</h2>
            <Button
              variant="outline"
              size="sm"
              onClick={() => exportExcel(data.purchasesByVendor, '매입')}
            >
              <DownloadIcon />
              엑셀
            </Button>
          </div>
          <DataTable
            data={data.purchasesByVendor}
            columns={partyColumns()}
            getRowId={(r) => `${r.businessNo ?? 'na'}-${r.name}`}
            showTotals
            totalLabel={`총 ${data.purchasesByVendor.length}곳`}
            empty={<EmptyState icon={ReceiptTextIcon} title="매입 내역이 없습니다" />}
          />
        </section>
      </div>
    </div>
  )
}
