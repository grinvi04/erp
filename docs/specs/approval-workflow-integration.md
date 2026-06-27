# approval-workflow-integration 스펙·플랜

## Context (왜)
GL 전표 전기(post)·재고 이동 확정(confirm)이 **결재 없이 즉시 실행**된다 — 작성자가 본인 전표를 바로 원장에 올리고, 재고 조정을 바로 반영(직무분리 공백). 공통 결재 엔진(`common/workflow` ApprovalRequest)은 이미 있고 AP인보이스가 쓰고 있으나(submit→ApprovalRequest, approve→전결한도+작성자차단, `PendingApprovalContributor`로 결재함 라우팅, `/approvals` 페이지), GL·재고엔 미연결. **AP인보이스 패턴을 복제**해 두 액션에 결재선을 끼운다.

**확정된 제품 결정**: ① GL 전기 = **항상 결재**(AP인보이스 동일). ② 재고 확정 = **ADJUSTMENT 이동만 결재**(입출고·이동은 직접 확정 유지 — 빈번). ③ 결재 권한 = **전용 신설 + 백필**(#65 pay권한 패턴: write 보유 역할에 마이그레이션 자동부여, 회귀 0).

---

## 1. 목표 & Why
GL 전기·재고 조정에 작성자≠결재자 직무분리를 결재선으로 강제. **성공 기준: GL은 DRAFT→submit→PENDING_APPROVAL→approve→POSTED만 가능(직접 POST 불가), 재고 ADJUSTMENT는 PENDING_APPROVAL 거쳐 CONFIRMED, 결재함(`/approvals`)에 노출, 작성자 본인 결재·전결한도 위반 차단.**

## 2. Scope
- **In:**
  - **GL 전기 결재(항상)**: `JournalEntryStatus`에 `PENDING_APPROVAL` 추가. `submitForApproval`(FINANCE_WRITE, 차대변균형·회계기간 검증 후 DRAFT→PENDING_APPROVAL + `ApprovalRequest(entityType="GL_ENTRY", "@role:finance:gl:approve")`) → `approve`(FINANCE_GL_APPROVE, 작성자차단 + 전결한도(totalDebit) + `approvalRequest.approve()` + `entry.post()`). `GlEntryApprovalInboxContributor`로 결재함 기여.
  - **재고 ADJUSTMENT 결재**: `MovementStatus`에 `PENDING_APPROVAL` 추가. ADJUSTMENT 이동만 `submitForApproval`(INVENTORY_WRITE, DRAFT→PENDING_APPROVAL + `ApprovalRequest(entityType="STOCK_MOVEMENT")`) → `approve`(INVENTORY_MOVEMENT_APPROVE, 작성자차단 + Stock 반영 + `approvalRequest.approve()`). 비-ADJUSTMENT는 기존 `confirm`(직접 DRAFT→CONFIRMED) 유지. `StockMovementApprovalInboxContributor`.
  - **전용 권한**: `FINANCE_GL_APPROVE="finance:gl:approve"`, `INVENTORY_MOVEMENT_APPROVE="inventory:movement:approve"` 신설 + `all()` 등록 + **백필 마이그레이션**(finance:write→gl:approve, inventory:write→movement:approve, NOT EXISTS 멱등).
  - **DB**: journal_entry·movement에 `approval_request_id BIGINT` 추가(+인덱스).
  - **프론트**: journal-entries 화면 "전기"→"결재상신"(DRAFT), PENDING_APPROVAL 상태/배지. movements 화면 ADJUSTMENT는 "확정"→"결재상신". `/approvals` 결재함에 GL_ENTRY·STOCK_MOVEMENT entityType 매핑·승인 액션 연결.
- **Out (Non-goals):**
  - 임계값 기반 트리거(테넌트 결재임계 설정) — 채택 안 함(항상/유형 기반).
  - 다단계 결재선(1단계만, AP인보이스 동일).
  - REVERSED(GL 역분개)·CANCELLED 흐름 변경, 비-ADJUSTMENT 재고 결재.
  - FX·analytics(별도).
  - 결재 반려(REJECT) UI 신규 — 엔진 reject는 재사용하되 UI는 기존 `/approvals` 범위.

## 3. 기능 요구사항 + 수용기준 (테스트 계약)
- **AC-1 (GL 상신, 정상):** WHEN DRAFT 전표 `submitForApproval`, the system SHALL 차대변 균형·회계기간 open 검증 후 PENDING_APPROVAL로 전이하고 ApprovalRequest(GL_ENTRY) 생성·링크.
- **AC-2 (GL 상신 검증, 예외):** IF 불균형 또는 회계기간 closed THEN 기존 에러코드(JOURNAL_ENTRY_NOT_BALANCED/FISCAL_PERIOD_CLOSED)로 거부, 상태 불변.
- **AC-3 (GL 승인→전기, 정상):** WHEN PENDING_APPROVAL 전표 `approve`(FINANCE_GL_APPROVE 보유, 작성자≠결재자, totalDebit≤전결한도), the system SHALL POSTED 전이 + ApprovalRequest APPROVED.
- **AC-4 (GL 직무분리, 예외):** IF 결재자=작성자 THEN APPROVER_NOT_AUTHORIZED(403).
- **AC-5 (GL 전결한도, 예외):** IF totalDebit>전결한도 THEN APPROVAL_LIMIT_EXCEEDED(403).
- **AC-6 (GL 직접전기 차단, 경계):** WHILE 결재 도입 후, 직접 DRAFT→POSTED 경로 SHALL 불가(상태 가드: post는 PENDING_APPROVAL에서만).
- **AC-7 (GL 권한, 예외):** IF FINANCE_GL_APPROVE 없음 THEN approve 403.
- **AC-8 (GL 결재함, 정상):** PENDING_APPROVAL 전표가 FINANCE_GL_APPROVE 보유·totalDebit≤한도·작성자≠본인인 사용자의 `/approvals` 대기목록에 노출.
- **AC-9 (재고 ADJUSTMENT 상신, 정상):** WHEN ADJUSTMENT DRAFT `submitForApproval`, PENDING_APPROVAL + ApprovalRequest(STOCK_MOVEMENT).
- **AC-10 (재고 승인→확정, 정상):** WHEN PENDING_APPROVAL ADJUSTMENT `approve`(INVENTORY_MOVEMENT_APPROVE, 작성자≠결재자), CONFIRMED 전이 + Stock 증감 반영 + ApprovalRequest APPROVED.
- **AC-11 (재고 비-ADJUSTMENT, 경계):** WHILE 이동유형 RECEIPT/ISSUE/TRANSFER/RETURN, 기존 `confirm`(직접 DRAFT→CONFIRMED) 동작 불변(결재 미적용).
- **AC-12 (재고 ADJUSTMENT 직접확정 차단, 경계):** IF ADJUSTMENT를 직접 `confirm` 시도 THEN 거부(결재 경유 강제).
- **AC-13 (재고 직무분리·권한, 예외):** IF 결재자=작성자 THEN APPROVER_NOT_AUTHORIZED; IF INVENTORY_MOVEMENT_APPROVE 없음 THEN 403.
- **AC-14 (권한 백필, 정상):** 마이그레이션 후 finance:write 보유 역할은 finance:gl:approve, inventory:write 보유 역할은 inventory:movement:approve 보유(중복 없음).
- **AC-15 (프론트, 정상):** journal-entries·movements(ADJUSTMENT) 화면이 "결재상신" 액션·PENDING_APPROVAL 배지 표시, `/approvals`에서 GL_ENTRY·STOCK_MOVEMENT 승인 가능.

## 4. 제약/비기능
- 자동 생성 GL 분개(AP/AR 결제·승인 시 `apInvoicePostingService` 등)는 **DRAFT로 생성**되어 동일 GL 결재를 거쳐야 POSTED — **자동 POST 경로가 있으면 DRAFT 생성으로 조정**(구현 시 확인). AP/AR 자체 결재흐름은 불변.
- forward-only 마이그레이션, 기존 릴리즈 마이그레이션 무수정.

## 5. 경계 / Do-Not
- ✅ 해도 됨: AP인보이스 결재 패턴 복제, 공통 ApprovalRequest 직접 사용, 새 권한·백필, 상태 enum 확장, 프론트 액션 변경.
- ⚠️ 먼저 물어봐: 결재 단수→다단계 확장, 재고에 전결한도(금액) 적용 여부, 비-ADJUSTMENT 결재 확대, 임계값 설정 도입.
- 🚫 절대 금지: finance/inventory에 ApprovalRequest **복제 생성**(공통 엔진만), 결재 우회 경로 잔존(직접 POST/confirm), AP/AR 기존 결재 동작 변경, 기존 마이그레이션 수정.

## 7. 기술 접근 (HOW)
**패턴(AP인보이스 복제 — 참고 파일):**
- `ApInvoiceService.submit/approve`(ApprovalRequest 생성·`@role:` step·`approvalRequest.approve()`), `ApInvoiceApprovalInboxContributor`(권한+전결한도+작성자차단 필터), `ApprovalAuthorityProvider.getApprovalLimit()`, `common/workflow/ApprovalRequest`·`ApprovalStep`·`ApprovalRequestRepository`·`PendingApprovalContributor`.
- 상태머신: `JournalEntry`/`Movement` 도메인에 `submitForApproval()`·가드 추가, `post()`/`confirm()`은 PENDING_APPROVAL에서만(또는 approve 경로 내부). 도메인 불변식은 도메인 메서드에.
- 권한: `Permission`에 2개 상수+`all()`, 백필 마이그레이션(V0005 common 또는 모듈별 — #65 V0004 패턴: `INSERT ... SELECT ... WHERE permission_code='finance:write' AND NOT EXISTS(...)`).
- 엔티티 링크: journal_entry/movement에 `approval_request_id` 컬럼+`linkApprovalRequest()`(ApInvoice.linkApprovalRequest 동형). 마이그레이션 2xxx(finance)·3xxx(inventory).
- 결재함 기여: `GlEntryApprovalInboxContributor`·`StockMovementApprovalInboxContributor` implements `PendingApprovalContributor`(GL은 전결한도 필터, 재고는 권한+작성자차단). entityType "GL_ENTRY"/"STOCK_MOVEMENT".
- 프론트: `finance/journal-entries`·`inventory/movements` client 액션 라벨·상태배지, `frontend/src/types/approval.ts`·`/approvals` 페이지 entityType 라벨 매핑·승인 호출(기존 AP_INVOICE 처리 재사용).

**테스트 전략(AC↔테스트):** 백엔드 통합/단위(`AbstractIntegrationTest`/Mockito): AC-1~13 상태전이·검증·직무분리·전결한도·권한403·비ADJUSTMENT불변·직접경로차단, AC-14 백필(flyway+조회), AC-8 결재함 노출. 프론트 표시는 type-check+build.

## 8. 태스크 (test-first, 모듈별 2 PR)
### PR1 — feature/gl-approval (GL 전기 결재)
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 1 | 권한 FINANCE_GL_APPROVE 신설+all()+백필 마이그레이션, journal_entry approval_request_id 컬럼 | AC-14 | Permission, db/migration(2xxx·common) | `./gradlew check` | — |
| 2 | JournalEntry 상태 PENDING_APPROVAL + submitForApproval/post 가드(도메인) | AC-1,2,6 | JournalEntry, JournalEntryStatus | `./gradlew check` | #1 |
| 3 | JournalEntryService submitForApproval/approve(ApprovalRequest 생성·전결한도·작성자차단) + 컨트롤러 | AC-1~7 | JournalEntryService, Controller | `./gradlew check` | #2 |
| 4 | GlEntryApprovalInboxContributor + 자동분개 DRAFT 확인/조정 | AC-8, 제약 | contributor, (AP/AR posting 확인) | `./gradlew check` | #3 |
| 5 | 프론트 journal-entries 결재상신·배지 + /approvals GL_ENTRY 매핑 | AC-15 | journal-entries-client, approvals, types | `type-check && build` | #1~4 |

### PR2 — feature/stock-adjustment-approval (재고 조정 결재)
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 6 | 권한 INVENTORY_MOVEMENT_APPROVE 신설+all()+백필, movement approval_request_id 컬럼 | AC-14 | Permission, db/migration(3xxx·common) | `./gradlew check` | — |
| 7 | Movement 상태 PENDING_APPROVAL + submitForApproval(도메인, ADJUSTMENT 가드) | AC-9,12 | Movement, MovementStatus | `./gradlew check` | #6 |
| 8 | MovementService submitForApproval/approve(Stock 반영은 approve에서) + confirm은 비-ADJUSTMENT만 + 컨트롤러 | AC-9~13 | MovementService, Controller | `./gradlew check` | #7 |
| 9 | StockMovementApprovalInboxContributor | AC-13 | contributor | `./gradlew check` | #8 |
| 10 | 프론트 movements ADJUSTMENT 결재상신 + /approvals STOCK_MOVEMENT 매핑 | AC-15 | movements-client, approvals, types | `type-check && build` | #6~9 |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 신규 결재 통합테스트 green + 회귀 없음(기존 AP인보이스 결재·비-ADJUSTMENT confirm·기존 post 호출처 무영향).
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): GL DRAFT 상신→다른 사용자로 `/approvals` 승인→POSTED 확인. ADJUSTMENT 동일. 작성자 본인 결재 차단·전결한도 초과 차단 확인. RECEIPT는 직접 확정 유지.
4. 게이트: 모듈별 `/feature-add`(태스크 TDD·원자 커밋) → `/feature-merge`(focused 리뷰·CI). PR1(GL)→PR2(재고).
