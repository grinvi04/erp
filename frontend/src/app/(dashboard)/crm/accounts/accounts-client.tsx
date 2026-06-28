'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon } from 'lucide-react'
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
import { SearchInput } from '@/components/ui/search-input'
import { createAccount, updateAccount, deactivateAccount, type AccountPayload } from './actions'
import type { CrmAccount, AccountType } from '@/types/crm'
import type { PageResponse } from '@/types/api'

const TYPE_LABEL: Record<AccountType, string> = {
  PROSPECT: '가망',
  CUSTOMER: '고객',
  PARTNER: '파트너',
  COMPETITOR: '경쟁사',
}

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; account: CrmAccount }
  | { type: 'deactivate'; account: CrmAccount }

interface Props {
  data: PageResponse<CrmAccount>
  keyword: string
}

export default function AccountsClient({ data, keyword }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.CRM_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [businessNo, setBusinessNo] = useState('')
  const [industry, setIndustry] = useState('')
  const [website, setWebsite] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [employeeCount, setEmployeeCount] = useState('')
  const [annualRevenue, setAnnualRevenue] = useState('')
  const [accountType, setAccountType] = useState<AccountType>('PROSPECT')
  const [ownerId, setOwnerId] = useState('')

  const openCreate = () => {
    setCode('')
    setName('')
    setBusinessNo('')
    setIndustry('')
    setWebsite('')
    setPhone('')
    setAddress('')
    setEmployeeCount('')
    setAnnualRevenue('')
    setAccountType('PROSPECT')
    setDialog({ type: 'create' })
  }

  const openEdit = (acc: CrmAccount) => {
    setName(acc.name)
    setBusinessNo(acc.businessNo ?? '')
    setIndustry(acc.industry ?? '')
    setWebsite(acc.website ?? '')
    setPhone(acc.phone ?? '')
    setAddress(acc.address ?? '')
    setEmployeeCount(acc.employeeCount != null ? String(acc.employeeCount) : '')
    setAnnualRevenue(acc.annualRevenue != null ? String(acc.annualRevenue) : '')
    setAccountType(acc.accountType)
    setOwnerId(acc.ownerId)
    setDialog({ type: 'edit', account: acc })
  }

  const buildPayload = (): Omit<AccountPayload, 'ownerId'> => ({
    name: name.trim(),
    businessNo: businessNo.trim() || null,
    industry: industry.trim() || null,
    website: website.trim() || null,
    phone: phone.trim() || null,
    address: address.trim() || null,
    employeeCount: employeeCount ? Number(employeeCount) : null,
    annualRevenue: annualRevenue ? Number(annualRevenue) : null,
    accountType,
  })

  const validate = (): boolean => {
    if (!name.trim()) {
      toast.error('고객사명은 필수입니다')
      return false
    }
    if (employeeCount && (Number(employeeCount) < 0 || isNaN(Number(employeeCount)))) {
      toast.error('직원 수가 올바르지 않습니다')
      return false
    }
    if (annualRevenue && (Number(annualRevenue) < 0 || isNaN(Number(annualRevenue)))) {
      toast.error('연 매출이 올바르지 않습니다')
      return false
    }
    return true
  }

  const handleCreate = () => {
    if (!code.trim()) {
      toast.error('코드는 필수입니다')
      return
    }
    if (!validate()) return
    startTransition(async () => {
      try {
        await createAccount({ code: code.trim(), ...buildPayload() })
        toast.success('고객사가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (acc: CrmAccount) => {
    if (!validate()) return
    if (!ownerId.trim()) {
      toast.error('담당자는 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateAccount(acc.id, {
          ...buildPayload(),
          ownerId: ownerId.trim(),
          version: acc.version,
        })
        toast.success('고객사가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDeactivate = (acc: CrmAccount) => {
    startTransition(async () => {
      try {
        await deactivateAccount(acc.id)
        toast.success('고객사가 비활성화되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const accountForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        {dialog.type === 'create' && (
          <div className="grid gap-1.5">
            <Label>코드 *</Label>
            <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="ACC-001" />
          </div>
        )}
        <div className="grid gap-1.5">
          <Label>고객사명 *</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="회사명" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>유형 *</Label>
          <Select
            value={accountType}
            onValueChange={(v) => setAccountType((v ?? 'PROSPECT') as AccountType)}
          >
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(TYPE_LABEL) as AccountType[]).map((t) => (
                <SelectItem key={t} value={t}>
                  {TYPE_LABEL[t]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-1.5">
          <Label>업종</Label>
          <Input value={industry} onChange={(e) => setIndustry(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>사업자번호</Label>
          <Input value={businessNo} onChange={(e) => setBusinessNo(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>전화</Label>
          <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label>웹사이트</Label>
        <Input
          value={website}
          onChange={(e) => setWebsite(e.target.value)}
          placeholder="https://"
        />
      </div>
      <div className="grid gap-1.5">
        <Label>주소</Label>
        <Textarea rows={2} value={address} onChange={(e) => setAddress(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>직원 수</Label>
          <Input
            type="number"
            min={0}
            value={employeeCount}
            onChange={(e) => setEmployeeCount(e.target.value)}
          />
        </div>
        <div className="grid gap-1.5">
          <Label>연 매출</Label>
          <Input
            type="number"
            min={0}
            step={0.01}
            value={annualRevenue}
            onChange={(e) => setAnnualRevenue(e.target.value)}
          />
        </div>
      </div>
      {dialog.type === 'edit' && (
        <div className="grid gap-1.5">
          <Label>담당자 ID *</Label>
          <Input value={ownerId} onChange={(e) => setOwnerId(e.target.value)} />
        </div>
      )}
    </div>
  )

  const columns: Column<CrmAccount>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (acc) => acc.code,
      cell: (acc) => <span className="font-mono text-sm">{acc.code}</span>,
    },
    {
      key: 'name',
      header: '고객사명',
      sortable: true,
      sortValue: (acc) => acc.name,
      cell: (acc) => <span className="font-medium">{acc.name}</span>,
    },
    {
      key: 'accountType',
      header: '유형',
      sortable: true,
      sortValue: (acc) => TYPE_LABEL[acc.accountType],
      cell: (acc) => <Badge variant="secondary">{TYPE_LABEL[acc.accountType]}</Badge>,
    },
    {
      key: 'industry',
      header: '업종',
      cell: (acc) => <span className="text-sm text-muted-foreground">{acc.industry ?? '—'}</span>,
    },
    {
      key: 'phone',
      header: '전화',
      cell: (acc) => <span className="text-sm text-muted-foreground">{acc.phone ?? '—'}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (acc) => (acc.isActive ? 0 : 1),
      cell: (acc) => (
        <Badge variant={acc.isActive ? 'default' : 'secondary'}>
          {acc.isActive ? '활성' : '비활성'}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (acc) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(acc)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && acc.isActive && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="비활성화"
              onClick={() => setDialog({ type: 'deactivate', account: acc })}
            >
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="고객사"
        description="고객사 및 잠재 고객 정보를 관리합니다"
        className="mb-6"
      >
        <SearchInput placeholder="이름·코드 검색" className="w-64" />
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 고객사
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <DataTable
          data={data.content}
          columns={columns}
          getRowId={(acc) => acc.id}
          empty={
            <EmptyState
              title="등록된 고객사가 없습니다"
              description={canWrite ? '우측 상단에서 새 고객사를 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/crm/accounts"
          searchParams={keyword ? { keyword } : undefined}
        />
      </div>

      {/* Create */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>새 고객사 등록</DialogTitle>
          </DialogHeader>
          {accountForm}
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
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              고객사 수정{dialog.type === 'edit' && ` — ${dialog.account.code}`}
            </DialogTitle>
          </DialogHeader>
          {accountForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.account)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Deactivate */}
      <Dialog
        open={dialog.type === 'deactivate'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>고객사 비활성화</DialogTitle>
          </DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.account.code} {dialog.account.name}
              </strong>
              을(를) 비활성화하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'deactivate' && handleDeactivate(dialog.account)}
              disabled={isPending}
            >
              비활성화
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
