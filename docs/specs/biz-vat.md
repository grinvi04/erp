# biz-vat 스펙

> 로드맵 단일 출처: [`docs/roadmap-localization.md`](roadmap-localization.md) Tier 1 #1·#2.
> 후속(범위 밖): 전자세금계산서 발행(#3) · 부가세 신고서 집계(#4).

## 1. 목표 & Why
한국 B2B의 전제인 **부가세 처리 기반**을 깐다 — (#1) 거래처 사업자등록번호 검증, (#2) 매입/매출
인보이스의 공급가액·부가세액 분리와 부가세대급금/예수금 GL 자동분개. 기존 AP/AR + GL posting
(#43/#44)에 얹는 확장. **성공 기준: 과세 인보이스를 승인·전기하면 결제 분개가 공급가액(비용/매출)
·세액(부가세 통제계정)·총액(거래처 통제계정)으로 분리 전기되어 기준통화 차대변이 균형하고, 잘못된
사업자번호는 저장이 거부된다.**

## 2. Scope
- **In:**
  - **사업자번호 검증**: Customer·Vendor의 기존 `business_no` 필드에 한국 사업자등록번호 **체크섬 검증**(10자리 알고리즘)을 생성·수정 시 적용. 빈값은 허용(선택 필드), 입력 시 형식·체크섬 검증.
  - **인보이스 부가세 분리**: AP/AR 인보이스에 `taxType`(과세 TAXABLE / 영세율 ZERO_RATED / 면세 EXEMPT, **인보이스 단위 단일**), `supplyAmount`(공급가액=라인 합), `vatAmount`(세액) 추가. `totalAmount = supplyAmount + vatAmount`(파생).
  - **세액 자동계산·수정불가**: 과세 → `vatAmount = 공급가액 × 10%`, **원 미만 절사**. 영세율·면세 → `vatAmount = 0`.
  - **부가세 GL 자동분개**: 승인 전기 분개에 부가세 라인 추가. AP: (차)비용/자산[라인=공급가액]·부가세대급금[세액] (대)외상매입금[총액]. AR: (대)매출[라인=공급가액]·부가세예수금[세액] (차)외상매출금[총액].
  - **부가세 통제계정 설정**: 테넌트가 부가세대급금(매입)·부가세예수금(매출) 계정을 지정. **FX 손익계정 패턴 재사용**(TenantBaseCurrency 확장, FINANCE_SETTING_WRITE).
  - **폴백**: 세액=0이거나 부가세 통제계정 미설정이면 부가세 라인 없이 기존 분개 그대로(전기 차단 안 함) + 사유 로그.
  - 프론트: 거래처 사업자번호 필드, 인보이스 과세구분·공급가액·세액·총액 표시.
- **Out (Non-goals):**
  - **전자세금계산서 발행·국세청 XML/전송**(roadmap #3) — 별도 기능.
  - **부가세 신고서 집계**(roadmap #4).
  - **국세청 사업자번호 진위확인 API**(외부 인증키 필요) — 체크섬 검증까지만.
  - **라인 단위 과세구분**(혼합 과세/면세 인보이스) — 인보이스 단위로 결정됨.
  - **세액 수동 보정** — 자동계산·수정불가로 결정됨.
  - **수입부가세(세관)·외화 인보이스 특수 세무** — 자동계산은 인보이스 통화·공급가액 기준, 세관 부가세는 범위 밖.

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (사업자번호 검증, 정상):** WHEN 유효한 10자리 사업자등록번호로 거래처를 생성/수정, the system SHALL 저장한다. (빈값도 저장 — 선택 필드)
- **AC-2 (사업자번호 검증, 예외):** IF 사업자번호 형식·체크섬이 틀리면 THEN the system SHALL 400(검증 오류 코드)을 반환하고 저장하지 않는다.
- **AC-3 (과세 자동세액, 정상):** WHEN 과세 인보이스의 공급가액=100,000, the system SHALL vatAmount=10,000·totalAmount=110,000으로 산출한다.
- **AC-4 (원 미만 절사, 경계):** WHILE 공급가액=12,345·과세, the system SHALL vatAmount=1,234(12,345×0.1=1,234.5 → 절사)·totalAmount=13,579으로 산출한다.
- **AC-5 (영세율·면세, 경계):** WHILE taxType=영세율 또는 면세, the system SHALL vatAmount=0·totalAmount=공급가액으로 산출한다.
- **AC-6 (AP 부가세 분개, 정상):** WHEN 과세 AP 인보이스를 승인 전기(부가세 통제계정 설정됨), the system SHALL (차)라인 비용/자산=공급가액·부가세대급금=세액, (대)외상매입금=총액 분개를 생성하고 차대변이 균형한다.
- **AC-7 (AR 부가세 분개, 정상):** WHEN 과세 AR 인보이스를 승인 전기(설정됨), the system SHALL (대)매출=공급가액·부가세예수금=세액, (차)외상매출금=총액 분개를 생성하고 균형한다.
- **AC-8 (통제계정 미설정 폴백, 예외):** IF 세액>0이나 부가세 통제계정 미설정 THEN the system SHALL 부가세 라인 없이 기존 분개(공급가액=총액 가정)를 생성하고 전기를 차단하지 않으며 사유를 로그한다.
- **AC-9 (기존 데이터 백필, 경계):** WHILE 마이그레이션 적용, the system SHALL 기존 인보이스를 supplyAmount=totalAmount·vatAmount=0·taxType=과세로 백필하고 기존 전기 분개에 영향을 주지 않는다.
- **AC-10 (설정 권한, 예외):** IF FINANCE_SETTING_WRITE 권한 없이 부가세 통제계정 설정을 변경 THEN the system SHALL 403을 반환한다.
- **AC-11 (균형 불변, 정상):** WHILE 부가세 라인 포함 분개, the system SHALL 항상 차대변 균형(±0.01)이며 기존 결재·전기 흐름을 깨지 않는다.

## 4. 제약 / 비기능
- BigDecimal scale 2(금액). 세액 원 미만 절사 = `supplyAmount.multiply(0.10).setScale(0, DOWN)` 후 scale 2 복원. 균형 ±0.01.
- forward-only 마이그레이션(V2009~). 기존 V2001~V2008 수정 금지.
- 모듈 경계: 전부 finance 내(거래처·인보이스·posting·설정). 타 모듈 무관.
- 폴백 로그 event=VAT_POSTING_SKIPPED (operations.md 패턴, FX_GAINLOSS_SKIPPED와 동형).

## 5. 경계 / Do-Not
- ✅ 해도 됨: 거래처 체크섬 검증, 인보이스 taxType/supplyAmount/vatAmount 추가·자동계산, posting 부가세 라인, TenantBaseCurrency에 부가세 통제계정 2개 추가, 폴백 로그, 프론트 표시.
- ⚠️ 먼저 물어봐: 라인 단위 과세 도입, 세액 수동 보정 허용, 전자세금계산서·신고서, 수입부가세, 부가세 통제계정 유형 강제, 부가세 분개 자동 POST.
- 🚫 절대 금지: 세액을 조용히 0 처리(원장 왜곡), 인보이스 생성/전기 자체 차단(폴백으로 진행), 기존 #43/#44·FX 분개 동작 변경, 기존 AP/AR 결재·전기 흐름 깨기, 기존 마이그레이션 수정, 시크릿 커밋.

## 7. 기술 접근 (HOW)
**근거(실제 코드 선독):**
- `Customer`/`Vendor`에 `business_no`(businessNo) **필드 이미 존재** → 검증·프론트만 추가(필드 신설 아님).
- `ApInvoice`/`ArInvoice`: `totalAmount`(총액)·라인(account, amount). `create(...)`가 totalAmount를 받음 → **공급가액·세액 분리 시 totalAmount는 파생**으로 전환(supplyAmount=라인 합, vatAmount=계산, total=합).
- `ApInvoicePostingService`/`ArInvoicePostingService`: 라인→차/대변 + 거래처 통제계정(payablesAccount/receivablesAccount). **부가세 라인 추가 지점**. Javadoc이 이미 "부가세대급금" 상정.
- `TenantBaseCurrency.assignFxAccounts(fxGain, fxLoss)` + V2008: **테넌트 통제계정 설정 패턴** → 부가세대급금/예수금 동형 추가(`assignVatAccounts`).
- `JournalEntryService.createInternal(request)`: 분개 라인 추가. `FxPaymentJournalFactory`: 라인 빌드 재사용 패턴.
- 계정 시드 없음(테넌트 생성) → 부가세 계정도 테넌트가 만들어 설정으로 지정.

**접근:** 모델(taxType·supplyAmount·vatAmount + 자동계산 도메인 메서드) → 설정(부가세 통제계정) → posting 확장(부가세 라인 + 폴백) → 프론트. 도메인 계산은 엔티티/도메인서비스에 둔다(IO 비의존).

**테스트 전략(AC↔레벨):** 단위 — 세액 자동계산·절사(AC-3,4,5), 사업자번호 체크섬(AC-1,2). 통합(ApInvoiceGlPostingIntegrationTest 패턴) — AP/AR 부가세 분개·균형(AC-6,7,11)·폴백(AC-8)·백필(AC-9)·설정 권한(AC-10).

## 8. 태스크 (test-first 순서, feature/biz-vat 한 브랜치)
| # | 태스크 | AC | 대상 | 검증(exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | 사업자번호 체크섬 검증(Customer·Vendor 생성·수정) | AC-1,2 | Customer/Vendor 도메인 또는 검증기, 서비스 | `cd backend && ./gradlew check` | — | [P] |
| 2 | 부가세 통제계정 설정(TenantBaseCurrency 확장+V2009 컬럼+서비스 get/set+FxController 노출) | AC-10 | TenantBaseCurrency, BaseCurrencyService/FxController, V2009 | `./gradlew check` | — | |
| 3 | 인보이스 taxType·supplyAmount·vatAmount 모델 + 자동세액계산 + V2009 컬럼·백필 | AC-3,4,5,9 | ApInvoice/ArInvoice(+Line 합), V2009 | `./gradlew check` | — | |
| 4 | AP posting 부가세대급금 라인 + 폴백·로그 | AC-6,8,11 | ApInvoicePostingService | `./gradlew check` | #2,#3 | |
| 5 | AR posting 부가세예수금 라인 + 폴백·로그 | AC-7,8,11 | ArInvoicePostingService | `./gradlew check` | #2,#3 | |
| 6 | 프론트: 거래처 사업자번호 필드 + 인보이스 과세구분·공급가액·세액·총액 표시 | AC-1 | frontend customers/vendors·invoices/ar-invoices, finance/fx 설정 | `npm run type-check && lint && build` | #2,#3 | |

> 마이그레이션은 V2009 한 파일에 인보이스 컬럼·백필 + tenant_base_currency 부가세 계정 컬럼을 함께(또는 V2009/V2010 분할). forward-only, V2008까지 수정 금지.

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 부가세 통합테스트 green(과세/영세/면세 세액·절사·균형·폴백·백필·권한) + 회귀 없음(기존 #43/#44·FX·결재 무영향).
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 부가세 통제계정 설정 → 과세 매출 인보이스(공급가액 100,000) 생성·승인·전기 → 분개에 (대)매출 100,000·부가세예수금 10,000·(차)외상매출금 110,000 확인. 잘못된 사업자번호 거부 확인. 미설정/면세는 부가세 라인 없음 확인.
4. 게이트: 태스크별 `/feature-add`(TDD) → `/feature-merge`(focused 리뷰·CI).
