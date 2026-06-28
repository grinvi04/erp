package com.erp.common.workflow;

public enum ApprovalStatus {
  PENDING, // 결재 대기
  APPROVED, // 승인 완료
  REJECTED, // 반려
  CANCELLED, // 취소
  RETURNED // 반송 (수정 요청)
}
