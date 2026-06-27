import { apiGet, apiGetPage } from '@/lib/api'
import type { Warehouse, StockBalance } from '@/types/inventory'
import type { PageResponse } from '@/types/api'
import StocksClient from './stocks-client'

export const metadata = { title: '재고 현황 | ERP' }

export default async function StocksPage(props: {
  searchParams: Promise<{ warehouseId?: string; page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const warehouseId = sp.warehouseId ?? ''

  const warehouses = await apiGet<Warehouse[]>('/api/inventory/warehouses')

  let data: PageResponse<StockBalance> | null = null
  if (warehouseId) {
    data = await apiGetPage<StockBalance>(
      `/api/inventory/stocks/by-warehouse?warehouseId=${warehouseId}&page=${page}&size=${size}`,
    )
  }

  return (
    <StocksClient
      warehouses={Array.isArray(warehouses) ? warehouses : []}
      warehouseId={warehouseId}
      data={data}
    />
  )
}
