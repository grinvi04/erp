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
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import { FormField } from '@/components/ui/form-field'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { SearchInput } from '@/components/ui/search-input'
import { createVendor, updateVendor, deactivateVendor } from './actions'
import type { Account, Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const NONE = 'NONE'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; vendor: Vendor }
  | { type: 'deactivate'; vendor: Vendor }

interface Props { data: PageResponse<Vendor>; accounts: Account[]; keyword: string }

export default function VendorsClient({ data, accounts, keyword }: Props) {
  const liabilityAccounts = accounts.filter((a) => !a.isSummary && a.isActive && a.accountType === 'LIABILITY')
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
    setCode(''); setName(''); setBusinessNo(''); setContactName('')
    setContactEmail(''); setContactPhone(''); setPaymentTerms('30'); setPayablesAccountId('')
    setErrors({})
    setDialog({ type: 'create' })
  }

  const openEdit = (vendor: Vendor) => {
    setName(vendor.name); setBusinessNo(vendor.businessNo ?? '')
    setContactName(vendor.contactName ?? ''); setContactEmail(vendor.contactEmail ?? '')
    setContactPhone(vendor.contactPhone ?? ''); setPaymentTerms(String(vendor.paymentTerms))
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
          code: code.trim(), name: name.trim(),
          businessNo: businessNo || null, contactName: contactName || null,
          contactEmail: contactEmail || null, contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          payablesAccountId: payablesAccountId ? Number(payablesAccountId) : null,
        })
        toast.success('공급업체가 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (vendor: Vendor) => {
    if (!name.trim()) { setErrors({ name: '업체명을 입력하세요' }); return }
    setErrors({})
    startTransition(async () => {
      try {
        await updateVendor(vendor.id, {
          name: name.trim(),
          businessNo: businessNo || null, contactName: contactName || null,
          contactEmail: contactEmail || null, contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          payablesAccountId: payablesAccountId ? Number(payablesAccountId) : null,
          version: vendor.version,
        })
        toast.success('공급업체 정보가 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDeactivate = (vendor: Vendor) => {
    startTransition(async () => {
      try {
        await deactivateVendor(vendor.id)
        toast.success('공급업체가 비활성화되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다') }
    })
  }

  const handleBulkDeactivate = (vendors: Vendor[], clear: () => void) => {
    const active = vendors.filter((v) => v.isActive)
    if (active.length === 0) { toast.info('비활성화할 활성 업체가 없습니다'); return }
    startTransition(async () => {
      try {
        await Promise.all(active.map((v) => deactivateVendor(v.id)))
        toast.success(`${active.length}개 공급업체를 비활성화했습니다`)
        clear()
      } catch (e) { toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다') }
    })
  }

  const columns: Column<Vendor>[] = [
    { key: 'code', header: '코드', sortable: true, sortValue: (v) => v.code, cell: (v) => <span className="font-mono text-sm">{v.code}</span> },
    { key: 'name', header: '업체명', sortable: true, sortValue: (v) => v.name, cell: (v) => <span className="font-medium">{v.name}</span> },
    { key: 'businessNo', header: '사업자번호', cell: (v) => <span className="text-sm text-muted-foreground">{v.businessNo ?? '—'}</span> },
    { key: 'email', header: '이메일', cell: (v) => <span className="text-sm text-muted-foreground">{v.contactEmail ?? '—'}</span> },
    { key: 'phone', header: '전화', cell: (v) => <span className="text-sm text-muted-foreground">{v.contactPhone ?? '—'}</span> },
    { key: 'paymentTerms', header: '결제기한(일)', align: 'right', sortable: true, sortValue: (v) => v.paymentTerms, cell: (v) => <span className="text-sm text-muted-foreground">{v.paymentTerms}</span> },
    { key: 'status', header: '상태', sortable: true, sortValue: (v) => (v.isActive ? 0 : 1), cell: (v) => <Badge variant={v.isActive ? 'default' : 'secondary'}>{v.isActive ? '활성' : '비활성'}</Badge> },
    {
      key: 'actions', header: '', align: 'right', headerClassName: 'w-20',
      cell: (v) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(v)}><PencilIcon /></Button>
          )}
          {canWrite && v.isActive && (
            <Button variant="ghost" size="icon-xs" title="비활성화" onClick={() => setDialog({ type: 'deactivate', vendor: v })}>
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  const vendorForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        <FormField label="코드" required error={errors.code}>
          <Input
            value={code} placeholder="V001" disabled={dialog.type === 'edit'} aria-invalid={!!errors.code}
            onChange={(e) => { setCode(e.target.value); if (errors.code) setErrors((p) => ({ ...p, code: undefined })) }}
          />
        </FormField>
        <FormField label="업체명" required error={errors.name}>
          <Input
            value={name} aria-invalid={!!errors.name}
            onChange={(e) => { setName(e.target.value); if (errors.name) setErrors((p) => ({ ...p, name: undefined })) }}
          />
        </FormField>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>사업자번호</Label>
          <Input value={businessNo} onChange={(e) => setBusinessNo(e.target.value)} placeholder="000-00-00000" />
        </div>
        <div className="grid gap-1.5">
          <Label>담당자명</Label>
          <Input value={contactName} onChange={(e) => setContactName(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>이메일</Label>
          <Input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>전화</Label>
          <Input value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>결제기한 (일)</Label>
          <Input type="number" min={0} value={paymentTerms} onChange={(e) => setPaymentTerms(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>외상매입금 계정 (대변)</Label>
          <Select value={payablesAccountId || NONE}
            onValueChange={(v) => setPayablesAccountId(!v || v === NONE ? '' : v)}>
            <SelectTrigger className="w-full"><SelectValue placeholder="미설정" /></SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE}>미설정</SelectItem>
              {liabilityAccounts.map((a) => (
                <SelectItem key={a.id} value={String(a.id)}>{a.code} {a.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">설정 시 AP 전표 승인이 이 계정으로 자동 분개됩니다.</p>
        </div>
      </div>
    </div>
  )

  return (
    <div className="p-6">
      <PageHeader title="공급업체" description="매입 거래처 정보를 관리합니다" className="mb-6">
        <SearchInput placeholder="이름·코드 검색" className="w-64" />
        {canWrite && <Button onClick={openCreate}><PlusIcon />새 공급업체</Button>}
      </PageHeader>

      <div className="space-y-3">
        <DataTable
          data={data.content}
          columns={columns}
          getRowId={(v) => v.id}
          selectable={canWrite}
          renderBulkActions={canWrite ? (selected, clear) => (
            <Button size="sm" variant="outline" onClick={() => handleBulkDeactivate(selected, clear)} disabled={isPending}>
              선택 비활성화
            </Button>
          ) : undefined}
          empty={<EmptyState title="등록된 공급업체가 없습니다" description={canWrite ? '우측 상단에서 새 공급업체를 등록하세요.' : undefined} />}
        />
        <PaginationBar
          page={data.page} totalPages={data.totalPages}
          totalElements={data.totalElements} size={data.size}
          basePath="/finance/vendors"
          searchParams={keyword ? { keyword } : undefined}
        />
      </div>

      {/* Create Dialog */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>새 공급업체 등록</DialogTitle></DialogHeader>
          {vendorForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={dialog.type === 'edit'} onOpenChange={(o) => { if (!o) close() }}>
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
      <Dialog open={dialog.type === 'deactivate'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>공급업체 비활성화</DialogTitle></DialogHeader>
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
