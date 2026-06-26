import { apiGet } from '@/lib/api'
import type { LeavePolicy } from '@/types/hr'
import LeavePoliciesClient from './leave-policies-client'

export const metadata = { title: '휴가 정책 | ERP' }

export default async function LeavePoliciesPage() {
  const policies = await apiGet<LeavePolicy[]>('/api/hr/leave-policies')
  return <LeavePoliciesClient policies={Array.isArray(policies) ? policies : []} />
}
