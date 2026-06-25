# role-permission-tenant 스펙·플랜

## Context

`release-check`(v0.2.0 릴리즈 전 검증)의 마이그레이션 게이트가 **`common.role_permission` 테이블에 `tenant_id` 부재**를 ❌로 잡았다. 당시 게이트를 임의로 덮어쓰고 릴리즈를 진행했으나(하네스 우회), 이를 철회하고 **엄격 하네스 플로우(/plan→/feature-add→/feature-merge)의 정식 수정**으로 처리한다.

`role_permission`은 인가(권한) 데이터다. 현재 격리는 **앱 쿼리 레벨**(`findPermissionCodes`가 `ur.tenantId`+`r.tenantId` 검증)에만 의존하고, 테이블 자체엔 `tenant_id`가 없어 **DB 레벨 방어가 없다**. 고민감 권한 데이터엔 심층 방어가 정당하다.

## 1. 목표 & Why
`role_permission`을 `@ElementCollection<String>`에서 **`tenant_id`를 가진 `RolePermission` 엔티티**로 전환해 DB 레벨 cross-tenant 격리를 추가한다. **성공 기준: role_permission 행이 부모 역할의 tenant_id를 보유하고, 권한 해석 쿼리가 `p.tenantId`로도 필터링하며, 기존 인가 테스트 전부 green.**

## 2. Scope
- **In:** `RolePermission` 엔티티 신설, `Role.permissions` 매핑 전환, `findPermissionCodes`에 `p.tenantId` 추가, V0003 마이그레이션(컬럼 추가·backfill·PK 전환), 신규 DB-격리 테스트.
- **Out:** 다른 authz 테이블 변경 없음. 공개 API/DTO(`RoleResponse.permissions`는 계속 `Set<String>`)·관리화면·권한코드 의미 불변. approval_step 등 다른 자식 테이블은 범위 밖.

## 3. 수용기준
- **AC-1 (정상):** 역할에 권한 grant·저장 시, 각 `role_permission` 행에 부모 역할의 `tenant_id`가 채워져 영속화.
- **AC-2 (회귀):** `Role.getPermissions()`·`AuthorizationResolver.permissionCodes()`는 여전히 `Set<String>` 반환 — 기존 동작 보존(기존 테스트 green).
- **AC-3 (격리):** 테넌트1 사용자의 user_role이 테넌트2 역할을 참조해도 `permissionCodes(테넌트1,user)`=빈 집합(쿼리가 ur·r·p tenant 모두 검증).
- **AC-4 (경계):** updateRole 권한 교체 시 이전 행 삭제·새 행만(orphanRemoval), (role,code) 중복 없음(grant 멱등+UNIQUE).
- **AC-5 (기동):** `ddl-auto: validate`에서 앱 정상 기동(엔티티↔V0003 정확 일치).

## 4. 제약
- 마이그레이션 forward-only: V0002(릴리즈됨) 수정 금지 — 신규 V0003만. NOT NULL 전 backfill 필수. 비파괴(데이터 보존).

## 5. 경계 / Do-Not
- ✅: `RolePermission` 내부 설계, `Role.clearPermissions()` 추가, 쿼리 projection 변경.
- ⚠️: 공개 DTO 형태 변경(현 계획 불변), 다른 authz 테이블 확대.
- 🚫: 기존 마이그레이션 수정(checksum), 권한코드 의미 변경, Keycloak/시크릿.

## 6. Open Questions
없음 — 설계·범위 확정.

## 7. 기술 접근 (HOW)
**핵심: 공개 표면 `Set<String>` 유지(외과적), 내부만 엔티티 전환 → blast radius 최소.**
- **신규 `RolePermission`**(common/security, **BaseEntity 미상속** — authz 패턴, approval_step처럼): `id`(seq `common.role_permission_id_seq`), `tenantId`, `@ManyToOne(LAZY) Role role`, `permissionCode`. `of(tenantId, role, code)`.
- **`Role.permissions`**: @ElementCollection<String> → `@OneToMany(mappedBy="role", cascade=ALL, orphanRemoval=true, LAZY) Set<RolePermission>`.
  - `grant(code)`: 중복 없으면 `RolePermission.of(this.tenantId,this,code)` 추가(멱등). 모든 호출처가 `Role.of()`로 tenantId 설정 후 grant(확인됨).
  - `revoke(code)`: 해당 code removeIf. `getPermissions()`: stream→permissionCode→`Set<String>`(불변). 신규 `clearPermissions()`: `permissions.clear()`.
- **`IamService` 1줄**: `role.getPermissions().clear()`(L79) → `role.clearPermissions()`. grantAll/validatePermissions(Set<String>) 불변.
- **`UserRoleRepository.findPermissionCodes`**: `JOIN r.permissions p ... SELECT DISTINCT p` → `SELECT DISTINCT p.permissionCode ... AND p.tenantId=:tenantId`(DB-레벨 격리). 반환 `Set<String>` 불변.
- **V0003**(common): seq 생성; `id`·`tenant_id` 컬럼 추가; backfill(`id=nextval`, tenant_id=부모 role tenant); NOT NULL; 복합PK→`id` PK + `UNIQUE(role_id,permission_code)` + role_id 인덱스.

**영향 파일**: `Role.java`·신규 `RolePermission.java`·`UserRoleRepository.java`·`IamService.java`(1줄)·`V0003__role_permission_entity.sql`. 그 외(RoleResponse·IamBootstrap·컨트롤러·기존 테스트) **무변경**.

**테스트(AC↔테스트)**: AC-2/3/4=기존 `AuthorizationResolverIntegrationTest`·`IamIntegrationTest`·`IamServiceTest`·`PermissionCatalogTest` green 유지. AC-1/3=신규 통합테스트(grant 후 role_permission 행 tenant_id==role tenant + cross-tenant 비누출). AC-5=`./gradlew check`의 @SpringBootTest가 ddl validate 자동 검증.

## 8. 태스크 (test-first)
| # | 태스크 | AC | 대상 파일 | 검증(exit 0) | 의존 |
|---|---|---|---|---|---|
| 1 | `RolePermission` 엔티티 + `Role` 매핑 전환(grant/revoke/getPermissions/clearPermissions) + `IamService` clear 1줄 + `findPermissionCodes` `p.tenantId` + V0003 | AC-1,2,4,5 | `RolePermission.java`(신규)·`Role.java`·`IamService.java`·`UserRoleRepository.java`·`V0003__role_permission_entity.sql` | `cd backend && ./gradlew check` | — |
| 2 | DB-레벨 격리 신규 테스트(role_permission 행 tenant_id 검증 + cross-tenant 비누출 p.tenantId 경로) | AC-1,3 | `AuthorizationResolverIntegrationTest.java`(추가) | `./gradlew test --tests *AuthorizationResolverIntegrationTest` | #1 |

- 태스크1은 엔티티-마이그레이션-쿼리가 ddl validate로 강결합돼 **원자적**(쪼개면 비기동 중간상태); 기존 테스트가 회귀 안전망. 태스크2 [P] 단독 revert 가능, 태스크1은 fix-forward.

## 검증 (end-to-end)
```bash
cd backend && ./gradlew check
```
기존 인가 테스트 green=동작보존, 신규 격리 테스트 green=tenant_id DB 영속·필터. 브랜치 `feature/role-permission-tenant` → 태스크별 `/feature-add` → `/feature-merge`(한 PR) → `/code-review` → `/solo-merge`.
