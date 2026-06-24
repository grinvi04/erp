import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Department } from '@/types/hr'
import type { PageResponse } from '@/types/api'

export const metadata = { title: '부서 관리 | ERP' }

export default async function DepartmentsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Department> = await apiGetPage<Department>(
    `/api/hr/departments?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">부서 관리</h1>
        <p className="text-sm text-gray-500 mt-1">조직 부서 구조를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>부서명</TableHead>
              <TableHead>상위 부서 ID</TableHead>
              <TableHead>부서장 ID</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>생성일</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-gray-400 py-10">
                  등록된 부서가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((dept) => (
              <TableRow key={dept.id}>
                <TableCell className="font-mono text-sm">{dept.code}</TableCell>
                <TableCell className="font-medium">{dept.name}</TableCell>
                <TableCell className="text-sm text-gray-500">
                  {dept.parentId ?? '—'}
                </TableCell>
                <TableCell className="text-sm text-gray-500">
                  {dept.headEmployeeId ?? '—'}
                </TableCell>
                <TableCell>
                  <Badge variant={dept.isActive ? 'default' : 'secondary'}>
                    {dept.isActive ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-gray-600">
                  {dept.createdAt.slice(0, 10)}
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
          basePath="/hr/departments"
        />
      </div>
    </div>
  )
}
