'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, Trash2Icon } from 'lucide-react'
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
import { createLeavePolicy, deleteLeavePolicy } from './actions'
import type { LeavePolicy, LeaveType } from '@/types/hr'

const LEAVE_TYPE_LABEL: Record<LeaveType, string> = {
  ANNUAL: '연차',
  SICK: '병가',
  PARENTAL: '육아휴직',
  BEREAVEMENT: '경조사',
  UNPAID: '무급',
  COMPENSATORY: '보상 휴가',
}
const LEAVE_TYPES = Object.keys(LEAVE_TYPE_LABEL) as LeaveType[]

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'delete'; policy: LeavePolicy }

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
    setCode(''); setName(''); setLeaveType(''); setAnnualDays('0')
    setCarryOverDays('0'); setRequiresApproval('true'); setMinNoticeDays('0')
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

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">휴가 정책</h1>
          <p className="text-sm text-gray-500 mt-1">휴가 종류별 부여 일수·승인 규칙을 관리합니다</p>
        </div>
        <Button onClick={openCreate}>
          <PlusIcon />
          새 정책
        </Button>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>정책명</TableHead>
              <TableHead>휴가 종류</TableHead>
              <TableHead className="text-right">연간 일수</TableHead>
              <TableHead className="text-right">이월 일수</TableHead>
              <TableHead className="text-right">최소 통보일</TableHead>
              <TableHead>승인 필요</TableHead>
              <TableHead className="w-16" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {policies.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-gray-400 py-10">
                  등록된 휴가 정책이 없습니다
                </TableCell>
              </TableRow>
            )}
            {policies.map((p) => (
              <TableRow key={p.id}>
                <TableCell className="font-mono text-sm">{p.code}</TableCell>
                <TableCell className="font-medium">{p.name}</TableCell>
                <TableCell className="text-sm">
                  {LEAVE_TYPE_LABEL[p.leaveType] ?? p.leaveType}
                </TableCell>
                <TableCell className="text-right text-sm text-gray-600">{p.annualDays}</TableCell>
                <TableCell className="text-right text-sm text-gray-600">{p.carryOverDays}</TableCell>
                <TableCell className="text-right text-sm text-gray-600">{p.minNoticeDays}</TableCell>
                <TableCell>
                  <Badge variant={p.requiresApproval ? 'default' : 'secondary'}>
                    {p.requiresApproval ? '필요' : '불필요'}
                  </Badge>
                </TableCell>
                <TableCell>
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
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Create Dialog */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 휴가 정책</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label htmlFor="lp-code">코드 *</Label>
                <Input
                  id="lp-code"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="예: ANNUAL_DEFAULT"
                  maxLength={30}
                />
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="lp-name">정책명 *</Label>
                <Input
                  id="lp-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="예: 기본 연차"
                  maxLength={100}
                />
              </div>
            </div>
            <div className="grid gap-1.5">
              <Label>휴가 종류 *</Label>
              <Select value={leaveType} onValueChange={(v) => setLeaveType(v ?? '')}>
                <SelectTrigger className="w-full"><SelectValue placeholder="선택" /></SelectTrigger>
                <SelectContent>
                  {LEAVE_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>{LEAVE_TYPE_LABEL[t]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div className="grid gap-1.5">
                <Label htmlFor="lp-annual">연간 일수</Label>
                <Input
                  id="lp-annual"
                  type="number"
                  value={annualDays}
                  onChange={(e) => setAnnualDays(e.target.value)}
                  min={0}
                />
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="lp-carry">이월 일수</Label>
                <Input
                  id="lp-carry"
                  type="number"
                  value={carryOverDays}
                  onChange={(e) => setCarryOverDays(e.target.value)}
                  min={0}
                />
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="lp-notice">최소 통보일</Label>
                <Input
                  id="lp-notice"
                  type="number"
                  value={minNoticeDays}
                  onChange={(e) => setMinNoticeDays(e.target.value)}
                  min={0}
                />
              </div>
            </div>
            <div className="grid gap-1.5">
              <Label>승인 필요 여부</Label>
              <Select value={requiresApproval} onValueChange={(v) => setRequiresApproval(v ?? 'true')}>
                <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="true">승인 필요</SelectItem>
                  <SelectItem value="false">승인 불필요</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog open={dialog.type === 'delete'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>휴가 정책 삭제</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-gray-600 py-2">
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
