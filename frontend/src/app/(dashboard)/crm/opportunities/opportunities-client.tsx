'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, Trash2Icon } from 'lucide-react'
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
import {
  createOpportunity, updateOpportunity, deleteOpportunity, type OpportunityPayload,
} from './actions'
import type { Opportunity, CrmAccount, PipelineStage } from '@/types/crm'
import type { PageResponse } from '@/types/api'

function formatAmount(amount: number | null, currency: string) {
  if (amount === null) return '—'
  return `${currency} ${amount.toLocaleString('ko-KR')}`
}

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; opp: Opportunity }
  | { type: 'delete'; opp: Opportunity }

interface Props {
  data: PageResponse<Opportunity>
  accounts: CrmAccount[]
  stages: PipelineStage[]
}

export default function OpportunitiesClient({ data, accounts, stages }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.CRM_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [accountId, setAccountId] = useState('')
  const [name, setName] = useState('')
  const [stageId, setStageId] = useState('')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('')
  const [closeDate, setCloseDate] = useState('')
  const [probability, setProbability] = useState('')
  const [ownerId, setOwnerId] = useState('')
  const [source, setSource] = useState('')
  const [description, setDescription] = useState('')

  const openCreate = () => {
    setAccountId(''); setName(''); setAmount(''); setCurrency('KRW')
    setCloseDate(''); setProbability('0')
    setSource(''); setDescription('')
    setStageId(stages.length > 0 ? String(stages[0].id) : '')
    setDialog({ type: 'create' })
  }

  const openEdit = (opp: Opportunity) => {
    setName(opp.name); setStageId(String(opp.stageId))
    setAmount(opp.amount != null ? String(opp.amount) : '')
    setCurrency(opp.currency ?? '')
    setCloseDate(opp.closeDate ?? '')
    setProbability(String(opp.probability))
    setOwnerId(opp.ownerId)
    setSource(opp.source ?? '')
    setDescription(opp.description ?? '')
    setDialog({ type: 'edit', opp })
  }

  const buildPayload = (): Omit<OpportunityPayload, 'ownerId'> => ({
    name: name.trim(),
    stageId: Number(stageId),
    amount: amount ? Number(amount) : null,
    currency: currency.trim() || null,
    closeDate: closeDate || null,
    probability: Number(probability) || 0,
    source: source.trim() || null,
    description: description.trim() || null,
  })

  const validate = (): boolean => {
    if (!name.trim()) { toast.error('기회명은 필수입니다'); return false }
    if (!stageId) { toast.error('단계를 선택하세요'); return false }
    if (currency.trim() && currency.trim().length !== 3) {
      toast.error('통화 코드는 3자리여야 합니다'); return false
    }
    if (isNaN(Number(probability)) || Number(probability) < 0 || Number(probability) > 100) {
      toast.error('확률은 0~100 사이여야 합니다'); return false
    }
    if (amount && (isNaN(Number(amount)) || Number(amount) < 0)) {
      toast.error('금액이 올바르지 않습니다'); return false
    }
    return true
  }

  const handleCreate = () => {
    if (!accountId) { toast.error('고객사를 선택하세요'); return }
    if (!validate()) return
    startTransition(async () => {
      try {
        await createOpportunity({ accountId: Number(accountId), ...buildPayload() })
        toast.success('영업 기회가 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (opp: Opportunity) => {
    if (!validate()) return
    if (!ownerId.trim()) { toast.error('담당자는 필수입니다'); return }
    startTransition(async () => {
      try {
        await updateOpportunity(opp.id, { ...buildPayload(), ownerId: ownerId.trim(), version: opp.version })
        toast.success('영업 기회가 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDelete = (opp: Opportunity) => {
    startTransition(async () => {
      try {
        await deleteOpportunity(opp.id)
        toast.success('영업 기회가 삭제되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다') }
    })
  }

  const oppForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        {dialog.type === 'create' && (
          <div className="grid gap-1.5">
            <Label>고객사 *</Label>
            <Select value={accountId} onValueChange={(v) => setAccountId(v ?? '')}>
              <SelectTrigger className="w-full"><SelectValue placeholder="선택" /></SelectTrigger>
              <SelectContent>
                {accounts.map((acc) => (
                  <SelectItem key={acc.id} value={String(acc.id)}>{acc.code} {acc.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
        <div className="grid gap-1.5">
          <Label>기회명 *</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="기회명" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>단계 *</Label>
          <Select value={stageId} onValueChange={(v) => setStageId(v ?? '')}>
            <SelectTrigger className="w-full"><SelectValue placeholder="선택" /></SelectTrigger>
            <SelectContent>
              {stages.map((s) => (
                <SelectItem key={s.id} value={String(s.id)}>{s.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-1.5">
          <Label>확률(%) *</Label>
          <Input type="number" min={0} max={100} value={probability}
            onChange={(e) => setProbability(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-4">
        <div className="grid gap-1.5">
          <Label>금액</Label>
          <Input type="number" min={0} step={0.01} value={amount}
            onChange={(e) => setAmount(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>통화</Label>
          <Input maxLength={3} value={currency}
            onChange={(e) => setCurrency(e.target.value.toUpperCase())} placeholder="KRW" />
        </div>
        <div className="grid gap-1.5">
          <Label>예상 종결일</Label>
          <Input type="date" value={closeDate} onChange={(e) => setCloseDate(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        {dialog.type === 'edit' && (
          <div className="grid gap-1.5">
            <Label>담당자 ID *</Label>
            <Input value={ownerId} onChange={(e) => setOwnerId(e.target.value)} />
          </div>
        )}
        <div className="grid gap-1.5">
          <Label>출처</Label>
          <Input value={source} onChange={(e) => setSource(e.target.value)} />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label>설명</Label>
        <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">영업 기회</h1>
          <p className="text-sm text-muted-foreground mt-1">영업 파이프라인을 관리합니다</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate} disabled={stages.length === 0}>
            <PlusIcon />새 영업기회
          </Button>
        )}
      </div>
      {stages.length === 0 && (
        <div className="mb-4 rounded-md border border-warning/20 bg-warning/10 px-4 py-2 text-sm text-warning">
          영업 기회를 등록하려면 먼저 파이프라인 단계를 1개 이상 정의해야 합니다.
        </div>
      )}

      <div className="bg-card rounded-lg border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>기회명</TableHead>
              <TableHead>고객사</TableHead>
              <TableHead>단계</TableHead>
              <TableHead className="text-right">금액</TableHead>
              <TableHead className="text-right">확률(%)</TableHead>
              <TableHead>예상 종결일</TableHead>
              <TableHead className="w-20" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground py-10">
                  등록된 영업 기회가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((opp) => (
              <TableRow key={opp.id}>
                <TableCell className="font-medium">{opp.name}</TableCell>
                <TableCell className="text-sm text-foreground">{opp.accountName}</TableCell>
                <TableCell><Badge variant="secondary">{opp.stageName}</Badge></TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatAmount(opp.amount, opp.currency)}
                </TableCell>
                <TableCell className="text-right text-sm">{opp.probability}%</TableCell>
                <TableCell className="text-sm text-muted-foreground">{opp.closeDate ?? '—'}</TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    {canWrite && (
                      <>
                        <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(opp)}>
                          <PencilIcon />
                        </Button>
                        <Button variant="ghost" size="icon-xs" title="삭제"
                          onClick={() => setDialog({ type: 'delete', opp })}>
                          <Trash2Icon className="text-destructive" />
                        </Button>
                      </>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationBar
          page={data.page} totalPages={data.totalPages}
          totalElements={data.totalElements} size={data.size}
          basePath="/crm/opportunities"
        />
      </div>

      {/* Create */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>새 영업 기회 등록</DialogTitle></DialogHeader>
          {oppForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit */}
      <Dialog open={dialog.type === 'edit'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>영업 기회 수정{dialog.type === 'edit' && ` — ${dialog.opp.name}`}</DialogTitle>
          </DialogHeader>
          {oppForm}
          <DialogFooter showCloseButton>
            <Button onClick={() => dialog.type === 'edit' && handleUpdate(dialog.opp)}
              disabled={isPending}>저장</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete */}
      <Dialog open={dialog.type === 'delete'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>영업 기회 삭제</DialogTitle></DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.opp.name}</strong>을(를) 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.opp)}
              disabled={isPending}>삭제</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
