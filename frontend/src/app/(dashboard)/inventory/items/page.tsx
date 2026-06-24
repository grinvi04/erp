import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Item } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

function formatPrice(n: number | null) {
  if (n === null) return '—'
  return n.toLocaleString('ko-KR')
}

export const metadata = { title: '품목 관리 | ERP' }

export default async function ItemsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Item> = await apiGetPage<Item>(
    `/api/inventory/items?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">품목 관리</h1>
        <p className="text-sm text-gray-500 mt-1">재고 품목 마스터를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>SKU</TableHead>
              <TableHead>품목명</TableHead>
              <TableHead>분류</TableHead>
              <TableHead>단위</TableHead>
              <TableHead className="text-right">원가</TableHead>
              <TableHead className="text-right">판매가</TableHead>
              <TableHead>재고추적</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-gray-400 py-10">
                  등록된 품목이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((item) => (
              <TableRow key={item.id}>
                <TableCell className="font-mono text-sm">{item.sku}</TableCell>
                <TableCell className="font-medium">{item.name}</TableCell>
                <TableCell className="text-sm text-gray-600">{item.categoryName}</TableCell>
                <TableCell className="text-sm text-gray-600">{item.uomCode}</TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatPrice(item.costPrice)}
                </TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatPrice(item.salePrice)}
                </TableCell>
                <TableCell className="text-sm text-center">
                  {item.isStockTracked ? '●' : '○'}
                </TableCell>
                <TableCell>
                  <Badge variant={item.isActive ? 'default' : 'secondary'}>
                    {item.isActive ? '활성' : '비활성'}
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
          basePath="/inventory/items"
        />
      </div>
    </div>
  )
}
