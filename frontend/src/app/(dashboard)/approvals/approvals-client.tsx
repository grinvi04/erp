'use client'
import Link from 'next/link'
import { Badge } from '@/components/ui/badge'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { ChevronRight } from 'lucide-react'
import type { ApprovalSummary, ApprovalStatus } from '@/types/approval'

const STATUS_LABEL: Record<ApprovalStatus, string> = {
  PENDING: '대기', APPROVED: '승인', REJECTED: '반려', CANCELLED: '취소', RETURNED: '반송',
}
const STATUS_VARIANT: Record<ApprovalStatus, 'default' | 'secondary' | 'destructive'> = {
  PENDING: 'secondary', APPROVED: 'default', REJECTED: 'destructive',
  CANCELLED: 'secondary', RETURNED: 'destructive',
}

// 결재 대상 도메인(entityType) → 처리 화면 라우트. 모듈 경계상 결재 자체는
// 해당 모듈 화면에서 수행한다(여기선 라우팅만).
const ENTITY_ROUTE: Record<string, { label: string; href: string }> = {
  LEAVE_REQUEST: { label: '휴가 신청', href: '/hr/leave-requests' },
  AP_INVOICE: { label: 'AP 전표', href: '/finance/invoices' },
}

function entityInfo(entityType: string) {
  return ENTITY_ROUTE[entityType] ?? { label: entityType, href: '#' }
}

function ApprovalTable({ rows, emptyText, showApprover }: {
  rows: ApprovalSummary[]
  emptyText: string
  showApprover: boolean
}) {
  return (
    <div className="bg-white rounded-lg border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>유형</TableHead>
            <TableHead>제목</TableHead>
            <TableHead>단계</TableHead>
            {showApprover && <TableHead>상신자</TableHead>}
            <TableHead>상태</TableHead>
            <TableHead>요청일</TableHead>
            <TableHead className="w-28" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length === 0 && (
            <TableRow>
              <TableCell colSpan={showApprover ? 7 : 6} className="text-center text-gray-400 py-10">
                {emptyText}
              </TableCell>
            </TableRow>
          )}
          {rows.map((a) => {
            const info = entityInfo(a.entityType)
            return (
              <TableRow key={a.id}>
                <TableCell><Badge variant="secondary">{info.label}</Badge></TableCell>
                <TableCell className="font-medium max-w-xs truncate">{a.title}</TableCell>
                <TableCell className="text-sm text-gray-600">
                  {a.currentStep}/{a.totalSteps}
                  {a.currentStepName ? ` · ${a.currentStepName}` : ''}
                </TableCell>
                {showApprover && (
                  <TableCell className="text-sm text-gray-500 font-mono">{a.requesterId}</TableCell>
                )}
                <TableCell>
                  <Badge variant={STATUS_VARIANT[a.status]}>{STATUS_LABEL[a.status]}</Badge>
                </TableCell>
                <TableCell className="text-sm text-gray-600">{a.requestedAt.slice(0, 10)}</TableCell>
                <TableCell>
                  {info.href !== '#' && (
                    <Link href={info.href}
                      className="text-sm text-blue-600 hover:underline flex items-center justify-end">
                      처리하러 가기<ChevronRight className="h-3 w-3" />
                    </Link>
                  )}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </div>
  )
}

interface Props {
  pending: ApprovalSummary[]
  mine: ApprovalSummary[]
}

export default function ApprovalsClient({ pending, mine }: Props) {
  return (
    <div className="p-6 space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-gray-900">결재함</h1>
        <p className="text-sm text-gray-500 mt-1">내가 처리할 결재와 내가 상신한 결재를 한 곳에서 확인합니다</p>
      </div>

      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">
          처리 대기 <span className="text-blue-600">{pending.length}</span>
        </h2>
        <ApprovalTable rows={pending} emptyText="처리할 결재가 없습니다" showApprover />
      </section>

      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">내가 상신한 결재</h2>
        <ApprovalTable rows={mine} emptyText="상신한 결재가 없습니다" showApprover={false} />
      </section>
    </div>
  )
}
