'use server'
import { apiPost } from '@/lib/api'
import { revalidatePath } from 'next/cache'

// 결재함에서 직접 승인/반려 — entityType별로 해당 모듈 엔드포인트로 디스패치한다.
// 실제 상태 전이·권한 검증은 각 모듈 서비스가 소유한다(모듈 경계 유지).
// 현재 인라인 처리 지원: LEAVE_REQUEST(승인+반려). AP_INVOICE는 전용 결재자
// 모델 부재로 링크 이동만 — 인박스 인라인 대상 아님.

export async function approveInboxItem(
  entityType: string, entityId: number, comment: string,
): Promise<void> {
  if (entityType === 'LEAVE_REQUEST') {
    await apiPost(`/api/hr/leave-requests/${entityId}/approve`, { comment })
  } else {
    throw new Error('인박스 인라인 승인을 지원하지 않는 결재 유형입니다')
  }
  revalidatePath('/approvals')
}

export async function rejectInboxItem(
  entityType: string, entityId: number, comment: string,
): Promise<void> {
  if (entityType === 'LEAVE_REQUEST') {
    await apiPost(`/api/hr/leave-requests/${entityId}/reject`, { comment })
  } else {
    throw new Error('인박스 인라인 반려를 지원하지 않는 결재 유형입니다')
  }
  revalidatePath('/approvals')
}
