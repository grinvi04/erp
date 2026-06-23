import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { LeaveRequest } from '@/types/hr'
import type { PageResponse } from '@/types/api'

const STATUS_LABEL = { PENDING: '대기', APPROVED: '승인', REJECTED: '반려' }
const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive'> = {
  PENDING: 'secondary',
  APPROVED: 'default',
  REJECTED: 'destructive',
}

export const metadata = { title: '휴가 신청 | ERP' }

export default async function LeaveRequestsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<LeaveRequest> = await apiGetPage<LeaveRequest>(
    `/api/hr/leave-requests?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">휴가 신청</h1>
        <p className="text-sm text-gray-500 mt-1">직원 휴가 신청 현황을 조회합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>직원</TableHead>
              <TableHead>휴가 종류</TableHead>
              <TableHead>시작일</TableHead>
              <TableHead>종료일</TableHead>
              <TableHead className="text-right">일수</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>신청일</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  휴가 신청 내역이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((lr) => (
              <TableRow key={lr.id}>
                <TableCell className="font-medium">{lr.employeeName}</TableCell>
                <TableCell className="text-sm">{lr.policyName}</TableCell>
                <TableCell className="text-sm">{lr.startDate}</TableCell>
                <TableCell className="text-sm">{lr.endDate}</TableCell>
                <TableCell className="text-right text-sm">{lr.days}일</TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[lr.status]}>
                    {STATUS_LABEL[lr.status]}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-gray-600">
                  {lr.createdAt.slice(0, 10)}
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
          basePath="/hr/leave-requests"
        />
      </div>
    </div>
  )
}
