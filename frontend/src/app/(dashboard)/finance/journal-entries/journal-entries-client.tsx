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
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { formatMoneyOne } from '@/lib/money'
import { createJournalEntry, postJournalEntry } from './actions'
import type {
  FiscalYear, FiscalPeriod, JournalEntry, JournalEntryStatus,
  JournalEntryType, Account,
} from '@/types/finance'
import type { PageResponse } from '@/types/api'

const ENTRY_TYPE_LABEL: Record<JournalEntryType, string> = {
  MANUAL: '수기', AP: '매입', AR: '매출', PAYROLL: '급여', ADJUSTMENT: '조정',
}
const STATUS_LABEL: Record<JournalEntryStatus, string> = {
  DRAFT: '임시', POSTED: '전기완료', REVERSED: '역분개',
}
const STATUS_VARIANT: Record<JournalEntryStatus, 'default' | 'secondary' | 'destructive'> = {
  DRAFT: 'secondary', POSTED: 'default', REVERSED: 'destructive',
}


interface LineRow { accountId: string; debitAmount: string; creditAmount: string; description: string }
const emptyLine = (): LineRow => ({ accountId: '', debitAmount: '', creditAmount: '', description: '' })

interface Props {
  fiscalYears: FiscalYear[]
  selectedYearId: number | null
  periods: FiscalPeriod[]
  selectedPeriodId: number | null
  entries: PageResponse<JournalEntry> | null
  accounts: Account[]
}

export default function JournalEntriesClient({
  fiscalYears, selectedYearId, periods, selectedPeriodId, entries, accounts,
}: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)
  const router = useRouter()
  const [showCreate, setShowCreate] = useState(false)
  const [isPending, startTransition] = useTransition()

  const [entryDate, setEntryDate] = useState('')
  const [description, setDescription] = useState('')
  const [entryType, setEntryType] = useState<JournalEntryType>('MANUAL')
  const [currency, setCurrency] = useState('KRW')
  const [lines, setLines] = useState<LineRow[]>([emptyLine(), emptyLine()])

  const activeAccounts = useMemo(() => accounts.filter((a) => a.isActive && !a.isSummary), [accounts])
  const selectedPeriod = useMemo(() => periods.find((p) => p.id === selectedPeriodId), [periods, selectedPeriodId])
  const isPeriodClosed = selectedPeriod?.status === 'CLOSED'

  const totalDebit = useMemo(
    () => lines.reduce((s, l) => s + (Number(l.debitAmount) || 0), 0), [lines]
  )
  const totalCredit = useMemo(
    () => lines.reduce((s, l) => s + (Number(l.creditAmount) || 0), 0), [lines]
  )
  const balanced = Math.abs(totalDebit - totalCredit) < 0.005

  const onYearChange = (val: string | null) => {
    if (val) router.push(`/finance/journal-entries?fiscalYearId=${val}`)
  }
  const onPeriodChange = (val: string | null) => {
    if (val) router.push(`/finance/journal-entries?fiscalYearId=${selectedYearId}&fiscalPeriodId=${val}`)
  }

  const openCreate = () => {
    setEntryDate(''); setDescription(''); setEntryType('MANUAL'); setCurrency('KRW')
    setLines([emptyLine(), emptyLine()])
    setShowCreate(true)
  }

  const addLine = () => setLines((prev) => [...prev, emptyLine()])
  const removeLine = (i: number) => setLines((prev) => prev.filter((_, idx) => idx !== i))
  const setLine = (i: number, field: keyof LineRow, val: string) =>
    setLines((prev) => prev.map((l, idx) => idx === i ? { ...l, [field]: val } : l))

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
      toast.error(`차변(${formatMoneyOne(totalDebit, currency)})과 대변(${formatMoneyOne(totalCredit, currency)})이 일치해야 합니다`)
      return
    }
    if (selectedPeriodId == null) { toast.error('회계 기간을 먼저 선택해주세요'); return }

    startTransition(async () => {
      try {
        await createJournalEntry({
          entryDate, fiscalPeriodId: selectedPeriodId, description: description.trim(),
          entryType, currency,
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
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handlePost = (entry: JournalEntry) => {
    startTransition(async () => {
      try {
        await postJournalEntry(entry.id)
        toast.success('전기 처리되었습니다')
      } catch (e) { toast.error(e instanceof Error ? e.message : '전기 중 오류가 발생했습니다') }
    })
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">분개장</h1>
          <p className="text-sm text-gray-500 mt-1">회계 기간을 선택하여 분개 내역을 조회합니다</p>
        </div>
        {canWrite && selectedPeriodId != null && (
          <Button onClick={openCreate} disabled={isPending || isPeriodClosed} title={isPeriodClosed ? '마감된 기간에는 분개를 등록할 수 없습니다' : undefined}>
            <PlusIcon />새 분개
          </Button>
        )}
      </div>

      {/* Fiscal Period Selector */}
      <div className="mb-6 flex gap-4">
        <div className="w-48">
          <Label className="text-xs text-gray-500 mb-1 block">회계연도</Label>
          <Select
            value={selectedYearId != null ? String(selectedYearId) : ''}
            onValueChange={onYearChange}
          >
            <SelectTrigger className="w-full bg-white">
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
            <Label className="text-xs text-gray-500 mb-1 block">회계 기간</Label>
            <Select
              value={selectedPeriodId != null ? String(selectedPeriodId) : ''}
              onValueChange={onPeriodChange}
            >
              <SelectTrigger className="w-full bg-white">
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
        <div className="bg-white rounded-lg border py-20 text-center text-gray-400">
          회계 기간을 선택해주세요
        </div>
      ) : (
        <div className="bg-white rounded-lg border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>전표번호</TableHead>
                <TableHead>전표일</TableHead>
                <TableHead>유형</TableHead>
                <TableHead>설명</TableHead>
                <TableHead className="text-right">차변 합계</TableHead>
                <TableHead className="text-right">대변 합계</TableHead>
                <TableHead>상태</TableHead>
                <TableHead className="w-20" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {(!entries || entries.content.length === 0) && (
                <TableRow>
                  <TableCell colSpan={8} className="text-center text-gray-400 py-10">
                    등록된 분개가 없습니다
                  </TableCell>
                </TableRow>
              )}
              {entries?.content.map((entry) => (
                <TableRow key={entry.id}>
                  <TableCell className="font-mono text-sm">{entry.entryNo}</TableCell>
                  <TableCell className="text-sm">{entry.entryDate}</TableCell>
                  <TableCell className="text-sm">{ENTRY_TYPE_LABEL[entry.entryType]}</TableCell>
                  <TableCell className="text-sm max-w-xs truncate">{entry.description}</TableCell>
                  <TableCell className="text-right font-mono text-sm">
                    {formatMoneyOne(entry.totalDebit, entry.currency)}
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm">
                    {formatMoneyOne(entry.totalCredit, entry.currency)}
                  </TableCell>
                  <TableCell>
                    <Badge variant={STATUS_VARIANT[entry.status]}>{STATUS_LABEL[entry.status]}</Badge>
                  </TableCell>
                  <TableCell>
                    {canWrite && entry.status === 'DRAFT' && (
                      <Button variant="ghost" size="sm" onClick={() => handlePost(entry)}
                        disabled={isPending} title="전기">
                        전기
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {entries && (
            <PaginationBar
              page={entries.page} totalPages={entries.totalPages}
              totalElements={entries.totalElements} size={entries.size}
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
      <Dialog open={showCreate} onOpenChange={(o) => { if (!o) setShowCreate(false) }}>
        <DialogContent className="max-w-3xl">
          <DialogHeader><DialogTitle>새 분개 등록</DialogTitle></DialogHeader>
          <div className="grid gap-4 py-2">
            {/* Header fields */}
            <div className="grid grid-cols-3 gap-4">
              <div className="grid gap-1.5">
                <Label>전표일 *</Label>
                <Input type="date" value={entryDate} onChange={(e) => setEntryDate(e.target.value)} />
              </div>
              <div className="grid gap-1.5">
                <Label>유형 *</Label>
                <Select value={entryType} onValueChange={(v) => setEntryType(v as JournalEntryType)}>
                  <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {(Object.keys(ENTRY_TYPE_LABEL) as JournalEntryType[]).map((t) => (
                      <SelectItem key={t} value={t}>{ENTRY_TYPE_LABEL[t]}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-1.5">
                <Label>통화</Label>
                <Select value={currency} onValueChange={(v) => setCurrency(v ?? 'KRW')}>
                  <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
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
              <Textarea rows={2} value={description}
                onChange={(e) => setDescription(e.target.value)} />
            </div>

            {/* Lines */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <Label>분개 행</Label>
                <Button variant="ghost" size="sm" onClick={addLine}><PlusIcon />행 추가</Button>
              </div>
              <div className="border rounded overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-gray-50">
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
                          <Select value={line.accountId}
                            onValueChange={(v) => setLine(i, 'accountId', v ?? '')}>
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
                          <Input className="text-right h-8 text-sm font-mono"
                            type="number" min={0} step={0.01} value={line.debitAmount}
                            onChange={(e) => setLine(i, 'debitAmount', e.target.value)}
                            placeholder="0" />
                        </TableCell>
                        <TableCell className="py-1">
                          <Input className="text-right h-8 text-sm font-mono"
                            type="number" min={0} step={0.01} value={line.creditAmount}
                            onChange={(e) => setLine(i, 'creditAmount', e.target.value)}
                            placeholder="0" />
                        </TableCell>
                        <TableCell className="py-1">
                          <Input className="h-8 text-sm" value={line.description}
                            onChange={(e) => setLine(i, 'description', e.target.value)} />
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
                <span>차변 합계: <strong>{formatMoneyOne(totalDebit, currency)}</strong></span>
                <span>대변 합계: <strong>{formatMoneyOne(totalCredit, currency)}</strong></span>
                <span className={balanced ? 'text-green-600' : 'text-destructive'}>
                  {balanced ? '✓ 균형' : '✗ 불일치'}
                </span>
              </div>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending || !balanced}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
