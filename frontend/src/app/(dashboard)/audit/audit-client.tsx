'use client'
import { useRouter } from 'next/navigation'
import { Badge } from '@/components/ui/badge'
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
import type { AuditAction, AuditLog } from '@/types/audit'
import type { PageResponse } from '@/types/api'

const ACTION_LABEL: Record<AuditAction, string> = {
  CREATE: '생성',
  UPDATE: '수정',
  DELETE: '삭제',
  VIEW: '조회',
  APPROVE: '승인',
  REJECT: '반려',
}
const ACTION_VARIANT: Record<AuditAction, 'default' | 'secondary' | 'destructive'> = {
  CREATE: 'default',
  UPDATE: 'secondary',
  DELETE: 'destructive',
  VIEW: 'secondary',
  APPROVE: 'default',
  REJECT: 'destructive',
}
const ENTITY_LABEL: Record<string, string> = {
  LEAVE_REQUEST: '휴가 신청',
  AP_INVOICE: '매입계산서',
  EMPLOYEE: '직원',
  ROLE: '역할',
  USER_ROLE: '역할 배정',
  ACCESS_PROFILE: '접근 프로파일',
}

const ALL = 'ALL'

function fmtDateTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR')
}

export default function AuditClient({
  data,
  entityType,
}: {
  data: PageResponse<AuditLog>
  entityType: string
}) {
  const router = useRouter()

  function onFilterChange(value: string | null) {
    router.push(
      !value || value === ALL ? '/audit' : `/audit?entityType=${encodeURIComponent(value)}`,
    )
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">감사 로그</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            누가 무엇을 언제 결재·변경했는지 추적합니다.
          </p>
        </div>
        <Select value={entityType || ALL} onValueChange={onFilterChange}>
          <SelectTrigger className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>전체 대상</SelectItem>
            <SelectItem value="LEAVE_REQUEST">휴가 신청</SelectItem>
            <SelectItem value="AP_INVOICE">매입계산서</SelectItem>
            <SelectItem value="EMPLOYEE">직원</SelectItem>
            <SelectItem value="ROLE">역할</SelectItem>
            <SelectItem value="USER_ROLE">역할 배정</SelectItem>
            <SelectItem value="ACCESS_PROFILE">접근 프로파일</SelectItem>
          </SelectContent>
        </Select>
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
              <TableHead>IP</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-sm text-muted-foreground py-8">
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
                    <Badge variant={ACTION_VARIANT[log.action]}>{ACTION_LABEL[log.action]}</Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs">{log.performedBy}</TableCell>
                  <TableCell className="text-muted-foreground">{log.ipAddress ?? '—'}</TableCell>
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
          searchParams={entityType ? { entityType } : undefined}
        />
      </div>
    </div>
  )
}
