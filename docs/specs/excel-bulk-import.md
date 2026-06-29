# excel-bulk-import (CSV 대량 업로드) 스펙

> 현지화 로드맵 Tier3 #9(엑셀 업/다운로드)의 **업로드(import)** 절반. 단일 출처: `docs/roadmap-localization.md`.
> 다운로드(내보내기)는 기존 `lib/csv.ts downloadCsv`(UTF-8 BOM)로 목록 화면에 이미 존재 — 본 작업은 **대량 입력(업로드) 갭**에 집중.

## 1. 목표 & Why
CSV 파일로 거래처·품목을 **대량 등록**한다 — 템플릿 다운로드 → 작성 → 업로드 → 행별 검증 → (전부 유효하면) 일괄 생성, 실패 시 행별 사유 리포트. 한 건씩 입력하던 반복을 없애는 한국 ERP 필수 UX. 재사용 가능한 import 기반을 만들고 거래처(finance)·품목(inventory) 2개에 적용해 모듈 간 재사용성을 증명한다. **성공 기준(측정 가능): 유효한 N행 CSV 업로드 시 N건이 생성되고 결과에 importedCount=N·오류 0이 반환되며, 한 행이라도 검증 실패하면 아무것도 생성되지 않고 실패 행번호·사유가 전부 리포트된다.**

## 2. Scope
- **In:**
  - **재사용 import 기반**: multipart CSV 업로드 → 파싱(헤더 + 데이터 행) → 행별 매핑·검증 → **전부 유효할 때만 일괄 생성(all-or-nothing)** → 결과(`총 행수`·`생성 건수`·`실패 행 목록[행번호, 사유]`).
  - **CSV 파싱**: UTF-8 BOM 제거, 따옴표 필드·따옴표 내 콤마·`""` 이스케이프 처리. 헤더 행으로 컬럼 매핑.
  - **템플릿 다운로드**: 엔티티별 헤더(+예시 1행) CSV 템플릿 제공.
  - **거래처(Customer) import**: 코드·업체명(필수)·사업자번호·대표자·주소·업태·종목·연락처·결제기한 매핑, 검증(코드 형식·**파일 내 중복**·**DB 기존 중복**·사업자번호 체크섬), FINANCE_WRITE.
  - **품목(Item) import**: SKU·품목명(필수)·단위·원가법·표준원가·재주문점 등 매핑, 검증(SKU 중복·단위 FK 존재·필수 수치), INVENTORY_WRITE.
  - **프론트**: 재사용 업로드 다이얼로그(템플릿 다운로드·파일 선택·업로드·결과 표시 — 생성 건수·실패 행 목록) + 거래처·품목 목록 화면 툴바 "엑셀 업로드" 연결.
- **Out (Non-goals):**
  - **xlsx(.xlsx) 파싱** — CSV만(Apache POI 미도입). xlsx는 후속.
  - **수정/업서트(upsert)** — 신규 생성만. 이미 존재하는 코드/SKU는 **오류**(덮어쓰기 안 함).
  - **부분 성공(유효 행만 생성)** — all-or-nothing만(한 행이라도 실패면 전체 미생성).
  - **비동기/백그라운드 대용량 처리** — 동기 처리, 행 수 상한(예: 1,000행) 초과는 거부.
  - **거래처·품목 외 엔티티**(공급업체·계정과목 등) — 후속 확장(기반은 재사용 가능하게).
  - 다운로드(내보내기) 변경 — 기존 CSV 유지.

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (정상 일괄 생성):** WHEN 모든 행이 유효한 CSV를 업로드, the system SHALL 모든 행을 생성하고 `importedCount = 행수`·실패 목록 빈 결과를 반환한다.
- **AC-2 (검증 실패 → 전체 미생성):** IF 한 행이라도 검증 실패(필수 누락·형식 오류·중복·FK 부재), THEN the system SHALL **아무 행도 생성하지 않고**(롤백) 실패한 모든 행의 [행번호, 사유]를 반환한다(`importedCount = 0`).
- **AC-3 (파일 내 중복, 예외):** IF 같은 파일에 코드/SKU가 중복된 두 행이 있으면, THEN the system SHALL 중복 행을 오류로 리포트(전체 미생성).
- **AC-4 (DB 기존 중복, 예외):** IF 행의 코드/SKU가 이미 DB에 존재하면, THEN 오류로 리포트(생성·덮어쓰기 안 함).
- **AC-5 (헤더/빈 파일/형식, 예외):** IF 헤더가 기대 컬럼과 불일치하거나 데이터 행이 0이거나 파일이 비었으면, THEN `400`과 명확한 사유.
- **AC-6 (CSV 파싱, 경계):** WHILE 필드에 BOM·따옴표로 감싼 콤마·`""` 이스케이프·한글이 포함되면, the system SHALL 정확히 파싱한다.
- **AC-7 (행 수 상한, 경계):** IF 데이터 행이 상한(1,000)을 초과하면, THEN `400`(상한 안내).
- **AC-8 (템플릿, 정상):** WHEN 엔티티 템플릿을 요청, the system SHALL 기대 헤더(+예시 행)를 담은 CSV를 반환한다.
- **AC-9 (권한, 예외):** IF FINANCE_WRITE(거래처)·INVENTORY_WRITE(품목) 권한이 없으면, THEN `403`.
- **AC-10 (테넌트 격리, 경계):** the system SHALL 생성 행에 현재 테넌트를 적용한다(@TenantId).
- **AC-11 (파일 크기, 예외):** IF 업로드가 multipart 크기 상한을 넘으면, THEN 명확한 4xx(빈 500 금지).

## 4. 제약 / 비기능
- 동기 처리, 행 상한 1,000(초과 거부). 일괄 생성은 단일 트랜잭션(all-or-nothing).
- 행별 검증은 기존 검증 재사용(BusinessNoValidator·중복 체크·@Valid 규칙·FK 조회).
- multipart 크기 상한 설정(예: 2MB) — 1,000행 CSV에 충분, 남용 방지.
- 응답은 공통 Envelope(ApiResponse) — data에 import 결과(생성수·실패목록).
- 모듈 경계 존중: import 기반은 common(또는 각 모듈 application)에 두되 도메인 서비스(CustomerService·ItemService.create) 재사용. 크로스모듈 직접참조 금지.

## 5. 경계 / Do-Not (3단계)
- ✅ 해도 됨: CSV 파서·BulkImportResult·import 골격·거래처/품목 매핑·검증·multipart 설정·업로드 다이얼로그·템플릿.
- ⚠️ 먼저 물어봐: xlsx 도입(POI), upsert(덮어쓰기), 부분 성공 전환, 비동기 대용량, 추가 엔티티 확장, 행 상한·크기 상한 상향.
- 🚫 절대 금지: 일부 행만 커밋해 부분 상태 만들기(all-or-nothing 위반), 기존 코드/SKU 조용히 덮어쓰기, 검증 우회 생성, 크로스테넌트 생성, 파싱 오류를 500으로 흡수(4xx로 매핑), 시크릿 커밋.

## 6. Open Questions
- (없음 — 포맷=CSV·전략=all-or-nothing·엔티티=거래처+품목으로 확정.)

---

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):** `CustomerService.create`/`ItemService.create`(검증·생성 그대로 재사용), `BusinessNoValidator`, ErrorCode(중복·FK), `ApiResponse`, `lib/csv.ts`(다운로드·BOM 관례), 권한 `PermissionChecker`.

- **CSV 파서(신규, 의존성 무): `CsvReader`** — RFC4180 부분집합(따옴표 필드·`""` 이스케이프·BOM 제거). 다중행 필드(필드 내 개행)는 미지원(거부·문서화). `downloadCsv`의 이스케이프와 대칭. **신규 라이브러리(opencsv·POI) 미도입** — flat 표 데이터라 소규모 파서로 충분, 단위테스트로 엣지(따옴표·콤마·BOM·빈필드) 고정.
- **재사용 골격**: `BulkImportResult`(totalRows·importedCount·List<RowError{rowNumber,message}>) + 헬퍼 — 헤더 검증 → 행 매핑(Function<row,CreateRequest>) → 행별 검증 수집 → **오류 0이면** 단일 @Transactional에서 일괄 create, 아니면 생성 없이 결과 반환. 파일 내 중복은 매핑 단계에서 코드/SKU set으로 검출.
- **엔드포인트**: `POST /api/finance/customers/import`(multipart `file`) → BulkImportResult, `GET /api/finance/customers/import/template`(CSV); 품목은 `/api/inventory/items/import`(+template). 권한은 각 서비스에서 검사.
- **multipart 설정**: `application.yml`에 `spring.servlet.multipart.max-file-size: 2MB`·`max-request-size: 2MB`. `MaxUploadSizeExceededException`을 GlobalExceptionHandler에서 4xx로 매핑(AC-11).
- **프론트**: 재사용 `BulkImportDialog`(템플릿 다운로드 링크·`<input type=file accept=.csv>`·업로드·결과 — 생성 건수·실패 행 테이블). `lib/api`에 multipart 전송 헬퍼(`apiPostForm`) 추가(FormData). 거래처·품목 목록 툴바에 "엑셀 업로드" 버튼.

**테스트 전략(AC↔테스트):**
- 단위: `CsvReader`(AC-6 따옴표·BOM·콤마·빈필드, AC-5 헤더불일치/빈파일). `BulkImportResult` 골격(AC-1 전부생성·AC-2 일부실패시 전체미생성·AC-3 파일내중복).
- 통합(@SpringBootTest): 거래처/품목 업로드 end-to-end — AC-1 정상, AC-2 롤백(실패행 리포트·DB 미생성 확인), AC-4 DB중복, AC-7 행상한, AC-9 권한, AC-10 테넌트. AC-11 크기상한(또는 핸들러 단위).
- 프론트: type-check/lint/build + 업로드 다이얼로그 동작(파일선택·결과 표시) 스모크.

## 8. 태스크 (test-first 순서, 한 기능=한 브랜치 `feature/excel-bulk-import`=한 PR, 태스크당 원자적 커밋)
| # | 태스크 | AC | 대상 파일 | 검증(exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | CSV 파서(CsvReader) + BulkImportResult·재사용 import 골격(all-or-nothing·행오류수집·파일내중복) + multipart 설정·크기예외 매핑 | AC-1,2,3,5,6,7,11 | CsvReader, BulkImportResult, 공통 import 헬퍼, application.yml, GlobalExceptionHandler | `cd backend && ./gradlew check` | — | |
| 2 | 거래처(Customer) import — 매핑·검증·서비스·컨트롤러(업로드+템플릿) | AC-1,2,4,8,9,10 | CustomerImportService, CustomerController, finance | `./gradlew check` | #1 | |
| 3 | 품목(Item) import — 매핑·검증(SKU·UOM FK)·서비스·컨트롤러(업로드+템플릿) | AC-1,2,4,8,9,10 | ItemImportService, ItemController, inventory | `./gradlew check` | #1 | [P] |
| 4 | 프론트 재사용 BulkImportDialog + 거래처·품목 화면 연결 + multipart 서버액션·템플릿·결과표시 | AC-1,2,8 | frontend BulkImportDialog, customers·items client, lib/api | `npm run type-check && npm run build` | #2,#3 | |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 파서·골격 단위 + 거래처/품목 업로드 통합(정상 일괄생성·실패시 전체롤백·DB중복·행상한·권한·테넌트) green + 회귀 없음.
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 거래처 템플릿 다운로드 → 3행 작성(1행 사업자번호 오류 포함) 업로드 → **0건 생성 + 1·오류행 리포트** 확인 → 수정 후 재업로드 → 3건 생성. 품목 동일. 1,000행 초과·권한 없음 거부 확인.
4. 게이트: 태스크별 `/feature-add`(TDD) → 전 태스크 GREEN → `/feature-merge`(한 PR, AI 리뷰·CI).
