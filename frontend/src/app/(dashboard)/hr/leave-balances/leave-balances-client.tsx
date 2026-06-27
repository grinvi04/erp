'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { Label } from '@/components/ui/label'
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
    },
    {
      key: 'carryOverDays',
      header: '이월 일수',
      align: 'right',
      sortable: true,
      sortValue: (b) => b.carryOverDays,
      cell: (b) => <span className="text-sm text-muted-foreground">{b.carryOverDays}</span>,
    },
    {
      key: 'usedDays',
      header: '사용 일수',
      align: 'right',
      sortable: true,
      sortValue: (b) => b.usedDays,
      cell: (b) => <span className="text-sm text-muted-foreground">{b.usedDays}</span>,
    },
    {
      key: 'remainingDays',
      header: '잔여 일수',
      align: 'right',
      sortable: true,
      sortValue: (b) => b.remainingDays,
      cell: (b) => <span className="text-sm font-medium">{b.remainingDays}</span>,
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="휴가 잔여"
        description="직원별 연도 휴가 부여·사용·잔여 현황을 조회합니다"
        className="mb-6"
      />

      <div className="mb-4 flex flex-wrap gap-4">
        <div className="w-72">
          <Label className="mb-1.5 block">직원</Label>
          <Select value={empId} onValueChange={onSelectEmployee}>
            <SelectTrigger className="w-full">
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
        </div>
        <div className="w-40">
          <Label className="mb-1.5 block">연도</Label>
          <Select value={year} onValueChange={onSelectYear}>
            <SelectTrigger className="w-full">
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
        </div>
      </div>

      <DataTable
        data={empId ? balances : []}
        columns={columns}
        getRowId={(b) => b.id}
        loading={!!empId && loading}
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
  )
}
