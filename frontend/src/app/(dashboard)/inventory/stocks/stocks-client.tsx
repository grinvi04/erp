'use client'
import { useRouter } from 'next/navigation'
import { PackageSearchIcon } from 'lucide-react'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Warehouse, StockBalance } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

interface Props {
  warehouses: Warehouse[]
  warehouseId: string
  data: PageResponse<StockBalance> | null
}

function fmtNum(n: number) { return n.toLocaleString('ko-KR') }

export default function StocksClient({ warehouses, warehouseId, data }: Props) {
  const router = useRouter()

  const onWarehouseChange = (val: string | null) => {
    const wId = val ?? ''
    if (wId) {
      router.push(`/inventory/stocks?warehouseId=${wId}`)
    } else {
      router.push('/inventory/stocks')
    }
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">재고 현황</h1>
          <p className="text-sm text-muted-foreground mt-1">창고별 품목 재고 보유·예약·가용 수량을 조회합니다</p>
        </div>
        <div className="w-64">
          <Select value={warehouseId} onValueChange={onWarehouseChange}>
            <SelectTrigger className="w-full"><SelectValue placeholder="창고 선택" /></SelectTrigger>
            <SelectContent>
              {warehouses.filter((w) => w.active).map((w) => (
                <SelectItem key={w.id} value={String(w.id)}>{w.code} {w.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {!warehouseId || !data ? (
        <div className="bg-card rounded-lg border flex flex-col items-center justify-center text-center py-20">
          <PackageSearchIcon className="h-10 w-10 text-muted-foreground mb-3" />
          <p className="text-sm text-muted-foreground">조회할 창고를 선택해주세요</p>
        </div>
      ) : (
        <div className="bg-card rounded-lg border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>SKU</TableHead>
                <TableHead>품목명</TableHead>
                <TableHead>위치</TableHead>
                <TableHead>Lot / Serial</TableHead>
                <TableHead className="text-right">보유</TableHead>
                <TableHead className="text-right">예약</TableHead>
                <TableHead className="text-right">가용</TableHead>
                <TableHead className="text-right">단가</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8} className="text-center text-muted-foreground py-10">
                    재고 내역이 없습니다
                  </TableCell>
                </TableRow>
              )}
              {data.content.map((s) => (
                <TableRow key={s.id}>
                  <TableCell className="font-mono text-sm">{s.itemSku}</TableCell>
                  <TableCell className="font-medium">{s.itemName}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {s.locationCode}
                    {s.locationName ? <span className="text-muted-foreground"> — {s.locationName}</span> : null}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {s.lotNo ?? s.serialNo ?? '—'}
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm">{fmtNum(s.qtyOnHand)}</TableCell>
                  <TableCell className="text-right font-mono text-sm text-muted-foreground">
                    {fmtNum(s.qtyReserved)}
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm font-medium">
                    {fmtNum(s.qtyOnHand - s.qtyReserved)}
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm">{fmtNum(s.unitCost)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <PaginationBar
            page={data.page} totalPages={data.totalPages}
            totalElements={data.totalElements} size={data.size}
            basePath="/inventory/stocks"
            searchParams={{ warehouseId }}
          />
        </div>
      )}
    </div>
  )
}
