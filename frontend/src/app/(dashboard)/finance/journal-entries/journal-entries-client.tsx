'use client'
import { useState, useTransition, useMemo } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, TrashIcon } from 'lucide-react'
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
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { formatMoneyOne } from '@/lib/money'
import {
  createJournalEntry,
  submitJournalEntry,
  approveJournalEntry,
  withdrawJournalEntry,
} from './actions'
import type {
  FiscalYear,
  FiscalPeriod,
  JournalEntry,
  JournalEntryStatus,
  JournalEntryType,
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
  DRAFT: '임시',
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
  const [isPending, startTransition] = useTransition()

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
        toast.success('분개가 등록되었습니다')
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
        <div className="flex justify-end gap-1">
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
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="분개장"
        description="회계 기간을 선택하여 분개 내역을 조회합니다"
        className="mb-6"
      >
        {canWrite && selectedPeriodId != null && (
          <Button
            onClick={openCreate}
            disabled={isPending || isPeriodClosed}
            title={isPeriodClosed ? '마감된 기간에는 분개를 등록할 수 없습니다' : undefined}
          >
            <PlusIcon />새 분개
          </Button>
        )}
      </PageHeader>

      {/* Fiscal Period Selector */}
      <div className="mb-6 flex gap-4">
        <div className="w-48">
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
          <div className="w-56">
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
          <DataTable
            data={entries?.content ?? []}
            columns={columns}
            getRowId={(e) => e.id}
            empty={<EmptyState title="등록된 분개가 없습니다" />}
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

      {/* Create Dialog */}
      <Dialog
        open={showCreate}
        onOpenChange={(o) => {
          if (!o) setShowCreate(false)
        }}
      >
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>새 분개 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            {/* Header fields */}
            <div className="grid grid-cols-3 gap-4">
              <div className="grid gap-1.5">
                <Label>전표일 *</Label>
                <Input
                  type="date"
                  value={entryDate}
                  onChange={(e) => setEntryDate(e.target.value)}
                />
              </div>
              <div className="grid gap-1.5">
                <Label>유형 *</Label>
                <Select
                  value={entryType}
                  onValueChange={(v) => setEntryType(v as JournalEntryType)}
                >
                  <SelectTrigger className="w-full">
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
              </div>
              <div className="grid gap-1.5">
                <Label>통화</Label>
                <Select value={currency} onValueChange={(v) => setCurrency(v ?? 'KRW')}>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="KRW">KRW</SelectItem>
                    <SelectItem value="USD">USD</SelectItem>
                    <SelectItem value="EUR">EUR</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid gap-1.5">
              <Label>설명 *</Label>
              <Textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            {/* Lines */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <Label>분개 행</Label>
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
    </div>
  )
}
