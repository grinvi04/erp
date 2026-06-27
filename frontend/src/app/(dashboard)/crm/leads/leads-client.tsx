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
import { createLead, updateLead, convertLead, deleteLead, type LeadPayload } from './actions'
import type { CrmAccount, Lead, LeadStatus } from '@/types/crm'
import type { PageResponse } from '@/types/api'

const STATUS_LABEL: Record<LeadStatus, string> = {
  NEW: '신규',
  CONTACTED: '접촉',
  QUALIFIED: '적격',
  CONVERTED: '전환',
  DISQUALIFIED: '불량',
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
}

export default function LeadsClient({ data, accounts }: Props) {
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

  const openCreate = () => {
    setLastName(''); setFirstName(''); setCompany(''); setTitle(''); setEmail('')
    setPhone(''); setSource(''); setNote('')
    setDialog({ type: 'create' })
  }

  const openEdit = (lead: Lead) => {
    if (lead.status === 'CONVERTED') {
      toast.error('전환된 리드는 수정할 수 없습니다')
      return
    }
    setLastName(lead.lastName); setFirstName(lead.firstName)
    setCompany(lead.company ?? ''); setTitle(lead.title ?? '')
    setEmail(lead.email ?? ''); setPhone(lead.phone ?? '')
    setSource(lead.source ?? ''); setOwnerId(lead.ownerId); setNote(lead.note ?? '')
    setDialog({ type: 'edit', lead })
  }

  const openConvert = (lead: Lead) => {
    setAccountId('')
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
    if (!lastName.trim()) { toast.error('성은 필수입니다'); return false }
    if (!firstName.trim()) { toast.error('이름은 필수입니다'); return false }
    return true
  }

  const handleCreate = () => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await createLead(buildPayload())
        toast.success('리드가 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (lead: Lead) => {
    if (!validate()) return
    if (!ownerId.trim()) { toast.error('담당자는 필수입니다'); return }
    startTransition(async () => {
      try {
        await updateLead(lead.id, { ...buildPayload(), ownerId: ownerId.trim(), version: lead.version })
        toast.success('리드가 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleConvert = (lead: Lead) => {
    if (!accountId) { toast.error('고객사를 선택해주세요'); return }
    startTransition(async () => {
      try {
        await convertLead(lead.id, { accountId: Number(accountId), opportunityId: null })
        toast.success('리드가 전환되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '전환 중 오류가 발생했습니다') }
    })
  }

  const handleDelete = (lead: Lead) => {
    startTransition(async () => {
      try {
        await deleteLead(lead.id)
        toast.success('리드가 삭제되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다') }
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

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">리드</h1>
          <p className="text-sm text-muted-foreground mt-1">잠재 고객 리드를 관리합니다</p>
        </div>
        {canWrite && <Button onClick={openCreate}><PlusIcon />새 리드</Button>}
      </div>

      <div className="bg-card rounded-lg border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>이름</TableHead>
              <TableHead>회사</TableHead>
              <TableHead>직함</TableHead>
              <TableHead>이메일</TableHead>
              <TableHead>출처</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>생성일</TableHead>
              <TableHead className="w-28" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground py-10">
                  등록된 리드가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((lead) => (
              <TableRow key={lead.id}>
                <TableCell className="font-medium">
                  {lead.lastName}{lead.firstName}
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">{lead.company ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{lead.title ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{lead.email ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{lead.source ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[lead.status]}>
                    {STATUS_LABEL[lead.status]}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {lead.createdAt.slice(0, 10)}
                </TableCell>
                <TableCell>
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
                      <Button variant="ghost" size="icon-xs" title="삭제"
                        onClick={() => setDialog({ type: 'delete', lead })}>
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
          basePath="/crm/leads"
        />
      </div>

      {/* Create */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>새 리드 등록</DialogTitle></DialogHeader>
          {leadForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit */}
      <Dialog open={dialog.type === 'edit'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              리드 수정{dialog.type === 'edit' && ` — ${dialog.lead.lastName}${dialog.lead.firstName}`}
            </DialogTitle>
          </DialogHeader>
          {leadForm}
          <DialogFooter showCloseButton>
            <Button onClick={() => dialog.type === 'edit' && handleUpdate(dialog.lead)}
              disabled={isPending}>저장</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Convert */}
      <Dialog open={dialog.type === 'convert'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              리드 전환{dialog.type === 'convert' && ` — ${dialog.lead.lastName}${dialog.lead.firstName}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>고객사 *</Label>
              <Select value={accountId} onValueChange={(v) => setAccountId(v ?? '')}>
                <SelectTrigger className="w-full"><SelectValue placeholder="고객사 선택" /></SelectTrigger>
                <SelectContent>
                  {accounts.map((acc) => (
                    <SelectItem key={acc.id} value={String(acc.id)}>
                      {acc.code} {acc.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={() => dialog.type === 'convert' && handleConvert(dialog.lead)}
              disabled={isPending}>전환</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete */}
      <Dialog open={dialog.type === 'delete'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>리드 삭제</DialogTitle></DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.lead.lastName}{dialog.lead.firstName}</strong> 리드를 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.lead)}
              disabled={isPending}>삭제</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
