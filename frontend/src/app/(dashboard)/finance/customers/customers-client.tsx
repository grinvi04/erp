'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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
import { PaginationBar } from '@/components/ui/pagination-bar'
import { SearchInput } from '@/components/ui/search-input'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { createCustomer, updateCustomer, deactivateCustomer } from './actions'
import type { Account, Customer } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const NONE = 'NONE'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; customer: Customer }
  | { type: 'deactivate'; customer: Customer }

interface Props {
  data: PageResponse<Customer>
  accounts: Account[]
  keyword: string
}

export default function CustomersClient({ data, accounts, keyword }: Props) {
  const assetAccounts = accounts.filter(
    (a) => !a.isSummary && a.isActive && a.accountType === 'ASSET',
  )
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [businessNo, setBusinessNo] = useState('')
  const [contactName, setContactName] = useState('')
  const [contactEmail, setContactEmail] = useState('')
  const [contactPhone, setContactPhone] = useState('')
  const [paymentTerms, setPaymentTerms] = useState('30')
  const [receivablesAccountId, setReceivablesAccountId] = useState('')

  const openCreate = () => {
    setCode('')
    setName('')
    setBusinessNo('')
    setContactName('')
    setContactEmail('')
    setContactPhone('')
    setPaymentTerms('30')
    setReceivablesAccountId('')
    setDialog({ type: 'create' })
  }

  const openEdit = (customer: Customer) => {
    setName(customer.name)
    setBusinessNo(customer.businessNo ?? '')
    setContactName(customer.contactName ?? '')
    setContactEmail(customer.contactEmail ?? '')
    setContactPhone(customer.contactPhone ?? '')
    setPaymentTerms(String(customer.paymentTerms))
    setReceivablesAccountId(
      customer.receivablesAccountId != null ? String(customer.receivablesAccountId) : '',
    )
    setDialog({ type: 'edit', customer })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 업체명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createCustomer({
          code: code.trim(),
          name: name.trim(),
          businessNo: businessNo || null,
          contactName: contactName || null,
          contactEmail: contactEmail || null,
          contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          receivablesAccountId: receivablesAccountId ? Number(receivablesAccountId) : null,
        })
        toast.success('고객이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (customer: Customer) => {
    if (!name.trim()) {
      toast.error('업체명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateCustomer(customer.id, {
          name: name.trim(),
          businessNo: businessNo || null,
          contactName: contactName || null,
          contactEmail: contactEmail || null,
          contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          receivablesAccountId: receivablesAccountId ? Number(receivablesAccountId) : null,
          version: customer.version,
        })
        toast.success('고객 정보가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDeactivate = (customer: Customer) => {
    startTransition(async () => {
      try {
        await deactivateCustomer(customer.id)
        toast.success('고객이 비활성화되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const handleBulkDeactivate = (customers: Customer[], clear: () => void) => {
    const active = customers.filter((c) => c.isActive)
    if (active.length === 0) {
      toast.info('비활성화할 활성 고객이 없습니다')
      return
    }
    startTransition(async () => {
      try {
        await Promise.all(active.map((c) => deactivateCustomer(c.id)))
        toast.success(`${active.length}개 고객을 비활성화했습니다`)
        clear()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Customer>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (c) => c.code,
      cell: (c) => <span className="font-mono text-sm">{c.code}</span>,
    },
    {
      key: 'name',
      header: '업체명',
      sortable: true,
      sortValue: (c) => c.name,
      cell: (c) => <span className="font-medium">{c.name}</span>,
    },
    {
      key: 'businessNo',
      header: '사업자번호',
      cell: (c) => <span className="text-sm text-muted-foreground">{c.businessNo ?? '—'}</span>,
    },
    {
      key: 'email',
      header: '이메일',
      cell: (c) => <span className="text-sm text-muted-foreground">{c.contactEmail ?? '—'}</span>,
    },
    {
      key: 'phone',
      header: '전화',
      cell: (c) => <span className="text-sm text-muted-foreground">{c.contactPhone ?? '—'}</span>,
    },
    {
      key: 'paymentTerms',
      header: '결제기한(일)',
      align: 'right',
      sortable: true,
      sortValue: (c) => c.paymentTerms,
      cell: (c) => <span className="text-sm text-muted-foreground">{c.paymentTerms}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (c) => (c.isActive ? 0 : 1),
      cell: (c) => (
        <Badge variant={c.isActive ? 'default' : 'secondary'}>
          {c.isActive ? '활성' : '비활성'}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (c) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(c)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && c.isActive && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="비활성화"
              onClick={() => setDialog({ type: 'deactivate', customer: c })}
            >
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qStatus, setQStatus] = useState('')
  const [qAccount, setQAccount] = useState('')
  const [applied, setApplied] = useState({ status: '', account: '' })
  const onSearch = () => setApplied({ status: qStatus, account: qAccount })
  const onReset = () => {
    setQStatus('')
    setQAccount('')
    setApplied({ status: '', account: '' })
  }
  const filtered = data.content.filter((c) => {
    if (applied.status === 'ACTIVE' && !c.isActive) return false
    if (applied.status === 'INACTIVE' && c.isActive) return false
    if (applied.account && String(c.receivablesAccountId ?? '') !== applied.account) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `고객_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '업체명', '사업자번호', '담당자', '이메일', '전화', '결제기한(일)', '상태'],
      filtered.map((c) => [
        c.code,
        c.name,
        c.businessNo ?? '',
        c.contactName ?? '',
        c.contactEmail ?? '',
        c.contactPhone ?? '',
        c.paymentTerms,
        c.isActive ? '활성' : '비활성',
      ]),
    )

  const customerForm = (
    <div className="grid gap-4 py-2">
      <FormGrid>
        <FormRow label="코드" required>
          <Input
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="C001"
            disabled={dialog.type === 'edit'}
            className="h-8"
          />
        </FormRow>
        <FormRow label="업체명" required>
          <Input value={name} onChange={(e) => setName(e.target.value)} className="h-8" />
        </FormRow>
        <FormRow label="사업자번호">
          <Input
            value={businessNo}
            onChange={(e) => setBusinessNo(e.target.value)}
            placeholder="000-00-00000"
            className="h-8"
          />
        </FormRow>
        <FormRow label="담당자명">
          <Input
            value={contactName}
            onChange={(e) => setContactName(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="이메일">
          <Input
            type="email"
            value={contactEmail}
            onChange={(e) => setContactEmail(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="전화">
          <Input
            value={contactPhone}
            onChange={(e) => setContactPhone(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="결제기한 (일)">
          <Input
            type="number"
            min={0}
            value={paymentTerms}
            onChange={(e) => setPaymentTerms(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="외상매출금 계정 (차변)">
          <div className="grid w-full gap-1">
            <Select
              value={receivablesAccountId || NONE}
              onValueChange={(v) => setReceivablesAccountId(!v || v === NONE ? '' : v)}
            >
              <SelectTrigger className="h-8 w-full">
                <SelectValue placeholder="미설정" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {assetAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.code} {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              설정 시 AR 전표 승인이 이 계정으로 자동 분개됩니다.
            </p>
          </div>
        </FormRow>
      </FormGrid>
    </div>
  )

  return (
    <div className="p-5">
      <PageHeader title="고객" description="매출 거래처 정보를 관리합니다" className="mb-4">
        <SearchInput placeholder="이름·코드 검색" className="w-64" />
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 고객
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="상태">
            <Select value={qStatus || 'ALL'} onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}>
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="ACTIVE">활성</SelectItem>
                <SelectItem value="INACTIVE">비활성</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="외상매출금계정">
            <Select value={qAccount || 'ALL'} onValueChange={(v) => setQAccount(v === 'ALL' ? '' : (v ?? ''))}>
              <SelectTrigger className="h-8 w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {assetAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.code} {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(c) => c.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          selectable={canWrite}
          renderBulkActions={
            canWrite
              ? (selected, clear) => (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleBulkDeactivate(selected, clear)}
                    disabled={isPending}
                  >
                    선택 비활성화
                  </Button>
                )
              : undefined
          }
          empty={
            <EmptyState
              title="등록된 고객이 없습니다"
              description={canWrite ? '우측 상단에서 새 고객을 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/finance/customers"
          searchParams={keyword ? { keyword } : undefined}
        />
      </div>

      {/* Create Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 고객 등록</DialogTitle>
          </DialogHeader>
          {customerForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog
        open={dialog.type === 'edit'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              고객 수정{dialog.type === 'edit' && ` — ${dialog.customer.name}`}
            </DialogTitle>
          </DialogHeader>
          {customerForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.customer)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Deactivate Dialog */}
      <Dialog
        open={dialog.type === 'deactivate'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>고객 비활성화</DialogTitle>
          </DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.customer.name}</strong>을(를) 비활성화하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'deactivate' && handleDeactivate(dialog.customer)}
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
