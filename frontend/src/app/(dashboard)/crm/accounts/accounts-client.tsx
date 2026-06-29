'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { createAccount, updateAccount, deactivateAccount, type AccountPayload } from './actions'
import type { CrmAccount, AccountType } from '@/types/crm'
import type { PageResponse } from '@/types/api'
import { formatUserName } from '@/lib/utils'

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
  names: Record<string, string>
}

export default function AccountsClient({ data, keyword, names }: Props) {
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
      <FormGrid>
        {dialog.type === 'create' && (
          <FormRow label="코드" required>
            <Input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="ACC-001"
              className="h-8"
            />
          </FormRow>
        )}
        <FormRow label="고객사명" required>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="회사명"
            className="h-8"
          />
        </FormRow>
        <FormRow label="유형" required>
          <Select
            value={accountType}
            onValueChange={(v) => setAccountType((v ?? 'PROSPECT') as AccountType)}
          >
            <SelectTrigger className="h-8 w-full">
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
        </FormRow>
        <FormRow label="업종">
          <Input value={industry} onChange={(e) => setIndustry(e.target.value)} className="h-8" />
        </FormRow>
        <FormRow label="사업자번호">
          <Input
            value={businessNo}
            onChange={(e) => setBusinessNo(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="전화">
          <Input value={phone} onChange={(e) => setPhone(e.target.value)} className="h-8" />
        </FormRow>
        <FormRow label="웹사이트" span>
          <Input
            value={website}
            onChange={(e) => setWebsite(e.target.value)}
            placeholder="https://"
            className="h-8"
          />
        </FormRow>
        <FormRow label="주소" span>
          <Textarea
            rows={2}
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            className="w-full"
          />
        </FormRow>
        <FormRow label="직원 수">
          <Input
            type="number"
            min={0}
            value={employeeCount}
            onChange={(e) => setEmployeeCount(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="연 매출">
          <Input
            type="number"
            min={0}
            step={0.01}
            value={annualRevenue}
            onChange={(e) => setAnnualRevenue(e.target.value)}
            className="h-8"
          />
        </FormRow>
        {dialog.type === 'edit' && (
          <FormRow label="담당자 ID" required span>
            <Input value={ownerId} onChange={(e) => setOwnerId(e.target.value)} className="h-8" />
          </FormRow>
        )}
      </FormGrid>
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
      key: 'owner',
      header: '담당자',
      sortable: true,
      sortValue: (acc) => formatUserName(acc.ownerId, names),
      cell: (acc) => (
        <span className="text-sm" title={acc.ownerId}>
          {formatUserName(acc.ownerId, names)}
        </span>
      ),
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

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const ownerOptions = Array.from(new Set(data.content.map((acc) => acc.ownerId)))
  const [qType, setQType] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [qOwner, setQOwner] = useState('')
  const [applied, setApplied] = useState({ type: '', status: '', owner: '' })
  const onSearch = () => setApplied({ type: qType, status: qStatus, owner: qOwner })
  const onReset = () => {
    setQType('')
    setQStatus('')
    setQOwner('')
    setApplied({ type: '', status: '', owner: '' })
  }
  const filtered = data.content.filter((acc) => {
    if (applied.type && acc.accountType !== applied.type) return false
    if (applied.status && (applied.status === 'ACTIVE') !== acc.isActive) return false
    if (applied.owner && acc.ownerId !== applied.owner) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `고객사_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '고객사명', '유형', '업종', '전화', '담당자', '상태'],
      filtered.map((acc) => [
        acc.code,
        acc.name,
        TYPE_LABEL[acc.accountType],
        acc.industry ?? '',
        acc.phone ?? '',
        formatUserName(acc.ownerId, names),
        acc.isActive ? '활성' : '비활성',
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader
        title="고객사"
        description="고객사 및 잠재 고객 정보를 관리합니다"
        className="mb-4"
      >
        <SearchInput placeholder="이름·코드 검색" className="w-64" />
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 고객사
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="유형">
            <Select
              value={qType || 'ALL'}
              onValueChange={(v) => setQType(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {(Object.keys(TYPE_LABEL) as AccountType[]).map((t) => (
                  <SelectItem key={t} value={t}>
                    {TYPE_LABEL[t]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="담당자">
            <Select
              value={qOwner || 'ALL'}
              onValueChange={(v) => setQOwner(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {ownerOptions.map((id) => (
                  <SelectItem key={id} value={id}>
                    {formatUserName(id, names)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-28">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="ACTIVE">활성</SelectItem>
                <SelectItem value="INACTIVE">비활성</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(acc) => acc.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
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
        <DialogContent className="sm:max-w-2xl">
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
        <DialogContent className="sm:max-w-2xl">
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
