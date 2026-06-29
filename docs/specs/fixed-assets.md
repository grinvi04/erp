# fixed-assets (고정자산·감가상각) 스펙

> 현지화 로드맵 Tier3 #7. 단일 출처: `docs/roadmap-localization.md`.
> 기존 GL 자동분개(#43/#44)·계정 설정(부가세·환차 계정 FK)·회계기간·결재(GL #69) 패턴을 재사용한다.

## 1. 목표 & Why
고정자산 대장을 관리하고 월별 감가상각(정액법·정률법)을 자동 계산해 **감가상각비/감가상각누계액을 GL에 자동 분개**하며, 처분(매각·폐기) 시 처분손익을 분개한다. 수기 상각표·분개를 없애고 자산 생애주기(취득→상각→처분)를 회계에 통합한다. **성공 기준(측정 가능): 자산을 등록하고 특정 월의 상각을 처리하면, 상각방법에 맞는 월 상각액이 계산되어 (차)감가상각비 (대)감가상각누계액 균형 분개(DRAFT)가 생성되고 자산 누계상각액·장부가액이 갱신되며, 같은 월을 다시 처리해도 중복 상각되지 않는다.**

## 2. Scope
- **In:**
  - **자산대장(FixedAsset)**: 코드·명칭·취득일·취득원가·잔존가치·내용연수(월)·상각방법(정액/정률)·정률 상각률(정률 시)·자산계정(유형자산 FK)·상태(가동/처분). 파생: 누계상각액·장부가액.
  - **감가상각 계산**: **정액법**(월상각=(취득원가−잔존가치)/내용연수월) · **정률법**(월상각=월초 장부가액×(연상각률/12)). 둘 다 **잔존가치 하한**(장부가액이 잔존가치 미만으로 내려가지 않게 마지막 상각 보정). 취득월부터 상각.
  - **월별 상각 처리(run)**: 지정 회계기간(월) 기준 가동 자산들의 당월 상각액 계산 → **GL 자동분개**((차)감가상각비 (대)감가상각누계액, DRAFT→GL 결재 경유) + 누계상각액 갱신 + 상각 이력 기록. **멱등**: 이미 상각한 (자산,기간)은 건너뛴다.
  - **처분(매각·폐기)**: 처분일까지 상각 반영 후 장부가액 제거 — (차)감가상각누계액·현금(매각액) (대)자산, 차액=처분손익((차)처분손실 또는 (대)처분이익) GL 분개. 상태 처분, 이후 상각 제외.
  - **계정 설정(테넌트)**: 감가상각비·감가상각누계액·처분이익·처분손실 계정 FK. 조회/변경 FINANCE_SETTING_WRITE.
  - **화면**: 고정자산 목록(취득원가·누계상각·장부가액·상태·합계)·등록/수정 폼·상세(상각 이력·관련 전표)·월별 상각 실행·처분 다이얼로그. 계정 설정(기존 FX/설정 화면 확장). 사이드바.
- **Out (Non-goals):**
  - **세무상 상각**(상각부인·시인·세무조정·세무상 내용연수) — **회계상 상각만**.
  - 자산 **재평가·손상차손·리스(사용권)자산**.
  - 정액·정률 **외 상각방법**(생산량비례·연수합계 등).
  - 자산 **이동(부서/위치 변경)·부분처분·자산 분할/병합**.
  - **비동기 배치**(월말 자동 스케줄) — 사용자 트리거 동기 실행, 자산 수 상한 내.
  - 상각 분개 자동 POST(결재 없이 전기) — 기존 GL 결재 흐름 경유.

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (자산 등록, 정상):** WHEN 자산(취득원가·취득일·내용연수·상각방법·자산계정 등)을 등록(FINANCE_WRITE), the system SHALL 자산을 저장하고 누계상각액 0·장부가액=취득원가로 초기화.
- **AC-2 (정액 상각액, 정상):** WHILE 상각방법=정액, the system SHALL 월상각액 = (취득원가−잔존가치)/내용연수월(원 미만 처리 일관).
- **AC-3 (정률 상각액, 정상):** WHILE 상각방법=정률, the system SHALL 월상각액 = 월초 장부가액×(연상각률/12).
- **AC-4 (월 상각 처리·분개, 정상):** WHEN 특정 회계기간의 상각을 처리, the system SHALL 가동 자산별 당월 상각액으로 (차)감가상각비 (대)감가상각누계액 균형 DRAFT 분개를 만들고 누계상각액·장부가액을 갱신하며 상각 이력을 남긴다.
- **AC-5 (멱등, 경계):** IF 같은 (자산,기간)을 다시 처리하면, THEN the system SHALL 중복 상각·중복 분개를 만들지 않는다(이미 처리분 건너뜀).
- **AC-6 (잔존가치/내용연수 한도, 경계):** WHILE 장부가액이 잔존가치에 도달(또는 내용연수 종료), the system SHALL 추가 상각액을 0으로(과대상각 금지, 마지막 기간은 잔존가치까지만).
- **AC-7 (처분, 정상):** WHEN 자산을 매각/폐기, the system SHALL 장부가액 제거·처분손익 균형 분개를 만들고 상태=처분으로 전이하며 이후 상각에서 제외.
- **AC-8 (계정 미설정, 예외):** IF 감가상각비/누계액(또는 처분 계정) 미설정 상태에서 상각/처분을 처리하면, THEN the system SHALL 차단하고 명확한 오류(4xx) 반환(빈 값 분개 금지).
- **AC-9 (권한, 예외):** 자산 등록·상각·처분 FINANCE_WRITE, 계정 설정 FINANCE_SETTING_WRITE, 생성된 분개 결재 FINANCE_GL_APPROVE. 없으면 403.
- **AC-10 (회계기간, 경계):** IF 대상 회계기간이 OPEN이 아니면(마감/잠금), THEN 상각/처분 분개를 거부.
- **AC-11 (테넌트 격리, 경계):** the system SHALL 현재 테넌트의 자산·분개만 처리(@TenantId).
- **AC-12 (균형, 정상):** 상각·처분 분개는 차대변 균형(±0.01)이며 GL 결재로 POST 가능.

## 4. 제약 / 비기능
- 금액 BigDecimal scale 2, 균형 ±0.01. 상각액 원 미만 처리(절사 등) 일관 — 누적 오차가 잔존가치 하한을 깨지 않게 마지막 기간 보정.
- 상각 처리는 단일 트랜잭션(자산별 분개 생성). 회계기간 OPEN·계정 설정 전제.
- 분개는 기존 `JournalEntryService.createInternal`로 DRAFT 생성→GL 결재(#69) 경유, 역참조(ReferenceType) 연결.
- 모듈 경계: 전부 finance 내. 타 모듈 미참조.

## 5. 경계 / Do-Not (3단계)
- ✅ 해도 됨: FixedAsset 엔티티·정액/정률 계산·월 상각 처리·GL 자동분개·처분손익·계정 설정·화면.
- ⚠️ 먼저 물어봐: 세무상 상각·세무조정, 재평가/손상, 정액·정률 외 방법, 비동기 배치, 부분처분, 상각 분개 자동 POST.
- 🚫 절대 금지: 잔존가치 미만 과대상각, 같은 기간 중복 상각, 계정 미설정 빈 값 분개, 마감 회계기간 분개, 크로스테넌트, 차대변 불균형 분개, 시크릿 커밋.

## 6. Open Questions
- (없음 — 정액+정률·월별 상각 GL 자동분개·처분 포함·회계상만으로 확정.)

---

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):** `JournalEntryService.createInternal(JournalEntryCreateRequest)` + `JournalLineRequest`(차/대 라인)로 상각·처분 분개 DRAFT 생성(ApInvoicePostingService 동형). `ReferenceTypes`에 `DEPRECIATION`·`ASSET_DISPOSAL` 추가, `linkReference`로 역참조. 계정 설정은 `TenantBaseCurrency`(부가세·환차 계정 FK 패턴) 확장 + `BaseCurrencyService` get/set. 회계기간 `FiscalPeriodRepository.findBy…Between`(OPEN 검증). `Account.assertPostable`. `FINANCE_GL_APPROVE` 결재.

- **엔티티**: `FixedAsset extends BaseEntity`(finance) — code·name·acquisitionDate·acquisitionCost·residualValue·usefulLifeMonths·method(STRAIGHT_LINE/DECLINING_BALANCE)·decliningAnnualRate(nullable)·assetAccount(FK)·status(ACTIVE/DISPOSED)·accumulatedDepreciation. 도메인 메서드: `monthlyDepreciation()`(방법별·잔존하한)·`applyDepreciation(amount)`·`bookValue()`·`dispose()`. `DepreciationEntry`(자산·기간·금액·journalEntryId, UNIQUE(asset,fiscalPeriod) — 멱등). V2014 마이그레이션(테이블·SEQUENCE·인덱스 + tenant_base_currency 계정 컬럼).
- **계산**: 정액=(cost−residual)/usefulLifeMonths(scale 2). 정률=bookValue×(decliningAnnualRate/12). 둘 다 `min(계산액, bookValue−residual)`로 잔존하한·과대상각 방지.
- **상각 처리(DepreciationPostingService)**: 기간 입력 → OPEN·계정 설정 검증 → 가동 자산 중 (자산,기간) 미처리분 → monthlyDepreciation 계산(>0) → createInternal((차)감가상각비 (대)누계액) DRAFT → DepreciationEntry 저장(UNIQUE로 멱등 보강)·applyDepreciation. 멱등은 서비스 사전조회 + DB UNIQUE 이중.
- **처분**: 처분일 기준 (차)누계액·현금(매각) (대)자산, 차액 처분손익 분개 + dispose(). 처분 계정 설정 필수.
- **프론트**: `finance/fixed-assets` 목록(합계)·폼·상세(상각 이력·전표 링크)·상각 실행·처분 다이얼로그. 계정 설정은 FX/설정 화면 확장. 사이드바·커맨드팔레트.

**테스트 전략(AC↔테스트):**
- 단위(도메인): AC-2 정액·AC-3 정률 월상각액, AC-6 잔존하한·내용연수 종료 0.
- 통합(@SpringBootTest): AC-1 등록, AC-4 상각 처리·분개 균형·누계갱신·이력, AC-5 멱등(재처리 무변화), AC-7 처분손익·상태, AC-8 계정 미설정 차단, AC-10 마감기간 거부, AC-9 권한, AC-11 테넌트, AC-12 균형.
- 프론트: type-check/lint/build + 화면 렌더·상각 실행 스모크.

## 8. 태스크 (test-first 순서, 한 기능=한 브랜치 `feature/fixed-assets`=한 PR, 태스크당 원자적 커밋)
| # | 태스크 | AC | 대상 파일 | 검증(exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | FixedAsset 엔티티+상각 계산(정액/정률·잔존하한) 도메인 + V2014 + 상각계정 설정(TenantBaseCurrency 확장·서비스 get/set) | AC-1,2,3,6,8(전제),11 | FixedAsset, DepreciationEntry, V2014, TenantBaseCurrency, BaseCurrencyService | `cd backend && ./gradlew check` | — | |
| 2 | 월별 상각 처리(DepreciationPostingService): GL 자동분개·누계갱신·이력·멱등·계정필수·회계기간 | AC-4,5,8,10,12 | DepreciationPostingService, DepreciationService, ReferenceTypes | `./gradlew check` | #1 | |
| 3 | 처분(매각·폐기) 처분손익 분개·상태 전이 | AC-7,8,12 | FixedAssetService(dispose), 처분 posting | `./gradlew check` | #1,#2 | |
| 4 | 컨트롤러(자산 CRUD·상각 실행·처분·계정설정)+DTO | AC-1,4,7,9 | FixedAssetController, DTO | `./gradlew check` | #2,#3 | |
| 5 | 프론트 고정자산 화면(목록·폼·상세 상각이력·상각실행·처분)+계정설정+사이드바 | AC-1,4,7(표시) | frontend finance/fixed-assets | `npm run type-check && build` | #4 | |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 상각 계산 단위 + 처리·처분 통합(분개 균형·누계·멱등·잔존하한·계정필수·마감거부·권한·테넌트) green + 회귀 없음.
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 감가상각비·누계액 계정 설정 → 자산(취득 1,200만·내용연수 60월·정액) 등록 → 당월 상각 → (차)감가상각비 20만/(대)누계액 20만 DRAFT 확인·장부가액 1,180만. 같은 월 재처리 무변화(멱등). 정률 자산 체감 확인. 처분 → 처분손익 분개. 계정 미설정·마감기간 차단 확인.
4. 게이트: 태스크별 `/feature-add`(TDD) → 전 태스크 GREEN → `/feature-merge`(한 PR, AI 리뷰·CI).
