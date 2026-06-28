'use client'
import { useState, useTransition } from 'react'
import Link from 'next/link'
import { toast } from 'sonner'
import { ChevronRight, CheckIcon, XIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { approveInboxItem, rejectInboxItem } from './actions'
import { formatUserName } from '@/lib/utils'
import type { ApprovalSummary, ApprovalStatus } from '@/types/approval'

const STATUS_LABEL: Record<ApprovalStatus, string> = {
  PENDING: '결재중',
  APPROVED: '승인',
  REJECTED: '반려',
  CANCELLED: '취소',
  RETURNED: '반송',
}
const STATUS_VARIANT: Record<ApprovalStatus, 'default' | 'secondary' | 'destructive'> = {
  PENDING: 'secondary',
  APPROVED: 'default',
  REJECTED: 'destructive',
  CANCELLED: 'secondary',
  RETURNED: 'destructive',
}

// 결재 대상 도메인(entityType) → 처리 화면 라우트 + 인박스 인라인 처리 지원 여부.
// inlineApprove=true: 결재함에서 직접 승인. inlineReject=true: 결재함에서 직접 반려.
// 일반전표·재고 조정은 승인이 전결한도·전기 판단을 동반해 링크 이동하되, 반려는 인라인 지원한다.
const ENTITY_ROUTE: Record<
  string,
  { label: string; href: string; inlineApprove: boolean; inlineReject: boolean }
> = {
  LEAVE_REQUEST: {
    label: '휴가 신청',
    href: '/hr/leave-requests',
    inlineApprove: true,
    inlineReject: true,
  },
  AP_INVOICE: {
    label: '매입전표',
    href: '/finance/invoices',
    inlineApprove: false,
    inlineReject: false,
  },
  GL_ENTRY: {
    label: '일반전표',
    href: '/finance/journal-entries',
    inlineApprove: false,
    inlineReject: true,
  },
  STOCK_MOVEMENT: {
    label: '재고 조정',
    href: '/inventory/movements',
    inlineApprove: false,
    inlineReject: true,
  },
}

function entityInfo(entityType: string) {
  return (
    ENTITY_ROUTE[entityType] ?? {
      label: entityType,
      href: '#',
      inlineApprove: false,
      inlineReject: false,
    }
  )
}

type ActionDialog =
  | { type: 'none' }
  | { type: 'approve'; item: ApprovalSummary }
  | { type: 'reject'; item: ApprovalSummary }

interface Props {
  pending: ApprovalSummary[]
  pendingFailed: boolean
  mine: ApprovalSummary[]
  mineFailed: boolean
  names: Record<string, string>
}

export default function ApprovalsClient({
  pending,
  pendingFailed,
  mine,
  mineFailed,
  names,
}: Props) {
  const [dialog, setDialog] = useState<ActionDialog>({ type: 'none' })
  const [comment, setComment] = useState('')
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const openApprove = (item: ApprovalSummary) => {
    setComment('')
    setDialog({ type: 'approve', item })
  }
  const openReject = (item: ApprovalSummary) => {
    setComment('')
    setDialog({ type: 'reject', item })
  }

  const submit = (kind: 'approve' | 'reject', item: ApprovalSummary) => {
    if (kind === 'reject' && !comment.trim()) {
      toast.error('반려 사유를 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        if (kind === 'approve') {
          await approveInboxItem(item.entityType, item.entityId, comment.trim())
          toast.success('승인되었습니다')
        } else {
          await rejectInboxItem(item.entityType, item.entityId, comment.trim())
          toast.success('반려되었습니다')
        }
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '처리 중 오류가 발생했습니다')
      }
    })
  }

  const renderActionCell = (a: ApprovalSummary, actionable: boolean) => {
    const info = entityInfo(a.entityType)
    const showApprove = actionable && info.inlineApprove
    const showReject = actionable && info.inlineReject
    // 승인을 인라인 지원하지 않으면(예: 일반전표·재고 조정) 처리 화면으로의 링크를 함께 노출한다.
    const showLink = !showApprove && info.href !== '#'
    if (!showApprove && !showReject && !showLink) {
      return null
    }
    return (
      <div className="flex justify-end items-center gap-1">
        {showApprove && (
          <Button
            variant="ghost"
            size="sm"
            title="승인"
            onClick={() => openApprove(a)}
            disabled={isPending}
          >
            <CheckIcon className="text-success" />
            승인
          </Button>
        )}
        {showReject && (
          <Button
            variant="ghost"
            size="sm"
            title="반려"
            onClick={() => openReject(a)}
            disabled={isPending}
            className="text-destructive"
          >
            <XIcon />
            반려
          </Button>
        )}
        {showLink && (
          <Link href={info.href} className="text-sm text-primary hover:underline flex items-center">
            처리하러 가기
            <ChevronRight className="h-3 w-3" />
          </Link>
        )}
      </div>
    )
  }

  const renderTable = (
    rows: ApprovalSummary[],
    emptyText: string,
    showRequester: boolean,
    failed: boolean,
    actionable: boolean,
  ) => (
    <div className="bg-card rounded-lg border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>유형</TableHead>
            <TableHead>제목</TableHead>
            <TableHead>단계</TableHead>
            {showRequester && <TableHead>상신자</TableHead>}
            <TableHead>상태</TableHead>
            <TableHead>요청일</TableHead>
            <TableHead className="w-40" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length === 0 && (
            <TableRow>
              <TableCell
                colSpan={showRequester ? 7 : 6}
                className={`text-center py-10 ${failed ? 'text-destructive' : 'text-muted-foreground'}`}
              >
                {failed ? '결재 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.' : emptyText}
              </TableCell>
            </TableRow>
          )}
          {rows.map((a) => (
            <TableRow key={a.id}>
              <TableCell>
                <Badge variant="secondary">{entityInfo(a.entityType).label}</Badge>
              </TableCell>
              <TableCell className="font-medium max-w-xs truncate">{a.title}</TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {a.currentStep}/{a.totalSteps}
                {a.currentStepName ? ` · ${a.currentStepName}` : ''}
              </TableCell>
              {showRequester && (
                <TableCell className="text-sm text-muted-foreground" title={a.requesterId}>
                  {formatUserName(a.requesterId, names)}
                </TableCell>
              )}
              <TableCell>
                <Badge variant={STATUS_VARIANT[a.status]}>{STATUS_LABEL[a.status]}</Badge>
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {a.requestedAt.slice(0, 10)}
              </TableCell>
              <TableCell>{renderActionCell(a, actionable)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )

  const dialogItem = dialog.type !== 'none' ? dialog.item : null

  return (
    <div className="p-6 space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">결재함</h1>
        <p className="text-sm text-muted-foreground mt-1">
          내가 처리할 결재와 내가 상신한 결재를 한 곳에서 확인합니다
        </p>
      </div>

      <section>
        <h2 className="text-lg font-semibold text-foreground mb-3">
          처리 대기 <span className="text-primary">{pending.length}</span>
        </h2>
        {renderTable(pending, '처리할 결재가 없습니다', true, pendingFailed, true)}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-foreground mb-3">내가 상신한 결재</h2>
        {renderTable(mine, '상신한 결재가 없습니다', false, mineFailed, false)}
      </section>

      {/* 승인/반려 다이얼로그 */}
      <Dialog
        open={dialog.type !== 'none'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{dialog.type === 'reject' ? '결재 반려' : '결재 승인'}</DialogTitle>
          </DialogHeader>
          {dialogItem && (
            <div className="space-y-3 py-2">
              <p className="text-sm text-muted-foreground">
                <strong>{dialogItem.title}</strong>
              </p>
              <div className="grid gap-1.5">
                <Label>{dialog.type === 'reject' ? '반려 사유 *' : '의견 (선택)'}</Label>
                <Textarea
                  rows={3}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder={
                    dialog.type === 'reject' ? '반려 사유를 입력하세요' : '승인 의견(선택)'
                  }
                />
              </div>
            </div>
          )}
          <DialogFooter showCloseButton>
            {dialog.type === 'approve' && (
              <Button
                onClick={() => dialogItem && submit('approve', dialogItem)}
                disabled={isPending}
              >
                승인
              </Button>
            )}
            {dialog.type === 'reject' && (
              <Button
                variant="destructive"
                onClick={() => dialogItem && submit('reject', dialogItem)}
                disabled={isPending}
              >
                반려
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
