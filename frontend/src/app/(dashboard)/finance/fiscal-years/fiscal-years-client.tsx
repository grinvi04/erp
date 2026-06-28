'use client'
import { useState, useTransition, useMemo } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, LockIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import { createFiscalYear, closeFiscalYear, closeFiscalPeriod } from './actions'
import type {
  FiscalYear,
  FiscalPeriod,
  FiscalYearStatus,
  FiscalPeriodStatus,
} from '@/types/finance'

const YEAR_LABEL: Record<FiscalYearStatus, string> = { OPEN: '진행중', CLOSED: '마감' }
const YEAR_VARIANT: Record<FiscalYearStatus, 'default' | 'secondary'> = {
  OPEN: 'default',
  CLOSED: 'secondary',
}
const PERIOD_LABEL: Record<FiscalPeriodStatus, string> = {
  OPEN: '열림',
  CLOSED: '마감',
  LOCKED: '잠금',
}
const PERIOD_VARIANT: Record<FiscalPeriodStatus, 'default' | 'secondary' | 'outline'> = {
  OPEN: 'default',
  CLOSED: 'secondary',
  LOCKED: 'outline',
}

const dateRange = (start: string, end: string) => `${start} ~ ${end}`

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'closeYear'; year: FiscalYear }
  | { type: 'closePeriod'; period: FiscalPeriod }

interface Props {
  years: FiscalYear[]
  periods: FiscalPeriod[]
}

export default function FiscalYearsClient({ years, periods }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)

  const periodCountByYear = useMemo(() => {
    const m = new Map<number, number>()
    for (const p of periods) m.set(p.fiscalYearId, (m.get(p.fiscalYearId) ?? 0) + 1)
    return m
  }, [periods])

  const [selectedYearId, setSelectedYearId] = useState<number | null>(years[0]?.id ?? null)
  const selectedYear = years.find((y) => y.id === selectedYearId) ?? null
  const selectedPeriods = useMemo(
    () =>
      selectedYearId == null ? [] : periods.filter((p) => p.fiscalYearId === selectedYearId),
    [periods, selectedYearId],
  )

  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [year, setYear] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  const openCreate = () => {
    const next = new Date().getFullYear()
    setYear(String(next))
    setStartDate(`${next}-01-01`)
    setEndDate(`${next}-12-31`)
    setDialog({ type: 'create' })
  }

  const onYearChange = (v: string) => {
    setYear(v)
    const n = Number(v)
    if (Number.isInteger(n) && n >= 2000 && n <= 2100) {
      setStartDate(`${n}-01-01`)
      setEndDate(`${n}-12-31`)
    }
  }

  const handleCreate = () => {
    const n = Number(year)
    if (!Number.isInteger(n) || n < 2000 || n > 2100) {
      toast.error('연도는 2000~2100 사이여야 합니다')
      return
    }
    if (!startDate || !endDate) {
      toast.error('시작일과 종료일을 입력해주세요')
      return
    }
    if (startDate > endDate) {
      toast.error('종료일은 시작일 이후여야 합니다')
      return
    }
    startTransition(async () => {
      try {
        await createFiscalYear({ year: n, startDate, endDate })
        toast.success(`${n} 회계연도가 생성되었습니다 (12개 기간)`)
        setSelectedYearId(null)
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '생성 중 오류가 발생했습니다')
      }
    })
  }

  const handleCloseYear = (y: FiscalYear) => {
    startTransition(async () => {
      try {
        await closeFiscalYear(y.id)
        toast.success(`${y.year} 회계연도가 마감되었습니다`)
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '마감 중 오류가 발생했습니다')
      }
    })
  }

  const handleClosePeriod = (p: FiscalPeriod) => {
    startTransition(async () => {
      try {
        await closeFiscalPeriod(p.id)
        toast.success(`${p.periodNumber}월 기간이 마감되었습니다`)
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '마감 중 오류가 발생했습니다')
      }
    })
  }

  const yearColumns: Column<FiscalYear>[] = [
    {
      key: 'year',
      header: '회계연도',
      sortable: true,
      sortValue: (y) => y.year,
      cell: (y) => <span className="font-medium tabular-nums">{y.year}</span>,
    },
    {
      key: 'range',
      header: '기간',
      cell: (y) => (
        <span className="text-sm text-muted-foreground tabular-nums">
          {dateRange(y.startDate, y.endDate)}
        </span>
      ),
    },
    {
      key: 'periodCount',
      header: '기간수',
      align: 'right',
      cell: (y) => (
        <span className="text-sm text-muted-foreground tabular-nums">
          {periodCountByYear.get(y.id) ?? 0}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (y) => (y.status === 'OPEN' ? 0 : 1),
      cell: (y) => <Badge variant={YEAR_VARIANT[y.status]}>{YEAR_LABEL[y.status]}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (y) => (
        <div className="flex justify-end gap-1">
          {canWrite && y.status === 'OPEN' && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="연도 마감"
              onClick={(e) => {
                e.stopPropagation()
                setDialog({ type: 'closeYear', year: y })
              }}
            >
              <LockIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  const periodColumns: Column<FiscalPeriod>[] = [
    {
      key: 'periodNumber',
      header: '기간',
      sortable: true,
      sortValue: (p) => p.periodNumber,
      cell: (p) => <span className="font-medium tabular-nums">{p.periodNumber}월</span>,
    },
    {
      key: 'range',
      header: '일자',
      cell: (p) => (
        <span className="text-sm text-muted-foreground tabular-nums">
          {dateRange(p.startDate, p.endDate)}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      cell: (p) => <Badge variant={PERIOD_VARIANT[p.status]}>{PERIOD_LABEL[p.status]}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (p) => (
        <div className="flex justify-end gap-1">
          {canWrite && p.status === 'OPEN' && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="기간 마감"
              onClick={() => setDialog({ type: 'closePeriod', period: p })}
            >
              <LockIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="회계기간"
        description="회계연도와 월별 기간을 생성·마감합니다. 전표 입력·재무제표의 선행 조건입니다."
        className="mb-6"
      >
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 회계연도
          </Button>
        )}
      </PageHeader>

      <DataTable
        data={years}
        columns={yearColumns}
        getRowId={(y) => y.id}
        onRowClick={(y) => setSelectedYearId(y.id)}
        initialSort={{ key: 'year', dir: 'desc' }}
        empty={
          <EmptyState
            title="등록된 회계연도가 없습니다"
            description={canWrite ? '우측 상단에서 새 회계연도를 생성하세요.' : undefined}
          />
        }
      />

      {selectedYear && (
        <section className="mt-8">
          <h2 className="mb-3 text-sm font-semibold text-foreground">
            {selectedYear.year} 회계연도 기간
          </h2>
          <DataTable
            data={selectedPeriods}
            columns={periodColumns}
            getRowId={(p) => p.id}
            initialSort={{ key: 'periodNumber', dir: 'asc' }}
            empty={<EmptyState title="등록된 기간이 없습니다" />}
          />
        </section>
      )}

      {/* Create year dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 회계연도 생성</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>회계연도 *</Label>
              <Input
                type="number"
                value={year}
                onChange={(e) => onYearChange(e.target.value)}
                placeholder="2026"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>시작일 *</Label>
                <Input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
              <div className="grid gap-1.5">
                <Label>종료일 *</Label>
                <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
              </div>
            </div>
            <p className="text-sm text-muted-foreground">
              생성 시 시작일~종료일 범위의 월별 기간이 자동으로 만들어집니다.
            </p>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              생성
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Close year dialog */}
      <Dialog
        open={dialog.type === 'closeYear'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>회계연도 마감</DialogTitle>
          </DialogHeader>
          {dialog.type === 'closeYear' && (
            <p className="py-2 text-sm text-muted-foreground">
              <strong>{dialog.year.year}</strong> 회계연도를 마감하시겠습니까? 소속된 열린 기간도 함께
              마감되며, 이후 해당 연도에는 전표를 입력할 수 없습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'closeYear' && handleCloseYear(dialog.year)}
              disabled={isPending}
            >
              마감
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Close period dialog */}
      <Dialog
        open={dialog.type === 'closePeriod'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>기간 마감</DialogTitle>
          </DialogHeader>
          {dialog.type === 'closePeriod' && (
            <p className="py-2 text-sm text-muted-foreground">
              <strong>{dialog.period.periodNumber}월</strong> 기간을 마감하시겠습니까? 마감 후에는
              해당 기간에 전표를 입력할 수 없습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'closePeriod' && handleClosePeriod(dialog.period)}
              disabled={isPending}
            >
              마감
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
