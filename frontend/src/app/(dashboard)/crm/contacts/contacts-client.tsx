'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  getContactsByAccount, createContact, updateContact, deleteContact,
  type ContactPayload,
} from './actions'
import type { Contact, CrmAccount } from '@/types/crm'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; contact: Contact }
  | { type: 'delete'; contact: Contact }

interface Props {
  accounts: CrmAccount[]
}

export default function ContactsClient({ accounts }: Props) {
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [contacts, setContacts] = useState<Contact[]>([])
  const [isLoadingContacts, setIsLoadingContacts] = useState(false)

  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [lastName, setLastName] = useState('')
  const [firstName, setFirstName] = useState('')
  const [title, setTitle] = useState('')
  const [department, setDepartment] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [mobile, setMobile] = useState('')
  const [isPrimary, setIsPrimary] = useState(false)

  const reload = () => {
    if (!selectedAccountId) return
    setIsLoadingContacts(true)
    getContactsByAccount(Number(selectedAccountId))
      .then((cs) => setContacts(cs))
      .catch(() => toast.error('담당자 목록을 불러오지 못했습니다'))
      .finally(() => setIsLoadingContacts(false))
  }

  const onAccountChange = (val: string | null) => {
    const aId = val ?? ''
    setSelectedAccountId(aId)
    setContacts([])
    if (aId) {
      setIsLoadingContacts(true)
      getContactsByAccount(Number(aId))
        .then((cs) => setContacts(cs))
        .catch(() => toast.error('담당자 목록을 불러오지 못했습니다'))
        .finally(() => setIsLoadingContacts(false))
    }
  }

  const openCreate = () => {
    setLastName(''); setFirstName(''); setTitle(''); setDepartment('')
    setEmail(''); setPhone(''); setMobile(''); setIsPrimary(false)
    setDialog({ type: 'create' })
  }

  const openEdit = (ct: Contact) => {
    setLastName(ct.lastName); setFirstName(ct.firstName)
    setTitle(ct.title ?? ''); setDepartment(ct.department ?? '')
    setEmail(ct.email ?? ''); setPhone(ct.phone ?? ''); setMobile(ct.mobile ?? '')
    setIsPrimary(ct.isPrimary)
    setDialog({ type: 'edit', contact: ct })
  }

  const buildPayload = (): ContactPayload => ({
    lastName: lastName.trim(),
    firstName: firstName.trim(),
    title: title.trim() || null,
    department: department.trim() || null,
    email: email.trim() || null,
    phone: phone.trim() || null,
    mobile: mobile.trim() || null,
    isPrimary,
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
        await createContact({ accountId: Number(selectedAccountId), ...buildPayload() })
        toast.success('담당자가 등록되었습니다')
        close()
        reload()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (ct: Contact) => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await updateContact(ct.id, buildPayload())
        toast.success('담당자가 수정되었습니다')
        close()
        reload()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDelete = (ct: Contact) => {
    startTransition(async () => {
      try {
        await deleteContact(ct.id)
        toast.success('담당자가 삭제되었습니다')
        close()
        reload()
      } catch (e) { toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다') }
    })
  }

  const contactForm = (
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
          <Label>직함</Label>
          <Input value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>부서</Label>
          <Input value={department} onChange={(e) => setDepartment(e.target.value)} />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label>이메일</Label>
        <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>전화</Label>
          <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>휴대폰</Label>
          <Input value={mobile} onChange={(e) => setMobile(e.target.value)} />
        </div>
      </div>
      <label className="flex items-center gap-2 cursor-pointer">
        <input type="checkbox" checked={isPrimary}
          onChange={(e) => setIsPrimary(e.target.checked)}
          className="h-4 w-4 rounded border-gray-300" />
        <span className="text-sm">주 담당자</span>
      </label>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">담당자</h1>
          <p className="text-sm text-gray-500 mt-1">고객사 담당자 정보를 관리합니다</p>
        </div>
        <Button onClick={openCreate} disabled={!selectedAccountId}>
          <PlusIcon />새 담당자
        </Button>
      </div>

      <div className="mb-4 max-w-md">
        <Label className="mb-1.5 block">고객사</Label>
        <Select value={selectedAccountId} onValueChange={onAccountChange}>
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

      {!selectedAccountId ? (
        <div className="bg-white rounded-lg border py-16 text-center text-gray-400">
          고객사를 선택하면 담당자 목록이 표시됩니다
        </div>
      ) : (
        <div className="bg-white rounded-lg border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>이름</TableHead>
                <TableHead>직함</TableHead>
                <TableHead>부서</TableHead>
                <TableHead>이메일</TableHead>
                <TableHead>전화</TableHead>
                <TableHead>휴대폰</TableHead>
                <TableHead>주 담당자</TableHead>
                <TableHead className="w-20" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoadingContacts ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center text-gray-400 py-10">
                    불러오는 중...
                  </TableCell>
                </TableRow>
              ) : contacts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center text-gray-400 py-10">
                    등록된 담당자가 없습니다
                  </TableCell>
                </TableRow>
              ) : (
                contacts.map((ct) => (
                  <TableRow key={ct.id}>
                    <TableCell className="font-medium">{ct.lastName}{ct.firstName}</TableCell>
                    <TableCell className="text-sm text-gray-600">{ct.title ?? '—'}</TableCell>
                    <TableCell className="text-sm text-gray-600">{ct.department ?? '—'}</TableCell>
                    <TableCell className="text-sm text-gray-600">{ct.email ?? '—'}</TableCell>
                    <TableCell className="text-sm text-gray-600">{ct.phone ?? '—'}</TableCell>
                    <TableCell className="text-sm text-gray-600">{ct.mobile ?? '—'}</TableCell>
                    <TableCell>{ct.isPrimary && <Badge>주 담당자</Badge>}</TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(ct)}>
                          <PencilIcon />
                        </Button>
                        <Button variant="ghost" size="icon-xs" title="삭제"
                          onClick={() => setDialog({ type: 'delete', contact: ct })}>
                          <Trash2Icon className="text-destructive" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Create */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>새 담당자 등록</DialogTitle></DialogHeader>
          {contactForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit */}
      <Dialog open={dialog.type === 'edit'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>담당자 수정</DialogTitle></DialogHeader>
          {contactForm}
          <DialogFooter showCloseButton>
            <Button onClick={() => dialog.type === 'edit' && handleUpdate(dialog.contact)}
              disabled={isPending}>저장</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete */}
      <Dialog open={dialog.type === 'delete'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>담당자 삭제</DialogTitle></DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.contact.lastName}{dialog.contact.firstName}</strong> 담당자를 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.contact)}
              disabled={isPending}>삭제</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
