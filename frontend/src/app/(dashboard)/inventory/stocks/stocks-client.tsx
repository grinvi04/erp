'use client'
import { useRouter } from 'next/navigation'
import { PackageSearchIcon } from 'lucide-react'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Warehouse, StockBalance } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

interface Props {
  warehouses: Warehouse[]
  warehouseId: string
  data: PageResponse<StockBalance> | null
}

function fmtNum(n: number) {
  return n.toLocaleString('ko-KR')
}

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

  const columns: Column<StockBalance>[] = [
    {
      key: 'sku',
      header: 'SKU',
      sortable: true,
      sortValue: (s) => s.itemSku,
      cell: (s) => <span className="font-mono text-sm">{s.itemSku}</span>,
    },
    {
      key: 'name',
      header: '품목명',
      sortable: true,
      sortValue: (s) => s.itemName,
      cell: (s) => <span className="font-medium">{s.itemName}</span>,
    },
    {
      key: 'location',
      header: '위치',
      cell: (s) => (
        <span className="text-sm text-muted-foreground">
          {s.locationCode}
          {s.locationName ? (
            <span className="text-muted-foreground"> — {s.locationName}</span>
          ) : null}
        </span>
      ),
    },
    {
      key: 'lotSerial',
      header: 'Lot / Serial',
      cell: (s) => (
        <span className="text-sm text-muted-foreground">{s.lotNo ?? s.serialNo ?? '—'}</span>
      ),
    },
    {
      key: 'qtyOnHand',
      header: '보유',
      align: 'right',
      sortable: true,
      sortValue: (s) => s.qtyOnHand,
      cell: (s) => <span className="font-mono text-sm">{fmtNum(s.qtyOnHand)}</span>,
    },
    {
      key: 'qtyReserved',
      header: '예약',
      align: 'right',
      sortable: true,
      sortValue: (s) => s.qtyReserved,
      cell: (s) => (
        <span className="font-mono text-sm text-muted-foreground">{fmtNum(s.qtyReserved)}</span>
      ),
    },
    {
      key: 'qtyAvailable',
      header: '가용',
      align: 'right',
      sortable: true,
      sortValue: (s) => s.qtyOnHand - s.qtyReserved,
      cell: (s) => (
        <span className="font-mono text-sm font-medium">{fmtNum(s.qtyOnHand - s.qtyReserved)}</span>
      ),
    },
    {
      key: 'unitCost',
      header: '단가',
      align: 'right',
      sortable: true,
      sortValue: (s) => s.unitCost,
      cell: (s) => <span className="font-mono text-sm">{fmtNum(s.unitCost)}</span>,
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="재고 현황"
        description="창고별 품목 재고 보유·예약·가용 수량을 조회합니다"
        className="mb-6"
      >
        <div className="w-64">
          <Select value={warehouseId} onValueChange={onWarehouseChange}>
            <SelectTrigger className="w-full">
              <SelectValue placeholder="창고 선택" />
            </SelectTrigger>
            <SelectContent>
              {warehouses
                .filter((w) => w.active)
                .map((w) => (
                  <SelectItem key={w.id} value={String(w.id)}>
                    {w.code} {w.name}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>
      </PageHeader>

      {!warehouseId || !data ? (
        <div className="rounded-xl border border-border bg-card">
          <EmptyState
            icon={PackageSearchIcon}
            title="조회할 창고를 선택해주세요"
            className="py-20"
          />
        </div>
      ) : (
        <div className="space-y-3">
          <DataTable
            data={data.content}
            columns={columns}
            getRowId={(s) => s.id}
            empty={<EmptyState icon={PackageSearchIcon} title="재고 내역이 없습니다" />}
          />
          <PaginationBar
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={data.size}
            basePath="/inventory/stocks"
            searchParams={{ warehouseId }}
          />
        </div>
      )}
    </div>
  )
}
