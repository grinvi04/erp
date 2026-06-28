'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, CheckIcon, XIcon } from 'lucide-react'
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
import { createLeaveRequest, approveLeaveRequest, rejectLeaveRequest } from './actions'
import type { LeaveRequest, Employee, LeavePolicy, ApprovalStatus } from '@/types/hr'
import type { PageResponse } from '@/types/api'

const STATUS_LABEL: Record<ApprovalStatus, string> = {
  PENDING: '결재중',
  APPROVED: '승인',
  REJECTED: '반려',
}
const STATUS_VARIANT: Record<ApprovalStatus, 'default' | 'secondary' | 'destructive'> = {
  PENDING: 'secondary',
  APPROVED: 'default',
  REJECTED: 'destructive',
}

type DialogState =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'approve'; req: LeaveRequest }
  | { type: 'reject'; req: LeaveRequest }

interface Props {
  data: PageResponse<LeaveRequest>
  employees: Employee[]
  policies: LeavePolicy[]
}

export default function LeaveRequestsClient({ data, employees, policies }: Props) {
  const { content: requests } = data
  const [dialog, setDialog] = useState<DialogState>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  // Create form
  const [empId, setEmpId] = useState<string>('')
  const [policyId, setPolicyId] = useState<string>('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [reqDays, setReqDays] = useState('')
  const [reason, setReason] = useState('')

  // Approval comment
  const [comment, setComment] = useState('')

  const openCreate = () => {
    setEmpId('')
    setPolicyId('')
    setStartDate('')
    setEndDate('')
    setReqDays('')
    setReason('')
    setDialog({ type: 'create' })
  }

  const handleCreate = () => {
    if (!empId || !policyId || !startDate || !endDate || !reqDays) {
      toast.error('필수 항목을 모두 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        await createLeaveRequest({
          employeeId: Number(empId),
          leavePolicyId: Number(policyId),
          startDate,
          endDate,
          requestedDays: Number(reqDays),
          reason: reason || null,
        })
        toast.success('휴가 신청이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '신청 중 오류가 발생했습니다')
      }
    })
  }

  const handleApprove = (req: LeaveRequest) => {
    startTransition(async () => {
      try {
        await approveLeaveRequest(req.id, comment)
        toast.success('승인 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '승인 중 오류가 발생했습니다')
      }
    })
  }

  const handleReject = (req: LeaveRequest) => {
    startTransition(async () => {
      try {
        await rejectLeaveRequest(req.id, comment)
        toast.success('반려 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '반려 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<LeaveRequest>[] = [
    {
      key: 'employeeName',
      header: '직원',
      sortable: true,
      sortValue: (lr) => lr.employeeName,
      cell: (lr) => <span className="font-medium">{lr.employeeName}</span>,
    },
    {
      key: 'leavePolicyName',
      header: '휴가 종류',
      sortable: true,
      sortValue: (lr) => lr.leavePolicyName,
      cell: (lr) => <span className="text-sm">{lr.leavePolicyName}</span>,
    },
    {
      key: 'startDate',
      header: '시작일',
      sortable: true,
      sortValue: (lr) => lr.startDate,
      cell: (lr) => <span className="text-sm">{lr.startDate}</span>,
    },
    {
      key: 'endDate',
      header: '종료일',
      sortable: true,
      sortValue: (lr) => lr.endDate,
      cell: (lr) => <span className="text-sm">{lr.endDate}</span>,
    },
    {
      key: 'requestedDays',
      header: '일수',
      align: 'right',
      sortable: true,
      sortValue: (lr) => lr.requestedDays,
      cell: (lr) => <span className="text-sm">{lr.requestedDays}일</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (lr) => STATUS_LABEL[lr.approvalStatus],
      cell: (lr) => (
        <Badge variant={STATUS_VARIANT[lr.approvalStatus]}>{STATUS_LABEL[lr.approvalStatus]}</Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (lr) =>
        lr.approvalStatus === 'PENDING' ? (
          <div className="flex justify-end gap-1">
            <Button
              variant="ghost"
              size="icon-xs"
              title="승인"
              onClick={() => {
                setComment('')
                setDialog({ type: 'approve', req: lr })
              }}
            >
              <CheckIcon className="text-success" />
            </Button>
            <Button
              variant="ghost"
              size="icon-xs"
              title="반려"
              onClick={() => {
                setComment('')
                setDialog({ type: 'reject', req: lr })
              }}
            >
              <XIcon className="text-destructive" />
            </Button>
          </div>
        ) : null,
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="휴가 신청"
        description="직원 휴가 신청 현황을 조회하고 결재합니다"
        className="mb-6"
      >
        <Button onClick={openCreate}>
          <PlusIcon />새 휴가 신청
        </Button>
      </PageHeader>

      <div className="space-y-3">
        <DataTable
          data={requests}
          columns={columns}
          getRowId={(lr) => lr.id}
          empty={<EmptyState title="휴가 신청 내역이 없습니다" />}
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/hr/leave-requests"
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
            <DialogTitle>새 휴가 신청</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>직원 *</Label>
              <Select value={empId} onValueChange={(v) => setEmpId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="직원 선택" />
                </SelectTrigger>
                <SelectContent>
                  {employees.map((e) => (
                    <SelectItem key={e.id} value={String(e.id)}>
                      {e.fullName} ({e.employeeNo})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>휴가 종류 *</Label>
              <Select value={policyId} onValueChange={(v) => setPolicyId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                  {policies.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>시작일 *</Label>
                <Input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
              <div className="grid gap-1.5">
                <Label>종료일 *</Label>
                <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
              </div>
            </div>
            <div className="grid gap-1.5">
              <Label>신청 일수 *</Label>
              <Input
                type="number"
                value={reqDays}
                onChange={(e) => setReqDays(e.target.value)}
                min={0.5}
                step={0.5}
                placeholder="1"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>사유</Label>
              <Textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="휴가 사유를 입력하세요"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              신청
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Approve Dialog */}
      <Dialog
        open={dialog.type === 'approve'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>휴가 승인</DialogTitle>
          </DialogHeader>
          {dialog.type === 'approve' && (
            <div className="text-sm text-muted-foreground py-1">
              <strong>{dialog.req.employeeName}</strong>의{' '}
              <strong>{dialog.req.leavePolicyName}</strong> 신청 ({dialog.req.startDate} ~{' '}
              {dialog.req.endDate}, {dialog.req.requestedDays}일)을 승인하시겠습니까?
            </div>
          )}
          <div className="grid gap-1.5">
            <Label>코멘트</Label>
            <Textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={2} />
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'approve' && handleApprove(dialog.req)}
              disabled={isPending}
            >
              승인
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog
        open={dialog.type === 'reject'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>휴가 반려</DialogTitle>
          </DialogHeader>
          {dialog.type === 'reject' && (
            <div className="text-sm text-muted-foreground py-1">
              <strong>{dialog.req.employeeName}</strong>의{' '}
              <strong>{dialog.req.leavePolicyName}</strong> 신청을 반려하시겠습니까?
            </div>
          )}
          <div className="grid gap-1.5">
            <Label>반려 사유</Label>
            <Textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={2} />
          </div>
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'reject' && handleReject(dialog.req)}
              disabled={isPending}
            >
              반려
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
