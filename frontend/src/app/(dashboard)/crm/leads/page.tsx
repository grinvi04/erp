import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Lead, LeadStatus } from '@/types/crm'
import type { PageResponse } from '@/types/api'

const STATUS_LABEL: Record<LeadStatus, string> = {
  NEW: '신규',
  CONTACTED: '접촉',
  QUALIFIED: '적격',
  CONVERTED: '전환',
  DISQUALIFIED: '불량',
}
const STATUS_VARIANT: Record<LeadStatus, 'default' | 'secondary' | 'destructive'> = {
  NEW: 'secondary',
  CONTACTED: 'secondary',
  QUALIFIED: 'default',
  CONVERTED: 'default',
  DISQUALIFIED: 'destructive',
}

export const metadata = { title: '리드 | ERP' }

export default async function LeadsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Lead> = await apiGetPage<Lead>(
    `/api/crm/leads?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">리드</h1>
        <p className="text-sm text-gray-500 mt-1">잠재 고객 리드를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>이름</TableHead>
              <TableHead>회사</TableHead>
              <TableHead>직함</TableHead>
              <TableHead>이메일</TableHead>
              <TableHead>출처</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>생성일</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  등록된 리드가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((lead) => (
              <TableRow key={lead.id}>
                <TableCell className="font-medium">
                  {lead.lastName}{lead.firstName}
                </TableCell>
                <TableCell className="text-sm text-gray-600">{lead.company ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{lead.title ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{lead.email ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{lead.source ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[lead.status]}>
                    {STATUS_LABEL[lead.status]}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-gray-600">
                  {lead.createdAt.slice(0, 10)}
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
          basePath="/crm/leads"
        />
      </div>
    </div>
  )
}
