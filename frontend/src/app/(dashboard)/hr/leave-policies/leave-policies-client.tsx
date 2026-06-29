'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, Trash2Icon, DownloadIcon } from 'lucide-react'
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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { createLeavePolicy, deleteLeavePolicy } from './actions'
import type { LeavePolicy, LeaveType } from '@/types/hr'

const LEAVE_TYPE_LABEL: Record<LeaveType, string> = {
  ANNUAL: '연차',
  SICK: '병가',
  PARENTAL: '육아휴직',
  BEREAVEMENT: '경조사',
  UNPAID: '무급',
  COMPENSATORY: '보상휴가',
}
const LEAVE_TYPES = Object.keys(LEAVE_TYPE_LABEL) as LeaveType[]

type DialogMode = { type: 'none' } | { type: 'create' } | { type: 'delete'; policy: LeavePolicy }

interface Props {
  policies: LeavePolicy[]
}

export default function LeavePoliciesClient({ policies }: Props) {
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [leaveType, setLeaveType] = useState<string>('')
  const [annualDays, setAnnualDays] = useState('0')
  const [carryOverDays, setCarryOverDays] = useState('0')
  const [requiresApproval, setRequiresApproval] = useState<string>('true')
  const [minNoticeDays, setMinNoticeDays] = useState('0')

  const openCreate = () => {
    setCode('')
    setName('')
    setLeaveType('')
    setAnnualDays('0')
    setCarryOverDays('0')
    setRequiresApproval('true')
    setMinNoticeDays('0')
    setDialog({ type: 'create' })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim() || !leaveType) {
      toast.error('코드·정책명·휴가 종류는 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createLeavePolicy({
          code: code.trim(),
          name: name.trim(),
          leaveType: leaveType as LeaveType,
          annualDays: Number(annualDays),
          carryOverDays: Number(carryOverDays),
          requiresApproval: requiresApproval === 'true',
          minNoticeDays: Number(minNoticeDays),
        })
        toast.success('휴가 정책이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (policy: LeavePolicy) => {
    startTransition(async () => {
      try {
        await deleteLeavePolicy(policy.id)
        toast.success('휴가 정책이 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<LeavePolicy>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (p) => p.code,
      cell: (p) => <span className="font-mono text-sm">{p.code}</span>,
    },
    {
      key: 'name',
      header: '정책명',
      sortable: true,
      sortValue: (p) => p.name,
      cell: (p) => <span className="font-medium">{p.name}</span>,
    },
    {
      key: 'leaveType',
      header: '휴가 종류',
      cell: (p) => <span className="text-sm">{LEAVE_TYPE_LABEL[p.leaveType] ?? p.leaveType}</span>,
    },
    {
      key: 'annualDays',
      header: '연간 일수',
      align: 'right',
      sortable: true,
      sortValue: (p) => p.annualDays,
      cell: (p) => <span className="text-sm text-muted-foreground">{p.annualDays}</span>,
      footer: (rows) => (
        <span className="font-mono">{rows.reduce((s, r) => s + r.annualDays, 0).toLocaleString('ko-KR')}</span>
      ),
    },
    {
      key: 'carryOverDays',
      header: '이월 일수',
      align: 'right',
      sortable: true,
      sortValue: (p) => p.carryOverDays,
      cell: (p) => <span className="text-sm text-muted-foreground">{p.carryOverDays}</span>,
      footer: (rows) => (
        <span className="font-mono">{rows.reduce((s, r) => s + r.carryOverDays, 0).toLocaleString('ko-KR')}</span>
      ),
    },
    {
      key: 'minNoticeDays',
      header: '최소 통보일',
      align: 'right',
      sortable: true,
      sortValue: (p) => p.minNoticeDays,
      cell: (p) => <span className="text-sm text-muted-foreground">{p.minNoticeDays}</span>,
      footer: (rows) => (
        <span className="font-mono">{rows.reduce((s, r) => s + r.minNoticeDays, 0).toLocaleString('ko-KR')}</span>
      ),
    },
    {
      key: 'requiresApproval',
      header: '승인 필요',
      sortable: true,
      sortValue: (p) => (p.requiresApproval ? 0 : 1),
      cell: (p) => (
        <Badge variant={p.requiresApproval ? 'default' : 'secondary'}>
          {p.requiresApproval ? '필요' : '불필요'}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-16',
      cell: (p) => (
        <div className="flex justify-end">
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={() => setDialog({ type: 'delete', policy: p })}
          >
            <Trash2Icon className="text-destructive" />
            <span className="sr-only">삭제</span>
          </Button>
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 로드된 데이터 기준 클라이언트 필터.
  const [qType, setQType] = useState('')
  const [qApproval, setQApproval] = useState('')
  const [qKeyword, setQKeyword] = useState('')
  const [applied, setApplied] = useState({ type: '', approval: '', keyword: '' })
  const onSearch = () => setApplied({ type: qType, approval: qApproval, keyword: qKeyword.trim() })
  const onReset = () => {
    setQType('')
    setQApproval('')
    setQKeyword('')
    setApplied({ type: '', approval: '', keyword: '' })
  }
  const filtered = policies.filter((p) => {
    if (applied.type && p.leaveType !== applied.type) return false
    if (applied.approval && String(p.requiresApproval) !== applied.approval) return false
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      if (!p.code.toLowerCase().includes(kw) && !p.name.toLowerCase().includes(kw)) return false
    }
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `휴가정책_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '정책명', '휴가 종류', '연간 일수', '이월 일수', '최소 통보일', '승인 필요'],
      filtered.map((p) => [
        p.code,
        p.name,
        LEAVE_TYPE_LABEL[p.leaveType] ?? p.leaveType,
        p.annualDays,
        p.carryOverDays,
        p.minNoticeDays,
        p.requiresApproval ? '필요' : '불필요',
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader
        title="휴가 정책"
        description="휴가 종류별 부여 일수·승인 규칙을 관리합니다"
        className="mb-4"
      >
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        <Button onClick={openCreate}>
          <PlusIcon />새 정책
        </Button>
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="휴가 종류">
            <Select value={qType || 'ALL'} onValueChange={(v) => setQType(v === 'ALL' ? '' : (v ?? ''))}>
              <SelectTrigger className="h-8 w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {LEAVE_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {LEAVE_TYPE_LABEL[t]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="승인 필요">
            <Select
              value={qApproval || 'ALL'}
              onValueChange={(v) => setQApproval(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="true">필요</SelectItem>
                <SelectItem value="false">불필요</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="검색어">
            <Input
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
              placeholder="코드·정책명"
              className="h-8 w-44"
            />
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(p) => p.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={<EmptyState title="등록된 휴가 정책이 없습니다" />}
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
            <DialogTitle>새 휴가 정책</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="코드" required>
                <Input
                  id="lp-code"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="예: ANNUAL_DEFAULT"
                  maxLength={30}
                  className="h-8"
                />
              </FormRow>
              <FormRow label="정책명" required>
                <Input
                  id="lp-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="예: 기본 연차"
                  maxLength={100}
                  className="h-8"
                />
              </FormRow>
              <FormRow label="휴가 종류" required span>
                <Select value={leaveType} onValueChange={(v) => setLeaveType(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue placeholder="선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {LEAVE_TYPES.map((t) => (
                      <SelectItem key={t} value={t}>
                        {LEAVE_TYPE_LABEL[t]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="연간 일수">
                <Input
                  id="lp-annual"
                  type="number"
                  value={annualDays}
                  onChange={(e) => setAnnualDays(e.target.value)}
                  min={0}
                  className="h-8"
                />
              </FormRow>
              <FormRow label="이월 일수">
                <Input
                  id="lp-carry"
                  type="number"
                  value={carryOverDays}
                  onChange={(e) => setCarryOverDays(e.target.value)}
                  min={0}
                  className="h-8"
                />
              </FormRow>
              <FormRow label="최소 통보일">
                <Input
                  id="lp-notice"
                  type="number"
                  value={minNoticeDays}
                  onChange={(e) => setMinNoticeDays(e.target.value)}
                  min={0}
                  className="h-8"
                />
              </FormRow>
              <FormRow label="승인 필요 여부">
                <Select
                  value={requiresApproval}
                  onValueChange={(v) => setRequiresApproval(v ?? 'true')}
                >
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="true">승인 필요</SelectItem>
                    <SelectItem value="false">승인 불필요</SelectItem>
                  </SelectContent>
                </Select>
              </FormRow>
            </FormGrid>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog
        open={dialog.type === 'delete'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>휴가 정책 삭제</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground py-2">
            {dialog.type === 'delete' && (
              <>
                <strong>{dialog.policy.name}</strong> 정책을 삭제하시겠습니까?
                <br />
                해당 정책으로 신청된 휴가·잔여 내역이 있으면 삭제할 수 없습니다.
              </>
            )}
          </p>
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.policy)}
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
