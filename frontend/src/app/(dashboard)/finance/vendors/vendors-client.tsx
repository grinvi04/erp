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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { createVendor, updateVendor, deactivateVendor } from './actions'
import type { Account, Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const NONE = 'NONE'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; vendor: Vendor }
  | { type: 'deactivate'; vendor: Vendor }

interface Props {
  data: PageResponse<Vendor>
  accounts: Account[]
  keyword: string
}

export default function VendorsClient({ data, accounts, keyword }: Props) {
  const liabilityAccounts = accounts.filter(
    (a) => !a.isSummary && a.isActive && a.accountType === 'LIABILITY',
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
  const [payablesAccountId, setPayablesAccountId] = useState('')
  const [errors, setErrors] = useState<{ code?: string; name?: string }>({})

  const openCreate = () => {
    setCode('')
    setName('')
    setBusinessNo('')
    setContactName('')
    setContactEmail('')
    setContactPhone('')
    setPaymentTerms('30')
    setPayablesAccountId('')
    setErrors({})
    setDialog({ type: 'create' })
  }

  const openEdit = (vendor: Vendor) => {
    setCode(vendor.code)
    setName(vendor.name)
    setBusinessNo(vendor.businessNo ?? '')
    setContactName(vendor.contactName ?? '')
    setContactEmail(vendor.contactEmail ?? '')
    setContactPhone(vendor.contactPhone ?? '')
    setPaymentTerms(String(vendor.paymentTerms))
    setPayablesAccountId(vendor.payablesAccountId != null ? String(vendor.payablesAccountId) : '')
    setErrors({})
    setDialog({ type: 'edit', vendor })
  }

  const handleCreate = () => {
    const errs: { code?: string; name?: string } = {}
    if (!code.trim()) errs.code = '코드를 입력하세요'
    if (!name.trim()) errs.name = '업체명을 입력하세요'
    setErrors(errs)
    if (errs.code || errs.name) return
    startTransition(async () => {
      try {
        await createVendor({
          code: code.trim(),
          name: name.trim(),
          businessNo: businessNo || null,
          contactName: contactName || null,
          contactEmail: contactEmail || null,
          contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          payablesAccountId: payablesAccountId ? Number(payablesAccountId) : null,
        })
        toast.success('공급업체가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (vendor: Vendor) => {
    if (!name.trim()) {
      setErrors({ name: '업체명을 입력하세요' })
      return
    }
    setErrors({})
    startTransition(async () => {
      try {
        await updateVendor(vendor.id, {
          name: name.trim(),
          businessNo: businessNo || null,
          contactName: contactName || null,
          contactEmail: contactEmail || null,
          contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          payablesAccountId: payablesAccountId ? Number(payablesAccountId) : null,
          version: vendor.version,
        })
        toast.success('공급업체 정보가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDeactivate = (vendor: Vendor) => {
    startTransition(async () => {
      try {
        await deactivateVendor(vendor.id)
        toast.success('공급업체가 비활성화되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const handleBulkDeactivate = (vendors: Vendor[], clear: () => void) => {
    const active = vendors.filter((v) => v.isActive)
    if (active.length === 0) {
      toast.info('비활성화할 활성 업체가 없습니다')
      return
    }
    startTransition(async () => {
      try {
        await Promise.all(active.map((v) => deactivateVendor(v.id)))
        toast.success(`${active.length}개 공급업체를 비활성화했습니다`)
        clear()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Vendor>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (v) => v.code,
      cell: (v) => <span className="font-mono text-sm">{v.code}</span>,
    },
    {
      key: 'name',
      header: '업체명',
      sortable: true,
      sortValue: (v) => v.name,
      cell: (v) => <span className="font-medium">{v.name}</span>,
    },
    {
      key: 'businessNo',
      header: '사업자번호',
      cell: (v) => <span className="text-sm text-muted-foreground">{v.businessNo ?? '—'}</span>,
    },
    {
      key: 'email',
      header: '이메일',
      cell: (v) => <span className="text-sm text-muted-foreground">{v.contactEmail ?? '—'}</span>,
    },
    {
      key: 'phone',
      header: '전화',
      cell: (v) => <span className="text-sm text-muted-foreground">{v.contactPhone ?? '—'}</span>,
    },
    {
      key: 'paymentTerms',
      header: '결제기한(일)',
      align: 'right',
      sortable: true,
      sortValue: (v) => v.paymentTerms,
      cell: (v) => <span className="text-sm text-muted-foreground">{v.paymentTerms}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (v) => (v.isActive ? 0 : 1),
      cell: (v) => (
        <Badge variant={v.isActive ? 'default' : 'secondary'}>
          {v.isActive ? '활성' : '비활성'}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (v) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(v)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && v.isActive && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="비활성화"
              onClick={() => setDialog({ type: 'deactivate', vendor: v })}
            >
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qKeyword, setQKeyword] = useState(keyword)
  const [qStatus, setQStatus] = useState('')
  const [applied, setApplied] = useState({ keyword, status: '' })
  const onSearch = () => setApplied({ keyword: qKeyword, status: qStatus })
  const onReset = () => {
    setQKeyword('')
    setQStatus('')
    setApplied({ keyword: '', status: '' })
  }
  const filtered = data.content.filter((v) => {
    if (applied.status === 'ACTIVE' && !v.isActive) return false
    if (applied.status === 'INACTIVE' && v.isActive) return false
    if (applied.keyword) {
      const k = applied.keyword.toLowerCase()
      const hay = `${v.code} ${v.name} ${v.businessNo ?? ''}`.toLowerCase()
      if (!hay.includes(k)) return false
    }
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `공급업체_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '업체명', '사업자번호', '담당자', '이메일', '전화', '결제기한(일)', '상태'],
      filtered.map((v) => [
        v.code,
        v.name,
        v.businessNo ?? '',
        v.contactName ?? '',
        v.contactEmail ?? '',
        v.contactPhone ?? '',
        v.paymentTerms,
        v.isActive ? '활성' : '비활성',
      ]),
    )

  const vendorForm = (
    <div className="grid gap-4 py-2">
      <FormGrid>
        <FormRow label="코드" required>
          <div className="w-full">
            <Input
              value={code}
              placeholder="V001"
              disabled={dialog.type === 'edit'}
              aria-invalid={!!errors.code}
              className="h-8"
              onChange={(e) => {
                setCode(e.target.value)
                if (errors.code) setErrors((p) => ({ ...p, code: undefined }))
              }}
            />
            {errors.code && (
              <p className="mt-1 text-xs font-medium text-destructive">{errors.code}</p>
            )}
          </div>
        </FormRow>
        <FormRow label="업체명" required>
          <div className="w-full">
            <Input
              value={name}
              aria-invalid={!!errors.name}
              className="h-8"
              onChange={(e) => {
                setName(e.target.value)
                if (errors.name) setErrors((p) => ({ ...p, name: undefined }))
              }}
            />
            {errors.name && (
              <p className="mt-1 text-xs font-medium text-destructive">{errors.name}</p>
            )}
          </div>
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
        <FormRow label="결제기한(일)">
          <Input
            type="number"
            min={0}
            value={paymentTerms}
            onChange={(e) => setPaymentTerms(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="외상매입금 계정">
          <div className="w-full">
            <Select
              value={payablesAccountId || NONE}
              onValueChange={(v) => setPayablesAccountId(!v || v === NONE ? '' : v)}
            >
              <SelectTrigger className="h-8 w-full">
                <SelectValue placeholder="미설정" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>미설정</SelectItem>
                {liabilityAccounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.code} {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="mt-1 text-xs text-muted-foreground">
              설정 시 AP 전표 승인이 이 계정으로 자동 분개됩니다.
            </p>
          </div>
        </FormRow>
      </FormGrid>
    </div>
  )

  return (
    <div className="p-5">
      <PageHeader title="공급업체" description="매입 거래처 정보를 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 공급업체
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="검색어">
            <Input
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
              placeholder="이름·코드·사업자번호"
              className="h-8 w-52"
            />
          </FilterField>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
            >
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
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(v) => v.id}
          selectable={canWrite}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
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
              title="등록된 공급업체가 없습니다"
              description={canWrite ? '우측 상단에서 새 공급업체를 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/finance/vendors"
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
            <DialogTitle>새 공급업체 등록</DialogTitle>
          </DialogHeader>
          {vendorForm}
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
              공급업체 수정{dialog.type === 'edit' && ` — ${dialog.vendor.name}`}
            </DialogTitle>
          </DialogHeader>
          {vendorForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.vendor)}
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
            <DialogTitle>공급업체 비활성화</DialogTitle>
          </DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.vendor.name}</strong>을(를) 비활성화하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'deactivate' && handleDeactivate(dialog.vendor)}
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
