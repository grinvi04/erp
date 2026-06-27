package com.erp.common.workflow;

import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import java.util.List;

/**
 * 통합 결재함에 모듈별 '현재 사용자가 결재할 수 있는 대기 건'을 기여하는 포트(SPI).
 *
 * <p>역할·금액 한도 기반 결재(예: AP 전표 전결규정)는 사람 단위 결재선(person-assigned {@code ApprovalStep})으로 표현되지 않으므로,
 * {@code findPendingForApprover}(결재자=나) 쿼리로는 라우팅되지 않는다. 각 모듈이 자기 도메인 규칙으로 대기 목록을 산출해 결재함에 제공한다 —
 * 결재함(common)은 모듈 규칙을 알 필요가 없다(모듈 경계 유지).
 */
public interface PendingApprovalContributor {

  /** 현재 인증 사용자가 자기 도메인 규칙상 결재할 수 있는 대기 결재 요약 목록. */
  List<ApprovalSummaryResponse> pendingForCurrentUser();
}
