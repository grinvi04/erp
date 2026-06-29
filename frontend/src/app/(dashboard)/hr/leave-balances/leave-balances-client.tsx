'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { downloadCsv } from '@/lib/csv'
import { fetchLeaveBalances } from './actions'
import type { LeaveBalance, Employee } from '@/types/hr'

interface Props {
  employees: Employee[]
}

const CURRENT_YEAR = new Date().getFullYear()
const YEARS = Array.from({ length: 5 }, (_, i) => CURRENT_YEAR - i)

export default function LeaveBalancesClient({ employees }: Props) {
  const [empId, setEmpId] = useState<string>('')
  const [year, setYear] = useState<string>(String(CURRENT_YEAR))
  const [balances, setBalances] = useState<LeaveBalance[]>([])
  const [loading, setLoading] = useState(false)
  const [, startTransition] = useTransition()

  const load = (id: string, yr: string) => {
    if (!id) {
      setBalances([])
      return
    }
    setLoading(true)
    startTransition(async () => {
      try {
        const list = await fetchLeaveBalances(Number(id), Number(yr))
        setBalances(list)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '잔여 조회 중 오류가 발생했습니다')
        setBalances([])
      } finally {
        setLoading(false)
      }
    })
  }

  const onSelectEmployee = (v: string | null) => {
    const next = v ?? ''
    setEmpId(next)
    load(next, year)
  }

  const onSelectYear = (v: string | null) => {
    const next = v ?? String(CURRENT_YEAR)
    setYear(next)
    load(empId, next)
  }

  const columns: Column<LeaveBalance>[] = [
    {
      key: 'leavePolicyName',
      header: '휴가 정책',
      sortable: true,
      sortValue: (b) => b.leavePolicyName,
      cell: (b) => <span className="font-medium">{b.leavePolicyName}</span>,
    },
    {
      key: 'entitledDays',
      header: '부여 일수',
      align: 'right',
      sortable: true,
      sortValue: (b) => b.entitledDays,
      cell: (b) => <span className="text-sm text-muted-foreground">{b.entitledDays}</span>,
      footer: (rows) => (
        <span className="font-medium">
          {rows.reduce((s, r) => s + r.entitledDays, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'carryOverDays',
      header: '이월 일수',
      align: 'right',
      sortable: true,
      sortValue: (b) => b.carryOverDays,
      cell: (b) => <span className="text-sm text-muted-foreground">{b.carryOverDays}</span>,
      footer: (rows) => (
        <span className="font-medium">
          {rows.reduce((s, r) => s + r.carryOverDays, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'usedDays',
      header: '사용 일수',
      align: 'right',
      sortable: true,
      sortValue: (b) => b.usedDays,
      cell: (b) => <span className="text-sm text-muted-foreground">{b.usedDays}</span>,
      footer: (rows) => (
        <span className="font-medium">
          {rows.reduce((s, r) => s + r.usedDays, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'remainingDays',
      header: '잔여 일수',
      align: 'right',
      sortable: true,
      sortValue: (b) => b.remainingDays,
      cell: (b) => <span className="text-sm font-medium">{b.remainingDays}</span>,
      footer: (rows) => (
        <span className="font-medium">
          {rows.reduce((s, r) => s + r.remainingDays, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 직원·연도는 서버 조회 드라이버(기존 로직 유지). 휴가정책·잔여여부는
  // 로드된 데이터에 대한 클라이언트 필터(입력 draft + 적용 applied 분리).
  const [qPolicy, setQPolicy] = useState('')
  const [qRemaining, setQRemaining] = useState('')
  const [applied, setApplied] = useState({ policy: '', remaining: '' })
  const onSearch = () => setApplied({ policy: qPolicy, remaining: qRemaining })
  const onReset = () => {
    setQPolicy('')
    setQRemaining('')
    setApplied({ policy: '', remaining: '' })
  }
  const filtered = balances.filter((b) => {
    if (applied.policy && !b.leavePolicyName.toLowerCase().includes(applied.policy.toLowerCase()))
      return false
    if (applied.remaining === 'Y' && b.remainingDays <= 0) return false
    if (applied.remaining === 'N' && b.remainingDays > 0) return false
    return true
  })

  const exportExcel = () =>
    downloadCsv(
      `휴가잔여_${year}_${new Date().toISOString().slice(0, 10)}`,
      ['휴가 정책', '부여 일수', '이월 일수', '사용 일수', '잔여 일수'],
      filtered.map((b) => [
        b.leavePolicyName,
        b.entitledDays,
        b.carryOverDays,
        b.usedDays,
        b.remainingDays,
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader
        title="휴가 잔여"
        description="직원별 연도 휴가 부여·사용·잔여 현황을 조회합니다"
        className="mb-4"
      >
        <Button variant="outline" onClick={exportExcel} disabled={!empId || filtered.length === 0}>
          <DownloadIcon />
          엑셀
        </Button>
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="직원">
            <Select value={empId} onValueChange={onSelectEmployee}>
              <SelectTrigger className="h-8 w-64">
                <SelectValue placeholder="직원을 선택하세요" />
              </SelectTrigger>
              <SelectContent>
                {employees.map((e) => (
                  <SelectItem key={e.id} value={String(e.id)}>
                    {e.fullName} ({e.employeeNo})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="연도">
            <Select value={year} onValueChange={onSelectYear}>
              <SelectTrigger className="h-8 w-28">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {YEARS.map((y) => (
                  <SelectItem key={y} value={String(y)}>
                    {y}년
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="휴가 정책">
            <Input
              value={qPolicy}
              onChange={(e) => setQPolicy(e.target.value)}
              placeholder="정책명"
              className="h-8 w-40"
            />
          </FilterField>
          <FilterField label="잔여여부">
            <Select
              value={qRemaining || 'ALL'}
              onValueChange={(v) => setQRemaining(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="Y">잔여 있음</SelectItem>
                <SelectItem value="N">잔여 없음</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={empId ? filtered : []}
          columns={columns}
          getRowId={(b) => b.id}
          loading={!!empId && loading}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title={
                empId
                  ? '해당 연도의 휴가 잔여 내역이 없습니다'
                  : '직원을 선택하면 휴가 잔여가 표시됩니다'
              }
            />
          }
        />
      </div>
    </div>
  )
}
