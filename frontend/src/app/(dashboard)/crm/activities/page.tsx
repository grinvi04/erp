import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Activity, ActivityType, ActivityStatus } from '@/types/crm'
import type { PageResponse } from '@/types/api'

const TYPE_LABEL: Record<ActivityType, string> = {
  CALL: '전화',
  EMAIL: '이메일',
  MEETING: '미팅',
  TASK: '과업',
  NOTE: '메모',
}
const STATUS_LABEL: Record<ActivityStatus, string> = {
  OPEN: '진행',
  COMPLETED: '완료',
  CANCELLED: '취소',
}
const STATUS_VARIANT: Record<ActivityStatus, 'default' | 'secondary' | 'destructive'> = {
  OPEN: 'secondary',
  COMPLETED: 'default',
  CANCELLED: 'destructive',
}

export const metadata = { title: '활동 | ERP' }

export default async function ActivitiesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Activity> = await apiGetPage<Activity>(
    `/api/crm/activities?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">활동</h1>
        <p className="text-sm text-gray-500 mt-1">영업 활동 이력을 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>유형</TableHead>
              <TableHead>제목</TableHead>
              <TableHead>고객사</TableHead>
              <TableHead>담당자</TableHead>
              <TableHead>마감일</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-gray-400 py-10">
                  등록된 활동이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((act) => (
              <TableRow key={act.id}>
                <TableCell>
                  <Badge variant="secondary">{TYPE_LABEL[act.activityType]}</Badge>
                </TableCell>
                <TableCell className="font-medium max-w-xs truncate">{act.subject}</TableCell>
                <TableCell className="text-sm text-gray-600">
                  {act.accountName ?? '—'}
                </TableCell>
                <TableCell className="text-sm text-gray-600">
                  {act.contactName ?? '—'}
                </TableCell>
                <TableCell className="text-sm text-gray-600">
                  {act.dueDate ?? '—'}
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[act.status]}>
                    {STATUS_LABEL[act.status]}
                  </Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/crm/activities"
        />
      </div>
    </div>
  )
}
