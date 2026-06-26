'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, CheckIcon, BanIcon, Trash2Icon } from 'lucide-react'
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
  createActivity, completeActivity, cancelActivity, deleteActivity, type ActivityPayload,
} from './actions'
import type { Activity, ActivityType, ActivityStatus, CrmAccount } from '@/types/crm'
import type { PageResponse } from '@/types/api'

const TYPE_LABEL: Record<ActivityType, string> = {
  CALL: '전화',
  EMAIL: '이메일',
  MEETING: '미팅',
  TASK: '과업',
  NOTE: '메모',
}
const STATUS_LABEL: Record<ActivityStatus, string> = {
  OPEN: '진행',
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
  const [dueDate, setDueDate] = useState('')
  const [description, setDescription] = useState('')

  const openCreate = () => {
    setActivityType('CALL'); setSubject(''); setAccountId('')
    setDueDate(''); setDescription('')
    setDialog({ type: 'create' })
  }

  const buildPayload = (): ActivityPayload => ({
    activityType,
    subject: subject.trim(),
    accountId: accountId ? Number(accountId) : null,
    dueDate: dueDate ? `${dueDate}:00` : null,
    description: description.trim() || null,
  })

  const validate = (): boolean => {
    if (!subject.trim()) { toast.error('제목은 필수입니다'); return false }
    return true
  }

  const handleCreate = () => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await createActivity(buildPayload())
        toast.success('활동이 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleComplete = (act: Activity) => {
    startTransition(async () => {
      try {
        await completeActivity(act.id)
        toast.success('활동이 완료 처리되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '완료 처리 중 오류가 발생했습니다') }
    })
  }

  const handleCancel = (act: Activity) => {
    startTransition(async () => {
      try {
        await cancelActivity(act.id)
        toast.success('활동이 취소 처리되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '취소 처리 중 오류가 발생했습니다') }
    })
  }

  const handleDelete = (act: Activity) => {
    startTransition(async () => {
      try {
        await deleteActivity(act.id)
        toast.success('활동이 삭제되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다') }
    })
  }

  const activityForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>유형 *</Label>
          <Select value={activityType} onValueChange={(v) => setActivityType((v ?? 'CALL') as ActivityType)}>
            <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
            <SelectContent>
              {(Object.keys(TYPE_LABEL) as ActivityType[]).map((t) => (
                <SelectItem key={t} value={t}>{TYPE_LABEL[t]}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-1.5">
          <Label>고객사</Label>
          <Select value={accountId} onValueChange={(v) => setAccountId(v ?? '')}>
            <SelectTrigger className="w-full"><SelectValue placeholder="선택 안 함" /></SelectTrigger>
            <SelectContent>
              {accounts.map((acc) => (
                <SelectItem key={acc.id} value={String(acc.id)}>{acc.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label>제목 *</Label>
        <Input value={subject} onChange={(e) => setSubject(e.target.value)} maxLength={300} />
      </div>
      <div className="grid gap-1.5">
        <Label>마감일</Label>
        <Input type="datetime-local" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
      </div>
      <div className="grid gap-1.5">
        <Label>설명</Label>
        <Textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">활동</h1>
          <p className="text-sm text-gray-500 mt-1">영업 활동 이력을 관리합니다</p>
        </div>
        {canWrite && <Button onClick={openCreate}><PlusIcon />새 활동</Button>}
      </div>

      <div className="bg-white rounded-lg border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>유형</TableHead>
              <TableHead>제목</TableHead>
              <TableHead>고객사</TableHead>
              <TableHead>담당자</TableHead>
              <TableHead>마감일</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-28" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  등록된 활동이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((act) => (
              <TableRow key={act.id}>
                <TableCell>
                  <Badge variant="secondary">{TYPE_LABEL[act.activityType]}</Badge>
                </TableCell>
                <TableCell className="font-medium max-w-xs truncate">{act.subject}</TableCell>
                <TableCell className="text-sm text-gray-600">{act.accountName ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{act.contactName ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{act.dueDate ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[act.status]}>{STATUS_LABEL[act.status]}</Badge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    {canWrite && act.status === 'OPEN' && (
                      <>
                        <Button variant="ghost" size="icon-xs" title="완료"
                          onClick={() => setDialog({ type: 'complete', activity: act })}>
                          <CheckIcon />
                        </Button>
                        <Button variant="ghost" size="icon-xs" title="취소"
                          onClick={() => setDialog({ type: 'cancel', activity: act })}>
                          <BanIcon />
                        </Button>
                      </>
                    )}
                    {canWrite && (
                      <Button variant="ghost" size="icon-xs" title="삭제"
                        onClick={() => setDialog({ type: 'delete', activity: act })}>
                        <Trash2Icon className="text-destructive" />
                      </Button>
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
          basePath="/crm/activities"
        />
      </div>

      {/* Create */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>새 활동 등록</DialogTitle></DialogHeader>
          {activityForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Complete */}
      <Dialog open={dialog.type === 'complete'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>활동 완료</DialogTitle></DialogHeader>
          {dialog.type === 'complete' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.activity.subject}</strong> — 이 활동을 완료 처리하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button onClick={() => dialog.type === 'complete' && handleComplete(dialog.activity)}
              disabled={isPending}>완료</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Cancel */}
      <Dialog open={dialog.type === 'cancel'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>활동 취소</DialogTitle></DialogHeader>
          {dialog.type === 'cancel' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.activity.subject}</strong> — 이 활동을 취소 처리하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button variant="destructive"
              onClick={() => dialog.type === 'cancel' && handleCancel(dialog.activity)}
              disabled={isPending}>취소 처리</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete */}
      <Dialog open={dialog.type === 'delete'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>활동 삭제</DialogTitle></DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.activity.subject}</strong> — 이 활동을 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.activity)}
              disabled={isPending}>삭제</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
