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
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { createVendor, updateVendor, deactivateVendor } from './actions'
import type { Account, Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const NONE = 'NONE'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; vendor: Vendor }
  | { type: 'deactivate'; vendor: Vendor }

interface Props { data: PageResponse<Vendor>; accounts: Account[] }

export default function VendorsClient({ data, accounts }: Props) {
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

  const openCreate = () => {
    setCode(''); setName(''); setBusinessNo(''); setContactName('')
    setContactEmail(''); setContactPhone(''); setPaymentTerms('30'); setPayablesAccountId('')
    setDialog({ type: 'create' })
  }

  const openEdit = (vendor: Vendor) => {
    setName(vendor.name); setBusinessNo(vendor.businessNo ?? '')
    setContactName(vendor.contactName ?? ''); setContactEmail(vendor.contactEmail ?? '')
    setContactPhone(vendor.contactPhone ?? ''); setPaymentTerms(String(vendor.paymentTerms))
    setPayablesAccountId(vendor.payablesAccountId != null ? String(vendor.payablesAccountId) : '')
    setDialog({ type: 'edit', vendor })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) { toast.error('코드와 업체명은 필수입니다'); return }
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
    if (!name.trim()) { toast.error('업체명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await updateVendor(vendor.id, {
          name: name.trim(),
          businessNo: businessNo || null, contactName: contactName || null,
          contactEmail: contactEmail || null, contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          payablesAccountId: payablesAccountId ? Number(payablesAccountId) : null,
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

  const vendorForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>코드 *</Label>
          <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="V001" disabled={dialog.type === 'edit'} />
        </div>
        <div className="grid gap-1.5">
          <Label>업체명 *</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} />
        </div>
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
          <p className="text-xs text-gray-400">설정 시 AP 전표 승인이 이 계정으로 자동 분개됩니다.</p>
        </div>
      </div>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">공급업체</h1>
          <p className="text-sm text-gray-500 mt-1">매입 거래처 정보를 관리합니다</p>
        </div>
        {canWrite && <Button onClick={openCreate}><PlusIcon />새 공급업체</Button>}
      </div>

      <div className="bg-white rounded-lg border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>업체명</TableHead>
              <TableHead>사업자번호</TableHead>
              <TableHead>이메일</TableHead>
              <TableHead>전화</TableHead>
              <TableHead className="text-right">결제기한(일)</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-20" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-gray-400 py-10">
                  등록된 공급업체가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((v) => (
              <TableRow key={v.id}>
                <TableCell className="font-mono text-sm">{v.code}</TableCell>
                <TableCell className="font-medium">{v.name}</TableCell>
                <TableCell className="text-sm text-gray-600">{v.businessNo ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{v.contactEmail ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{v.contactPhone ?? '—'}</TableCell>
                <TableCell className="text-right text-sm text-gray-600">{v.paymentTerms}</TableCell>
                <TableCell>
                  <Badge variant={v.isActive ? 'default' : 'secondary'}>
                    {v.isActive ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    {canWrite && (
                      <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(v)}>
                        <PencilIcon />
                      </Button>
                    )}
                    {canWrite && v.isActive && (
                      <Button
                        variant="ghost" size="icon-xs" title="비활성화"
                        onClick={() => setDialog({ type: 'deactivate', vendor: v })}
                      >
                        <BanIcon className="text-destructive" />
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
          basePath="/finance/vendors"
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
            <p className="text-sm text-gray-600 py-2">
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
