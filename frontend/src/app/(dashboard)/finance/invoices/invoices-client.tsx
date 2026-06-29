'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, SendIcon, CheckIcon, BanIcon, Trash2Icon, BookOpenIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DatePicker } from '@/components/ui/date-picker'
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
import { PaginationBar } from '@/components/ui/pagination-bar'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { DownloadIcon } from 'lucide-react'
import {
  createInvoice,
  submitInvoice,
  approveInvoice,
  payInvoice,
  cancelInvoice,
  exportAllInvoices,
} from './actions'
import type { Account, ApInvoice, ApInvoiceStatus, TaxType, Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const TAX_TYPE_LABEL: Record<TaxType, string> = {
  TAXABLE: '과세 (10%)',
  ZERO_RATED: '영세율 (0%)',
  EXEMPT: '면세',
}

type LineForm = { accountId: string; amount: string; description: string }

const STATUS_LABEL: Record<ApInvoiceStatus, string> = {
  DRAFT: '임시',
  PENDING_APPROVAL: '결재중',
  APPROVED: '승인',
  PAID: '완납',
  CANCELLED: '취소',
}
const STATUS_VARIANT: Record<ApInvoiceStatus, 'default' | 'secondary' | 'destructive'> = {
  DRAFT: 'secondary',
  PENDING_APPROVAL: 'secondary',
  APPROVED: 'default',
  PAID: 'default',
  CANCELLED: 'destructive',
}

function fmt(n: number, currency: string) {
  return `${currency} ${n.toLocaleString('ko-KR')}`
}

type DialogState =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'pay'; inv: ApInvoice }
  | { type: 'cancel'; inv: ApInvoice }

interface Props {
  data: PageResponse<ApInvoice>
  vendors: Vendor[]
  accounts: Account[]
}

export default function InvoicesClient({ data, vendors, accounts }: Props) {
  const postableAccounts = accounts.filter((a) => !a.isSummary && a.isActive)
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)
  // 결재(전결)는 작성권과 분리 — 별도 전결권 보유자만. 서버가 전결 한도까지 최종 검증한다.
  const canApprove = can(PERM.FINANCE_INVOICE_APPROVE)
  // 지급(현금이동)은 작성권과 분리 — 별도 지급권 보유자만. 서버가 SoD·전결 한도까지 최종 검증한다.
  const canPay = can(PERM.FINANCE_INVOICE_PAY)
  const [dialog, setDialog] = useState<DialogState>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [invoiceNo, setInvoiceNo] = useState('')
  const [vendorId, setVendorId] = useState('')
  const [invoiceDate, setInvoiceDate] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [supplyAmount, setSupplyAmount] = useState('')
  const [currency, setCurrency] = useState('KRW')
  const [note, setNote] = useState('')
  const [payAmount, setPayAmount] = useState('')
  const [payCashAccountId, setPayCashAccountId] = useState('')
  const [payDate, setPayDate] = useState('')
  const [lines, setLines] = useState<LineForm[]>([])
  const [taxType, setTaxType] = useState<TaxType>('TAXABLE')

  // 라인이 있으면 공급가액 = 라인 합계(전 단위로 합산해 부동소수 오차 회피). 없으면 직접 입력.
  const supplyLineTotal =
    lines.reduce((s, l) => s + Math.round((Number(l.amount) || 0) * 100), 0) / 100
  const supplyEffective = lines.length > 0 ? supplyLineTotal : Number(supplyAmount) || 0
  // 부가세 자동계산 — 과세는 공급가액×10% 원 미만 절사, 영세·면세는 0(백엔드와 동일).
  const vatComputed = taxType === 'TAXABLE' ? Math.floor(supplyEffective * 0.1) : 0
  const totalComputed = Math.round((supplyEffective + vatComputed) * 100) / 100

  const addLine = () => setLines((ls) => [...ls, { accountId: '', amount: '', description: '' }])
  const removeLine = (i: number) => setLines((ls) => ls.filter((_, idx) => idx !== i))
  const updateLine = (i: number, patch: Partial<LineForm>) =>
    setLines((ls) => ls.map((l, idx) => (idx === i ? { ...l, ...patch } : l)))

  const openCreate = () => {
    setInvoiceNo('')
    setVendorId('')
    setInvoiceDate('')
    setDueDate('')
    setSupplyAmount('')
    setCurrency('KRW')
    setNote('')
    setLines([])
    setTaxType('TAXABLE')
    setDialog({ type: 'create' })
  }

  const handleCreate = () => {
    if (!invoiceNo.trim() || !vendorId || !invoiceDate || !dueDate || supplyEffective <= 0) {
      toast.error('필수 항목을 모두 입력해주세요')
      return
    }
    if (lines.length > 0 && lines.some((l) => !l.accountId || !l.amount || Number(l.amount) <= 0)) {
      toast.error('분개 라인의 계정과 금액을 모두 입력해주세요')
      return
    }
    // 라인 합계 = 공급가액(서버가 검증). 세액·총액은 서버가 과세구분으로 자동계산.
    const supplyLines = lines.map((l) => ({
      accountId: Number(l.accountId),
      amount: Math.round(Number(l.amount) * 100) / 100,
      description: l.description || null,
    }))
    startTransition(async () => {
      try {
        await createInvoice({
          invoiceNo: invoiceNo.trim(),
          vendorId: Number(vendorId),
          invoiceDate,
          dueDate,
          supplyAmount: Math.round(supplyEffective * 100) / 100,
          taxType,
          currency: currency || 'KRW',
          note: note || null,
          lines: lines.length > 0 ? supplyLines : null,
        })
        toast.success('계산서가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleSubmit = (inv: ApInvoice) => {
    startTransition(async () => {
      try {
        await submitInvoice(inv.id)
        toast.success('결재 상신되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '상신 중 오류가 발생했습니다')
      }
    })
  }

  const handleApprove = (inv: ApInvoice) => {
    startTransition(async () => {
      try {
        await approveInvoice(inv.id)
        toast.success('계산서가 승인되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '승인 중 오류가 발생했습니다')
      }
    })
  }

  const handlePay = (inv: ApInvoice) => {
    if (!payAmount || Number(payAmount) <= 0) {
      toast.error('지급 금액을 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        await payInvoice(
          inv.id,
          Number(payAmount),
          payCashAccountId ? Number(payCashAccountId) : null,
          payDate || null,
        )
        toast.success('지급이 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '지급 중 오류가 발생했습니다')
      }
    })
  }

  const handleCancel = (inv: ApInvoice) => {
    startTransition(async () => {
      try {
        await cancelInvoice(inv.id)
        toast.success('계산서가 취소되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '취소 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<ApInvoice>[] = [
    {
      key: 'invoiceNo',
      header: '계산서번호',
      sortable: true,
      sortValue: (inv) => inv.invoiceNo,
      cell: (inv) => <span className="font-mono text-sm">{inv.invoiceNo}</span>,
    },
    {
      key: 'vendorName',
      header: '공급업체',
      sortable: true,
      sortValue: (inv) => inv.vendorName,
      cell: (inv) => <span className="font-medium">{inv.vendorName}</span>,
    },
    {
      key: 'invoiceDate',
      header: '계산서일',
      sortable: true,
      sortValue: (inv) => inv.invoiceDate,
      cell: (inv) => <span className="text-sm">{inv.invoiceDate}</span>,
    },
    {
      key: 'dueDate',
      header: '만기일',
      sortable: true,
      sortValue: (inv) => inv.dueDate,
      cell: (inv) => <span className="text-sm">{inv.dueDate}</span>,
    },
    {
      key: 'totalAmount',
      header: '총금액',
      align: 'right',
      sortable: true,
      sortValue: (inv) => inv.totalAmount,
      cell: (inv) => (
        <span className="font-mono text-sm">{fmt(inv.totalAmount, inv.currency)}</span>
      ),
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + r.totalAmount, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'outstandingAmount',
      header: '미납금액',
      align: 'right',
      sortable: true,
      sortValue: (inv) => inv.outstandingAmount,
      cell: (inv) => (
        <span className="font-mono text-sm">{fmt(inv.outstandingAmount, inv.currency)}</span>
      ),
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + r.outstandingAmount, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (inv) => STATUS_LABEL[inv.status],
      cell: (inv) => (
        <span className="inline-flex items-center gap-1">
          <Badge variant={STATUS_VARIANT[inv.status]}>{STATUS_LABEL[inv.status]}</Badge>
          {inv.journalEntryId && (
            <span
              title={`연결 분개 #${inv.journalEntryId}`}
              className="inline-flex items-center text-muted-foreground"
            >
              <BookOpenIcon className="size-3.5" />
            </span>
          )}
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-28',
      cell: (inv) => (
        <div className="flex justify-end gap-1">
          {canWrite && inv.status === 'DRAFT' && (
            <>
              <Button
                variant="ghost"
                size="icon-xs"
                title="결재상신"
                onClick={() => handleSubmit(inv)}
                disabled={isPending}
              >
                <SendIcon className="text-primary" />
              </Button>
              <Button
                variant="ghost"
                size="icon-xs"
                title="취소"
                onClick={() => setDialog({ type: 'cancel', inv })}
                disabled={isPending}
              >
                <BanIcon className="text-destructive" />
              </Button>
            </>
          )}
          {inv.status === 'PENDING_APPROVAL' && (
            <>
              {canApprove && (
                <Button
                  variant="ghost"
                  size="icon-xs"
                  title="승인"
                  onClick={() => handleApprove(inv)}
                  disabled={isPending}
                >
                  <CheckIcon className="text-success" />
                </Button>
              )}
              {canWrite && (
                <Button
                  variant="ghost"
                  size="icon-xs"
                  title="취소"
                  onClick={() => setDialog({ type: 'cancel', inv })}
                  disabled={isPending}
                >
                  <BanIcon className="text-destructive" />
                </Button>
              )}
            </>
          )}
          {canPay && inv.status === 'APPROVED' && (
            <Button
              variant="ghost"
              size="sm"
              title="지급처리"
              onClick={() => {
                setPayAmount(String(inv.outstandingAmount))
                setPayCashAccountId('')
                setPayDate(new Date().toISOString().slice(0, 10))
                setDialog({ type: 'pay', inv })
              }}
              disabled={isPending}
            >
              지급
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qVendor, setQVendor] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [qFrom, setQFrom] = useState('')
  const [qTo, setQTo] = useState('')
  const [applied, setApplied] = useState({ vendor: '', status: '', from: '', to: '' })
  const onSearch = () => setApplied({ vendor: qVendor, status: qStatus, from: qFrom, to: qTo })
  const onReset = () => {
    setQVendor('')
    setQStatus('')
    setQFrom('')
    setQTo('')
    setApplied({ vendor: '', status: '', from: '', to: '' })
  }
  const matchesFilter = (inv: ApInvoice) => {
    if (applied.vendor && String(inv.vendorId) !== applied.vendor) return false
    if (applied.status && inv.status !== applied.status) return false
    if (applied.from && inv.invoiceDate < applied.from) return false
    if (applied.to && inv.invoiceDate > applied.to) return false
    return true
  }
  const filtered = data.content.filter(matchesFilter)
  const exportColumns = [
    '계산서번호',
    '공급업체',
    '계산서일',
    '만기일',
    '통화',
    '총금액',
    '미납금액',
    '상태',
  ]
  const exportRow = (inv: ApInvoice) => [
    inv.invoiceNo,
    inv.vendorName,
    inv.invoiceDate,
    inv.dueDate,
    inv.currency,
    inv.totalAmount,
    inv.outstandingAmount,
    STATUS_LABEL[inv.status],
  ]
  // 전체 엑셀 — 현재 페이지가 아닌 전체 데이터셋(전 페이지 순회) 내보내기. 화면 조회조건을 동일 적용.
  const exportExcel = () => {
    startTransition(async () => {
      try {
        const { rows, truncated } = await exportAllInvoices()
        downloadCsv(
          `매입계산서_${new Date().toISOString().slice(0, 10)}`,
          exportColumns,
          rows.filter(matchesFilter).map(exportRow),
        )
        if (truncated) {
          toast.warning('데이터가 많아 최대 5,000건까지만 내보냈습니다')
        }
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '엑셀 내보내기 중 오류가 발생했습니다')
      }
    })
  }

  return (
    <div className="p-5">
      <PageHeader
        title="매입계산서"
        description="공급업체 계산서 및 지급 현황을 관리합니다"
        className="mb-4"
      >
        <Button variant="outline" onClick={exportExcel} disabled={isPending}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 계산서
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="계산서일">
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
          <FilterField label="공급업체">
            <Select
              value={qVendor || 'ALL'}
              onValueChange={(v) => setQVendor(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {vendors.map((v) => (
                  <SelectItem key={v.id} value={String(v.id)}>
                    {v.name}
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
                {(Object.keys(STATUS_LABEL) as ApInvoiceStatus[]).map((s) => (
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
          getRowId={(inv) => inv.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 계산서가 없습니다"
              description={canWrite ? '우측 상단에서 새 계산서를 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/finance/invoices"
        />
      </div>

      {/* Create Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>새 계산서 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="계산서번호" required>
                <Input
                  value={invoiceNo}
                  onChange={(e) => setInvoiceNo(e.target.value)}
                  placeholder="INV-2024-001"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="공급업체" required>
                <Select value={vendorId} onValueChange={(v) => setVendorId(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue placeholder="선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {vendors
                      .filter((v) => v.isActive)
                      .map((v) => (
                        <SelectItem key={v.id} value={String(v.id)}>
                          {v.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="계산서일" required>
                <DatePicker value={invoiceDate} onChange={setInvoiceDate} />
              </FormRow>
              <FormRow label="만기일" required>
                <DatePicker value={dueDate} onChange={setDueDate} />
              </FormRow>
              <FormRow label="공급가액" required>
                <Input
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={lines.length > 0 ? supplyEffective : supplyAmount}
                  readOnly={lines.length > 0}
                  onChange={(e) => setSupplyAmount(e.target.value)}
                  placeholder="0"
                  className={lines.length > 0 ? 'h-8 bg-muted/40 text-muted-foreground' : 'h-8'}
                />
              </FormRow>
              <FormRow label="과세구분" required>
                <Select
                  value={taxType}
                  onValueChange={(v) => setTaxType((v ?? 'TAXABLE') as TaxType)}
                >
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(Object.keys(TAX_TYPE_LABEL) as TaxType[]).map((t) => (
                      <SelectItem key={t} value={t}>
                        {TAX_TYPE_LABEL[t]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="통화" span>
                <Select value={currency} onValueChange={(v) => setCurrency(v ?? 'KRW')}>
                  <SelectTrigger className="h-8 w-32">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="KRW">KRW</SelectItem>
                    <SelectItem value="USD">USD</SelectItem>
                    <SelectItem value="EUR">EUR</SelectItem>
                  </SelectContent>
                </Select>
              </FormRow>
            </FormGrid>

            {/* 분개 라인(차변) — 입력 시 승인 때 GL 자동 분개. 합계가 총금액이 된다. */}
            <div className="grid gap-2">
              <div className="flex items-center justify-between">
                <Label>분개 라인 (차변 — 비용/자산, 공급가액)</Label>
                <Button type="button" variant="outline" size="xs" onClick={addLine}>
                  <PlusIcon />
                  라인 추가
                </Button>
              </div>
              {lines.length === 0 ? (
                <p className="text-xs text-muted-foreground">
                  라인을 추가하면 승인 시 GL 분개가 자동 생성됩니다 (대변은 공급업체 외상매입금
                  계정). 미입력 시 분개 없이 전표만 등록됩니다.
                </p>
              ) : (
                <div className="space-y-2">
                  {lines.map((line, i) => (
                    <div key={i} className="flex gap-2 items-start">
                      <div className="flex-1">
                        <Select
                          value={line.accountId}
                          onValueChange={(v) => updateLine(i, { accountId: v ?? '' })}
                        >
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="계정 선택" />
                          </SelectTrigger>
                          <SelectContent>
                            {postableAccounts.map((a) => (
                              <SelectItem key={a.id} value={String(a.id)}>
                                {a.code} {a.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <Input
                        type="number"
                        min={0.01}
                        step={0.01}
                        className="w-32"
                        placeholder="금액"
                        value={line.amount}
                        onChange={(e) => updateLine(i, { amount: e.target.value })}
                      />
                      <Input
                        className="flex-1"
                        placeholder="적요(선택)"
                        value={line.description}
                        onChange={(e) => updateLine(i, { description: e.target.value })}
                      />
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        title="삭제"
                        onClick={() => removeLine(i)}
                      >
                        <Trash2Icon className="text-destructive" />
                      </Button>
                    </div>
                  ))}
                  <div className="text-right text-sm text-muted-foreground">
                    공급가 합계:{' '}
                    <span className="font-medium">{fmt(supplyLineTotal, currency)}</span>
                  </div>
                </div>
              )}
            </div>

            {/* 부가세 자동계산(과세구분 기준) — 승인 전기 시 부가세대급금 통제계정으로 자동 분개(설정 시). */}
            <div className="grid gap-1 rounded-md border bg-muted/40 p-3 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">공급가액</span>
                <span className="font-mono">{fmt(supplyEffective, currency)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">부가세 ({TAX_TYPE_LABEL[taxType]})</span>
                <span className="font-mono">{fmt(vatComputed, currency)}</span>
              </div>
              <div className="flex justify-between border-t pt-1 font-semibold">
                <span>합계</span>
                <span className="font-mono">{fmt(totalComputed, currency)}</span>
              </div>
            </div>

            <div className="grid gap-1.5">
              <Label>비고</Label>
              <Textarea value={note} onChange={(e) => setNote(e.target.value)} rows={2} />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Pay Dialog */}
      <Dialog
        open={dialog.type === 'pay'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>지급 처리</DialogTitle>
          </DialogHeader>
          {dialog.type === 'pay' && (
            <div className="grid gap-4 py-2">
              <div className="text-sm text-muted-foreground">
                <strong>{dialog.inv.vendorName}</strong> — {dialog.inv.invoiceNo}
                <br />
                미납금액: <strong>{fmt(dialog.inv.outstandingAmount, dialog.inv.currency)}</strong>
              </div>
              <div className="grid gap-1.5">
                <Label>지급금액 *</Label>
                <Input
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={payAmount}
                  onChange={(e) => setPayAmount(e.target.value)}
                />
              </div>
              <div className="grid gap-1.5">
                <Label>지급계정 (현금·예금)</Label>
                <Select
                  value={payCashAccountId || 'NONE'}
                  onValueChange={(v) => setPayCashAccountId(!v || v === 'NONE' ? '' : v)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="NONE">미선택 (분개 없이 잔액만)</SelectItem>
                    {postableAccounts.map((a) => (
                      <SelectItem key={a.id} value={String(a.id)}>
                        {a.code} {a.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-1.5">
                <Label>지급일</Label>
                <DatePicker value={payDate} onChange={setPayDate} />
              </div>
              <p className="text-xs text-muted-foreground">
                계정 선택 시 지급/수금 분개가 자동 생성됩니다.
              </p>
            </div>
          )}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'pay' && handlePay(dialog.inv)}
              disabled={isPending}
            >
              지급 처리
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Cancel Dialog */}
      <Dialog
        open={dialog.type === 'cancel'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>계산서 취소</DialogTitle>
          </DialogHeader>
          {dialog.type === 'cancel' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.inv.invoiceNo}</strong>을(를) 취소하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'cancel' && handleCancel(dialog.inv)}
              disabled={isPending}
            >
              취소
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
