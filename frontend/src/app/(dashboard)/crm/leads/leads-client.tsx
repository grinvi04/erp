'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, Trash2Icon, ArrowRightLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
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
import { createLead, updateLead, convertLead, deleteLead, type LeadPayload } from './actions'
import type { CrmAccount, Lead, LeadStatus, PipelineStage } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import { formatUserName, formatDate } from '@/lib/utils'

const STATUS_LABEL: Record<LeadStatus, string> = {
  NEW: '신규',
  CONTACTED: '접촉완료',
  QUALIFIED: '적격',
  CONVERTED: '전환',
  DISQUALIFIED: '부적격',
}
const STATUS_VARIANT: Record<LeadStatus, 'default' | 'secondary' | 'destructive'> = {
  NEW: 'secondary',
  CONTACTED: 'secondary',
  QUALIFIED: 'default',
  CONVERTED: 'default',
  DISQUALIFIED: 'destructive',
}

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; lead: Lead }
  | { type: 'convert'; lead: Lead }
  | { type: 'delete'; lead: Lead }

interface Props {
  data: PageResponse<Lead>
  accounts: CrmAccount[]
  stages: PipelineStage[]
  names: Record<string, string>
}

export default function LeadsClient({ data, accounts, stages, names }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.CRM_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [lastName, setLastName] = useState('')
  const [firstName, setFirstName] = useState('')
  const [company, setCompany] = useState('')
  const [title, setTitle] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [source, setSource] = useState('')
  const [ownerId, setOwnerId] = useState('')
  const [note, setNote] = useState('')

  const [accountId, setAccountId] = useState('')
  const [createNewAccount, setCreateNewAccount] = useState(false)
  const [createOpp, setCreateOpp] = useState(false)
  const [oppName, setOppName] = useState('')
  const [oppStageId, setOppStageId] = useState('')
  const [oppAmount, setOppAmount] = useState('')

  const openCreate = () => {
    setLastName('')
    setFirstName('')
    setCompany('')
    setTitle('')
    setEmail('')
    setPhone('')
    setSource('')
    setNote('')
    setDialog({ type: 'create' })
  }

  const openEdit = (lead: Lead) => {
    if (lead.status === 'CONVERTED') {
      toast.error('전환된 리드는 수정할 수 없습니다')
      return
    }
    setLastName(lead.lastName)
    setFirstName(lead.firstName)
    setCompany(lead.company ?? '')
    setTitle(lead.title ?? '')
    setEmail(lead.email ?? '')
    setPhone(lead.phone ?? '')
    setSource(lead.source ?? '')
    setOwnerId(lead.ownerId)
    setNote(lead.note ?? '')
    setDialog({ type: 'edit', lead })
  }

  const openConvert = (lead: Lead) => {
    setAccountId('')
    setCreateNewAccount(false)
    setCreateOpp(false)
    setOppName('')
    setOppStageId('')
    setOppAmount('')
    setDialog({ type: 'convert', lead })
  }

  const buildPayload = (): Omit<LeadPayload, 'ownerId'> => ({
    lastName: lastName.trim(),
    firstName: firstName.trim(),
    company: company.trim() || null,
    title: title.trim() || null,
    email: email.trim() || null,
    phone: phone.trim() || null,
    source: source.trim() || null,
    note: note.trim() || null,
  })

  const validate = (): boolean => {
    if (!lastName.trim()) {
      toast.error('성은 필수입니다')
      return false
    }
    if (!firstName.trim()) {
      toast.error('이름은 필수입니다')
      return false
    }
    return true
  }

  const handleCreate = () => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await createLead(buildPayload())
        toast.success('리드가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (lead: Lead) => {
    if (!validate()) return
    if (!ownerId.trim()) {
      toast.error('담당자는 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateLead(lead.id, {
          ...buildPayload(),
          ownerId: ownerId.trim(),
          version: lead.version,
        })
        toast.success('리드가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleConvert = (lead: Lead) => {
    if (!createNewAccount && !accountId) {
      toast.error('고객사를 선택하거나 신규 생성을 선택해주세요')
      return
    }
    if (createOpp && !oppStageId) {
      toast.error('영업기회 단계를 선택해주세요')
      return
    }
    startTransition(async () => {
      try {
        await convertLead(lead.id, {
          accountId: createNewAccount ? null : Number(accountId),
          createOpportunity: createOpp,
          opportunityName: createOpp ? oppName.trim() || null : null,
          stageId: createOpp ? Number(oppStageId) : null,
          opportunityAmount: createOpp && oppAmount.trim() ? Number(oppAmount) : null,
          opportunityCurrency: null,
          opportunityCloseDate: null,
        })
        toast.success('리드가 전환되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '전환 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (lead: Lead) => {
    startTransition(async () => {
      try {
        await deleteLead(lead.id)
        toast.success('리드가 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const leadForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>성 *</Label>
          <Input value={lastName} onChange={(e) => setLastName(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>이름 *</Label>
          <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>회사</Label>
          <Input value={company} onChange={(e) => setCompany(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>직함</Label>
          <Input value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>이메일</Label>
          <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>전화</Label>
          <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>출처</Label>
          <Input value={source} onChange={(e) => setSource(e.target.value)} />
        </div>
        {dialog.type === 'edit' && (
          <div className="grid gap-1.5">
            <Label>담당자 ID *</Label>
            <Input value={ownerId} onChange={(e) => setOwnerId(e.target.value)} />
          </div>
        )}
      </div>
      <div className="grid gap-1.5">
        <Label>메모</Label>
        <Textarea rows={3} value={note} onChange={(e) => setNote(e.target.value)} />
      </div>
    </div>
  )

  const columns: Column<Lead>[] = [
    {
      key: 'name',
      header: '이름',
      sortable: true,
      sortValue: (lead) => `${lead.lastName}${lead.firstName}`,
      cell: (lead) => (
        <span className="font-medium">
          {lead.lastName}
          {lead.firstName}
        </span>
      ),
    },
    {
      key: 'company',
      header: '회사',
      sortable: true,
      sortValue: (lead) => lead.company,
      cell: (lead) => <span className="text-sm text-muted-foreground">{lead.company ?? '—'}</span>,
    },
    {
      key: 'title',
      header: '직함',
      cell: (lead) => <span className="text-sm text-muted-foreground">{lead.title ?? '—'}</span>,
    },
    {
      key: 'email',
      header: '이메일',
      cell: (lead) => <span className="text-sm text-muted-foreground">{lead.email ?? '—'}</span>,
    },
    {
      key: 'source',
      header: '출처',
      cell: (lead) => <span className="text-sm text-muted-foreground">{lead.source ?? '—'}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (lead) => STATUS_LABEL[lead.status],
      cell: (lead) => (
        <Badge variant={STATUS_VARIANT[lead.status]}>{STATUS_LABEL[lead.status]}</Badge>
      ),
    },
    {
      key: 'owner',
      header: '담당자',
      sortable: true,
      sortValue: (lead) => formatUserName(lead.ownerId, names),
      cell: (lead) => (
        <span className="text-sm" title={lead.ownerId}>
          {formatUserName(lead.ownerId, names)}
        </span>
      ),
    },
    {
      key: 'createdAt',
      header: '생성일',
      sortable: true,
      sortValue: (lead) => lead.createdAt,
      cell: (lead) => (
        <span className="text-sm text-muted-foreground">{formatDate(lead.createdAt)}</span>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-28',
      cell: (lead) => (
        <div className="flex justify-end gap-1">
          {canWrite && lead.status !== 'CONVERTED' && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(lead)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && lead.status !== 'CONVERTED' && lead.status !== 'DISQUALIFIED' && (
            <Button variant="ghost" size="icon-xs" title="전환" onClick={() => openConvert(lead)}>
              <ArrowRightLeft />
            </Button>
          )}
          {canWrite && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="삭제"
              onClick={() => setDialog({ type: 'delete', lead })}
            >
              <Trash2Icon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader title="리드" description="잠재 고객 리드를 관리합니다" className="mb-6">
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 리드
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <DataTable
          data={data.content}
          columns={columns}
          getRowId={(lead) => lead.id}
          empty={
            <EmptyState
              title="등록된 리드가 없습니다"
              description={canWrite ? '우측 상단에서 새 리드를 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/crm/leads"
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
            <DialogTitle>새 리드 등록</DialogTitle>
          </DialogHeader>
          {leadForm}
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
              리드 수정
              {dialog.type === 'edit' && ` — ${dialog.lead.lastName}${dialog.lead.firstName}`}
            </DialogTitle>
          </DialogHeader>
          {leadForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.lead)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Convert */}
      <Dialog
        open={dialog.type === 'convert'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              리드 전환
              {dialog.type === 'convert' && ` — ${dialog.lead.lastName}${dialog.lead.firstName}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <p className="text-sm text-muted-foreground">
              전환 시 리드 정보로 담당자가 생성되어 아래 고객사에 연결됩니다.
            </p>
            <div className="flex items-center gap-2">
              <Checkbox
                id="convert-new-account"
                checked={createNewAccount}
                onCheckedChange={(c) => setCreateNewAccount(c === true)}
              />
              <Label htmlFor="convert-new-account" className="font-normal">
                리드 정보로 신규 고객사 생성
              </Label>
            </div>
            {!createNewAccount && (
              <div className="grid gap-1.5">
                <Label>고객사 *</Label>
                <Select value={accountId} onValueChange={(v) => setAccountId(v ?? '')}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="고객사 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {accounts.map((acc) => (
                      <SelectItem key={acc.id} value={String(acc.id)}>
                        {acc.code} {acc.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
            <div className="flex items-center gap-2 border-t pt-4">
              <Checkbox
                id="convert-create-opp"
                checked={createOpp}
                onCheckedChange={(c) => setCreateOpp(c === true)}
              />
              <Label htmlFor="convert-create-opp" className="font-normal">
                영업기회 함께 생성
              </Label>
            </div>
            {createOpp && (
              <>
                <div className="grid gap-1.5">
                  <Label>기회명</Label>
                  <Input
                    value={oppName}
                    placeholder="비워두면 회사명으로 생성됩니다"
                    onChange={(e) => setOppName(e.target.value)}
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="grid gap-1.5">
                    <Label>단계 *</Label>
                    <Select value={oppStageId} onValueChange={(v) => setOppStageId(v ?? '')}>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="단계 선택" />
                      </SelectTrigger>
                      <SelectContent>
                        {stages.map((stage) => (
                          <SelectItem key={stage.id} value={String(stage.id)}>
                            {stage.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="grid gap-1.5">
                    <Label>예상 금액</Label>
                    <Input
                      type="number"
                      min={0}
                      value={oppAmount}
                      onChange={(e) => setOppAmount(e.target.value)}
                    />
                  </div>
                </div>
              </>
            )}
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'convert' && handleConvert(dialog.lead)}
              disabled={isPending}
            >
              전환
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
            <DialogTitle>리드 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.lead.lastName}
                {dialog.lead.firstName}
              </strong>{' '}
              리드를 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.lead)}
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
