'use client'
import { useState, useTransition, useMemo } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, LockIcon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DatePicker } from '@/components/ui/date-picker'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
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
    () => (selectedYearId == null ? [] : periods.filter((p) => p.fiscalYearId === selectedYearId)),
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
      footer: (rows) => (
        <span className="tabular-nums">
          {rows.reduce((s, r) => s + (periodCountByYear.get(r.id) ?? 0), 0).toLocaleString('ko-KR')}
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

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용.
  const [qYear, setQYear] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [applied, setApplied] = useState({ year: '', status: '' })
  const onSearch = () => setApplied({ year: qYear, status: qStatus })
  const onReset = () => {
    setQYear('')
    setQStatus('')
    setApplied({ year: '', status: '' })
  }
  const filteredYears = years.filter((y) => {
    if (applied.year && !String(y.year).includes(applied.year)) return false
    if (applied.status && y.status !== applied.status) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `회계연도_${new Date().toISOString().slice(0, 10)}`,
      ['회계연도', '시작일', '종료일', '기간수', '상태'],
      filteredYears.map((y) => [
        y.year,
        y.startDate,
        y.endDate,
        periodCountByYear.get(y.id) ?? 0,
        YEAR_LABEL[y.status],
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader
        title="회계기간"
        description="회계연도와 월별 기간을 생성·마감합니다. 전표 입력·재무제표의 선행 조건입니다."
        className="mb-4"
      >
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 회계연도
          </Button>
        )}
      </PageHeader>

      <FilterBar onSearch={onSearch} onReset={onReset}>
        <FilterField label="회계연도">
          <Input
            value={qYear}
            onChange={(e) => setQYear(e.target.value)}
            placeholder="2026"
            className="h-8 w-28"
          />
        </FilterField>
        <FilterField label="상태">
          <Select value={qStatus || 'ALL'} onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}>
            <SelectTrigger className="h-8 w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체</SelectItem>
              {(Object.keys(YEAR_LABEL) as FiscalYearStatus[]).map((s) => (
                <SelectItem key={s} value={s}>
                  {YEAR_LABEL[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FilterField>
      </FilterBar>

      <div className="mt-3">
        <DataTable
          data={filteredYears}
          columns={yearColumns}
          getRowId={(y) => y.id}
          onRowClick={(y) => setSelectedYearId(y.id)}
          initialSort={{ key: 'year', dir: 'desc' }}
          showTotals
          totalLabel={`총 ${filteredYears.length}건`}
          empty={
            <EmptyState
              title="등록된 회계연도가 없습니다"
              description={canWrite ? '우측 상단에서 새 회계연도를 생성하세요.' : undefined}
            />
          }
        />
      </div>

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
            showTotals
            totalLabel={`총 ${selectedPeriods.length}건`}
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
            <FormGrid>
              <FormRow label="회계연도" required span>
                <Input
                  type="number"
                  value={year}
                  onChange={(e) => onYearChange(e.target.value)}
                  placeholder="2026"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="시작일" required>
                <DatePicker value={startDate} onChange={setStartDate} />
              </FormRow>
              <FormRow label="종료일" required>
                <DatePicker value={endDate} onChange={setEndDate} />
              </FormRow>
            </FormGrid>
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
              <strong>{dialog.year.year}</strong> 회계연도를 마감하시겠습니까? 소속된 열린 기간도
              함께 마감되며, 이후 해당 연도에는 전표를 입력할 수 없습니다.
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
