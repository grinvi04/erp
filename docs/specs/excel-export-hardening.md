# excel-export-hardening — 스펙·플랜

## Context (왜)
한국 ERP의 "엑셀 다운로드"(로드맵 #9)는 사실상 필수 UX다. 현재 `frontend/src/lib/csv.ts`의 `downloadCsv`가 32개 화면에서 쓰이지만 두 가지 결함이 있다: **(1) CSV 수식 인젝션 방어 부재** — team-harness `api-standards.md:79`가 **필수**로 규정한 선두 `= + - @ TAB CR` 중화를 하지 않아, `=cmd|...` 같은 셀 값이 엑셀에서 수식으로 실행될 수 있다(OWASP CSV Injection). 보안리뷰(#149 release-check)도 동일 지적. **(2) 현재 로드된 페이지(size≈20)만 내보냄** — 전체 데이터셋이 아니라 대용량 원장(매입·매출계산서·전표) export가 무의미. 이 기능은 ①보안 결함을 전 화면 공통 유틸에서 한 번에 고치고, ②재무 핵심 화면에 전체 데이터셋 CSV 내보내기를 추가한다.

**확정 결정(사용자)**: 포맷 = **강화된 CSV 유지**(진짜 .xlsx 바이너리·라이브러리 도입 안 함 — 로드맵 #9는 바이너리 미요구, 단순함 우선). 범위 = **전체 데이터셋 내보내기, 재무 핵심 화면 우선**.

---

## 1. 목표 & Why
CSV export의 수식 인젝션 취약점을 표준대로 제거하고, 재무 핵심 화면에서 현재 페이지가 아닌 전체 데이터셋을 CSV로 내보낸다. **성공 기준: (a) 선두 `= + - @ TAB CR` 셀이 `'` 프리픽스로 중화돼 엑셀에서 수식 실행되지 않으며 기존 인용/콤마/줄바꿈 처리는 회귀 없음, (b) 재무 핵심 화면의 '전체 엑셀'이 페이지 한정이 아니라 전체 행(전 페이지 순회)을 적용된 조회조건과 함께 내보낸다.**

## 2. Scope
- **In:**
  - **CSV 수식 인젝션 방어(공통 유틸)**: `downloadCsv`의 셀 직렬화에 선두 `= + - @ \t \r` 중화(`'` 프리픽스). 순수 함수로 추출(`sanitizeCsvValue`) — 32개 호출처 전부에 자동 적용. 기존 BOM·RFC4180 인용 유지.
  - **단위 테스트 인프라(최소)**: vitest(dev 의존성) + `test` 스크립트 + `sanitizeCsvValue` 단위 테스트. CI `ci-gate.yml` frontend job에 `npm run test --if-present` 스텝 추가(보안 회귀 게이트).
  - **전체 데이터셋 CSV 내보내기 헬퍼**: 모든 페이지를 `size≤100`(api-standards 상한)으로 순회·누적 후 `downloadCsv`. 안전 상한(예: 5000행/50페이지) 초과 시 **잘림 없이 toast 경고**(silent cap 금지). BFF 서버액션으로 구현(기존 조회 권한·테넌트 격리 경유).
  - **재무 핵심 화면 적용**: 매입계산서(invoices)·매출계산서(ar-invoices)·전표(journal-entries)에 '전체 엑셀' 실행. 화면의 적용된 조회조건(applied filters)을 전체 export에도 반영.
- **Out (Non-goals):**
  - 진짜 `.xlsx` 바이너리·xlsx/exceljs/sheetjs 라이브러리 도입(차후 고객 요구 시).
  - 재무 외 모듈(HR·재고·CRM) 전체 데이터셋 export(이번엔 보안 강화만 공통 적용, 전체셋은 후속).
  - 백엔드 전용 export 엔드포인트 신설·`size` 상한 정책 변경(서버액션이 기존 페이지 API 순회로 해결).
  - 비동기/스트리밍 대용량 export(상한 내 동기 처리).
  - 기존 화면별 '현재 페이지 엑셀' 버튼 동작 변경(전체 export는 별도 추가 또는 대체 — 태스크에서 화면별 확정).

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (수식 인젝션 방어, 정상):** WHEN export 셀 값이 `=`·`+`·`-`·`@`·TAB·CR로 시작, the system SHALL 값 앞에 `'`를 붙여 직렬화한다(엑셀 수식 실행 차단).
- **AC-2 (기존 직렬화 회귀 없음, 경계):** WHILE 값에 콤마·따옴표·줄바꿈이 포함, the system SHALL 기존 RFC4180 인용(`"..."`·`""`)과 UTF-8 BOM을 그대로 유지한다(중화 로직이 정상 값·숫자·null을 훼손하지 않음).
- **AC-3 (전체 데이터셋, 정상):** WHEN 재무 핵심 화면에서 '전체 엑셀'을 실행, the system SHALL 현재 페이지가 아니라 전체 행(전 페이지 순회 누적)을 CSV로 내보낸다.
- **AC-4 (상한·잘림 금지, 경계):** IF 전체 행이 안전 상한을 초과, THEN the system SHALL 행을 조용히 자르지 않고 toast로 초과/제한을 사용자에게 알린다.
- **AC-5 (조회조건 반영, 정상):** WHILE 화면에 조회조건이 적용된 상태, the system SHALL 전체 export에도 동일 조건을 반영한다(현재 페이지 export와 동일 필터 결과).
- **AC-6 (권한·테넌트, 예외):** the system SHALL 전체 export를 기존 조회 권한·테넌트 격리(BFF 서버액션·apiGetPage) 경유로만 수행한다(권한 우회·교차 테넌트 노출 없음).

## 4. 제약 / 비기능
- 보안: api-standards.md:79 준수(필수). 수식 인젝션 0건.
- 단순함: 신규 런타임 의존성 0(CSV 유지). vitest는 dev 전용.
- 성능: 전체 export는 상한 내 동기. `size≤100` 페이지네이션 준수.

## 5. 경계 / Do-Not
- ✅ 해도 됨: `downloadCsv` 셀 중화, 순수 함수 추출, vitest 최소 도입, 전체 export 서버액션·재무 3화면 적용, toast 경고.
- ⚠️ 먼저 물어봐: 진짜 .xlsx 도입, 백엔드 export 엔드포인트/size 정책 변경, 재무 외 모듈 전체 export 확대.
- 🚫 절대 금지: 전체 export에서 행 silent 잘림(메모리 교훈), 권한·테넌트 우회 직접 쿼리, 기존 BOM/RFC4180 인용 제거, 시크릿 커밋, `as any`.

## 6. Open Questions
- (없음 — 포맷·범위 사용자 확정)

---

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):** `frontend/src/lib/csv.ts`(`downloadCsv` — BOM·RFC4180 인용 기존), `frontend/src/lib/api.ts`(`apiGetPage<T>` — BFF 페이지 조회, 서버 전용), `PageResponse<T>`(`page`/`totalPages`/`content`), 화면별 `exportExcel`/`filtered`(예 `invoices-client.tsx:395`)·`applied` 필터 상태, `sonner` toast.

- **보안(Task 1)**: `csv.ts`에 `export function sanitizeCsvValue(s: string): string` 추출 — `/^[=+\-@\t\r]/.test(s)` 면 `"'" + s`. `downloadCsv`의 `esc()`가 문자열화 후 `sanitizeCsvValue`를 적용하고 기존 인용 로직은 유지(순서: 중화 → 인용). 숫자/null도 String 변환 후 통과(정상값 무변). vitest 설정(`vitest.config.ts` 최소·node 환경)·`"test": "vitest run"` 스크립트·`csv.test.ts`. CI `ci-gate.yml` frontend job에 `- run: npm run test --if-present`(build 뒤).
- **전체 export(Task 2~3)**: `frontend/src/lib/export.ts`(신규)에 서버액션 헬퍼 `fetchAllPages<T>(basePath, ...): Promise<{rows; truncated}>` — `apiGetPage`를 `page=0..totalPages-1`(`size=100`) 순회 누적, 상한(예 MAX_ROWS=5000) 도달 시 `truncated=true`로 중단. 화면의 server action(예 `invoices/actions.ts`에 `exportAllInvoices(filters)`)이 이를 호출해 전체 행 반환. 클라이언트는 받은 행에 화면의 `applied` 필터를 적용(기존 `filtered` 로직 재사용) 후 `downloadCsv`. `truncated`면 toast 경고.
  - **모듈 경계**: 전부 frontend. 백엔드 무변경.
- **테스트 전략(AC↔테스트)**: AC-1·2 → vitest 단위(`sanitizeCsvValue`: 각 위험문자·정상·콤마/인용/줄바꿈/숫자/null). AC-3·4·5·6 → 서버액션 로직(전 페이지 순회·상한·필터)은 e2e(Playwright, `E2E_BACKEND` 게이트)로 재무 화면 '전체 엑셀' 다운로드 검증 + 수동(docker-compose). (서버액션은 BFF 의존이라 단위 대신 e2e+빌드+타입체크로 커버 — 기존 화면 관행과 동일.)

## 8. 태스크 (test-first 순서)
| # | 태스크 | AC 참조 | 대상 파일 | 검증(exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | CSV 수식 인젝션 방어 + vitest 단위테스트 + CI 배선 (전 화면 공통 유틸) | AC-1,2 | `frontend/src/lib/csv.ts`, `frontend/vitest.config.ts`(신규), `frontend/src/lib/csv.test.ts`(신규), `frontend/package.json`, `.github/workflows/ci-gate.yml` | `cd frontend && npm run test && npm run type-check && npm run build` | — | |
| 2 | 전체 데이터셋 export 공유 헬퍼 + 매입계산서 적용('전체 엑셀'·상한 toast·필터 반영) | AC-3,4,5,6 | `frontend/src/lib/export.ts`(신규), `finance/invoices/actions.ts`, `finance/invoices/invoices-client.tsx` | `cd frontend && npm run type-check && npm run lint && npm run build` | #1 | |
| 3 | 매출계산서·전표 적용 (#2 패턴 재사용) | AC-3,4,5,6 | `finance/ar-invoices/{actions,ar-invoices-client}`, `finance/journal-entries/{actions,journal-entries-client}` | `cd frontend && npm run type-check && npm run lint && npm run build` | #2 | |

## Verification (end-to-end)
1. `cd frontend && npm run test`(vitest) — `sanitizeCsvValue` 정상·위험문자·회귀 green.
2. `cd frontend && npm run type-check && npm run lint && npm run lint:design && npm run format:check && npm run build` 전부 통과.
3. 수동(docker-compose): 거래처/계정 이름에 `=1+1` 입력 → 매입계산서 '현재 페이지 엑셀'·'전체 엑셀' 다운로드 → 엑셀에서 `'=1+1` 텍스트로 열림(수식 미실행). 전체 엑셀이 20행 초과(전 페이지) 내보냄. 조회조건 적용 시 동일 필터 반영. 상한 초과 데이터로 toast 경고 확인.
4. 게이트: `/feature-add`(태스크 TDD)→`/feature-merge`(focused 리뷰·CI, 신규 `test` 스텝 포함).
