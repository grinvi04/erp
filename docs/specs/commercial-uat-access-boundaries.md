# commercial-uat-access-boundaries 스펙

## 1. 목표 & Why

기존 상용 UAT에서 증거가 남지 않은 부가세 CSV 정합성, 세션 만료 처리, 저권한 사용자 UI/API 경계를 실제 로컬 스택에서 반복 검증한다. **성공 기준: 동일 테넌트의 무권한 사용자가 업무 메뉴를 볼 수 없고 보호 API에서 403/C005를 받으며, 만료 세션은 로그인으로 복귀하고, 날짜 필터로 내려받은 매출·매입 CSV가 같은 API 보고서와 정확히 일치한다.**

## 2. Scope

- **In:** UAT-A 무권한 사용자 준비, 모듈 사이드바·명령 팔레트 권한 필터, 보호 API 403 확인, Auth.js refresh 실패 후 로그인 복귀, 부가세 매출·매입 CSV 실제 다운로드와 API 결과 대조, 실행 문서·릴리즈 증거 갱신.
- **Out:** 새 CSV API, 권한 모델/코드 변경, 페이지별 라우트 가드 재설계, 운영·원격 환경 실행, Keycloak 수명 정책 변경, 기존 업무 API 변경.

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)

- **AC-1 (저권한 신원 준비):** WHEN 상용 UAT setup이 실행되면, the system SHALL 작성자·결재자와 subject가 다르고 UAT-A와 동일한 `tenant_id`를 가진 전용 사용자를 멱등 준비하며 배정 역할과 `/api/me/permissions`가 비어 있음을 업무 mutation 전에 확인한다.
- **AC-2 (UI 최소권한):** WHEN 권한이 없는 사용자가 로그인하면, the system SHALL 인사·재무·재고·CRM·IAM·감사 경로를 사이드바와 명령 팔레트에 노출하지 않는다. 각 업무 모듈은 해당 모듈의 read 권한 중 하나 이상이 있을 때만 노출한다.
- **AC-3 (서버 최종 인가):** WHEN 같은 사용자가 재무 및 감사 보호 API를 직접 호출하면, the system SHALL HTTP 403과 공통 오류 코드 `C005`를 반환하고 데이터를 포함하지 않는다.
- **AC-4 (세션 만료):** WHEN 만료된 Auth.js 세션의 refresh token 갱신이 실패하면, the system SHALL 대시보드 대신 `/login`으로 이동하고 브라우저 세션 응답이나 화면에 access/refresh token을 노출하지 않는다.
- **AC-5 (필터 CSV 정합성):** WHEN 작성자가 `from`/`to` 날짜로 부가세 신고 화면의 매출·매입 CSV를 각각 내려받으면, the system SHALL 파일명에 같은 기간을 포함하고 각 CSV의 거래처별 사업자번호·명칭·매수·공급가액·세액을 동일 필터의 `/api/finance/vat-return` 응답과 행 단위로 일치시킨다.
- **AC-6 (브라우저 안정성):** WHILE AC-2,4,5를 실제 Next.js 브라우저에서 수행하는 동안, the system SHALL 처리되지 않은 `pageerror`를 발생시키지 않는다.

## 4. 제약 / 비기능

- localhost와 기존 `E2E_COMMERCIAL` mutation 확인값에서만 실행한다.
- 전용 사용자의 기존 역할은 setup에서 전부 회수해 재실행 드리프트를 제거한다. SQL로 권한을 조작하지 않고 기존 IAM API만 사용한다.
- JWT, refresh token, 비밀번호, Auth.js cookie 값은 로그·trace·screenshot·artifact에 출력하지 않는다.
- UI 숨김은 편의/오노출 방지이며, 백엔드 403 검사가 최종 인가 증거다.
- CSV는 현재 클라이언트 다운로드 구현을 검증한다. 검증만을 위한 중복 서버 export API는 만들지 않는다.

## 5. 경계 / Do-Not

- ✅ 해도 됨: 로컬 Keycloak 전용 사용자 생성, UAT-A 배정, 기존 IAM API로 역할 회수, 로컬 Auth.js cookie로 만료 조건 구성, Playwright download 파일 읽기.
- ⚠️ 먼저 물어봐: 페이지별 서버 라우트 가드 도입, 권한 카탈로그 변경, 원격 환경 실행.
- 🚫 절대 금지: 운영 조작, 시크릿 출력/커밋, 권한 검사를 테스트 전용으로 우회, 데이터 정합성을 맞추기 위한 임의 SQL.

## 6. Open Questions

- 없음. 대시보드·결재함·분석은 여러 모듈을 집계하고 서버가 허용 범위를 제한하므로 이번 메뉴 필터 범위에 포함하지 않는다.

## 7. 기술 접근 (HOW)

- `CommercialUatConfig`와 identity topology를 네 번째 restricted 사용자까지 확장한다. setup은 Keycloak 사용자를 UAT-A에 배정한 뒤 현재 역할을 조회해 기존 IAM DELETE API로 모두 회수하고 권한 배열이 빈 값인지 확인한다.
- 모듈 탐색 항목에 필요한 read 권한 목록을 명시하고 하나라도 보유한 그룹만 계산하는 순수 helper를 둔다. 사이드바와 명령 팔레트가 같은 helper/정책을 사용해 노출 규칙이 갈라지지 않게 한다. 서버 권한 검사는 변경하지 않는다.
- 상용 Playwright는 restricted bearer token으로 부가세·감사 API의 403/C005를 확인하고, 같은 사용자 Auth.js 세션에서 보호 메뉴가 보이지 않음을 확인한다.
- 기존 Auth.js cookie helper가 token override를 받을 수 있게 최소 확장한다. 만료 시각과 무효 refresh token을 넣어 실제 Keycloak refresh 실패 경로를 거친 뒤 `/login` redirect와 브라우저 오류 0건을 단언한다.
- 부가세 화면의 두 `엑셀` 버튼에서 Playwright `download` 이벤트를 기다린다. BOM을 제거하고 CSV를 파싱해 같은 날짜의 VAT API `salesByBuyer`/`purchasesByVendor` 전체 행을 정규화·정렬한 결과와 비교하며 파일명도 검증한다.

### 테스트 전략

- config/topology/모듈 필터 helper를 Vitest에서 정상·누락·중복 신원·권한 조합으로 먼저 RED 처리한다.
- restricted 역할 회수와 실제 권한 없음은 commercial setup 및 API readback으로 검증한다.
- AC-2~6은 실제 Keycloak·Spring Boot·Next.js commercial Playwright에서 검증한다.
- 전체 회귀는 backend `check`, frontend unit/type/lint/design/build, 상용 UAT를 모두 통과시킨다.

## 8. 태스크 (test-first 순서)

| # | 태스크 | AC 참조 | 대상 파일 | 검증(이 명령 exit 0) | 의존 |
|---|---|---|---|---|---|
| 1 | restricted 환경 계약·4인 topology·모듈 권한 필터 계약을 RED로 추가하고 최소 구현 | AC-1,2 | `frontend/src/lib/commercial-uat.*`, 탐색 권한 helper/tests | `cd frontend && npm test && npm run type-check` | — |
| 2 | restricted 사용자를 UAT-A에 멱등 준비하고 기존 역할 전부 회수·빈 권한을 확인 | AC-1,3 | `frontend/e2e/commercial.setup.ts`, `scripts/commercial-uat.sh` | commercial setup + frontend unit/type-check | #1 |
| 3 | 사이드바·명령 팔레트에 공통 모듈 read 권한 필터를 적용하고 브라우저/API 거부를 검증 | AC-2,3,6 | `frontend/src/components/layout/`, `frontend/e2e/commercial-workflow.spec.ts` | commercial Playwright | #1,#2 |
| 4 | 실제 만료 cookie의 refresh 실패→로그인 복귀 계약을 추가 | AC-4,6 | `frontend/e2e/commercial-workflow.spec.ts` | commercial Playwright | #2 |
| 5 | 부가세 매출·매입 다운로드를 API 전체 행과 대조하고 문서·릴리즈 증거를 갱신 | AC-5,6 | `frontend/e2e/commercial-workflow.spec.ts`, `README.md`, `docs/release-readiness.md` | commercial UAT + frontend 전체 품질 + `cd backend && ./gradlew check` | #3,#4 |

구현은 하나의 feature 브랜치에서 진행하되 테스트 계약, 제품 UI 수정, UAT/문서 단위로 커밋을 나눠 롤백 가능하게 유지한다. DB migration과 업무 API 변경은 없다.
