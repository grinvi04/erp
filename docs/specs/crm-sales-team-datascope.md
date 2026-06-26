# crm-sales-team-datascope 스펙·플랜

## Context
`DataScope`(ALL/DEPARTMENT/SELF)는 HR에만 적용돼 있고 CRM은 권한만 있으면 테넌트 내 전 영업데이터를 조회 가능 — auth-standards의 "Permission + DataScope 이중구조"와 불일치. CRM 엔티티(Account·Lead·Opportunity·Activity)는 `owner_id`를 보유하나, **DEPARTMENT(팀) 스코프**를 구현하려면 "사용자→팀→팀원" 매핑이 필요한데 부서는 HR 개념이라 모듈 경계상 CRM이 직접 참조 불가. → **CRM 자체 팀(영업조직) 모델을 신설**해 DEPARTMENT 스코프를 정의한다.

## 1. 목표 & Why
CRM에 `SalesTeam`(영업팀)·`SalesTeamMember`(팀원=사용자) 모델을 신설하고, `CrmDataScopeResolver`로 owner 기반 데이터 스코프를 CRM 조회에 적용한다. **성공 기준: SELF=본인 owner만, DEPARTMENT=같은 팀 팀원들의 owner 데이터, ALL=전체. 4개 CRM 조회(Lead·Opportunity·Account·Activity)에 균일 적용. 기존 CRM 테스트 green.**

## 2. Scope
- **In:** SalesTeam·SalesTeamMember 엔티티+V4003 마이그레이션, SalesTeam 관리 API(CRUD+멤버 add/remove), CrmDataScopeResolver, 4개 search 쿼리에 owner 스코프 필터, 스코프 통합 테스트, 팀 관리 프론트 화면.
- **Out:** HR DataScope 변경 없음. 다른 모듈(finance·inventory) 스코프는 범위 밖. CRM 권한코드 의미 변경 없음.

## 3. 수용기준
- **AC-1 (SELF):** data_scope=SELF 사용자는 Lead/Opp/Account/Activity 조회 시 owner_id=본인 것만.
- **AC-2 (DEPARTMENT):** data_scope=DEPARTMENT 사용자는 자신이 속한 팀(들)의 모든 팀원 owner 데이터. 팀 미소속이면 본인 것만.
- **AC-3 (ALL):** data_scope=ALL(미설정 포함)은 narrowing 없음(테넌트 전체).
- **AC-4 (팀 관리):** SalesTeam CRUD + 멤버 추가/제거 API가 iam:write(또는 crm 관리 권한)로 동작, 멤버 (team,userId) 유일.
- **AC-5 (회귀):** 기존 CRM 검색/생성/수정 동작·테스트 green. 스코프는 조회만 좁히고 쓰기/권한은 불변.

## 4. 제약
- forward-only V4003. 모듈 경계: CRM은 HR/common 직접 참조 금지(DataScope·CurrentUserProvider 등 common 공유 타입만 사용).
- owner_id 세팅 방식(생성 시 클라이언트 지정)은 이번 변경 안 함.

## 5. 경계 / Do-Not
- ✅: SalesTeam 내부 설계, CrmDataScopeResolver, search 쿼리 owner 필터(scoped 플래그 패턴).
- ⚠️: owner 자동배정(현재 클라이언트 지정 유지), 팀 계층(중첩 팀)은 범위 밖(평면 팀).
- 🚫: HR DataScope 로직 변경, 권한코드 의미 변경.

## 6. Open Questions
없음 — 평면 팀, DEPARTMENT=소속 팀들의 팀원 합집합, 미소속=SELF로 확정.

## 7. 기술 접근 (HOW)
- **SalesTeam**(crm/domain/model, BaseEntity): `code`(테넌트 내 유일), `name`. **SalesTeamMember**(BaseEntity): `@ManyToOne SalesTeam team`, `userId`(사용자 sub). `UNIQUE(team_id, user_id)`.
- **CrmDataScopeResolver**(crm/application/service): `OwnerScope ownerScope()` 반환(record `OwnerScope(boolean scoped, Set<String> ownerIds)`):
  - ALL → `new OwnerScope(false, null)`
  - SELF → `(true, {currentUserId})`
  - DEPARTMENT → 현재 사용자가 속한 팀들의 전 멤버 userId 합집합(+본인). 미소속 → `(true, {currentUserId})`.
- **search 쿼리**(Lead·Opportunity·Account·Activity Repository): 파라미터 `boolean scoped`, `Collection<String> ownerIds` 추가, WHERE에 `(:scoped = false OR x.ownerId IN :ownerIds)`. 서비스가 resolver로 OwnerScope 구해 전달.
- **SalesTeam 관리**: SalesTeamController(`/api/crm/sales-teams`) + Service + DTO(Create/Update/Response, member add/remove). 권한 `iam:write`(역할관리와 동일 관리권한) 또는 신규 `crm:team:manage` — 기존 권한 재사용(iam:write)로 최소화.
- **V4003**(crm): `crm.sales_team`, `crm.sales_team_member` 테이블(공통 감사컬럼 — BaseEntity 표준).

**영향 파일**: 신규 SalesTeam·SalesTeamMember·각 Repository·CrmDataScopeResolver·SalesTeamService·SalesTeamController·DTO·V4003. 수정: Lead/Opportunity/CrmAccount/Activity Repository(쿼리)·Service(스코프 전달). 프론트: crm/sales-teams 화면.

**테스트**: CrmDataScopeResolver 단위(ALL/SELF/DEPARTMENT 분기) + 통합(스코프별 조회 격리 + 팀 미소속 SELF) + SalesTeam CRUD/멤버 + 기존 CRM 테스트 green.

## 8. 태스크
| # | 태스크 | AC | 검증 |
|---|---|---|---|
| 1 | SalesTeam·Member 엔티티+V4003+Repository | AC-4 | ./gradlew check |
| 2 | SalesTeam 관리 Service·Controller·DTO + 테스트 | AC-4 | 〃 |
| 3 | CrmDataScopeResolver + 4개 search 쿼리 스코프 적용 + 서비스 전달 | AC-1,2,3,5 | 〃 |
| 4 | 스코프 통합 테스트(SELF/DEPARTMENT/ALL 격리) | AC-1,2,3 | 〃 |
| 5 | 프론트 팀 관리 화면 | AC-4 | type-check·build |

## 검증
`cd backend && ./gradlew check` + `cd frontend && npm run type-check && npm run build`. 브랜치 feature/crm-sales-team-datascope → /feature-merge.
