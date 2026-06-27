'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { updateBaseCurrency, createExchangeRate, updateFxGainLossAccounts } from './actions'
import type { Account, ExchangeRate, FxGainLossAccounts } from '@/types/finance'

const CURRENCY_PATTERN = /^[A-Z]{3}$/
const NONE = 'NONE'

type DialogMode = { type: 'none' } | { type: 'base' } | { type: 'rate' }

interface Props {
  baseCurrency: string
  rates: ExchangeRate[]
  accounts: Account[]
  fxAccounts: FxGainLossAccounts
}

export default function FxClient({ baseCurrency, rates, accounts, fxAccounts }: Props) {
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
    fxAccounts.fxGainAccountId != null ? String(fxAccounts.fxGainAccountId) : '')
  const [fxLossAccountId, setFxLossAccountId] = useState(
    fxAccounts.fxLossAccountId != null ? String(fxAccounts.fxLossAccountId) : '')

  const handleSaveFxAccounts = () => {
    startTransition(async () => {
      try {
        await updateFxGainLossAccounts({
          fxGainAccountId: fxGainAccountId ? Number(fxGainAccountId) : null,
          fxLossAccountId: fxLossAccountId ? Number(fxLossAccountId) : null,
        })
        toast.success('환차손익 계정이 저장되었습니다')
      } catch (e) { toast.error(e instanceof Error ? e.message : '저장 중 오류가 발생했습니다') }
    })
  }

  const openBase = () => { setBaseInput(baseCurrency); setDialog({ type: 'base' }) }
  const openRate = () => {
    setFromCurrency('USD'); setToCurrency(baseCurrency); setEffectiveDate(''); setRate('')
    setDialog({ type: 'rate' })
  }

  const handleUpdateBase = () => {
    const code = baseInput.trim().toUpperCase()
    if (!CURRENCY_PATTERN.test(code)) { toast.error('통화 코드는 대문자 3자리(ISO 4217)여야 합니다'); return }
    startTransition(async () => {
      try {
        await updateBaseCurrency(code)
        toast.success('기준통화가 변경되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '변경 중 오류가 발생했습니다') }
    })
  }

  const handleCreateRate = () => {
    const from = fromCurrency.trim().toUpperCase()
    const to = toCurrency.trim().toUpperCase()
    if (!CURRENCY_PATTERN.test(from) || !CURRENCY_PATTERN.test(to)) {
      toast.error('통화 코드는 대문자 3자리(ISO 4217)여야 합니다'); return
    }
    if (!effectiveDate) { toast.error('발효일은 필수입니다'); return }
    const rateValue = Number(rate)
    if (!rate || Number.isNaN(rateValue) || rateValue <= 0) { toast.error('환율은 0보다 커야 합니다'); return }
    startTransition(async () => {
      try {
        await createExchangeRate({ fromCurrency: from, toCurrency: to, effectiveDate, rate: rateValue })
        toast.success('환율이 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-foreground">FX 설정</h1>
        <p className="text-sm text-muted-foreground mt-1">기준통화와 환율을 관리합니다 (혼합통화 거래의 환산 기준)</p>
      </div>

      <div className="bg-card rounded-lg border p-5 mb-6">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm text-muted-foreground">기준통화</div>
            <div className="text-2xl font-semibold font-mono mt-1">{baseCurrency}</div>
          </div>
          {canWrite && (
            <Button variant="outline" onClick={openBase}><PencilIcon />기준통화 변경</Button>
          )}
        </div>
      </div>

      <div className="bg-card rounded-lg border p-5 mb-6">
        <div className="mb-1 text-sm font-medium text-foreground">환차손익 계정 (실현)</div>
        <p className="text-xs text-muted-foreground mb-4">
          외화 결제 시 결제환율과 인보이스 환율의 차액을 분개할 계정입니다. 둘 다 설정해야 환차 분개가 적용됩니다.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="grid gap-1.5">
            <Label>외환차이익 계정</Label>
            <Select value={fxGainAccountId || NONE} disabled={!canWrite}
              onValueChange={(v) => setFxGainAccountId(!v || v === NONE ? '' : v)}>
              <SelectTrigger className="w-full"><SelectValue placeholder="미설정" /></SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {selectableAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>{a.code} {a.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-1.5">
            <Label>외환차손 계정</Label>
            <Select value={fxLossAccountId || NONE} disabled={!canWrite}
              onValueChange={(v) => setFxLossAccountId(!v || v === NONE ? '' : v)}>
              <SelectTrigger className="w-full"><SelectValue placeholder="미설정" /></SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {selectableAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>{a.code} {a.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        {canWrite && (
          <div className="mt-4 flex justify-end">
            <Button onClick={handleSaveFxAccounts} disabled={isPending}>저장</Button>
          </div>
        )}
      </div>

      <div className="bg-card rounded-lg border overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b">
          <h2 className="font-medium text-foreground">환율</h2>
          {canWrite && <Button onClick={openRate}><PlusIcon />환율 등록</Button>}
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
      <Dialog open={dialog.type === 'base'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>기준통화 변경</DialogTitle></DialogHeader>
          <div className="grid gap-1.5 py-2">
            <Label>기준통화 (ISO 4217, 예: KRW)</Label>
            <Input value={baseInput} maxLength={3}
              onChange={(e) => setBaseInput(e.target.value.toUpperCase())} placeholder="KRW" />
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleUpdateBase} disabled={isPending}>저장</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 환율 등록 Dialog */}
      <Dialog open={dialog.type === 'rate'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>환율 등록</DialogTitle></DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>기준 통화 *</Label>
                <Input value={fromCurrency} maxLength={3}
                  onChange={(e) => setFromCurrency(e.target.value.toUpperCase())} placeholder="USD" />
              </div>
              <div className="grid gap-1.5">
                <Label>대상 통화 *</Label>
                <Input value={toCurrency} maxLength={3}
                  onChange={(e) => setToCurrency(e.target.value.toUpperCase())} placeholder={baseCurrency} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>발효일 *</Label>
                <Input type="date" value={effectiveDate} onChange={(e) => setEffectiveDate(e.target.value)} />
              </div>
              <div className="grid gap-1.5">
                <Label>환율 *</Label>
                <Input type="number" min={0} step="0.00000001" value={rate}
                  onChange={(e) => setRate(e.target.value)} placeholder="1300.5" />
              </div>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreateRate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
