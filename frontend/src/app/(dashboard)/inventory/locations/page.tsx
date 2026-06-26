import { apiGet } from '@/lib/api'
import type { Warehouse, Location } from '@/types/inventory'
import LocationsClient from './locations-client'

export const metadata = { title: '로케이션 관리 | ERP' }

export default async function LocationsPage(props: {
  searchParams: Promise<{ warehouseId?: string }>
}) {
  const sp = await props.searchParams
  const warehouses = await apiGet<Warehouse[]>('/api/inventory/warehouses')
  const list = Array.isArray(warehouses) ? warehouses : []
  const selectedWarehouseId = sp.warehouseId ?? (list[0] ? String(list[0].id) : '')

  const locations = selectedWarehouseId
    ? await apiGet<Location[]>(`/api/inventory/locations?warehouseId=${selectedWarehouseId}`)
    : []

  return (
    <LocationsClient
      warehouses={list}
      selectedWarehouseId={selectedWarehouseId}
      locations={Array.isArray(locations) ? locations : []}
    />
  )
}
