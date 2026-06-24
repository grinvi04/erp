import { apiGet, apiGetPage } from '@/lib/api'
import type { Movement, Item, Warehouse } from '@/types/inventory'
import type { PageResponse } from '@/types/api'
import MovementsClient from './movements-client'

export const metadata = { title: '재고 이동 | ERP' }

export default async function MovementsPage(props: {
  searchParams: Promise<{ page?: string; size?: string; type?: string; status?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const typeFilter = sp.type ? `&type=${sp.type}` : ''
  const statusFilter = sp.status ? `&status=${sp.status}` : ''

  const [data, items, warehouses] = await Promise.all([
    apiGetPage<Movement>(
      `/api/inventory/movements?page=${page}&size=${size}${typeFilter}${statusFilter}`
    ),
    apiGetPage<Item>('/api/inventory/items?size=1000'),
    apiGet<Warehouse[]>('/api/inventory/warehouses'),
  ])

  return (
    <MovementsClient
      data={data as PageResponse<Movement>}
      items={(items as PageResponse<Item>).content}
      warehouses={warehouses}
    />
  )
}
