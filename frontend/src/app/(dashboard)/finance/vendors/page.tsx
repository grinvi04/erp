import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Vendor } from '@/types/finance'
import type { PageResponse } from '@/types/api'

export const metadata = { title: '공급업체 | ERP' }

export default async function VendorsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Vendor> = await apiGetPage<Vendor>(
    `/api/finance/vendors?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">공급업체</h1>
        <p className="text-sm text-gray-500 mt-1">매입 거래처 정보를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>업체명</TableHead>
              <TableHead>사업자번호</TableHead>
              <TableHead>이메일</TableHead>
              <TableHead>전화</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-gray-400 py-10">
                  등록된 공급업체가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((v) => (
              <TableRow key={v.id}>
                <TableCell className="font-mono text-sm">{v.code}</TableCell>
                <TableCell className="font-medium">{v.name}</TableCell>
                <TableCell className="text-sm text-gray-600">{v.businessNo ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{v.email ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{v.phone ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={v.isActive ? 'default' : 'secondary'}>
                    {v.isActive ? '활성' : '비활성'}
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
          basePath="/finance/vendors"
        />
      </div>
    </div>
  )
}
