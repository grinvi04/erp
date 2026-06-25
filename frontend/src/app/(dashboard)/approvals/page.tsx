import { apiGet } from '@/lib/api'
import type { ApprovalSummary } from '@/types/approval'
import ApprovalsClient from './approvals-client'

export const metadata = { title: '결재함 | ERP' }

interface ListResult {
  rows: ApprovalSummary[]
  failed: boolean
}

// 결재함은 false-empty(오류를 빈 목록으로 표시)가 위험하므로 — 결재자가 처리할 건이
// 없다고 오인 — 오류와 빈 목록을 구분해 전달한다.
async function loadList(path: string): Promise<ListResult> {
  try {
    return { rows: await apiGet<ApprovalSummary[]>(path), failed: false }
  } catch (e) {
    console.error(`결재함 조회 실패: ${path}`, e)
    return { rows: [], failed: true }
  }
}

export default async function ApprovalsPage() {
  const [pending, mine] = await Promise.all([
    loadList('/api/approvals/pending'),
    loadList('/api/approvals/mine'),
  ])
  return (
    <ApprovalsClient
      pending={pending.rows} pendingFailed={pending.failed}
      mine={mine.rows} mineFailed={mine.failed}
    />
  )
}
