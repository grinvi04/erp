# fx-gain-loss 스펙·플랜

## Context (왜)
외화 AP 인보이스를 지급(또는 AR 수금)할 때, **결제 시점 환율이 인보이스 생성 시 스냅샷 환율(#72)과 다르면** 기준통화 차액이 발생한다 — 실현 환차손익. 현재 결제 GL 자동분개(#43/#44)는 이를 반영하지 않아, 기준통화 원장에서 외상매입/매출금 통제계정이 완전히 청산되지 않는다(잔여 = 환차손익). 결제 분개에 **환차손익 라인을 자동 추가**해 원장을 정확히 청산한다. 데이터는 모두 가용(인보이스 exchangeRate/baseAmount, CurrencyConverter로 결제일 환율). 미실현(기말 평가)은 범위 밖.

**확정 결정**: ① 외환차손익 계정 = **테넌트 설정**(FX 설정에 환차손·환차이익 계정 지정, FINANCE_SETTING_WRITE). ② 외화 결제 분개는 **기준통화로 기록** — 통제계정@인보이스환율(원장 정확 청산)·현금@결제환율·차액@환차손익. ③ 외화 아님 또는 환율 부재 또는 환차손익 계정 미설정이면 **기존 동작 유지(환차 라인 생략)** — 결제는 막지 않음(플래그/로그).

---

## 1. 목표 & Why
외화 결제의 환율 변동 차액을 실현 환차손익으로 GL 자동 분개해 기준통화 원장을 정확히 청산. **성공 기준: 외화 인보이스를 인보이스환율과 다른 결제환율로 지급/수금 시, 결제 분개가 통제계정을 인보이스환율로 청산하고 차액을 환차손/환차이익 계정에 분개해 기준통화 차대변이 균형.**

## 2. Scope
- **In:**
  - **환차손익 계정 설정**: FX 설정(테넌트)에 `fxGainAccountId`·`fxLossAccountId`(Account FK) 추가. 조회/변경 FINANCE_SETTING_WRITE. 기존 `finance/fx` 화면에 계정 선택 추가.
  - **AP 지급 환차손익**(`ApInvoicePostingService.postPaymentDraft`): 외화·인보이스 exchangeRate 있음·결제일 환율 있음·환차손익 계정 설정됨 → 결제 분개를 **기준통화로**: (차)외상매입금=paidAmount×인보이스환율, (대)현금=paidAmount×결제환율, 차액 = (차)환차손 또는 (대)환차이익. 분개 통화=기준통화·rate=1.
  - **AR 수금 환차손익**(`ArInvoicePostingService.postPaymentDraft`): 부호 반전 — (차)현금=paidAmount×결제환율, (대)외상매출금=paidAmount×인보이스환율, 차액 = (차)환차손 또는 (대)환차이익.
  - **부호**: 차액 = paidAmount×(결제환율−인보이스환율). AP: 양수(더 지급)=환차손(비용·차변), 음수=환차이익(수익·대변). AR: 양수(더 수금)=환차이익(대변), 음수=환차손(차변).
  - **폴백**: 외화 아님(통화=기준)·인보이스환율 null·결제일환율 없음·환차손익 계정 미설정 중 하나면 **기존 원통화 결제 분개 그대로(환차 라인 없음)** + 사유 로그(event=FX_GAINLOSS_SKIPPED).
  - 프론트: FX 설정 화면에 환차손·환차이익 계정 선택.
- **Out (Non-goals):**
  - **미실현 환차손익**(기말 외화자산·부채 평가·재환산) — 별도 기능.
  - 환차손익 별도 분개(결제 분개에 라인으로 포함, 분리 분개 아님).
  - 다단계·부분환차 복잡 배분(부분지급은 paidAmount 비례로 자연 처리 — 추가 배분 로직 없음).
  - 환차손익 계정 자동 생성·계정과목 시드.
  - AP/AR 외 거래(분개 직접 입력 등)의 환차.

## 3. 기능 요구사항 + 수용기준 (테스트 계약)
- **AC-1 (환차손익 계정 설정, 정상):** WHEN 관리자가 환차손·환차이익 계정 설정(FINANCE_SETTING_WRITE), the system SHALL 테넌트 FX 설정에 저장.
- **AC-2 (AP 환차손, 정상):** WHEN 외화 AP를 인보이스환율보다 **높은** 결제환율로 지급(설정·환율 충족), the system SHALL 결제 분개에 (차)환차손 = paidAmount×(결제−인보이스)환율 라인을 추가하고 외상매입금을 인보이스환율로 청산, 기준통화 차대변 균형.
- **AC-3 (AP 환차이익, 정상):** WHEN 결제환율이 인보이스환율보다 **낮으면**, (대)환차이익 라인, 균형.
- **AC-4 (AR 환차이익, 정상):** WHEN 외화 AR을 인보이스환율보다 높은 결제환율로 수금, (대)환차이익 = paidAmount×(결제−인보이스)환율, 외상매출금 인보이스환율 청산, 균형.
- **AC-5 (AR 환차손, 정상):** 결제환율<인보이스환율이면 (차)환차손, 균형.
- **AC-6 (기준통화 거래, 경계):** WHILE 인보이스 통화=기준통화, the system SHALL 기존 원통화 결제 분개 유지(환차 라인 없음).
- **AC-7 (환율/설정 부재 폴백, 예외):** IF 인보이스 exchangeRate=null 또는 결제일 환율 없음 또는 환차손익 계정 미설정, THEN 환차 라인 없이 기존 결제 분개 생성(결제 차단 안 함) + 사유 로그.
- **AC-8 (부분지급, 경계):** 부분지급 시 환차 = **paidAmount** 기준(전액 아님), 청산도 paidAmount×인보이스환율.
- **AC-9 (균형, 정상):** 환차 라인 포함 결제 분개는 항상 차대변 균형(±0.01)이며 POSTED 가능.
- **AC-10 (권한, 예외):** 환차손익 계정 설정 변경에 FINANCE_SETTING_WRITE 없으면 403.

## 4. 제약/비기능
- 결제 분개는 기존처럼 DRAFT 생성→GL 결재(#69) 경유. 환차 라인 추가가 결재·균형을 깨지 않음.
- BigDecimal scale 2(금액)·8(환율), 균형 ±0.01. 환차 계정은 EXPENSE(환차손)·REVENUE(환차이익) 권장(검증은 강제 안 함, 설정 자유).
- forward-only 마이그레이션.

## 5. 경계 / Do-Not
- ✅ 해도 됨: FX 설정에 계정 2개 추가, postPaymentDraft에 환차 라인, 부호·기준통화 분개, 폴백 로그.
- ⚠️ 먼저 물어봐: 미실현 평가손익·재환산 도입, 환차 별도 분개 분리, 환차손익 계정 유형 강제, 환차 분개 자동 POST.
- 🚫 절대 금지: 환율 차액을 0으로 조용히 처리(원장 미청산), 결제 자체 차단(폴백으로 진행), 기존 #43/#44 기준통화 거래 동작 변경, AP/AR 기존 결제 흐름·결재 깨기, 시크릿 커밋.

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):** `TenantBaseCurrency`/`BaseCurrencyService`(테넌트 FX 설정), `ApInvoicePostingService.postPaymentDraft`·`ArInvoicePostingService.postPaymentDraft`(결제 분개 생성 지점), `JournalEntryService.createInternal`(라인 추가), `CurrencyConverter.tryConvert(amount,currency,date)`(결제일 환율), `ApInvoice.exchangeRate/baseAmount`(스냅샷), `Account`, `finance/fx` 화면, `lib/money`.
- **설정**: `tenant_base_currency`에 `fx_gain_account_id`·`fx_loss_account_id`(nullable FK) 컬럼 추가(테넌트당 1행 FX 설정 = 그 엔티티에 자연 귀속) + 마이그레이션. `BaseCurrencyService`(또는 FxSettingService)에 get/set. `FxController`에 노출.
- **환차 계산·분개**: postPaymentDraft에서 외화·rate 충족 시 결제일 환율 = `currencyConverter.tryConvert(paidAmount, currency, paymentDate)`. invoiceRate=invoice.getExchangeRate(). 통제계정청산=paidAmount×invoiceRate, 현금=paidAmount×paymentRate, 차액=둘의 차. 환차 라인(gain/loss 계정·차/대) 추가. 결제 분개를 **기준통화 금액**으로 구성(통화=기준, rate=1). 폴백 시 기존 원통화 2라인.
- **모듈 경계**: 전부 finance 내(설정·posting·account). CRM 무관.
- **로깅**: 폴백 시 event=FX_GAINLOSS_SKIPPED(operations.md 패턴, #78).

**테스트 전략(AC↔테스트):** 통합테스트(외화 인보이스 생성→승인→환율 변동 등록→지급/수금, ApInvoiceGlPostingIntegrationTest 패턴): AC-2~5 환차손/이익 라인 금액·부호·균형, AC-6 기준통화 무환차, AC-7 폴백(환율/설정 부재), AC-8 부분지급, AC-9 균형, AC-1/10 설정·권한. 단위(환차 계산·부호).

## 8. 태스크 (test-first, 1 PR 권장 — 응집)
### feature/fx-gain-loss
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 1 | FX 설정에 환차손·환차이익 계정(컬럼+마이그레이션+서비스 get/set+컨트롤러) | AC-1,10 | TenantBaseCurrency, BaseCurrencyService/FxController, V2xxx | `./gradlew check` | — |
| 2 | AP postPaymentDraft 환차손익 라인(기준통화 분개·부호·폴백) | AC-2,3,6,7,8,9 | ApInvoicePostingService | `./gradlew check` | #1 |
| 3 | AR postPaymentDraft 환차손익 라인(부호 반전) | AC-4,5,6,7,8,9 | ArInvoicePostingService | `./gradlew check` | #1 |
| 4 | 프론트 FX 설정 화면에 환차손·환차이익 계정 선택 | AC-1 | frontend finance/fx | `type-check && build` | #1 |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 환차 통합테스트 green(손/이익 부호·균형·폴백·부분지급) + 회귀 없음(기준통화 결제·기존 #43/#44 무영향).
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 환차손익 계정 설정 → USD 인보이스(환율 1300) 생성·승인 → 환율 1350 등록 → 지급 → 결제 분개에 환차손 라인(50×수량) + 외상매입금 1300 청산 확인. 미설정/기준통화는 환차 없음 확인.
4. 게이트: `/feature-add`(태스크 TDD)→`/feature-merge`(focused 리뷰·CI).
