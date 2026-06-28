import { apiGet } from '@/lib/api'
import { resolveUserNames } from '@/lib/users'
import type { ApprovalSummary } from '@/types/approval'
import ApprovalsClient from './approvals-client'

export const metadata = { title: '결재함 | ERP' }
// 인증 데이터(헤더/쿠키)를 서버에서 조회하므로 본질적으로 동적 — 빌드 타임 정적 프리렌더
// 시도(세션 없이 fetch 실패 로그)를 건너뛰도록 명시한다.
export const dynamic = 'force-dynamic'

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
  const names = await resolveUserNames(
    [...pending.rows, ...mine.rows].flatMap((a) => [a.requesterId, a.currentApproverId]),
  )
  return (
    <ApprovalsClient
      pending={pending.rows}
      pendingFailed={pending.failed}
      mine={mine.rows}
      mineFailed={mine.failed}
      names={names}
    />
  )
}
