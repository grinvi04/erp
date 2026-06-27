import { apiGet } from '@/lib/api'
import type { Uom } from '@/types/inventory'
import UomsClient from './uoms-client'

export const metadata = { title: '단위 관리 | ERP' }

export default async function UomsPage() {
  const uoms = await apiGet<Uom[]>('/api/inventory/uoms')
  return <UomsClient uoms={Array.isArray(uoms) ? uoms : []} />
}
