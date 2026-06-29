'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, Trash2Icon, DownloadIcon } from 'lucide-react'
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
import {
  createOpportunity,
  updateOpportunity,
  deleteOpportunity,
  type OpportunityPayload,
} from './actions'
import type { Opportunity, CrmAccount, PipelineStage } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import { formatUserName } from '@/lib/utils'

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
  names: Record<string, string>
}

export default function OpportunitiesClient({ data, accounts, stages, names }: Props) {
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
    setAccountId('')
    setName('')
    setAmount('')
    setCurrency('KRW')
    setCloseDate('')
    setSource('')
    setDescription('')
    const first = stages.length > 0 ? stages[0] : null
    setStageId(first ? String(first.id) : '')
    setProbability(first ? String(first.probability) : '0')
    setDialog({ type: 'create' })
  }

  // 단계 선택 시 해당 단계의 기본확률을 확률 입력에 자동 채운다(사용자가 덮어쓸 수 있음).
  const selectStage = (value: string) => {
    setStageId(value)
    const stage = stages.find((s) => String(s.id) === value)
    if (stage) setProbability(String(stage.probability))
  }

  const openEdit = (opp: Opportunity) => {
    setName(opp.name)
    setStageId(String(opp.stageId))
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
    if (!name.trim()) {
      toast.error('기회명은 필수입니다')
      return false
    }
    if (!stageId) {
      toast.error('단계를 선택하세요')
      return false
    }
    if (currency.trim() && currency.trim().length !== 3) {
      toast.error('통화 코드는 3자리여야 합니다')
      return false
    }
    if (isNaN(Number(probability)) || Number(probability) < 0 || Number(probability) > 100) {
      toast.error('확률은 0~100 사이여야 합니다')
      return false
    }
    if (amount && (isNaN(Number(amount)) || Number(amount) < 0)) {
      toast.error('금액이 올바르지 않습니다')
      return false
    }
    return true
  }

  const handleCreate = () => {
    if (!accountId) {
      toast.error('고객사를 선택하세요')
      return
    }
    if (!validate()) return
    startTransition(async () => {
      try {
        await createOpportunity({ accountId: Number(accountId), ...buildPayload() })
        toast.success('영업 기회가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (opp: Opportunity) => {
    if (!validate()) return
    if (!ownerId.trim()) {
      toast.error('담당자는 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateOpportunity(opp.id, {
          ...buildPayload(),
          ownerId: ownerId.trim(),
          version: opp.version,
        })
        toast.success('영업 기회가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (opp: Opportunity) => {
    startTransition(async () => {
      try {
        await deleteOpportunity(opp.id)
        toast.success('영업 기회가 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const oppForm = (
    <div className="grid gap-4 py-2">
      <FormGrid>
        {dialog.type === 'create' && (
          <FormRow label="고객사" required>
            <Select value={accountId} onValueChange={(v) => setAccountId(v ?? '')}>
              <SelectTrigger className="h-8 w-full">
                <SelectValue placeholder="선택" />
              </SelectTrigger>
              <SelectContent>
                {accounts.map((acc) => (
                  <SelectItem key={acc.id} value={String(acc.id)}>
                    {acc.code} {acc.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FormRow>
        )}
        <FormRow label="기회명" required>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="기회명"
            className="h-8"
          />
        </FormRow>
        <FormRow label="단계" required>
          <Select value={stageId} onValueChange={(v) => selectStage(v ?? '')}>
            <SelectTrigger className="h-8 w-full">
              <SelectValue placeholder="선택" />
            </SelectTrigger>
            <SelectContent>
              {stages.map((s) => (
                <SelectItem key={s.id} value={String(s.id)}>
                  {s.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="확률(%)" required>
          <Input
            type="number"
            min={0}
            max={100}
            value={probability}
            onChange={(e) => setProbability(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="금액">
          <Input
            type="number"
            min={0}
            step={0.01}
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="통화">
          <Input
            maxLength={3}
            value={currency}
            onChange={(e) => setCurrency(e.target.value.toUpperCase())}
            placeholder="KRW"
            className="h-8"
          />
        </FormRow>
        <FormRow label="예상 종결일">
          <DatePicker value={closeDate} onChange={setCloseDate} />
        </FormRow>
        {dialog.type === 'edit' && (
          <FormRow label="담당자 ID" required>
            <Input value={ownerId} onChange={(e) => setOwnerId(e.target.value)} className="h-8" />
          </FormRow>
        )}
        <FormRow label="출처">
          <Input value={source} onChange={(e) => setSource(e.target.value)} className="h-8" />
        </FormRow>
      </FormGrid>
      <div className="grid gap-1.5">
        <Label>설명</Label>
        <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
    </div>
  )

  const columns: Column<Opportunity>[] = [
    {
      key: 'name',
      header: '기회명',
      sortable: true,
      sortValue: (opp) => opp.name,
      cell: (opp) => <span className="font-medium">{opp.name}</span>,
    },
    {
      key: 'accountName',
      header: '고객사',
      sortable: true,
      sortValue: (opp) => opp.accountName,
      cell: (opp) => <span className="text-sm text-foreground">{opp.accountName}</span>,
    },
    {
      key: 'stage',
      header: '단계',
      sortable: true,
      sortValue: (opp) => opp.stageName,
      cell: (opp) => <Badge variant="secondary">{opp.stageName}</Badge>,
    },
    {
      key: 'amount',
      header: '금액',
      align: 'right',
      sortable: true,
      sortValue: (opp) => opp.amount,
      cell: (opp) => (
        <span className="font-mono text-sm">{formatAmount(opp.amount, opp.currency)}</span>
      ),
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + (r.amount ?? 0), 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'probability',
      header: '확률(%)',
      align: 'right',
      sortable: true,
      sortValue: (opp) => opp.probability,
      cell: (opp) => <span className="text-sm">{opp.probability}%</span>,
    },
    {
      key: 'closeDate',
      header: '예상 종결일',
      sortable: true,
      sortValue: (opp) => opp.closeDate,
      cell: (opp) => <span className="text-sm text-muted-foreground">{opp.closeDate ?? '—'}</span>,
    },
    {
      key: 'owner',
      header: '담당자',
      sortable: true,
      sortValue: (opp) => formatUserName(opp.ownerId, names),
      cell: (opp) => (
        <span className="text-sm" title={opp.ownerId}>
          {formatUserName(opp.ownerId, names)}
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (opp) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <>
              <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(opp)}>
                <PencilIcon />
              </Button>
              <Button
                variant="ghost"
                size="icon-xs"
                title="삭제"
                onClick={() => setDialog({ type: 'delete', opp })}
              >
                <Trash2Icon className="text-destructive" />
              </Button>
            </>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qAccount, setQAccount] = useState('')
  const [qStage, setQStage] = useState('')
  const [qFrom, setQFrom] = useState('')
  const [qTo, setQTo] = useState('')
  const [applied, setApplied] = useState({ account: '', stage: '', from: '', to: '' })
  const onSearch = () => setApplied({ account: qAccount, stage: qStage, from: qFrom, to: qTo })
  const onReset = () => {
    setQAccount('')
    setQStage('')
    setQFrom('')
    setQTo('')
    setApplied({ account: '', stage: '', from: '', to: '' })
  }
  const filtered = data.content.filter((opp) => {
    if (applied.account && String(opp.accountId) !== applied.account) return false
    if (applied.stage && String(opp.stageId) !== applied.stage) return false
    if (applied.from && (!opp.closeDate || opp.closeDate < applied.from)) return false
    if (applied.to && (!opp.closeDate || opp.closeDate > applied.to)) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `영업기회_${new Date().toISOString().slice(0, 10)}`,
      ['기회명', '고객사', '단계', '금액', '통화', '확률(%)', '예상종결일', '담당자'],
      filtered.map((opp) => [
        opp.name,
        opp.accountName,
        opp.stageName,
        opp.amount ?? '',
        opp.currency ?? '',
        opp.probability,
        opp.closeDate ?? '',
        formatUserName(opp.ownerId, names),
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader title="영업 기회" description="영업 파이프라인을 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate} disabled={stages.length === 0}>
            <PlusIcon />새 영업기회
          </Button>
        )}
      </PageHeader>
      {stages.length === 0 && (
        <div className="mb-4 rounded-md border border-warning/20 bg-warning/10 px-4 py-2 text-sm text-warning">
          영업 기회를 등록하려면 먼저 파이프라인 단계를 1개 이상 정의해야 합니다.
        </div>
      )}

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="고객사">
            <Select
              value={qAccount || 'ALL'}
              onValueChange={(v) => setQAccount(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {accounts.map((acc) => (
                  <SelectItem key={acc.id} value={String(acc.id)}>
                    {acc.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="단계">
            <Select
              value={qStage || 'ALL'}
              onValueChange={(v) => setQStage(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {stages.map((s) => (
                  <SelectItem key={s.id} value={String(s.id)}>
                    {s.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="예상 종결일">
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
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(opp) => opp.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 영업 기회가 없습니다"
              description={
                canWrite && stages.length > 0
                  ? '우측 상단에서 새 영업 기회를 등록하세요.'
                  : undefined
              }
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/crm/opportunities"
        />
      </div>

      {/* Create */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>새 영업 기회 등록</DialogTitle>
          </DialogHeader>
          {oppForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit */}
      <Dialog
        open={dialog.type === 'edit'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              영업 기회 수정{dialog.type === 'edit' && ` — ${dialog.opp.name}`}
            </DialogTitle>
          </DialogHeader>
          {oppForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.opp)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete */}
      <Dialog
        open={dialog.type === 'delete'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>영업 기회 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.opp.name}</strong>을(를) 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.opp)}
              disabled={isPending}
            >
              삭제
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
