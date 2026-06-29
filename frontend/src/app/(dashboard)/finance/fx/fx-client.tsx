'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DatePicker } from '@/components/ui/date-picker'
import { PageHeader } from '@/components/ui/page-header'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
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
import {
  updateBaseCurrency,
  createExchangeRate,
  updateFxGainLossAccounts,
  updateVatAccounts,
} from './actions'
import type { Account, ExchangeRate, FxGainLossAccounts, VatAccounts } from '@/types/finance'

const CURRENCY_PATTERN = /^[A-Z]{3}$/
const NONE = 'NONE'

type DialogMode = { type: 'none' } | { type: 'base' } | { type: 'rate' }

interface Props {
  baseCurrency: string
  rates: ExchangeRate[]
  accounts: Account[]
  fxAccounts: FxGainLossAccounts
  vatAccounts: VatAccounts
}

export default function FxClient({
  baseCurrency,
  rates,
  accounts,
  fxAccounts,
  vatAccounts,
}: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_SETTING_WRITE)
  const selectableAccounts = accounts.filter((a) => !a.isSummary && a.isActive)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [baseInput, setBaseInput] = useState(baseCurrency)
  const [fromCurrency, setFromCurrency] = useState('USD')
  const [toCurrency, setToCurrency] = useState(baseCurrency)
  const [effectiveDate, setEffectiveDate] = useState('')
  const [rate, setRate] = useState('')

  const [fxGainAccountId, setFxGainAccountId] = useState(
    fxAccounts.fxGainAccountId != null ? String(fxAccounts.fxGainAccountId) : '',
  )
  const [fxLossAccountId, setFxLossAccountId] = useState(
    fxAccounts.fxLossAccountId != null ? String(fxAccounts.fxLossAccountId) : '',
  )

  const handleSaveFxAccounts = () => {
    startTransition(async () => {
      try {
        await updateFxGainLossAccounts({
          fxGainAccountId: fxGainAccountId ? Number(fxGainAccountId) : null,
          fxLossAccountId: fxLossAccountId ? Number(fxLossAccountId) : null,
        })
        toast.success('환차손익 계정이 저장되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '저장 중 오류가 발생했습니다')
      }
    })
  }

  const [vatReceivableAccountId, setVatReceivableAccountId] = useState(
    vatAccounts.vatReceivableAccountId != null ? String(vatAccounts.vatReceivableAccountId) : '',
  )
  const [vatPayableAccountId, setVatPayableAccountId] = useState(
    vatAccounts.vatPayableAccountId != null ? String(vatAccounts.vatPayableAccountId) : '',
  )

  const handleSaveVatAccounts = () => {
    startTransition(async () => {
      try {
        await updateVatAccounts({
          vatReceivableAccountId: vatReceivableAccountId ? Number(vatReceivableAccountId) : null,
          vatPayableAccountId: vatPayableAccountId ? Number(vatPayableAccountId) : null,
        })
        toast.success('부가세 통제계정이 저장되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '저장 중 오류가 발생했습니다')
      }
    })
  }

  const openBase = () => {
    setBaseInput(baseCurrency)
    setDialog({ type: 'base' })
  }
  const openRate = () => {
    setFromCurrency('USD')
    setToCurrency(baseCurrency)
    setEffectiveDate('')
    setRate('')
    setDialog({ type: 'rate' })
  }

  const handleUpdateBase = () => {
    const code = baseInput.trim().toUpperCase()
    if (!CURRENCY_PATTERN.test(code)) {
      toast.error('통화 코드는 대문자 3자리(ISO 4217)여야 합니다')
      return
    }
    startTransition(async () => {
      try {
        await updateBaseCurrency(code)
        toast.success('기준통화가 변경되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '변경 중 오류가 발생했습니다')
      }
    })
  }

  const handleCreateRate = () => {
    const from = fromCurrency.trim().toUpperCase()
    const to = toCurrency.trim().toUpperCase()
    if (!CURRENCY_PATTERN.test(from) || !CURRENCY_PATTERN.test(to)) {
      toast.error('통화 코드는 대문자 3자리(ISO 4217)여야 합니다')
      return
    }
    if (!effectiveDate) {
      toast.error('발효일은 필수입니다')
      return
    }
    const rateValue = Number(rate)
    if (!rate || Number.isNaN(rateValue) || rateValue <= 0) {
      toast.error('환율은 0보다 커야 합니다')
      return
    }
    startTransition(async () => {
      try {
        await createExchangeRate({
          fromCurrency: from,
          toCurrency: to,
          effectiveDate,
          rate: rateValue,
        })
        toast.success('환율이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  return (
    <div className="p-5">
      <PageHeader
        title="FX 설정"
        description="기준통화와 환율을 관리합니다 (혼합통화 거래의 환산 기준)"
        className="mb-4"
      />

      <div className="bg-card rounded-lg border p-5 mb-4 mt-4">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm text-muted-foreground">기준통화</div>
            <div className="text-2xl font-semibold font-mono mt-1">{baseCurrency}</div>
          </div>
          {canWrite && (
            <Button variant="outline" onClick={openBase}>
              <PencilIcon />
              기준통화 변경
            </Button>
          )}
        </div>
      </div>

      <div className="bg-card rounded-lg border p-5 mb-4">
        <div className="mb-1 text-sm font-medium text-foreground">환차손익 계정 (실현)</div>
        <p className="text-xs text-muted-foreground mb-4">
          외화 결제 시 결제환율과 인보이스 환율의 차액을 분개할 계정입니다. 둘 다 설정해야 환차
          분개가 적용됩니다.
        </p>
        <FormGrid>
          <FormRow label="외환차이익 계정">
            <Select
              value={fxGainAccountId || NONE}
              disabled={!canWrite}
              onValueChange={(v) => setFxGainAccountId(!v || v === NONE ? '' : v)}
            >
              <SelectTrigger className="h-8 w-full">
                <SelectValue placeholder="미설정" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {selectableAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.code} {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FormRow>
          <FormRow label="외환차손 계정">
            <Select
              value={fxLossAccountId || NONE}
              disabled={!canWrite}
              onValueChange={(v) => setFxLossAccountId(!v || v === NONE ? '' : v)}
            >
              <SelectTrigger className="h-8 w-full">
                <SelectValue placeholder="미설정" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {selectableAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.code} {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FormRow>
        </FormGrid>
        {canWrite && (
          <div className="mt-4 flex justify-end">
            <Button onClick={handleSaveFxAccounts} disabled={isPending}>
              저장
            </Button>
          </div>
        )}
      </div>

      <div className="bg-card rounded-lg border p-5 mb-4">
        <div className="mb-1 text-sm font-medium text-foreground">부가세 통제계정</div>
        <p className="text-xs text-muted-foreground mb-4">
          과세 매입/매출 인보이스 승인 전기 시 매입세액·매출세액을 분개할 통제계정입니다. 설정하면
          해당 부가세 라인이 자동 분개되고, 미설정이면 부가세 없이 공급가액으로 전기됩니다.
        </p>
        <FormGrid>
          <FormRow label="부가세대급금 (매입)">
            <Select
              value={vatReceivableAccountId || NONE}
              disabled={!canWrite}
              onValueChange={(v) => setVatReceivableAccountId(!v || v === NONE ? '' : v)}
            >
              <SelectTrigger className="h-8 w-full">
                <SelectValue placeholder="미설정" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {selectableAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.code} {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FormRow>
          <FormRow label="부가세예수금 (매출)">
            <Select
              value={vatPayableAccountId || NONE}
              disabled={!canWrite}
              onValueChange={(v) => setVatPayableAccountId(!v || v === NONE ? '' : v)}
            >
              <SelectTrigger className="h-8 w-full">
                <SelectValue placeholder="미설정" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {selectableAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.code} {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FormRow>
        </FormGrid>
        {canWrite && (
          <div className="mt-4 flex justify-end">
            <Button onClick={handleSaveVatAccounts} disabled={isPending}>
              저장
            </Button>
          </div>
        )}
      </div>

      <div className="bg-card rounded-lg border overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b">
          <h2 className="font-medium text-foreground">환율</h2>
          {canWrite && (
            <Button onClick={openRate}>
              <PlusIcon />
              환율 등록
            </Button>
          )}
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>발효일</TableHead>
              <TableHead>기준 통화</TableHead>
              <TableHead>대상 통화</TableHead>
              <TableHead className="text-right">환율</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rates.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground py-10">
                  등록된 환율이 없습니다
                </TableCell>
              </TableRow>
            )}
            {rates.map((r) => (
              <TableRow key={r.id}>
                <TableCell className="text-sm text-muted-foreground">{r.effectiveDate}</TableCell>
                <TableCell className="font-mono text-sm">{r.fromCurrency}</TableCell>
                <TableCell className="font-mono text-sm">{r.toCurrency}</TableCell>
                <TableCell className="text-right font-mono text-sm">{r.rate}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* 기준통화 변경 Dialog */}
      <Dialog
        open={dialog.type === 'base'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>기준통화 변경</DialogTitle>
          </DialogHeader>
          <FormGrid cols={1} className="my-2">
            <FormRow label="기준통화" required>
              <Input
                value={baseInput}
                maxLength={3}
                onChange={(e) => setBaseInput(e.target.value.toUpperCase())}
                placeholder="KRW (ISO 4217)"
                className="h-8"
              />
            </FormRow>
          </FormGrid>
          <DialogFooter showCloseButton>
            <Button onClick={handleUpdateBase} disabled={isPending}>
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 환율 등록 Dialog */}
      <Dialog
        open={dialog.type === 'rate'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>환율 등록</DialogTitle>
          </DialogHeader>
          <FormGrid className="my-2">
            <FormRow label="기준 통화" required>
              <Input
                value={fromCurrency}
                maxLength={3}
                onChange={(e) => setFromCurrency(e.target.value.toUpperCase())}
                placeholder="USD"
                className="h-8"
              />
            </FormRow>
            <FormRow label="대상 통화" required>
              <Input
                value={toCurrency}
                maxLength={3}
                onChange={(e) => setToCurrency(e.target.value.toUpperCase())}
                placeholder={baseCurrency}
                className="h-8"
              />
            </FormRow>
            <FormRow label="발효일" required>
              <DatePicker value={effectiveDate} onChange={setEffectiveDate} />
            </FormRow>
            <FormRow label="환율" required>
              <Input
                type="number"
                min={0}
                step="0.00000001"
                value={rate}
                onChange={(e) => setRate(e.target.value)}
                placeholder="1300.5"
                className="h-8"
              />
            </FormRow>
          </FormGrid>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreateRate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
