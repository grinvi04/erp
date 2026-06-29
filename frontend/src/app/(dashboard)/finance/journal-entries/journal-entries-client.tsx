'use client'
import { useState, useTransition, useMemo } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, TrashIcon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { DataTable, type Column } from '@/components/ui/data-table'
import { DetailSheet, DetailRow, DetailSection } from '@/components/ui/detail-sheet'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import { DatePicker } from '@/components/ui/date-picker'
import { Skeleton } from '@/components/ui/skeleton'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { runCsvExport } from '@/lib/csv-export'
import { formatMoneyOne } from '@/lib/money'
import {
  createJournalEntry,
  submitJournalEntry,
  approveJournalEntry,
  withdrawJournalEntry,
  rejectJournalEntry,
  reverseJournalEntry,
  getJournalLines,
  exportAllJournalEntries,
} from './actions'
import type {
  FiscalYear,
  FiscalPeriod,
  JournalEntry,
  JournalEntryStatus,
  JournalEntryType,
  JournalLine,
  Account,
} from '@/types/finance'
import type { PageResponse } from '@/types/api'

const ENTRY_TYPE_LABEL: Record<JournalEntryType, string> = {
  MANUAL: '수기',
  AP: '매입',
  AR: '매출',
  PAYROLL: '급여',
  ADJUSTMENT: '조정',
}
const STATUS_LABEL: Record<JournalEntryStatus, string> = {
  DRAFT: '미결',
  PENDING_APPROVAL: '결재중',
  POSTED: '전기완료',
  REVERSED: '역분개',
}
const STATUS_VARIANT: Record<
  JournalEntryStatus,
  'default' | 'secondary' | 'destructive' | 'outline'
> = {
  DRAFT: 'secondary',
  PENDING_APPROVAL: 'outline',
  POSTED: 'default',
  REVERSED: 'destructive',
}

interface LineRow {
  accountId: string
  debitAmount: string
  creditAmount: string
  description: string
}
const emptyLine = (): LineRow => ({
  accountId: '',
  debitAmount: '',
  creditAmount: '',
  description: '',
})

type ActionDialog =
  | { type: 'none' }
  | { type: 'reject'; entry: JournalEntry }
  | { type: 'reverse'; entry: JournalEntry }

interface Props {
  fiscalYears: FiscalYear[]
  selectedYearId: number | null
  periods: FiscalPeriod[]
  selectedPeriodId: number | null
  entries: PageResponse<JournalEntry> | null
  accounts: Account[]
}

export default function JournalEntriesClient({
  fiscalYears,
  selectedYearId,
  periods,
  selectedPeriodId,
  entries,
  accounts,
}: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)
  const canApprove = can(PERM.FINANCE_GL_APPROVE)
  const router = useRouter()
  const [showCreate, setShowCreate] = useState(false)
  const [actionDialog, setActionDialog] = useState<ActionDialog>({ type: 'none' })
  const [comment, setComment] = useState('')
  const [isPending, startTransition] = useTransition()

  // 전표 상세(drill-in) — 행 클릭 시 헤더 정보 + 차/대변 라인 명세를 읽기전용으로 연다.
  const [detailEntry, setDetailEntry] = useState<JournalEntry | null>(null)
  const [detailLines, setDetailLines] = useState<JournalLine[] | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const openDetail = (entry: JournalEntry) => {
    setDetailEntry(entry)
    setDetailLines(null)
    setDetailLoading(true)
    getJournalLines(entry.id)
      .then((lines) => setDetailLines(lines))
      .catch(() => {
        toast.error('전표 라인을 불러오지 못했습니다')
        setDetailLines([])
      })
      .finally(() => setDetailLoading(false))
  }

  const [entryDate, setEntryDate] = useState('')
  const [description, setDescription] = useState('')
  const [entryType, setEntryType] = useState<JournalEntryType>('MANUAL')
  const [currency, setCurrency] = useState('KRW')
  const [lines, setLines] = useState<LineRow[]>([emptyLine(), emptyLine()])

  const activeAccounts = useMemo(
    () => accounts.filter((a) => a.isActive && !a.isSummary),
    [accounts],
  )
  const selectedPeriod = useMemo(
    () => periods.find((p) => p.id === selectedPeriodId),
    [periods, selectedPeriodId],
  )
  const isPeriodClosed = selectedPeriod?.status === 'CLOSED'

  const totalDebit = useMemo(
    () => lines.reduce((s, l) => s + (Number(l.debitAmount) || 0), 0),
    [lines],
  )
  const totalCredit = useMemo(
    () => lines.reduce((s, l) => s + (Number(l.creditAmount) || 0), 0),
    [lines],
  )
  const balanced = Math.abs(totalDebit - totalCredit) < 0.005

  const onYearChange = (val: string | null) => {
    if (val) router.push(`/finance/journal-entries?fiscalYearId=${val}`)
  }
  const onPeriodChange = (val: string | null) => {
    if (val)
      router.push(`/finance/journal-entries?fiscalYearId=${selectedYearId}&fiscalPeriodId=${val}`)
  }

  const openCreate = () => {
    setEntryDate('')
    setDescription('')
    setEntryType('MANUAL')
    setCurrency('KRW')
    setLines([emptyLine(), emptyLine()])
    setShowCreate(true)
  }

  const addLine = () => setLines((prev) => [...prev, emptyLine()])
  const removeLine = (i: number) => setLines((prev) => prev.filter((_, idx) => idx !== i))
  const setLine = (i: number, field: keyof LineRow, val: string) =>
    setLines((prev) => prev.map((l, idx) => (idx === i ? { ...l, [field]: val } : l)))

  const handleCreate = () => {
    if (!entryDate || !description.trim()) {
      toast.error('날짜와 설명은 필수입니다')
      return
    }
    if (lines.some((l) => !l.accountId)) {
      toast.error('모든 행에 계정과목을 선택해주세요')
      return
    }
    if (!balanced) {
      toast.error(
        `차변(${formatMoneyOne(totalDebit, currency)})과 대변(${formatMoneyOne(totalCredit, currency)})이 일치해야 합니다`,
      )
      return
    }
    if (selectedPeriodId == null) {
      toast.error('회계 기간을 먼저 선택해주세요')
      return
    }

    startTransition(async () => {
      try {
        await createJournalEntry({
          entryDate,
          fiscalPeriodId: selectedPeriodId,
          description: description.trim(),
          entryType,
          currency,
          lines: lines.map((l) => ({
            accountId: Number(l.accountId),
            debitAmount: Number(l.debitAmount) || 0,
            creditAmount: Number(l.creditAmount) || 0,
            description: l.description || null,
            departmentId: null,
          })),
        })
        toast.success('전표가 등록되었습니다')
        setShowCreate(false)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleSubmit = (entry: JournalEntry) => {
    startTransition(async () => {
      try {
        await submitJournalEntry(entry.id)
        toast.success('결재 상신되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '상신 중 오류가 발생했습니다')
      }
    })
  }

  const handleApprove = (entry: JournalEntry) => {
    startTransition(async () => {
      try {
        await approveJournalEntry(entry.id)
        toast.success('승인·전기 처리되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '승인 중 오류가 발생했습니다')
      }
    })
  }

  const handleWithdraw = (entry: JournalEntry) => {
    startTransition(async () => {
      try {
        await withdrawJournalEntry(entry.id)
        toast.success('상신을 철회했습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '철회 중 오류가 발생했습니다')
      }
    })
  }

  const openReject = (entry: JournalEntry) => {
    setComment('')
    setActionDialog({ type: 'reject', entry })
  }
  const openReverse = (entry: JournalEntry) => setActionDialog({ type: 'reverse', entry })
  const closeAction = () => setActionDialog({ type: 'none' })

  const handleReject = (entry: JournalEntry) => {
    if (!comment.trim()) {
      toast.error('반려 사유를 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        await rejectJournalEntry(entry.id, comment.trim())
        toast.success('반려되었습니다')
        closeAction()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '반려 중 오류가 발생했습니다')
      }
    })
  }

  const handleReverse = (entry: JournalEntry) => {
    startTransition(async () => {
      try {
        await reverseJournalEntry(entry.id)
        toast.success('역분개 전표가 생성되었습니다')
        closeAction()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '역분개 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<JournalEntry>[] = [
    {
      key: 'entryNo',
      header: '전표번호',
      sortable: true,
      sortValue: (e) => e.entryNo,
      cell: (e) => <span className="font-mono text-sm">{e.entryNo}</span>,
    },
    {
      key: 'entryDate',
      header: '전표일',
      sortable: true,
      sortValue: (e) => e.entryDate,
      cell: (e) => <span className="text-sm">{e.entryDate}</span>,
    },
    {
      key: 'entryType',
      header: '유형',
      sortable: true,
      sortValue: (e) => ENTRY_TYPE_LABEL[e.entryType],
      cell: (e) => <span className="text-sm">{ENTRY_TYPE_LABEL[e.entryType]}</span>,
    },
    {
      key: 'description',
      header: '설명',
      cellClassName: 'max-w-xs truncate',
      cell: (e) => <span className="text-sm">{e.description}</span>,
    },
    {
      key: 'totalDebit',
      header: '차변 합계',
      align: 'right',
      sortable: true,
      sortValue: (e) => e.totalDebit,
      cell: (e) => (
        <span className="font-mono text-sm">{formatMoneyOne(e.totalDebit, e.currency)}</span>
      ),
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + r.totalDebit, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'totalCredit',
      header: '대변 합계',
      align: 'right',
      sortable: true,
      sortValue: (e) => e.totalCredit,
      cell: (e) => (
        <span className="font-mono text-sm">{formatMoneyOne(e.totalCredit, e.currency)}</span>
      ),
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + r.totalCredit, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (e) => STATUS_LABEL[e.status],
      cell: (e) => <Badge variant={STATUS_VARIANT[e.status]}>{STATUS_LABEL[e.status]}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (entry) => (
        <div className="flex justify-end gap-1" onClick={(e) => e.stopPropagation()}>
          {canWrite && entry.status === 'DRAFT' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleSubmit(entry)}
              disabled={isPending}
              title="결재상신"
            >
              결재상신
            </Button>
          )}
          {canApprove && entry.status === 'PENDING_APPROVAL' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleApprove(entry)}
              disabled={isPending}
              title="승인"
            >
              승인
            </Button>
          )}
          {canApprove && entry.status === 'PENDING_APPROVAL' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => openReject(entry)}
              disabled={isPending}
              title="반려"
              className="text-destructive"
            >
              반려
            </Button>
          )}
          {canWrite && entry.status === 'PENDING_APPROVAL' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleWithdraw(entry)}
              disabled={isPending}
              title="철회"
              className="text-destructive"
            >
              철회
            </Button>
          )}
          {canApprove && entry.status === 'POSTED' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => openReverse(entry)}
              disabled={isPending}
              title="역분개"
              className="text-destructive"
            >
              역분개
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qType, setQType] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [qFrom, setQFrom] = useState('')
  const [qTo, setQTo] = useState('')
  const [applied, setApplied] = useState({ type: '', status: '', from: '', to: '' })
  const onSearch = () => setApplied({ type: qType, status: qStatus, from: qFrom, to: qTo })
  const onReset = () => {
    setQType('')
    setQStatus('')
    setQFrom('')
    setQTo('')
    setApplied({ type: '', status: '', from: '', to: '' })
  }
  const matchesFilter = (e: JournalEntry) => {
    if (applied.type && e.entryType !== applied.type) return false
    if (applied.status && e.status !== applied.status) return false
    if (applied.from && e.entryDate < applied.from) return false
    if (applied.to && e.entryDate > applied.to) return false
    return true
  }
  const filtered = (entries?.content ?? []).filter(matchesFilter)
  const exportColumns = [
    '전표번호',
    '전표일',
    '유형',
    '설명',
    '통화',
    '차변합계',
    '대변합계',
    '상태',
  ]
  const exportRow = (e: JournalEntry) => [
    e.entryNo,
    e.entryDate,
    ENTRY_TYPE_LABEL[e.entryType],
    e.description,
    e.currency,
    e.totalDebit,
    e.totalCredit,
    STATUS_LABEL[e.status],
  ]
  // 전체 엑셀 — 선택 회계기간의 전체 전표(전 페이지 순회) 내보내기. 화면 조회조건을 동일 적용.
  const exportExcel = () => {
    if (selectedPeriodId == null) return
    startTransition(async () => {
      try {
        await runCsvExport(() => exportAllJournalEntries(selectedPeriodId), {
          filename: `전표_${new Date().toISOString().slice(0, 10)}`,
          columns: exportColumns,
          matches: matchesFilter,
          row: exportRow,
        })
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '엑셀 내보내기 중 오류가 발생했습니다')
      }
    })
  }

  return (
    <div className="p-5">
      <PageHeader
        title="전표"
        description="회계 기간을 선택하여 전표 내역을 조회합니다"
        className="mb-4"
      >
        {selectedPeriodId != null && (
          <Button variant="outline" onClick={exportExcel} disabled={isPending}>
            <DownloadIcon />
            엑셀
          </Button>
        )}
        {canWrite && selectedPeriodId != null && (
          <Button
            onClick={openCreate}
            disabled={isPending || isPeriodClosed}
            title={isPeriodClosed ? '마감된 기간에는 전표를 등록할 수 없습니다' : undefined}
          >
            <PlusIcon />새 전표
          </Button>
        )}
      </PageHeader>

      {/* Fiscal Period Selector */}
      <div className="mb-6 flex flex-wrap gap-4">
        <div className="w-full sm:w-48">
          <Label className="text-xs text-muted-foreground mb-1 block">회계연도</Label>
          <Select
            value={selectedYearId != null ? String(selectedYearId) : ''}
            onValueChange={onYearChange}
          >
            <SelectTrigger className="w-full bg-card">
              <SelectValue placeholder="연도 선택" />
            </SelectTrigger>
            <SelectContent>
              {fiscalYears.map((y) => (
                <SelectItem key={y.id} value={String(y.id)}>
                  {y.year}년 ({y.status === 'OPEN' ? '진행' : '마감'})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        {selectedYearId != null && periods.length > 0 && (
          <div className="w-full sm:w-80">
            <Label className="text-xs text-muted-foreground mb-1 block">회계 기간</Label>
            <Select
              value={selectedPeriodId != null ? String(selectedPeriodId) : ''}
              onValueChange={onPeriodChange}
            >
              <SelectTrigger className="w-full bg-card">
                <SelectValue placeholder="기간 선택" />
              </SelectTrigger>
              <SelectContent>
                {periods.map((p) => (
                  <SelectItem key={p.id} value={String(p.id)} disabled={p.status === 'CLOSED'}>
                    {p.periodNumber}기 ({p.startDate} ~ {p.endDate})
                    {p.status === 'CLOSED' ? ' [마감]' : ''}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
      </div>

      {/* Entry Table */}
      {selectedPeriodId == null ? (
        <div className="bg-card rounded-lg border py-20 text-center text-muted-foreground">
          회계 기간을 선택해주세요
        </div>
      ) : (
        <div className="space-y-3">
          <FilterBar onSearch={onSearch} onReset={onReset}>
            <FilterField label="전표일">
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
            <FilterField label="유형">
              <Select
                value={qType || 'ALL'}
                onValueChange={(v) => setQType(v === 'ALL' ? '' : (v ?? ''))}
              >
                <SelectTrigger className="h-8 w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">전체</SelectItem>
                  {(Object.keys(ENTRY_TYPE_LABEL) as JournalEntryType[]).map((t) => (
                    <SelectItem key={t} value={t}>
                      {ENTRY_TYPE_LABEL[t]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FilterField>
            <FilterField label="상태">
              <Select
                value={qStatus || 'ALL'}
                onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
              >
                <SelectTrigger className="h-8 w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">전체</SelectItem>
                  {(Object.keys(STATUS_LABEL) as JournalEntryStatus[]).map((s) => (
                    <SelectItem key={s} value={s}>
                      {STATUS_LABEL[s]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FilterField>
          </FilterBar>

          <DataTable
            data={filtered}
            columns={columns}
            getRowId={(e) => e.id}
            onRowClick={openDetail}
            showTotals
            totalLabel={`총 ${filtered.length}건`}
            empty={
              <EmptyState
                title="등록된 전표가 없습니다"
                description={
                  !isPeriodClosed ? '우측 상단의 「새 전표」로 전표를 등록하세요.' : undefined
                }
              />
            }
          />
          {entries && (
            <PaginationBar
              page={entries.page}
              totalPages={entries.totalPages}
              totalElements={entries.totalElements}
              size={entries.size}
              basePath="/finance/journal-entries"
              searchParams={{
                ...(selectedYearId != null ? { fiscalYearId: String(selectedYearId) } : {}),
                ...(selectedPeriodId != null ? { fiscalPeriodId: String(selectedPeriodId) } : {}),
              }}
            />
          )}
        </div>
      )}

      {/* 전표 상세 (drill-in) */}
      <DetailSheet
        open={detailEntry !== null}
        onOpenChange={(o) => {
          if (!o) setDetailEntry(null)
        }}
        title={detailEntry ? `전표 ${detailEntry.entryNo}` : '전표 상세'}
        description="차변·대변 분개 라인 명세"
      >
        {detailEntry && (
          <>
            <DetailSection title="전표 정보">
              <dl>
                <DetailRow label="전표번호">
                  <span className="font-mono">{detailEntry.entryNo}</span>
                </DetailRow>
                <DetailRow label="전표일">{detailEntry.entryDate}</DetailRow>
                <DetailRow label="유형">{ENTRY_TYPE_LABEL[detailEntry.entryType]}</DetailRow>
                <DetailRow label="상태">
                  <Badge variant={STATUS_VARIANT[detailEntry.status]}>
                    {STATUS_LABEL[detailEntry.status]}
                  </Badge>
                </DetailRow>
                <DetailRow label="통화">{detailEntry.currency}</DetailRow>
                <DetailRow label="적요">{detailEntry.description}</DetailRow>
                {detailEntry.postedAt && (
                  <DetailRow label="전기일시">
                    {new Date(detailEntry.postedAt).toLocaleString('ko-KR')}
                  </DetailRow>
                )}
              </dl>
            </DetailSection>

            <DetailSection title="분개 라인">
              <div className="overflow-hidden rounded-lg border border-border">
                <Table>
                  <TableHeader>
                    <TableRow className="hover:bg-transparent">
                      <TableHead>계정과목</TableHead>
                      <TableHead className="text-right">차변</TableHead>
                      <TableHead className="text-right">대변</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {detailLoading ? (
                      Array.from({ length: 2 }).map((_, i) => (
                        <TableRow key={i} className="hover:bg-transparent">
                          <TableCell>
                            <Skeleton className="h-4 w-32" />
                          </TableCell>
                          <TableCell>
                            <Skeleton className="ml-auto h-4 w-16" />
                          </TableCell>
                          <TableCell>
                            <Skeleton className="ml-auto h-4 w-16" />
                          </TableCell>
                        </TableRow>
                      ))
                    ) : detailLines && detailLines.length > 0 ? (
                      detailLines.map((line) => (
                        <TableRow key={line.id} className="hover:bg-transparent">
                          <TableCell>
                            <div className="text-sm">
                              <span className="font-mono text-muted-foreground">
                                {line.accountCode}
                              </span>{' '}
                              {line.accountName}
                            </div>
                            {line.description && (
                              <div className="text-xs text-muted-foreground">
                                {line.description}
                              </div>
                            )}
                          </TableCell>
                          <TableCell className="text-right font-mono text-sm tabular-nums">
                            {line.debitAmount
                              ? formatMoneyOne(line.debitAmount, detailEntry.currency)
                              : '-'}
                          </TableCell>
                          <TableCell className="text-right font-mono text-sm tabular-nums">
                            {line.creditAmount
                              ? formatMoneyOne(line.creditAmount, detailEntry.currency)
                              : '-'}
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow className="hover:bg-transparent">
                        <TableCell
                          colSpan={3}
                          className="py-6 text-center text-sm text-muted-foreground"
                        >
                          분개 라인이 없습니다
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>
              <div className="mt-2 flex justify-end gap-6 font-mono text-sm">
                <span>
                  차변 합계:{' '}
                  <strong>{formatMoneyOne(detailEntry.totalDebit, detailEntry.currency)}</strong>
                </span>
                <span>
                  대변 합계:{' '}
                  <strong>{formatMoneyOne(detailEntry.totalCredit, detailEntry.currency)}</strong>
                </span>
              </div>
            </DetailSection>
          </>
        )}
      </DetailSheet>

      {/* Create Dialog */}
      <Dialog
        open={showCreate}
        onOpenChange={(o) => {
          if (!o) setShowCreate(false)
        }}
      >
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>새 전표 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            {/* Header fields */}
            <FormGrid>
              <FormRow label="전표일" required>
                <DatePicker value={entryDate} onChange={setEntryDate} />
              </FormRow>
              <FormRow label="유형" required>
                <Select
                  value={entryType}
                  onValueChange={(v) => setEntryType(v as JournalEntryType)}
                >
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(Object.keys(ENTRY_TYPE_LABEL) as JournalEntryType[]).map((t) => (
                      <SelectItem key={t} value={t}>
                        {ENTRY_TYPE_LABEL[t]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="통화">
                <Select value={currency} onValueChange={(v) => setCurrency(v ?? 'KRW')}>
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="KRW">KRW</SelectItem>
                    <SelectItem value="USD">USD</SelectItem>
                    <SelectItem value="EUR">EUR</SelectItem>
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="설명" required span>
                <Textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </FormRow>
            </FormGrid>

            {/* Lines */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <Label>전표 행</Label>
                <Button variant="ghost" size="sm" onClick={addLine}>
                  <PlusIcon />행 추가
                </Button>
              </div>
              <div className="border rounded overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/40">
                      <TableHead className="w-48">계정과목 *</TableHead>
                      <TableHead className="text-right w-32">차변</TableHead>
                      <TableHead className="text-right w-32">대변</TableHead>
                      <TableHead>적요</TableHead>
                      <TableHead className="w-10" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {lines.map((line, i) => (
                      <TableRow key={i}>
                        <TableCell className="py-1">
                          <Select
                            value={line.accountId}
                            onValueChange={(v) => setLine(i, 'accountId', v ?? '')}
                          >
                            <SelectTrigger className="w-full h-8 text-sm">
                              <SelectValue placeholder="선택" />
                            </SelectTrigger>
                            <SelectContent>
                              {activeAccounts.map((a) => (
                                <SelectItem key={a.id} value={String(a.id)}>
                                  {a.code} {a.name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </TableCell>
                        <TableCell className="py-1">
                          <Input
                            className="text-right h-8 text-sm font-mono"
                            type="number"
                            min={0}
                            step={0.01}
                            value={line.debitAmount}
                            onChange={(e) => setLine(i, 'debitAmount', e.target.value)}
                            placeholder="0"
                          />
                        </TableCell>
                        <TableCell className="py-1">
                          <Input
                            className="text-right h-8 text-sm font-mono"
                            type="number"
                            min={0}
                            step={0.01}
                            value={line.creditAmount}
                            onChange={(e) => setLine(i, 'creditAmount', e.target.value)}
                            placeholder="0"
                          />
                        </TableCell>
                        <TableCell className="py-1">
                          <Input
                            className="h-8 text-sm"
                            value={line.description}
                            onChange={(e) => setLine(i, 'description', e.target.value)}
                          />
                        </TableCell>
                        <TableCell className="py-1">
                          {lines.length > 2 && (
                            <Button variant="ghost" size="icon-xs" onClick={() => removeLine(i)}>
                              <TrashIcon className="text-destructive" />
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
              {/* Balance indicator */}
              <div className="mt-2 flex justify-end gap-6 text-sm font-mono">
                <span>
                  차변 합계: <strong>{formatMoneyOne(totalDebit, currency)}</strong>
                </span>
                <span>
                  대변 합계: <strong>{formatMoneyOne(totalCredit, currency)}</strong>
                </span>
                <span className={balanced ? 'text-success' : 'text-destructive'}>
                  {balanced ? '✓ 균형' : '✗ 불일치'}
                </span>
              </div>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending || !balanced}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 반려 / 역분개 다이얼로그 */}
      <Dialog
        open={actionDialog.type !== 'none'}
        onOpenChange={(o) => {
          if (!o) closeAction()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {actionDialog.type === 'reject' ? '전표 반려' : '전표 역분개'}
            </DialogTitle>
          </DialogHeader>
          {actionDialog.type === 'reject' && (
            <div className="grid gap-1.5 py-2">
              <Label>반려 사유 *</Label>
              <Textarea
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="반려 사유를 입력하세요"
              />
            </div>
          )}
          {actionDialog.type === 'reverse' && (
            <p className="py-2 text-sm text-muted-foreground">
              전표 <span className="font-mono">{actionDialog.entry.entryNo}</span>를 역분개합니다.
              차/대가 반대인 새 전표가 생성되고 원 전표는 역분개 처리됩니다. 계속하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            {actionDialog.type === 'reject' && (
              <Button
                variant="destructive"
                onClick={() => handleReject(actionDialog.entry)}
                disabled={isPending}
              >
                반려
              </Button>
            )}
            {actionDialog.type === 'reverse' && (
              <Button
                variant="destructive"
                onClick={() => handleReverse(actionDialog.entry)}
                disabled={isPending}
              >
                역분개
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
