import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { StockMovement } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

type MovementType = StockMovement['movementType']
type MovementStatus = StockMovement['status']

const TYPE_LABEL: Record<MovementType, string> = {
  RECEIPT: '입고',
  ISSUE: '출고',
  TRANSFER: '이전',
  ADJUSTMENT: '조정',
}
const STATUS_VARIANT: Record<MovementStatus, 'secondary' | 'default'> = {
  DRAFT: 'secondary',
  CONFIRMED: 'default',
}

export const metadata = { title: '재고 이동 | ERP' }

export default async function MovementsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<StockMovement> = await apiGetPage<StockMovement>(
    `/api/inventory/stock-movements?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">재고 이동</h1>
        <p className="text-sm text-gray-500 mt-1">재고 입출고 및 이전 내역을 조회합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>이동번호</TableHead>
              <TableHead>유형</TableHead>
              <TableHead>품목</TableHead>
              <TableHead>창고</TableHead>
              <TableHead className="text-right">수량</TableHead>
              <TableHead>이동일</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  재고 이동 내역이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((mv) => (
              <TableRow key={mv.id}>
                <TableCell className="font-mono text-sm">{mv.movementNo}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{TYPE_LABEL[mv.movementType]}</Badge>
                </TableCell>
                <TableCell className="font-medium">{mv.itemName}</TableCell>
                <TableCell className="text-sm text-gray-600">{mv.warehouseName}</TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {mv.quantity.toLocaleString('ko-KR')}
                </TableCell>
                <TableCell className="text-sm">{mv.movementDate}</TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[mv.status]}>
                    {mv.status === 'CONFIRMED' ? '확정' : '임시'}
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
          basePath="/inventory/movements"
        />
      </div>
    </div>
  )
}
