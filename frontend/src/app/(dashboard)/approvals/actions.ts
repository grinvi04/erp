'use server'
import { apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'

// 결재함에서 직접 승인/반려 — entityType별로 해당 모듈 엔드포인트로 디스패치한다.
// 실제 상태 전이·권한 검증은 각 모듈 서비스가 소유한다(모듈 경계 유지).
// 인라인 승인 지원: LEAVE_REQUEST. GL_ENTRY·STOCK_MOVEMENT 승인은 전결한도·전기 등
// 추가 판단이 필요해 엔티티 화면에서 처리(링크 이동) — 인박스에서는 반려만 인라인 지원한다.
// 인라인 반려 지원: LEAVE_REQUEST·GL_ENTRY·STOCK_MOVEMENT. AP_INVOICE는 링크 이동만.

export async function approveInboxItem(
  entityType: string,
  entityId: number,
  comment: string,
): Promise<void> {
  if (entityType === 'LEAVE_REQUEST') {
    await apiPost(`/api/hr/leave-requests/${entityId}/approve`, { comment })
  } else {
    throw new Error('인박스 인라인 승인을 지원하지 않는 결재 유형입니다')
  }
  revalidatePath('/approvals')
}

export async function rejectInboxItem(
  entityType: string,
  entityId: number,
  comment: string,
): Promise<void> {
  let endpoint: string
  switch (entityType) {
    case 'LEAVE_REQUEST':
      endpoint = `/api/hr/leave-requests/${entityId}/reject`
      break
    case 'GL_ENTRY':
      endpoint = `/api/finance/journal-entries/${entityId}/reject`
      break
    case 'STOCK_MOVEMENT':
      endpoint = `/api/inventory/movements/${entityId}/reject`
      break
    default:
      throw new Error('인박스 인라인 반려를 지원하지 않는 결재 유형입니다')
  }
  await apiPost(endpoint, { comment })
  revalidatePath('/approvals')
}
