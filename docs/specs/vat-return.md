# vat-return (부가세 신고서 집계) 스펙

> 현지화 로드맵 Tier1 #4. 단일 출처: `docs/roadmap-localization.md`.
> 전제: #2 부가세 분리(ApInvoice/ArInvoice supplyAmount/vatAmount/taxType)·#3 전자세금계산서(TaxInvoice) 완료.
> **바깥 경계**: 신고 **기초자료 산출까지**. 국세청 신고서 서식 XML·홈택스 전송·전자신고 파일은 범위 밖.

## 1. 목표 & Why
신고기간(from~to)의 **매출세액(발행 세금계산서 기준) − 매입세액(매입 인보이스 기준) = 납부(환급)세액**을 자동 집계하고, 매출처별·매입처별 합계표를 산출해 부가세 예정·확정 신고의 기초자료를 제공한다. 수기 집계·엑셀 대조를 없애고 #1~#3 데이터에 얹는 집계라 레버리지가 높다. **성공 기준(측정 가능): 신고기간을 지정하면 그 기간의 과세/영세율/면세별 매출 공급가액·세액, 매입 공급가액·세액, 납부(환급)세액이 정확히 합산되고(±0원), 매출처·매입처별 합계표가 사업자번호 단위로 집계되어 반환된다.**

## 2. Scope
- **In:**
  - **신고 요약**: 신고기간(from~to) 기준 — 매출(과세 공급가액·세액, 영세율 공급가액, 면세 공급가액), 매입(공급가액·세액), **납부(환급)세액 = 매출세액 − 매입세액**.
  - **매출 집계 원천 = 발행 세금계산서**(`TaxInvoice`, status=ISSUED, `writeDate` 기간 내). 취소(CANCELLED)는 제외. taxType별(과세/영세율/면세) 분류.
  - **매입 집계 원천 = 매입 인보이스**(`ApInvoice`, status∈{APPROVED, PAID}, `invoiceDate` 기간 내). DRAFT·결재중·취소 제외. taxType별 분류.
  - **매출처별 합계표**: 발행 세금계산서를 공급받는자 **사업자번호** 단위로 group — 거래처명·사업자번호·매수·공급가액 합계·세액 합계.
  - **매입처별 합계표**: 매입 인보이스를 공급업체(Vendor) **사업자번호** 단위로 group — 업체명·사업자번호·매수·공급가액 합계·세액 합계.
  - **조회 화면**: 부가세신고 화면(조회기간 — 직접 from/to + 분기 프리셋, 요약 카드, 매출처별·매입처별 합계표 그리드, 엑셀 다운로드). 사이드바 추가.
- **Out (Non-goals):**
  - **국세청 신고서 서식**(부가가치세 신고서 별지 서식)·**전자신고 파일/XML**·홈택스 전송.
  - **세금계산서 없는 매출**(신용카드·현금영수증·간주공급) — 본 집계는 **발행 세금계산서 기준**(미발행 과세매출은 미포함, 명시적 제외).
  - **매입세액 불공제·의제매입세액·대손세액공제·가산세·예정고지** 등 신고서 조정 항목.
  - **수정신고·경정청구**.
  - 매입 측 **수취 세금계산서 별도 모델링**(현재 미존재) — `ApInvoice`를 매입 원천으로 사용.
  - 집계 결과의 **영속/스냅샷 저장**(신고 확정본 보관) — 매번 실시간 집계(읽기 전용).

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (매출 집계, 정상):** WHEN 신고기간(from~to)으로 조회, the system SHALL writeDate가 기간 내이고 status=ISSUED인 세금계산서를 taxType별로 공급가액·세액 합산한다(과세 세액>0, 영세율·면세 세액=0).
- **AC-2 (매입 집계, 정상):** WHEN 동일 기간 조회, the system SHALL invoiceDate가 기간 내이고 status∈{APPROVED,PAID}인 매입 인보이스를 공급가액·세액 합산한다.
- **AC-3 (납부세액, 정상):** the system SHALL 납부(환급)세액 = (매출 세액 합계) − (매입 세액 합계)를 반환한다(음수면 환급).
- **AC-4 (매출처별 합계표, 정상):** the system SHALL 발행 세금계산서를 공급받는자 사업자번호 단위로 group해 거래처명·사업자번호·매수·공급가액합·세액합을 반환한다(같은 사업자번호 다건은 1행으로 합산).
- **AC-5 (매입처별 합계표, 정상):** the system SHALL 매입 인보이스를 공급업체 사업자번호 단위로 group해 동일 형태로 반환한다.
- **AC-6 (상태 필터, 경계):** WHILE 세금계산서가 CANCELLED이거나 매입 인보이스가 DRAFT/PENDING_APPROVAL/CANCELLED이면, the system SHALL 그 건을 집계에서 **제외**한다.
- **AC-7 (기간 경계, 경계):** the system SHALL from·to를 **포함**(inclusive)해 집계한다(경계일 포함). 데이터 없으면 0/빈 합계표.
- **AC-8 (사업자번호 부재, 경계):** IF 거래처/업체 사업자번호가 null/빈값이면, the system SHALL 그 건을 별도 그룹("미상" 등 식별 키)으로 합산하되 누락하지 않는다.
- **AC-9 (권한, 예외):** IF FINANCE_READ 권한이 없으면 THEN 403.
- **AC-10 (입력 검증, 예외):** IF from > to 이거나 날짜 형식이 잘못되면 THEN 400.
- **AC-11 (테넌트 격리, 경계):** the system SHALL 현재 테넌트의 세금계산서·매입 인보이스만 집계한다(@TenantId 자동 필터).

## 4. 제약 / 비기능
- 금액 BigDecimal scale 2, 합산 정확(±0원). 세액은 각 인보이스 저장값(절사 완료)을 단순 합산(재계산 없음).
- 집계는 읽기 전용(@Transactional readOnly), DB GROUP BY로 산출(애플리케이션 N+1 금지).
- 모듈 경계: 전부 finance 내(TaxInvoice·ApInvoice·Vendor 집계). 타 모듈 미참조.
- 기간은 LocalDate from/to(신고기간) — 회계기간(FiscalPeriod)에 강결합하지 않음(부가세 분기 ≠ 회계기간).

## 5. 경계 / Do-Not (3단계)
- ✅ 해도 됨: TaxInvoice/ApInvoice 집계 @Query·projection, VatReturnService 집계·납부세액, 합계표 group, 부가세신고 화면.
- ⚠️ 먼저 물어봐: 매출 원천을 AR 인보이스(미발행 과세매출 포함)로 확장, 신고 확정본 영속 저장, 국세청 서식/전자신고 파일, 매입세액 불공제·의제매입 등 조정 항목.
- 🚫 절대 금지: 세액을 재계산(저장값 합산만), 취소/미승인 건 집계 포함, 크로스테넌트 집계, 애플리케이션 루프 합산(GROUP BY 사용), 시크릿 커밋.

## 6. Open Questions
- (없음 — 매출=세금계산서 ISSUED·매입=ApInvoice(APPROVED/PAID)·기간 from/to inclusive·합계표 사업자번호 group으로 확정. 서식/전송·조정항목은 범위 밖.)

---

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):** `FinanceAnalyticsService`(집계 오케스트레이션·FINANCE_READ)·`ApInvoiceRepository.monthlyInvoices`(JPQL `EXTRACT`/`SUM`/`GROUP BY` + **projection interface**)·record 응답 DTO·`PageResponse` 외 단건 응답. `TaxType.computeVat` 재계산 안 함(저장값 합산).

- **매출 집계(TaxInvoice)**: `TaxInvoiceRepository`에 ① taxType별 합계(기간·ISSUED group by taxType → 공급가액합·세액합), ② 매출처별 합계(group by buyer.businessNo, buyer.companyName) projection. `writeDate BETWEEN :from AND :to AND status='ISSUED'`.
- **매입 집계(ApInvoice)**: `ApInvoiceRepository`에 ① 매입 합계(기간·APPROVED/PAID → 공급가액합·세액합), ② 매입처별 합계(group by vendor.businessNo, vendor.name) projection. `invoiceDate BETWEEN :from AND :to AND status IN ('APPROVED','PAID')`.
- **VatReturnService**: 기간 입력 → 위 집계 호출 → 요약(매출 taxType별·매입·납부세액=매출세액−매입세액) + 합계표 조립. FINANCE_READ. from>to 검증(400).
- **컨트롤러**: `GET /api/finance/vat-return?from=YYYY-MM-DD&to=YYYY-MM-DD` → `VatReturnResponse`(요약 + 매출처별[] + 매입처별[]).
- **프론트**: `finance/vat-return` 화면 — 조회기간(직접 + 분기 프리셋), 요약 카드(StatCard 톤), 합계표 2개(DataTable + 합계행·엑셀), 사이드바·커맨드팔레트. 한국 ERP UI 패러다임.

**테스트 전략(AC↔테스트):**
- `@DataJpaTest`(repository 집계): AC-1·2 합산, AC-6 상태필터 제외, AC-7 기간 경계(inclusive), AC-8 사업자번호 null group, AC-4·5 group by 정확. 실 DB GROUP BY 검증.
- 단위(VatReturnService, mock repo): AC-3 납부세액(양수/음수 환급), AC-10 from>to 400, AC-9 권한.
- 통합(`@Transactional`, AbstractIntegrationTest): 세금계산서 발행 + 매입 인보이스 승인 시드 → 기간 집계 → 요약·합계표·납부세액 end-to-end(AC-1~8·11).
- 프론트: type-check/lint/build + 화면 렌더 스모크.

## 8. 태스크 (test-first 순서, 한 기능=한 브랜치 `feature/vat-return`=한 PR, 태스크당 원자적 커밋)
| # | 태스크 | AC | 대상 파일 | 검증(exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | 매출 집계 쿼리(TaxInvoiceRepository: taxType별·매출처별 projection, 기간·ISSUED) | AC-1,4,6,7,8,11 | TaxInvoiceRepository, projection | `cd backend && ./gradlew check` | — | |
| 2 | 매입 집계 쿼리(ApInvoiceRepository: 합계·매입처별 projection, 기간·APPROVED/PAID) | AC-2,5,6,7,8,11 | ApInvoiceRepository, projection | `./gradlew check` | — | [P] |
| 3 | VatReturnService + DTO(요약·납부세액·합계표 조립, 권한, from>to 검증) + 컨트롤러 | AC-3,9,10 + 통합 1~8 | VatReturnService, VatReturnController, DTO | `./gradlew check` | #1,#2 | |
| 4 | 프론트 부가세신고 화면(조회기간·요약·매출처별/매입처별 합계표·엑셀)+사이드바 | AC-1~5(표시) | frontend finance/vat-return | `npm run type-check && npm run build` | #3 | |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 집계 쿼리·서비스·통합테스트 green(매출-매입 납부세액·합계표·상태필터·기간경계·사업자번호 null·권한) + 회귀 없음.
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 과세 세금계산서 발행(공급가액 100만·세액 10만) + 매입 인보이스 승인(공급가액 50만·세액 5만) → 부가세신고 화면에서 해당 분기 조회 → 매출세액 10만·매입세액 5만·**납부세액 5만**, 매출처별/매입처별 합계표 확인. 취소/미승인 건 제외 확인.
4. 게이트: 태스크별 `/feature-add`(TDD) → 전 태스크 GREEN → `/feature-merge`(한 PR, AI 리뷰·CI).
