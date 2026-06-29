'use client'
import { useState, useTransition, useRef } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, Trash2Icon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import {
  getContactsByAccount,
  createContact,
  updateContact,
  deleteContact,
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
  const { can } = usePermissions()
  const canWrite = can(PERM.CRM_WRITE)
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

  // 마지막으로 요청한 accountId만 반영해 응답 순서가 뒤바뀌어도 다른 고객사의
  // 담당자가 표시되지 않게 한다.
  const loadReqRef = useRef(0)

  const loadContacts = (aId: string) => {
    if (!aId) {
      setContacts([])
      return
    }
    const reqId = ++loadReqRef.current
    setIsLoadingContacts(true)
    getContactsByAccount(Number(aId))
      .then((cs) => {
        if (loadReqRef.current === reqId) setContacts(cs)
      })
      .catch(() => {
        if (loadReqRef.current === reqId) toast.error('담당자 목록을 불러오지 못했습니다')
      })
      .finally(() => {
        if (loadReqRef.current === reqId) setIsLoadingContacts(false)
      })
  }

  const reload = () => loadContacts(selectedAccountId)

  const onAccountChange = (val: string | null) => {
    const aId = val ?? ''
    setSelectedAccountId(aId)
    setContacts([])
    loadContacts(aId)
  }

  const openCreate = () => {
    setLastName('')
    setFirstName('')
    setTitle('')
    setDepartment('')
    setEmail('')
    setPhone('')
    setMobile('')
    setIsPrimary(false)
    setDialog({ type: 'create' })
  }

  const openEdit = (ct: Contact) => {
    setLastName(ct.lastName)
    setFirstName(ct.firstName)
    setTitle(ct.title ?? '')
    setDepartment(ct.department ?? '')
    setEmail(ct.email ?? '')
    setPhone(ct.phone ?? '')
    setMobile(ct.mobile ?? '')
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
        await createContact({ accountId: Number(selectedAccountId), ...buildPayload() })
        toast.success('담당자가 등록되었습니다')
        close()
        reload()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (ct: Contact) => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await updateContact(ct.id, { ...buildPayload(), version: ct.version })
        toast.success('담당자가 수정되었습니다')
        close()
        reload()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (ct: Contact) => {
    startTransition(async () => {
      try {
        await deleteContact(ct.id)
        toast.success('담당자가 삭제되었습니다')
        close()
        reload()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const contactForm = (
    <div className="grid gap-4 py-2">
      <FormGrid>
        <FormRow label="성" required>
          <Input value={lastName} onChange={(e) => setLastName(e.target.value)} className="h-8" />
        </FormRow>
        <FormRow label="이름" required>
          <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} className="h-8" />
        </FormRow>
        <FormRow label="직함">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} className="h-8" />
        </FormRow>
        <FormRow label="부서">
          <Input
            value={department}
            onChange={(e) => setDepartment(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="이메일" span>
          <Input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="전화">
          <Input value={phone} onChange={(e) => setPhone(e.target.value)} className="h-8" />
        </FormRow>
        <FormRow label="휴대폰">
          <Input value={mobile} onChange={(e) => setMobile(e.target.value)} className="h-8" />
        </FormRow>
      </FormGrid>
      <label className="flex items-center gap-2 cursor-pointer">
        <input
          type="checkbox"
          checked={isPrimary}
          onChange={(e) => setIsPrimary(e.target.checked)}
          className="h-4 w-4 rounded border-input"
        />
        <span className="text-sm">주 담당자</span>
      </label>
    </div>
  )

  const columns: Column<Contact>[] = [
    {
      key: 'name',
      header: '이름',
      sortable: true,
      sortValue: (ct) => `${ct.lastName}${ct.firstName}`,
      cell: (ct) => (
        <span className="font-medium">
          {ct.lastName}
          {ct.firstName}
        </span>
      ),
    },
    {
      key: 'title',
      header: '직함',
      cell: (ct) => <span className="text-sm text-muted-foreground">{ct.title ?? '—'}</span>,
    },
    {
      key: 'department',
      header: '부서',
      cell: (ct) => <span className="text-sm text-muted-foreground">{ct.department ?? '—'}</span>,
    },
    {
      key: 'email',
      header: '이메일',
      cell: (ct) => <span className="text-sm text-muted-foreground">{ct.email ?? '—'}</span>,
    },
    {
      key: 'phone',
      header: '전화',
      cell: (ct) => <span className="text-sm text-muted-foreground">{ct.phone ?? '—'}</span>,
    },
    {
      key: 'mobile',
      header: '휴대폰',
      cell: (ct) => <span className="text-sm text-muted-foreground">{ct.mobile ?? '—'}</span>,
    },
    {
      key: 'isPrimary',
      header: '주 담당자',
      cell: (ct) => ct.isPrimary && <Badge>주 담당자</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (ct) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <>
              <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(ct)}>
                <PencilIcon />
              </Button>
              <Button
                variant="ghost"
                size="icon-xs"
                title="삭제"
                onClick={() => setDialog({ type: 'delete', contact: ct })}
              >
                <Trash2Icon className="text-destructive" />
              </Button>
            </>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 로드된 담당자 목록 기준 클라이언트 필터.
  const [qKeyword, setQKeyword] = useState('')
  const [qDept, setQDept] = useState('')
  const [qPrimary, setQPrimary] = useState('')
  const [applied, setApplied] = useState({ keyword: '', dept: '', primary: '' })
  const onSearch = () => setApplied({ keyword: qKeyword, dept: qDept, primary: qPrimary })
  const onReset = () => {
    setQKeyword('')
    setQDept('')
    setQPrimary('')
    setApplied({ keyword: '', dept: '', primary: '' })
  }
  const departments = Array.from(
    new Set(contacts.map((ct) => ct.department).filter((d): d is string => !!d)),
  )
  const filtered = contacts.filter((ct) => {
    if (applied.dept && (ct.department ?? '') !== applied.dept) return false
    if (applied.primary === 'Y' && !ct.isPrimary) return false
    if (applied.primary === 'N' && ct.isPrimary) return false
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      const hay =
        `${ct.lastName}${ct.firstName} ${ct.title ?? ''} ${ct.email ?? ''} ${ct.phone ?? ''} ${ct.mobile ?? ''}`.toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `담당자_${new Date().toISOString().slice(0, 10)}`,
      ['이름', '직함', '부서', '이메일', '전화', '휴대폰', '주담당자'],
      filtered.map((ct) => [
        `${ct.lastName}${ct.firstName}`,
        ct.title ?? '',
        ct.department ?? '',
        ct.email ?? '',
        ct.phone ?? '',
        ct.mobile ?? '',
        ct.isPrimary ? 'Y' : 'N',
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader title="담당자" description="고객사 담당자 정보를 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel} disabled={!selectedAccountId}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate} disabled={!selectedAccountId}>
            <PlusIcon />새 담당자
          </Button>
        )}
      </PageHeader>

      <div className="mb-4 max-w-md">
        <Label className="mb-1.5 block">고객사</Label>
        <Select
          value={selectedAccountId}
          onValueChange={onAccountChange}
          disabled={dialog.type !== 'none'}
        >
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

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="검색어">
            <Input
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
              placeholder="이름·이메일·연락처"
              className="h-8 w-44"
            />
          </FilterField>
          <FilterField label="부서">
            <Select
              value={qDept || 'ALL'}
              onValueChange={(v) => setQDept(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {departments.map((d) => (
                  <SelectItem key={d} value={d}>
                    {d}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="주담당자">
            <Select
              value={qPrimary || 'ALL'}
              onValueChange={(v) => setQPrimary(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-28">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="Y">주담당자</SelectItem>
                <SelectItem value="N">일반</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={selectedAccountId ? filtered : []}
          columns={columns}
          getRowId={(ct) => ct.id}
          loading={!!selectedAccountId && isLoadingContacts}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title={
                selectedAccountId
                  ? '등록된 담당자가 없습니다'
                  : '고객사를 선택하면 담당자 목록이 표시됩니다'
              }
            />
          }
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
            <DialogTitle>새 담당자 등록</DialogTitle>
          </DialogHeader>
          {contactForm}
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
            <DialogTitle>담당자 수정</DialogTitle>
          </DialogHeader>
          {contactForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.contact)}
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
            <DialogTitle>담당자 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.contact.lastName}
                {dialog.contact.firstName}
              </strong>{' '}
              담당자를 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.contact)}
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
