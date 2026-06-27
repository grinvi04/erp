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
import { SearchInput } from '@/components/ui/search-input'
import { createCustomer, updateCustomer, deactivateCustomer } from './actions'
import type { Account, Customer } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const NONE = 'NONE'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; customer: Customer }
  | { type: 'deactivate'; customer: Customer }

interface Props { data: PageResponse<Customer>; accounts: Account[]; keyword: string }

export default function CustomersClient({ data, accounts, keyword }: Props) {
  const assetAccounts = accounts.filter((a) => !a.isSummary && a.isActive && a.accountType === 'ASSET')
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
    setCode(''); setName(''); setBusinessNo(''); setContactName('')
    setContactEmail(''); setContactPhone(''); setPaymentTerms('30'); setReceivablesAccountId('')
    setDialog({ type: 'create' })
  }

  const openEdit = (customer: Customer) => {
    setName(customer.name); setBusinessNo(customer.businessNo ?? '')
    setContactName(customer.contactName ?? ''); setContactEmail(customer.contactEmail ?? '')
    setContactPhone(customer.contactPhone ?? ''); setPaymentTerms(String(customer.paymentTerms))
    setReceivablesAccountId(customer.receivablesAccountId != null ? String(customer.receivablesAccountId) : '')
    setDialog({ type: 'edit', customer })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) { toast.error('코드와 업체명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await createCustomer({
          code: code.trim(), name: name.trim(),
          businessNo: businessNo || null, contactName: contactName || null,
          contactEmail: contactEmail || null, contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          receivablesAccountId: receivablesAccountId ? Number(receivablesAccountId) : null,
        })
        toast.success('고객이 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (customer: Customer) => {
    if (!name.trim()) { toast.error('업체명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await updateCustomer(customer.id, {
          name: name.trim(),
          businessNo: businessNo || null, contactName: contactName || null,
          contactEmail: contactEmail || null, contactPhone: contactPhone || null,
          paymentTerms: Number(paymentTerms) || 0,
          receivablesAccountId: receivablesAccountId ? Number(receivablesAccountId) : null,
          version: customer.version,
        })
        toast.success('고객 정보가 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDeactivate = (customer: Customer) => {
    startTransition(async () => {
      try {
        await deactivateCustomer(customer.id)
        toast.success('고객이 비활성화되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다') }
    })
  }

  const customerForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>코드 *</Label>
          <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="C001" disabled={dialog.type === 'edit'} />
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
          <Label>외상매출금 계정 (차변)</Label>
          <Select value={receivablesAccountId || NONE}
            onValueChange={(v) => setReceivablesAccountId(!v || v === NONE ? '' : v)}>
            <SelectTrigger className="w-full"><SelectValue placeholder="미설정" /></SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE}>미설정</SelectItem>
              {assetAccounts.map((a) => (
                <SelectItem key={a.id} value={String(a.id)}>{a.code} {a.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">설정 시 AR 전표 승인이 이 계정으로 자동 분개됩니다.</p>
        </div>
      </div>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">고객</h1>
          <p className="text-sm text-muted-foreground mt-1">매출 거래처 정보를 관리합니다</p>
        </div>
        <div className="flex items-center gap-2">
          <SearchInput placeholder="이름·코드 검색" className="w-64" />
          {canWrite && <Button onClick={openCreate}><PlusIcon />새 고객</Button>}
        </div>
      </div>

      <div className="bg-card rounded-lg border overflow-hidden">
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
                <TableCell colSpan={8} className="text-center text-muted-foreground py-10">
                  등록된 고객이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((c) => (
              <TableRow key={c.id}>
                <TableCell className="font-mono text-sm">{c.code}</TableCell>
                <TableCell className="font-medium">{c.name}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{c.businessNo ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{c.contactEmail ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{c.contactPhone ?? '—'}</TableCell>
                <TableCell className="text-right text-sm text-muted-foreground">{c.paymentTerms}</TableCell>
                <TableCell>
                  <Badge variant={c.isActive ? 'default' : 'secondary'}>
                    {c.isActive ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    {canWrite && (
                      <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(c)}>
                        <PencilIcon />
                      </Button>
                    )}
                    {canWrite && c.isActive && (
                      <Button
                        variant="ghost" size="icon-xs" title="비활성화"
                        onClick={() => setDialog({ type: 'deactivate', customer: c })}
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
          basePath="/finance/customers"
          searchParams={keyword ? { keyword } : undefined}
        />
      </div>

      {/* Create Dialog */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>새 고객 등록</DialogTitle></DialogHeader>
          {customerForm}
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
      <Dialog open={dialog.type === 'deactivate'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>고객 비활성화</DialogTitle></DialogHeader>
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
