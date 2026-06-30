# ERP 설계 결정 기록 (Decision Log)

확정된 아키텍처·도메인 결정의 단일 출처. AI 작업 노트(로컬 메모리)에 흩어져 있던 것을 repo로 이전(가시·공유·git).

---

## 인가(Authorization) — 기능 권한(RBAC) + 데이터 스코프 + 전결

### 모델 (정본 = 앱 DB, 인증만 Keycloak)
- **Keycloak = 인증만**(`sub`·`tenant_id`). **인가 정본 = 앱 DB**로 전면 전환(PR #32·#33·#34).
- `Role` + `role_permission` · `UserRole` · `UserAccessProfile`(data_scope·department_id·approval_limit).
  - audit_log처럼 `@TenantId` 비대상 + `tenant_id` 명시 필터(JWT 인증 단계라 TenantContext 없음).
- `AuthorizationResolver`: 권한코드 union + 접근프로파일. `ur.tenantId` + `r.tenantId` **둘 다 검증**(cross-tenant 누출 차단).
- `JwtAuthoritiesConverter`: DB 해석. **스프링 빈 아님** — Converter 빈이면 `@WebMvcTest` 오염되므로 SecurityConfig가 직접 생성.

### 기능 권한 (RBAC)
- 권한 코드 `{모듈}:{리소스}:{액션}` 상수(`Permission`), `PermissionChecker.require(code)`를 **application 유스케이스(service) 진입점**에서 호출(없으면 FORBIDDEN). **서버 검사가 최종.**
- 프론트 게이팅: `GET /api/me/permissions` → `PermissionsProvider` 컨텍스트 → `usePermissions().can()`.
- 적용: HR은 리소스별(hr:employee/department/position/jobgrade/leave, #22). Finance/Inventory/CRM은 모듈별 coarse(read/write, #23·#24). 휴가 결재는 도메인 규칙(매니저=currentStepApproverId).
- 관리: `IamService` + `/api/iam/*`(역할 CRUD·배정·접근프로파일, `iam:read/write`, 변경 전부 감사). `Permission.all()` 카탈로그(동기화 리플렉션 테스트). `IamBootstrap`(`erp.iam.bootstrap.admin-sub`→SUPER_ADMIN 멱등). 프론트 `/iam`.

### 데이터 스코프 (DataScope)
- `common/security` DataScope(ALL/DEPARTMENT/SELF) + DataScopeProvider(JWT data_scope·department_id, 미설정=ALL).
- `hr/HrDataScopeResolver`(부서 트리 BFS, 공통 Specification). EmployeeService + **형제 직원데이터 경로 전부**(Contract·LeaveBalance·LeaveRequest)에 적용 — 코드리뷰가 잡은 4개 우회 누출 차단(PR #27).
- 남음: Finance/Inventory/CRM DataScope(CRM=ownerId, Finance/Inventory는 대개 전사라 한정적), Hibernate @Filter 공통화, coarse→granular(선택).

### 전결 규정 (AP 전표 결재)
- 한국 전자결재 위임전결 자료조사 반영 3중 통제(PR #29): ①전결권 `finance:invoice:approve`(작성권과 분리) ②전결 한도 `ApprovalAuthorityProvider`(JWT `approval_limit` < 금액 → APPROVAL_LIMIT_EXCEEDED, 미설정=0 fail-closed) ③직무분리(작성자≠결재자).
- 모듈 경계상 finance가 HR 조직도 walk 불가 → 사람단위 결재선 대신 **금액 기반 한도(claim) 모델**.
- 결재함 라우팅(PR #30): `common/workflow` `PendingApprovalContributor` 포트(SPI) — 모듈이 역할/한도 기반 대기건 기여. `ApprovalInboxService` = person-assigned + 기여분 합산. `submit()`은 결재자를 역할 sentinel(`@role:...`)로 둬 오노출 차단.
- 미완: 다단계 결재선(담당→부장→임원) 에스컬레이션, 전결권/한도 DB 관리(현재 Keycloak claim).

### 감사 로그 (Audit)
- `common/audit` `AuditService.record()`(호출자 트랜잭션 내 **fail-closed** 기록, 테넌트=TenantContext·수행자=sub) + search(`audit:read` 게이팅). `GET /api/audit/logs`, 프론트 `/audit`. AuditAction에 APPROVE/REJECT.
- Wiring: 휴가 approve/reject·AP approve(#28), **HR 직원 생명주기 7개 쓰기 전부**(create/update/transfer/promote/terminate/onLeave/returnFromLeave, #31, afterData 이벤트 태그). **AOP 애스펙트 미채택**(advisor 순서 모호성으로 fail-closed 깨질 위험 → 명시적 호출).

> 테스트: Mockito 단위는 `@Mock PermissionChecker`/`@Mock Resolver`. 서비스 통합테스트는 claim 대신 `UserAccessProfile` 시드 + authority를 SecurityContext 직접 주입.

---

## AP/AR 전표 → GL 자동 분개

### 확정 설계 (실무 표준 — SAP reconciliation account / 한국 ERP 거래처별 채무계정 검증)
- **대변 외상매입금은 전표가 아니라 공급업체 마스터의 통제계정에서 온다**(`Vendor.payablesAccount`). AP 보조원장 ↔ GL 통제계정 일치, 통제계정은 보조원장 통해서만 전기. (당초 "전표마다 대변 계정 선택" 안은 실무 불일치라 폐기.)
- **차변은 라인별 계정**(`ApInvoiceLine`: 비용/자산·부가세대급금).
- 분개: (차) 각 라인 계정 / (대) 공급업체 외상매입금 총액. 승인 시 **DRAFT** 생성(전기는 사용자 선택), GL→AP 역참조.
- 승인 = 분개 생성이 한 트랜잭션(SAP invoice posting = FI document). 회계기간 닫혀있으면 승인도 롤백(실무 정합).

### 구현
- AP(PR #35 백엔드·#36 프론트, #2 완료): `ApInvoicePostingService.postDraft`(전표일 OPEN 회계기간). `JournalEntryService.createInternal`(권한게이트 없는 내부생성 — 결재승인이 인가, package-private). 라인·통제계정 없으면 전기 생략(additive). 부가세 자동 split(PR #40, 프론트 10% 자동·수정가능, 서버는 실제 세액이 정본).
- AR(PR #42·#43): AP 대칭 미러. `(차) 외상매출금[Customer.receivablesAccount] / (대) 매출·부가세예수금[라인]`, JournalEntryType.AR. 권한·전결규정·한도·결재함 기여자 전부 AP와 동일 재사용. **AP/AR 양변 장부 가능.**
- 부동소수 회피: 라인 합계는 정수 전 단위 합산 / 금액은 소수 2자리 반올림 전송(서버 BigDecimal 검증과 일치).

---

## 인프라/운영
- branch protection: 솔로 작업 기간 일시 제거했었으나 현재 develop·main 모두 재적용(required checks: backend·frontend·secret-scan·test-guard·migration-safety, review 1). **실제 설정은 GitHub repo 상태가 정본** — 이 문서는 결정 근거만.
