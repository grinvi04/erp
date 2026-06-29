'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, CheckIcon, BanIcon, Trash2Icon, DownloadIcon } from 'lucide-react'
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
  createActivity,
  completeActivity,
  cancelActivity,
  deleteActivity,
  fetchContactsByAccount,
  fetchOpportunitiesByAccount,
  type ActivityPayload,
} from './actions'
import type {
  Activity,
  ActivityType,
  ActivityStatus,
  CrmAccount,
  Contact,
  Opportunity,
} from '@/types/crm'
import type { PageResponse } from '@/types/api'
import { formatDateTime } from '@/lib/utils'

const TYPE_LABEL: Record<ActivityType, string> = {
  CALL: '전화',
  EMAIL: '이메일',
  MEETING: '회의',
  TASK: '업무',
  NOTE: '메모',
}
const STATUS_LABEL: Record<ActivityStatus, string> = {
  OPEN: '예정',
  COMPLETED: '완료',
  CANCELLED: '취소',
}
const STATUS_VARIANT: Record<ActivityStatus, 'default' | 'secondary' | 'destructive'> = {
  OPEN: 'secondary',
  COMPLETED: 'default',
  CANCELLED: 'destructive',
}

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'complete'; activity: Activity }
  | { type: 'cancel'; activity: Activity }
  | { type: 'delete'; activity: Activity }

interface Props {
  data: PageResponse<Activity>
  accounts: CrmAccount[]
}

export default function ActivitiesClient({ data, accounts }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.CRM_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [activityType, setActivityType] = useState<ActivityType>('CALL')
  const [subject, setSubject] = useState('')
  const [accountId, setAccountId] = useState('')
  const [contactId, setContactId] = useState('')
  const [opportunityId, setOpportunityId] = useState('')
  const [contacts, setContacts] = useState<Contact[]>([])
  const [opportunities, setOpportunities] = useState<Opportunity[]>([])
  const [dueDate, setDueDate] = useState('')
  const [description, setDescription] = useState('')

  const openCreate = () => {
    setActivityType('CALL')
    setSubject('')
    setAccountId('')
    setContactId('')
    setOpportunityId('')
    setContacts([])
    setOpportunities([])
    setDueDate('')
    setDescription('')
    setDialog({ type: 'create' })
  }

  // 고객사 선택 시 그 고객사의 담당자·영업기회 목록을 on-demand 조회한다.
  const selectAccount = (value: string) => {
    setAccountId(value)
    setContactId('')
    setOpportunityId('')
    setContacts([])
    setOpportunities([])
    if (!value) return
    const id = Number(value)
    void (async () => {
      try {
        const [c, o] = await Promise.all([
          fetchContactsByAccount(id),
          fetchOpportunitiesByAccount(id),
        ])
        setContacts(c)
        setOpportunities(o)
      } catch {
        toast.error('담당자·영업기회 목록을 불러오지 못했습니다')
      }
    })()
  }

  const buildPayload = (): ActivityPayload => ({
    activityType,
    subject: subject.trim(),
    accountId: accountId ? Number(accountId) : null,
    contactId: contactId ? Number(contactId) : null,
    opportunityId: opportunityId ? Number(opportunityId) : null,
    dueDate: dueDate ? `${dueDate}:00` : null,
    description: description.trim() || null,
  })

  const validate = (): boolean => {
    if (!subject.trim()) {
      toast.error('제목은 필수입니다')
      return false
    }
    return true
  }

  const handleCreate = () => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await createActivity(buildPayload())
        toast.success('활동이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleComplete = (act: Activity) => {
    startTransition(async () => {
      try {
        await completeActivity(act.id)
        toast.success('활동이 완료 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '완료 처리 중 오류가 발생했습니다')
      }
    })
  }

  const handleCancel = (act: Activity) => {
    startTransition(async () => {
      try {
        await cancelActivity(act.id)
        toast.success('활동이 취소 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '취소 처리 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (act: Activity) => {
    startTransition(async () => {
      try {
        await deleteActivity(act.id)
        toast.success('활동이 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const activityForm = (
    <div className="grid gap-4 py-2">
      <FormGrid>
        <FormRow label="유형" required>
          <Select
            value={activityType}
            onValueChange={(v) => setActivityType((v ?? 'CALL') as ActivityType)}
          >
            <SelectTrigger className="h-8 w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(TYPE_LABEL) as ActivityType[]).map((t) => (
                <SelectItem key={t} value={t}>
                  {TYPE_LABEL[t]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="고객사">
          <Select value={accountId} onValueChange={(v) => selectAccount(v ?? '')}>
            <SelectTrigger className="h-8 w-full">
              <SelectValue placeholder="고객사(선택)" />
            </SelectTrigger>
            <SelectContent>
              {accounts.map((acc) => (
                <SelectItem key={acc.id} value={String(acc.id)}>
                  {acc.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="담당자">
          <Select
            value={contactId}
            onValueChange={(v) => setContactId(v ?? '')}
            disabled={!accountId}
          >
            <SelectTrigger className="h-8 w-full">
              <SelectValue placeholder={accountId ? '선택 안 함' : '고객사 먼저 선택'} />
            </SelectTrigger>
            <SelectContent>
              {contacts.map((c) => (
                <SelectItem key={c.id} value={String(c.id)}>
                  {c.lastName} {c.firstName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="영업기회">
          <Select
            value={opportunityId}
            onValueChange={(v) => setOpportunityId(v ?? '')}
            disabled={!accountId}
          >
            <SelectTrigger className="h-8 w-full">
              <SelectValue placeholder={accountId ? '선택 안 함' : '고객사 먼저 선택'} />
            </SelectTrigger>
            <SelectContent>
              {opportunities.map((o) => (
                <SelectItem key={o.id} value={String(o.id)}>
                  {o.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="제목" required>
          <Input
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            maxLength={300}
            className="h-8"
          />
        </FormRow>
        <FormRow label="마감일">
          <Input
            type="datetime-local"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className="h-8"
          />
        </FormRow>
      </FormGrid>
      <div className="grid gap-1.5">
        <Label>설명</Label>
        <Textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
    </div>
  )

  const columns: Column<Activity>[] = [
    {
      key: 'activityType',
      header: '유형',
      sortable: true,
      sortValue: (act) => TYPE_LABEL[act.activityType],
      cell: (act) => <Badge variant="secondary">{TYPE_LABEL[act.activityType]}</Badge>,
    },
    {
      key: 'subject',
      header: '제목',
      sortable: true,
      sortValue: (act) => act.subject,
      cell: (act) => <span className="font-medium max-w-xs truncate block">{act.subject}</span>,
    },
    {
      key: 'accountName',
      header: '고객사',
      cell: (act) => (
        <span className="text-sm text-muted-foreground">{act.accountName ?? '—'}</span>
      ),
    },
    {
      key: 'contactName',
      header: '담당자',
      cell: (act) => (
        <span className="text-sm text-muted-foreground">{act.contactName ?? '—'}</span>
      ),
    },
    {
      key: 'opportunityName',
      header: '영업기회',
      cell: (act) => (
        <span className="text-sm text-muted-foreground">{act.opportunityName ?? '—'}</span>
      ),
    },
    {
      key: 'dueDate',
      header: '마감일',
      sortable: true,
      sortValue: (act) => act.dueDate,
      cell: (act) => (
        <span className="whitespace-nowrap text-sm text-muted-foreground">
          {formatDateTime(act.dueDate)}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (act) => STATUS_LABEL[act.status],
      cell: (act) => <Badge variant={STATUS_VARIANT[act.status]}>{STATUS_LABEL[act.status]}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-28',
      cell: (act) => (
        <div className="flex justify-end gap-1">
          {canWrite && act.status === 'OPEN' && (
            <>
              <Button
                variant="ghost"
                size="icon-xs"
                title="완료"
                onClick={() => setDialog({ type: 'complete', activity: act })}
              >
                <CheckIcon />
              </Button>
              <Button
                variant="ghost"
                size="icon-xs"
                title="취소"
                onClick={() => setDialog({ type: 'cancel', activity: act })}
              >
                <BanIcon />
              </Button>
            </>
          )}
          {canWrite && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="삭제"
              onClick={() => setDialog({ type: 'delete', activity: act })}
            >
              <Trash2Icon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qType, setQType] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [qAccount, setQAccount] = useState('')
  const [applied, setApplied] = useState({ type: '', status: '', account: '' })
  const onSearch = () => setApplied({ type: qType, status: qStatus, account: qAccount })
  const onReset = () => {
    setQType('')
    setQStatus('')
    setQAccount('')
    setApplied({ type: '', status: '', account: '' })
  }
  const filtered = data.content.filter((act) => {
    if (applied.type && act.activityType !== applied.type) return false
    if (applied.status && act.status !== applied.status) return false
    if (applied.account && String(act.accountId ?? '') !== applied.account) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `활동_${new Date().toISOString().slice(0, 10)}`,
      ['유형', '제목', '고객사', '담당자', '영업기회', '마감일', '상태'],
      filtered.map((act) => [
        TYPE_LABEL[act.activityType],
        act.subject,
        act.accountName ?? '',
        act.contactName ?? '',
        act.opportunityName ?? '',
        formatDateTime(act.dueDate),
        STATUS_LABEL[act.status],
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader title="활동" description="영업 활동 이력을 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 활동
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="유형">
            <Select value={qType || 'ALL'} onValueChange={(v) => setQType(v === 'ALL' ? '' : (v ?? ''))}>
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {(Object.keys(TYPE_LABEL) as ActivityType[]).map((t) => (
                  <SelectItem key={t} value={t}>
                    {TYPE_LABEL[t]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="상태">
            <Select value={qStatus || 'ALL'} onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}>
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {(Object.keys(STATUS_LABEL) as ActivityStatus[]).map((s) => (
                  <SelectItem key={s} value={s}>
                    {STATUS_LABEL[s]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="고객사">
            <Select value={qAccount || 'ALL'} onValueChange={(v) => setQAccount(v === 'ALL' ? '' : (v ?? ''))}>
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
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(act) => act.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 활동이 없습니다"
              description={canWrite ? '우측 상단에서 새 활동을 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/crm/activities"
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
            <DialogTitle>새 활동 등록</DialogTitle>
          </DialogHeader>
          {activityForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Complete */}
      <Dialog
        open={dialog.type === 'complete'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>활동 완료</DialogTitle>
          </DialogHeader>
          {dialog.type === 'complete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.activity.subject}</strong> — 이 활동을 완료 처리하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'complete' && handleComplete(dialog.activity)}
              disabled={isPending}
            >
              완료
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Cancel */}
      <Dialog
        open={dialog.type === 'cancel'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>활동 취소</DialogTitle>
          </DialogHeader>
          {dialog.type === 'cancel' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.activity.subject}</strong> — 이 활동을 취소 처리하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'cancel' && handleCancel(dialog.activity)}
              disabled={isPending}
            >
              취소 처리
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
            <DialogTitle>활동 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.activity.subject}</strong> — 이 활동을 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.activity)}
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
