import { apiGet } from '@/lib/api'
import type { Warehouse } from '@/types/inventory'
import WarehousesClient from './warehouses-client'

export const metadata = { title: '창고 관리 | ERP' }

export default async function WarehousesPage() {
  const warehouses = await apiGet<Warehouse[]>('/api/inventory/warehouses')
  return <WarehousesClient warehouses={warehouses} />
}
