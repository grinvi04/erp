import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Warehouse } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

export const metadata = { title: '창고 관리 | ERP' }

export default async function WarehousesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Warehouse> = await apiGetPage<Warehouse>(
    `/api/inventory/warehouses?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">창고 관리</h1>
        <p className="text-sm text-gray-500 mt-1">물류 창고 정보를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>창고명</TableHead>
              <TableHead>주소</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>생성일</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-gray-400 py-10">
                  등록된 창고가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((wh) => (
              <TableRow key={wh.id}>
                <TableCell className="font-mono text-sm">{wh.code}</TableCell>
                <TableCell className="font-medium">{wh.name}</TableCell>
                <TableCell className="text-sm text-gray-600 max-w-xs truncate">
                  {wh.address ?? '—'}
                </TableCell>
                <TableCell>
                  <Badge variant={wh.isActive ? 'default' : 'secondary'}>
                    {wh.isActive ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-gray-600">
                  {wh.createdAt.slice(0, 10)}
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
          basePath="/inventory/warehouses"
        />
      </div>
    </div>
  )
}
