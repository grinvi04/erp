# e-tax-invoice (전자세금계산서) 스펙

> 현지화 로드맵 Tier1 #3. 단일 출처: `docs/roadmap-localization.md`.
> 전제: #1 사업자등록번호(Customer.businessNo)·#2 부가세 분리·자동분개(ArInvoice supplyAmount/vatAmount/taxType, VAT 통제계정) 완료.
> **바깥 경계(로드맵 확정)**: 데이터 모델 + 국세청 **표준 XML 생성까지**. 홈택스 직접 전송·승인번호 수신·인증(공인 ASP)은 범위 밖.

## 1. 목표 & Why
승인된 매출(AR) 인보이스에서 **전자세금계산서를 발행**(파생·스냅샷 고정)하고, 국세청 전자세금계산서 **표준 XML을 생성**한다. B2B 법적 의무이며 기존 AR·부가세·거래처에 얹는 확장이라 레버리지가 높다. **성공 기준(측정 가능): 승인된 과세 AR 인보이스를 세금계산서로 발행하면 공급자·공급받는자·품목·공급가액·세액이 스냅샷 고정된 세금계산서가 생성되고, 그 세금계산서로 핵심 필수항목(공급자/공급받는자 사업자번호·상호·대표자·주소·업태·종목, 작성일자, 공급가액·세액, 품목)을 담은 well-formed 표준 XML이 산출된다.**

## 2. Scope
- **In:**
  - **회사정보 설정(공급자, 테넌트 1행)**: 상호·사업자등록번호·대표자·주소·업태·종목. 조회/변경 `FINANCE_SETTING_WRITE`. finance 설정 화면(FX 설정 옆)에 추가. 세금계산서 발행의 공급자 신원.
  - **거래처(공급받는자) 정보 보강**: `Customer`에 대표자명·주소·업태·종목 컬럼 추가(기존 `businessNo`·`contactName` 유지). AR 거래처 폼 보강.
  - **세금계산서(TaxInvoice) 엔티티**: 승인된 AR 인보이스에서 발행. **1 AR : 1 세금계산서**(중복 발행 차단). 필드 — 종류(과세/영세율/면세, AR taxType 승계), 청구/영수 구분, 작성일자, **공급가액·세액·합계(AR 승계)**, **공급자 스냅샷**(발행 시점 회사정보 복사), **공급받는자 스냅샷**(발행 시점 customer 복사), **단일 품목**(품목명·공급가액·세액 = 인보이스 헤더 전액, 품목명은 입력/대표품목/비고), 내부 발행번호(채번), 상태(DRAFT 없음 — 발행 즉시 ISSUED / CANCELLED), 비고. 국세청 승인번호는 미전송이므로 nullable.
  - **발행(issue)**: `APPROVED` 또는 `PAID` AR 인보이스 → 세금계산서 발행. 공급자/공급받는자/금액/품목을 **발행 시점에 스냅샷 고정**(이후 마스터 변경 무영향).
  - **취소(cancel)**: `ISSUED` → `CANCELLED`. 취소 후 동일 AR 재발행 허용. 수정세금계산서(수정사유별)는 범위 밖.
  - **국세청 표준 XML 생성**: 발행된(ISSUED) 세금계산서 → 전자세금계산서 표준 구조 XML(핵심 필수항목 포함, well-formed). 조회/다운로드. 실제 전송은 범위 밖.
  - **화면(한국 ERP UI 패러다임)**: 세금계산서 목록(조회조건·합계) · 상세(공급자/공급받는자/품목/금액) · AR에서 발행 · 취소 · XML 다운로드. 회사정보 설정 화면. 거래처 폼 보강.
- **Out (Non-goals):**
  - **홈택스/국세청 직접 전송·인증·승인번호 수신**(공인 ASP·사업자 인증 필요 — 솔로 단독 완결 불가).
  - **수정세금계산서**(수정사유 8종)·역발행·위수탁발행.
  - **정식 다품목 라인**(품목명·규격·수량·단가 다행) — 헤더 단일 품목으로 갈음.
  - **계산서(면세 전용 서식) 분리** — taxType=EXEMPT는 세금계산서 서식에 세액 0으로 처리(별도 계산서 서식 XML은 후속).
  - **매입 세금계산서 수취**(AP쪽 발행은 우리가 안 함 — 수취 관리는 후속).
  - **#4 부가세 신고서 집계**(다음 PR — 본 #3 데이터 위에 집계).

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (회사정보 설정, 정상):** WHEN 관리자가 회사정보(상호·사업자번호·대표자·주소·업태·종목) 저장(`FINANCE_SETTING_WRITE`), the system SHALL 테넌트 회사정보를 저장(테넌트 1행 upsert).
- **AC-2 (거래처 보강, 정상):** WHEN 거래처에 대표자·주소·업태·종목을 입력해 저장, the system SHALL 해당 필드를 영속(기존 businessNo·연락처 보존).
- **AC-3 (발행, 정상):** WHEN `APPROVED`/`PAID` AR 인보이스를 세금계산서로 발행, the system SHALL 공급자(회사정보)·공급받는자(customer)·작성일자·공급가액·세액·합계·품목을 **스냅샷 복사**해 `ISSUED` 세금계산서를 생성하고 AR과 연결한다. 이후 회사정보/거래처를 변경해도 발행본은 불변.
- **AC-4 (공급자 미설정, 예외):** IF 회사정보(공급자 상호·사업자번호 등 필수)가 미설정이면, THEN 발행을 **차단**하고 명확한 오류(예: `COMPANY_PROFILE_REQUIRED`, 4xx)를 반환(조용한 빈 값 발행 금지).
- **AC-5 (세액 정확, 정상):** WHILE AR taxType=TAXABLE, the system SHALL 세금계산서 세액 = 공급가액×10%(AR vatAmount 승계); ZERO_RATED·EXEMPT면 세액 0. 합계 = 공급가액+세액.
- **AC-6 (표준 XML, 정상):** WHEN `ISSUED` 세금계산서의 XML을 생성, the system SHALL well-formed XML에 **필수항목**(공급자·공급받는자 각각 사업자번호·상호·대표자·주소·업태·종목, 작성일자, 공급가액, 세액, 품목명·공급가액·세액, 세금계산서 종류)을 표준 구조로 포함한다.
- **AC-7 (취소, 정상):** WHEN `ISSUED` 세금계산서를 취소, the system SHALL `CANCELLED`로 전이하고, 취소본은 XML 생성을 거부(또는 취소 표식)하며 동일 AR의 재발행을 허용한다.
- **AC-8 (중복 발행, 예외):** IF 이미 `ISSUED` 세금계산서가 있는 AR을 다시 발행하면, THEN `409`(중복 발행)로 거부한다(CANCELLED 이력은 재발행 허용).
- **AC-9 (권한, 예외):** 발행·취소는 `FINANCE_WRITE`, 조회·XML은 `FINANCE_READ` 필요. 없으면 `403`.
- **AC-10 (테넌트 격리, 경계):** WHILE 다른 테넌트, the system SHALL 세금계산서·회사정보를 조회·발행 대상에서 제외(tenant_id 자동 필터).
- **AC-11 (작성일자, 경계):** WHEN 발행 시 작성일자 미지정이면, the system SHALL AR 인보이스 일자(invoiceDate)를 기본 작성일자로 사용한다.

## 4. 제약 / 비기능
- 금액 BigDecimal scale 2. 사업자등록번호는 #1 체크섬 검증 재사용(있으면). XML 인코딩 UTF-8.
- forward-only 마이그레이션 `V2011+`(finance=2xxx). 기존 V2003/V2010 수정 금지.
- 멀티테넌시: 신규 테이블 전부 `tenant_id`·공통 감사 컬럼(BaseEntity). `@SQLRestriction` 상속 주의(소프트삭제).
- XML 범위: **표준 구조 + 핵심 필수항목**의 well-formed 문서. 국세청 XSD 100% 준수·전자서명은 ASP 단계(범위 밖)임을 코드/문서에 명시.
- 모듈 경계: 전부 finance 내(회사정보·세금계산서·AR·Customer). 타 모듈 직접 참조 없음.

## 5. 경계 / Do-Not (3단계)
- ✅ 해도 됨: 회사정보 설정 엔티티·Customer 컬럼 보강·TaxInvoice 엔티티/발행·취소·표준 XML 생성기·세금계산서 화면, 헤더 단일 품목 매핑.
- ⚠️ 먼저 물어봐: 정식 다품목 라인 도입, 수정세금계산서, 계산서(면세) 별도 서식, 홈택스 전송/전자서명, 회사정보를 finance 밖(common)으로 이동.
- 🚫 절대 금지: 공급자 필수정보 없이 빈 값으로 조용히 발행(AC-4 위반), 발행본 스냅샷을 마스터 변경에 연동(불변성 위반), 기존 #43/#44 GL 분개·AR 결재 흐름 변경, 크로스테넌트 발행/조회, 시크릿 커밋, 기존 마이그레이션 수정.

## 6. Open Questions
- (없음 — 품목=헤더 단일·인적정보=정식 보강으로 확정. XML은 표준 구조+핵심 필수항목으로 확정.)

---

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):**
- 설정 1행 패턴: `TenantBaseCurrency`/`BaseCurrencyService`(테넌트 단일행 설정·get/set·권한) → `CompanyProfile`/`CompanyProfileService` 동형.
- 발행 파생: `ArInvoiceService.approve`의 GL 자동분개 지점과 별개로, 신규 `TaxInvoiceService.issue(arInvoiceId, request)`. AR 상태(APPROVED/PAID) 가드.
- 엔티티 기반: `BaseEntity`(tenant_id·version·soft delete·감사), `@SequenceGenerator`(finance.<t>_id_seq, allocationSize=50), `ApiResponse`/`PageResponse`.
- 금액·세액: AR의 supplyAmount/vatAmount/taxType 승계(`TaxType.computeVat` 재사용). 재계산 안 함(승계·스냅샷).
- 권한: `@PreAuthorize`/CurrentUserProvider 패턴(FINANCE_WRITE/READ/SETTING_WRITE).

**영향/신규 파일:**
- 신규 `finance.company_profile`(테넌트 1행): business_no·company_name·representative·address·business_type·business_item. 엔티티 `CompanyProfile` + `CompanyProfileService`(get/upsert) + `FinanceSettingController` 확장(또는 신규).
- `Customer` 보강: `representative_name`·`address`·`business_type`·`business_item` 컬럼·필드·`of`/`update`·DTO.
- 신규 `finance.tax_invoice`: ar_invoice_id(FK·UNIQUE where ISSUED)·issue_no(채번)·tax_type·charge_type(청구/영수)·write_date·supply_amount·vat_amount·total_amount·item_name·status(ISSUED/CANCELLED)·note + **공급자/공급받는자 스냅샷 컬럼**(supplier_*, buyer_*) 또는 임베디드. 엔티티 `TaxInvoice` + `TaxInvoiceLine` 불필요(단일 품목은 헤더 필드).
- `TaxInvoiceService`(issue·cancel·findAll·findById·toXml) + `TaxInvoiceRepository`(existsByArInvoiceIdAndStatus 등) + `TaxInvoiceController`(목록·상세·발행·취소·XML) + DTO.
- **XML 생성기** `NtsTaxInvoiceXmlGenerator`(전자세금계산서 표준 구조: 공급자/공급받는자/품목/세액). 순수 도메인 서비스(외부 의존 0), 표준 항목만.
- 프론트: `finance/company-profile`(또는 finance 설정 확장) 화면 · `finance/tax-invoices` 목록/상세/발행/XML 다운로드 · AR 거래처 폼 보강 · 사이드바 메뉴.

**모듈 경계:** 전부 finance. CompanyProfile은 세금계산서 공급자 신원으로 finance에 둠(타 모듈 미사용). #4(부가세 신고)도 finance라 재사용 가능.

**테스트 전략(AC↔테스트):**
- 통합(ArInvoiceGlPostingIntegrationTest 패턴): AR 생성→승인→세금계산서 발행→스냅샷·세액·합계(AC-3,5,11) / 공급자 미설정 발행 차단(AC-4) / 취소·재발행(AC-7) / 중복 발행 409(AC-8) / 권한 403(AC-9) / 테넌트 격리(AC-10).
- 단위: `CompanyProfileService` upsert(AC-1), `Customer` 보강 update(AC-2), `NtsTaxInvoiceXmlGenerator` 필수항목·well-formed·세액별(AC-6).
- 프론트: type-check/lint/build + e2e 발행·XML 다운로드 스모크.

## 8. 태스크 (test-first 순서, 한 기능=한 브랜치 `feature/e-tax-invoice`=한 PR, 태스크당 원자적 커밋)
| # | 태스크 | AC | 대상 파일 | 검증(exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | 회사정보 설정(공급자) 엔티티+마이그레이션+서비스 get/upsert+컨트롤러 | AC-1,4(전제),10 | CompanyProfile, CompanyProfileService, FinanceSettingController, V2011 | `cd backend && ./gradlew check` | — | |
| 2 | 거래처(Customer) 인적정보 보강(컬럼+엔티티+DTO+서비스) | AC-2,10 | Customer, CustomerService, DTO, V2012 | `./gradlew check` | — | [P] |
| 3 | TaxInvoice 엔티티+마이그레이션+발행·취소 서비스(스냅샷·중복차단·세액승계) | AC-3,4,5,7,8,9,10,11 | TaxInvoice, TaxInvoiceService, TaxInvoiceRepository, V2013 | `./gradlew check` | #1,#2 | |
| 4 | 국세청 표준 XML 생성기(필수항목·well-formed·세액별) | AC-6 | NtsTaxInvoiceXmlGenerator | `./gradlew check` | #3 | |
| 5 | 컨트롤러(목록·상세·발행·취소·XML)+DTO | AC-3,6,7,8,9 | TaxInvoiceController, DTO | `./gradlew check` | #3,#4 | |
| 6 | 프론트: 회사정보 설정 화면 + 거래처 폼 보강 | AC-1,2 | frontend finance 설정·ar 거래처 | `npm run type-check && npm run build` | #1,#2 | [P] |
| 7 | 프론트: 세금계산서 목록·상세·발행·XML 다운로드(한국 ERP UI) + 사이드바 | AC-3,7,8 | frontend finance/tax-invoices | `type-check && build` | #5 | |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 발행·스냅샷·세액·취소·중복·권한·테넌트 통합테스트 green + XML 필수항목 단위테스트 green + 회귀 없음(#2 부가세·#43/#44 GL·AR 결재 무영향).
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 회사정보 설정 → 과세 AR(공급가액 1,000,000) 승인 → 세금계산서 발행 → 상세(공급자/공급받는자/공급가액 1,000,000·세액 100,000·합계 1,100,000) → XML 다운로드(필수항목 확인). 공급자 미설정 시 발행 차단 확인. 중복 발행 409 확인.
4. 게이트: 태스크별 `/feature-add`(TDD) → 전 태스크 GREEN → `/feature-merge`(한 PR, AI 리뷰·CI).

> #4 부가세 신고서 집계는 본 #3 발행 데이터(세금계산서 매출 + 매입처별 합계표) 위에 집계 — 별도 후속 PR.
