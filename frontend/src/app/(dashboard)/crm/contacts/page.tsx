import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Contact } from '@/types/crm'
import type { PageResponse } from '@/types/api'

export const metadata = { title: '담당자 | ERP' }

export default async function ContactsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Contact> = await apiGetPage<Contact>(
    `/api/crm/contacts?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">담당자</h1>
        <p className="text-sm text-gray-500 mt-1">고객사 담당자 정보를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>이름</TableHead>
              <TableHead>고객사</TableHead>
              <TableHead>직함</TableHead>
              <TableHead>부서</TableHead>
              <TableHead>이메일</TableHead>
              <TableHead>전화</TableHead>
              <TableHead>주 담당자</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  등록된 담당자가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((ct) => (
              <TableRow key={ct.id}>
                <TableCell className="font-medium">
                  {ct.lastName}{ct.firstName}
                </TableCell>
                <TableCell className="text-sm text-gray-700">{ct.accountName}</TableCell>
                <TableCell className="text-sm text-gray-600">{ct.title ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{ct.department ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{ct.email ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{ct.phone ?? '—'}</TableCell>
                <TableCell>
                  {ct.isPrimary && <Badge>주 담당자</Badge>}
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
          basePath="/crm/contacts"
        />
      </div>
    </div>
  )
}
