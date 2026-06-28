'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { DownloadIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { AUDIT_ACTIONS, type AuditAction, type AuditFilters, type AuditLog } from '@/types/audit'
import { formatUserName } from '@/lib/utils'
import type { PageResponse } from '@/types/api'

const ACTION_LABEL: Record<AuditAction, string> = {
  CREATE: '생성',
  UPDATE: '수정',
  DELETE: '삭제',
  VIEW: '조회',
  APPROVE: '승인',
  REJECT: '반려',
  WITHDRAW: '회수',
  REVERSE: '역분개',
}
const ACTION_VARIANT: Record<AuditAction, 'default' | 'secondary' | 'destructive'> = {
  CREATE: 'default',
  UPDATE: 'secondary',
  DELETE: 'destructive',
  VIEW: 'secondary',
  APPROVE: 'default',
  REJECT: 'destructive',
  WITHDRAW: 'secondary',
  REVERSE: 'destructive',
}
const ENTITY_LABEL: Record<string, string> = {
  LEAVE_REQUEST: '휴가 신청',
  AP_INVOICE: '매입계산서',
  EMPLOYEE: '직원',
  ROLE: '역할',
  USER_ROLE: '역할 배정',
  ACCESS_PROFILE: '접근 프로파일',
}
const ENTITY_OPTIONS = Object.entries(ENTITY_LABEL)

const ALL = 'ALL'

function fmtDateTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR')
}

export default function AuditClient({
  data,
  filters,
  names,
}: {
  data: PageResponse<AuditLog>
  filters: AuditFilters
  names: Record<string, string>
}) {
  const router = useRouter()
  const [performedBy, setPerformedBy] = useState(filters.performedBy)

  // 필터 일부를 바꿔 URL로 반영한다 — URL이 단일 출처(공유·새로고침 가능), 변경 시 첫 페이지로.
  function applyFilters(patch: Partial<AuditFilters>) {
    const next: AuditFilters = { ...filters, ...patch }
    const params = new URLSearchParams()
    if (next.entityType) params.set('entityType', next.entityType)
    if (next.action) params.set('action', next.action)
    if (next.performedBy) params.set('performedBy', next.performedBy)
    if (next.from) params.set('from', next.from)
    if (next.to) params.set('to', next.to)
    const qs = params.toString()
    router.push(qs ? `/audit?${qs}` : '/audit')
  }

  const exportQs = new URLSearchParams()
  if (filters.entityType) exportQs.set('entityType', filters.entityType)
  if (filters.action) exportQs.set('action', filters.action)
  if (filters.performedBy) exportQs.set('performedBy', filters.performedBy)
  if (filters.from) exportQs.set('from', filters.from)
  if (filters.to) exportQs.set('to', filters.to)
  const exportHref = `/audit/export${exportQs.toString() ? `?${exportQs.toString()}` : ''}`

  // PaginationBar가 페이지 이동 시 보존할 현재 필터.
  const activeParams: Record<string, string> = {}
  if (filters.entityType) activeParams.entityType = filters.entityType
  if (filters.action) activeParams.action = filters.action
  if (filters.performedBy) activeParams.performedBy = filters.performedBy
  if (filters.from) activeParams.from = filters.from
  if (filters.to) activeParams.to = filters.to

  return (
    <div className="p-6">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">감사 로그</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            누가 무엇을 언제 결재·변경했는지 추적합니다.
          </p>
        </div>
        <a
          href={exportHref}
          download
          className={buttonVariants({ variant: 'outline', size: 'sm' })}
        >
          <DownloadIcon />
          CSV 내보내기
        </a>
      </div>

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <div className="grid gap-1.5">
          <Label className="text-xs text-muted-foreground">대상</Label>
          <Select
            value={filters.entityType || ALL}
            onValueChange={(v) => applyFilters({ entityType: !v || v === ALL ? '' : v })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>전체 대상</SelectItem>
              {ENTITY_OPTIONS.map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="grid gap-1.5">
          <Label className="text-xs text-muted-foreground">액션</Label>
          <Select
            value={filters.action || ALL}
            onValueChange={(v) => applyFilters({ action: !v || v === ALL ? '' : v })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>전체 액션</SelectItem>
              {AUDIT_ACTIONS.map((a) => (
                <SelectItem key={a} value={a}>
                  {ACTION_LABEL[a]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="grid gap-1.5">
          <Label className="text-xs text-muted-foreground">수행자</Label>
          <Input
            value={performedBy}
            placeholder="사용자 ID"
            onChange={(e) => setPerformedBy(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') applyFilters({ performedBy: performedBy.trim() })
            }}
            onBlur={() => {
              if (performedBy.trim() !== filters.performedBy)
                applyFilters({ performedBy: performedBy.trim() })
            }}
          />
        </div>

        <div className="grid gap-1.5">
          <Label className="text-xs text-muted-foreground">시작일</Label>
          <Input
            type="date"
            value={filters.from}
            max={filters.to || undefined}
            onChange={(e) => applyFilters({ from: e.target.value })}
          />
        </div>

        <div className="grid gap-1.5">
          <Label className="text-xs text-muted-foreground">종료일</Label>
          <Input
            type="date"
            value={filters.to}
            min={filters.from || undefined}
            onChange={(e) => applyFilters({ to: e.target.value })}
          />
        </div>
      </div>

      <div className="bg-card rounded-lg border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>일시</TableHead>
              <TableHead>대상</TableHead>
              <TableHead>대상 ID</TableHead>
              <TableHead>액션</TableHead>
              <TableHead>수행자</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-sm text-muted-foreground py-8">
                  감사 로그가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              data.content.map((log) => (
                <TableRow key={log.id}>
                  <TableCell className="whitespace-nowrap">
                    {fmtDateTime(log.performedAt)}
                  </TableCell>
                  <TableCell>{ENTITY_LABEL[log.entityType] ?? log.entityType}</TableCell>
                  <TableCell>{log.entityId}</TableCell>
                  <TableCell>
                    <Badge variant={ACTION_VARIANT[log.action]}>
                      {ACTION_LABEL[log.action] ?? log.action}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm" title={log.performedBy}>
                    {formatUserName(log.performedBy, names)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/audit"
          searchParams={Object.keys(activeParams).length ? activeParams : undefined}
        />
      </div>
    </div>
  )
}
