import { apiGet } from '@/lib/api'
import type { Position } from '@/types/hr'
import PositionsClient from './positions-client'

export const metadata = { title: '직위 관리 | ERP' }

export default async function PositionsPage() {
  const positions = await apiGet<Position[]>('/api/hr/positions')
  return <PositionsClient positions={Array.isArray(positions) ? positions : []} />
}
