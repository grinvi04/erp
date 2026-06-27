'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, SendIcon, CheckIcon, BanIcon, Trash2Icon, BookOpenIcon } from 'lucide-react'
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
import { PaginationBar } from '@/components/ui/pagination-bar'
import { createInvoice, submitInvoice, approveInvoice, payInvoice, cancelInvoice } from './actions'
import type { Account, ApInvoice, ApInvoiceStatus, Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'

type LineForm = { accountId: string; amount: string; description: string }

const STATUS_LABEL: Record<ApInvoiceStatus, string> = {
  DRAFT: '임시',
  PENDING_APPROVAL: '결재대기',
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
  const [totalAmount, setTotalAmount] = useState('')
  const [currency, setCurrency] = useState('KRW')
  const [note, setNote] = useState('')
  const [payAmount, setPayAmount] = useState('')
  const [payCashAccountId, setPayCashAccountId] = useState('')
  const [payDate, setPayDate] = useState('')
  const [lines, setLines] = useState<LineForm[]>([])
  const [vatAccountId, setVatAccountId] = useState('')
  const [vatAmount, setVatAmount] = useState('')

  // 라인이 있으면 총금액은 (공급가 합계 + 부가세)로 자동 산출(서버가 합계=총금액을 BigDecimal 검증).
  // 정수 전(錢) 단위로 합산해 부동소수 오차(0.1+0.2≠0.3)를 피한다 — 금액 컬럼은 소수 2자리.
  const supplyTotal = lines.reduce((s, l) => s + Math.round((Number(l.amount) || 0) * 100), 0) / 100
  const vatNum = Math.round((Number(vatAmount) || 0) * 100) / 100
  const grandTotal = Math.round((supplyTotal + vatNum) * 100) / 100
  const effectiveTotal = lines.length > 0 ? String(grandTotal) : totalAmount
  // 부가세 10% 자동 채움(편의값) — 실제 인보이스 세액으로 수정 가능.
  const fillVat10 = () => setVatAmount(String(Math.round(supplyTotal * 10) / 100))

  const addLine = () => setLines((ls) => [...ls, { accountId: '', amount: '', description: '' }])
  const removeLine = (i: number) => setLines((ls) => ls.filter((_, idx) => idx !== i))
  const updateLine = (i: number, patch: Partial<LineForm>) =>
    setLines((ls) => ls.map((l, idx) => (idx === i ? { ...l, ...patch } : l)))

  const openCreate = () => {
    setInvoiceNo('')
    setVendorId('')
    setInvoiceDate('')
    setDueDate('')
    setTotalAmount('')
    setCurrency('KRW')
    setNote('')
    setLines([])
    setVatAccountId('')
    setVatAmount('')
    setDialog({ type: 'create' })
  }

  const handleCreate = () => {
    if (
      !invoiceNo.trim() ||
      !vendorId ||
      !invoiceDate ||
      !dueDate ||
      !effectiveTotal ||
      Number(effectiveTotal) <= 0
    ) {
      toast.error('필수 항목을 모두 입력해주세요')
      return
    }
    if (lines.length > 0 && lines.some((l) => !l.accountId || !l.amount || Number(l.amount) <= 0)) {
      toast.error('분개 라인의 계정과 금액을 모두 입력해주세요')
      return
    }
    if (vatNum > 0 && !vatAccountId) {
      toast.error('부가세 금액을 입력했으면 부가세대급금 계정을 선택해주세요')
      return
    }
    // 공급 라인 + (부가세 라인) → 승인 시 GL 차변. 합계 = effectiveTotal(서버가 균형 검증).
    // 금액은 소수 2자리(전)로 반올림해 전송 — 라인합계(전 단위)·서버 BigDecimal 검증과 일치
    // (예: 사용자가 2자리 초과 입력/붙여넣기해도 총금액 불일치로 거부되지 않게).
    const supplyLines = lines.map((l) => ({
      accountId: Number(l.accountId),
      amount: Math.round(Number(l.amount) * 100) / 100,
      description: l.description || null,
    }))
    const vatLine =
      vatAccountId && vatNum > 0
        ? [{ accountId: Number(vatAccountId), amount: vatNum, description: '부가세대급금' }]
        : []
    startTransition(async () => {
      try {
        await createInvoice({
          invoiceNo: invoiceNo.trim(),
          vendorId: Number(vendorId),
          invoiceDate,
          dueDate,
          totalAmount: Number(effectiveTotal),
          currency: currency || 'KRW',
          note: note || null,
          lines: lines.length > 0 ? [...supplyLines, ...vatLine] : null,
        })
        toast.success('인보이스가 등록되었습니다')
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
        toast.success('인보이스가 승인되었습니다')
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
        toast.success('인보이스가 취소되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '취소 중 오류가 발생했습니다')
      }
    })
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">매입 인보이스</h1>
          <p className="text-sm text-muted-foreground mt-1">
            공급업체 인보이스 및 지급 현황을 관리합니다
          </p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 인보이스
          </Button>
        )}
      </div>

      <div className="bg-card rounded-lg border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>인보이스번호</TableHead>
              <TableHead>공급업체</TableHead>
              <TableHead>인보이스일</TableHead>
              <TableHead>만기일</TableHead>
              <TableHead className="text-right">총금액</TableHead>
              <TableHead className="text-right">미납금액</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-28" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground py-10">
                  등록된 인보이스가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((inv) => (
              <TableRow key={inv.id}>
                <TableCell className="font-mono text-sm">{inv.invoiceNo}</TableCell>
                <TableCell className="font-medium">{inv.vendorName}</TableCell>
                <TableCell className="text-sm">{inv.invoiceDate}</TableCell>
                <TableCell className="text-sm">{inv.dueDate}</TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {fmt(inv.totalAmount, inv.currency)}
                </TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {fmt(inv.outstandingAmount, inv.currency)}
                </TableCell>
                <TableCell>
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
                </TableCell>
                <TableCell>
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
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
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
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>새 인보이스 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>인보이스번호 *</Label>
                <Input
                  value={invoiceNo}
                  onChange={(e) => setInvoiceNo(e.target.value)}
                  placeholder="INV-2024-001"
                />
              </div>
              <div className="grid gap-1.5">
                <Label>공급업체 *</Label>
                <Select value={vendorId} onValueChange={(v) => setVendorId(v ?? '')}>
                  <SelectTrigger className="w-full">
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
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>인보이스일 *</Label>
                <Input
                  type="date"
                  value={invoiceDate}
                  onChange={(e) => setInvoiceDate(e.target.value)}
                />
              </div>
              <div className="grid gap-1.5">
                <Label>만기일 *</Label>
                <Input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div className="grid gap-1.5 col-span-2">
                <Label>총금액 *</Label>
                <Input
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={effectiveTotal}
                  readOnly={lines.length > 0}
                  onChange={(e) => setTotalAmount(e.target.value)}
                  placeholder="0"
                  className={lines.length > 0 ? 'bg-muted/40 text-muted-foreground' : undefined}
                />
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
                    공급가 합계: <span className="font-medium">{fmt(supplyTotal, currency)}</span>
                  </div>
                </div>
              )}
            </div>

            {/* 부가세 자동 split — 부가세대급금 차변 라인을 자동 생성. 10% 자동, 실제 세액으로 수정 가능. */}
            {lines.length > 0 && (
              <div className="grid gap-2 rounded-md border bg-muted/40 p-3">
                <Label>부가세 (자동 분개)</Label>
                <div className="flex gap-2 items-end">
                  <div className="flex-1 grid gap-1.5">
                    <span className="text-xs text-muted-foreground">부가세대급금 계정</span>
                    <Select value={vatAccountId} onValueChange={(v) => setVatAccountId(v ?? '')}>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="(부가세 없으면 비워두기)" />
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
                  <div className="grid gap-1.5 w-36">
                    <span className="text-xs text-muted-foreground">세액</span>
                    <Input
                      type="number"
                      min={0}
                      step={0.01}
                      placeholder="0"
                      value={vatAmount}
                      onChange={(e) => setVatAmount(e.target.value)}
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={fillVat10}
                    disabled={supplyTotal <= 0}
                  >
                    10% 자동
                  </Button>
                </div>
                <div className="text-right text-sm">
                  <span className="text-muted-foreground">
                    공급가 {fmt(supplyTotal, currency)} + 부가세 {fmt(vatNum, currency)} ={' '}
                  </span>
                  <span className="font-semibold text-foreground">
                    합계 {fmt(grandTotal, currency)}
                  </span>
                </div>
              </div>
            )}

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
                <Input type="date" value={payDate} onChange={(e) => setPayDate(e.target.value)} />
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
            <DialogTitle>인보이스 취소</DialogTitle>
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
