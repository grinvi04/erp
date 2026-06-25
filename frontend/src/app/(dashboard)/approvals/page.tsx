import { apiGet } from '@/lib/api'
import type { ApprovalSummary } from '@/types/approval'
import ApprovalsClient from './approvals-client'

export const metadata = { title: '결재함 | ERP' }

async function safeList(path: string): Promise<ApprovalSummary[]> {
  try {
    return await apiGet<ApprovalSummary[]>(path)
  } catch {
    return []
  }
}

export default async function ApprovalsPage() {
  const [pending, mine] = await Promise.all([
    safeList('/api/approvals/pending'),
    safeList('/api/approvals/mine'),
  ])
  return <ApprovalsClient pending={pending} mine={mine} />
}
