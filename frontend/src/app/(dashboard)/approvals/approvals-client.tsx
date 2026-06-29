'use client'
import { useState, useTransition } from 'react'
import Link from 'next/link'
import { toast } from 'sonner'
import { ChevronRight, CheckIcon, XIcon, DownloadIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { downloadCsv } from '@/lib/csv'
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
import { DetailSheet, DetailRow, DetailSection } from '@/components/ui/detail-sheet'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState, ErrorState } from '@/components/ui/empty-state'
import { approveInboxItem, rejectInboxItem } from './actions'
import { formatUserName, formatDate } from '@/lib/utils'
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

  // 결재 상세(drill-in) — 행 클릭 시 결재 건 정보(대상·상신자·현재단계 등)를 읽기전용으로 연다.
  // 전체 결재선(단계별 이력)을 주는 백엔드 엔드포인트는 아직 없어, 요약에 담긴 현재 단계까지 표시한다.
  const [detail, setDetail] = useState<ApprovalSummary | null>(null)

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
              <TableCell colSpan={showRequester ? 7 : 6} className="p-0">
                {failed ? (
                  <ErrorState
                    title="결재 정보를 불러오지 못했습니다"
                    description="잠시 후 다시 시도해주세요."
                  />
                ) : (
                  <EmptyState title={emptyText} />
                )}
              </TableCell>
            </TableRow>
          )}
          {rows.map((a) => (
            <TableRow key={a.id} className="cursor-pointer" onClick={() => setDetail(a)}>
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
                {formatDate(a.requestedAt)}
              </TableCell>
              <TableCell onClick={(e) => e.stopPropagation()}>
                {renderActionCell(a, actionable)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )

  const dialogItem = dialog.type !== 'none' ? dialog.item : null

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 양쪽 박스에 동일 적용.
  const [qType, setQType] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [qKeyword, setQKeyword] = useState('')
  const [applied, setApplied] = useState({ type: '', status: '', keyword: '' })
  const onSearch = () => setApplied({ type: qType, status: qStatus, keyword: qKeyword })
  const onReset = () => {
    setQType('')
    setQStatus('')
    setQKeyword('')
    setApplied({ type: '', status: '', keyword: '' })
  }
  const applyFilter = (rows: ApprovalSummary[]) =>
    rows.filter((a) => {
      if (applied.type && a.entityType !== applied.type) return false
      if (applied.status && a.status !== applied.status) return false
      if (applied.keyword && !a.title.toLowerCase().includes(applied.keyword.toLowerCase()))
        return false
      return true
    })
  const filteredPending = applyFilter(pending)
  const filteredMine = applyFilter(mine)

  const exportExcel = () =>
    downloadCsv(
      `결재함_${new Date().toISOString().slice(0, 10)}`,
      ['구분', '유형', '제목', '단계', '상신자', '상태', '요청일'],
      [
        ...filteredPending.map((a) => [
          '처리대기',
          entityInfo(a.entityType).label,
          a.title,
          `${a.currentStep}/${a.totalSteps}`,
          formatUserName(a.requesterId, names),
          STATUS_LABEL[a.status],
          formatDate(a.requestedAt),
        ]),
        ...filteredMine.map((a) => [
          '상신',
          entityInfo(a.entityType).label,
          a.title,
          `${a.currentStep}/${a.totalSteps}`,
          formatUserName(a.requesterId, names),
          STATUS_LABEL[a.status],
          formatDate(a.requestedAt),
        ]),
      ],
    )

  return (
    <div className="p-5 space-y-4">
      <PageHeader
        title="결재함"
        description="내가 처리할 결재와 내가 상신한 결재를 한 곳에서 확인합니다"
        className="mb-0"
      >
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
      </PageHeader>

      <FilterBar onSearch={onSearch} onReset={onReset}>
        <FilterField label="유형">
          <Select value={qType || 'ALL'} onValueChange={(v) => setQType(v === 'ALL' ? '' : (v ?? ''))}>
            <SelectTrigger className="h-8 w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체</SelectItem>
              {Object.keys(ENTITY_ROUTE).map((t) => (
                <SelectItem key={t} value={t}>
                  {ENTITY_ROUTE[t].label}
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
            <SelectTrigger className="h-8 w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체</SelectItem>
              {(Object.keys(STATUS_LABEL) as ApprovalStatus[]).map((s) => (
                <SelectItem key={s} value={s}>
                  {STATUS_LABEL[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FilterField>
        <FilterField label="제목">
          <Input
            value={qKeyword}
            onChange={(e) => setQKeyword(e.target.value)}
            placeholder="제목 검색"
            className="h-8 w-44"
          />
        </FilterField>
      </FilterBar>

      <section>
        <h2 className="text-lg font-semibold text-foreground mb-2">
          처리 대기 <span className="text-primary">{filteredPending.length}</span>
          <span className="ml-1 text-sm font-normal text-muted-foreground">건</span>
        </h2>
        {renderTable(filteredPending, '처리할 결재가 없습니다', true, pendingFailed, true)}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-foreground mb-2">
          내가 상신한 결재{' '}
          <span className="text-sm font-normal text-muted-foreground">
            총 {filteredMine.length}건
          </span>
        </h2>
        {renderTable(filteredMine, '상신한 결재가 없습니다', false, mineFailed, false)}
      </section>

      {/* 결재 상세 (drill-in) */}
      <DetailSheet
        open={detail !== null}
        onOpenChange={(o) => {
          if (!o) setDetail(null)
        }}
        title="결재 상세"
        description={detail?.title}
      >
        {detail && (
          <DetailSection title="결재 정보">
            <dl>
              <DetailRow label="유형">
                <Badge variant="secondary">{entityInfo(detail.entityType).label}</Badge>
              </DetailRow>
              <DetailRow label="제목">{detail.title}</DetailRow>
              <DetailRow label="상태">
                <Badge variant={STATUS_VARIANT[detail.status]}>{STATUS_LABEL[detail.status]}</Badge>
              </DetailRow>
              <DetailRow label="상신자">
                <span title={detail.requesterId}>{formatUserName(detail.requesterId, names)}</span>
              </DetailRow>
              <DetailRow label="현재 단계">
                {detail.currentStep}/{detail.totalSteps}
                {detail.currentStepName ? ` · ${detail.currentStepName}` : ''}
              </DetailRow>
              {detail.currentApproverId && (
                <DetailRow label="현재 결재자">
                  <span title={detail.currentApproverId}>
                    {formatUserName(detail.currentApproverId, names)}
                  </span>
                </DetailRow>
              )}
              <DetailRow label="요청일">{formatDate(detail.requestedAt)}</DetailRow>
              {detail.completedAt && (
                <DetailRow label="완료일">{formatDate(detail.completedAt)}</DetailRow>
              )}
              {entityInfo(detail.entityType).href !== '#' && (
                <DetailRow label="대상">
                  <Link
                    href={entityInfo(detail.entityType).href}
                    className="inline-flex items-center text-primary hover:underline"
                  >
                    처리 화면으로 이동
                    <ChevronRight className="h-3 w-3" />
                  </Link>
                </DetailRow>
              )}
            </dl>
          </DetailSection>
        )}
      </DetailSheet>

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
