# declining-full-depreciation (정률법 완전상각) 스펙 + 플랜

> GitHub Issue #170. 단일출처 로드맵 `docs/roadmap-localization.md`.

## Context (왜)
정률법(DECLINING_BALANCE) 월상각 = 월초 장부가액 × (연상각률/12)는 **기하급수 체감**이라 잔존가치(특히 0)에 **절대 도달하지 못한다** — 내용연수가 끝나도 장부가액이 잔존가치보다 큰 잔액이 남는다(현재 `min(raw, 장부가액−잔존)` 가드는 과대상각만 막을 뿐 완전상각을 보장하지 않음). K-IFRS/실무 표준은 **말기 정액 전환**: 잔여 장부가액을 잔여내용연수로 정액 상각한 값이 정률 상각액 이상이 되는 시점부터 정액으로 전환해 내용연수 말에 잔존가치까지 완전상각한다. 결과: 정률 자산도 내용연수 종료 시 장부가액=잔존가치.

## 1. 목표 & Why
정률법 자산이 내용연수 말에 잔존가치까지 완전상각되도록 **정률→정액 자동 전환**을 도입한다. **성공 기준(측정 가능): 정률 자산(예: 취득 1,000만·내용연수 N월·잔존 0)을 N개월 상각하면 누계상각액=취득원가−잔존(장부가액=잔존)에 정확히 도달하고, 초기 기간의 정률 상각액은 기존과 동일하다(정액이 정률보다 작은 구간).**

## 2. Scope
- **In:**
  - 정률 월상각 = `max(정률액, 잔여내용연수 정액액)`. 정액액 = (장부가액 − 잔존가치)/잔여내용연수. 정액이 커지는 시점(내용연수 후반)부터 자동 정액 전환 → 말기 잔존까지 완전상각.
  - **잔여내용연수 산정용 경과월수 추적**: `depreciatedMonths`(상각 처리 횟수) — `applyDepreciation` 시 증가. 잔여 = 내용연수월 − depreciatedMonths.
  - 잔존가치 > 0 정률 자산에도 적용(정률은 잔존>0에도 정확히 도달 못 함 — 동일 전환으로 해결).
  - 마이그레이션: fixed_asset에 depreciated_months 추가 + 기존 행 백필(상각이력 개수).
- **Out (Non-goals):**
  - 정액법 로직 변경(정액은 이미 완전상각·손상 override 사용 — 무변경).
  - 세무상 상각·상각방법 변경 UI.
  - 정률 상각률 자동 산정(입력값 유지).
  - 상각 스케줄 재계산 배치(기존 자산은 백필로 현행 스케줄 이어감).

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (초기 정률 불변, 정상):** WHILE 정률 AND 잔여내용연수 정액액 ≤ 정률액(내용연수 전반), the system SHALL 기존과 동일한 정률 상각액(월초 장부가액×연상각률/12, DOWN)을 산출한다.
- **AC-2 (정액 전환, 정상):** WHILE 정률 AND 잔여내용연수 정액액 > 정률액(내용연수 후반), the system SHALL 정액액((장부가액−잔존)/잔여내용연수)으로 상각한다.
- **AC-3 (완전상각, 경계):** WHEN 정률 자산을 내용연수(N월) 전 기간 상각, the system SHALL 누계상각액 = 취득원가 − 잔존가치(장부가액=잔존가치)에 도달한다(잔액 없음).
- **AC-4 (잔존>0 완전상각, 경계):** 잔존가치 > 0 정률 자산도 내용연수 말 장부가액=잔존가치에 도달.
- **AC-5 (과대상각 금지, 경계):** 어떤 기간에도 상각액 ≤ 장부가액 − 잔존가치(마지막 기간 정확히 잔존까지).
- **AC-6 (경과월 추적, 정상):** applyDepreciation 1회당 depreciatedMonths 1 증가. 잔여내용연수 = max(내용연수월 − depreciatedMonths, 1).
- **AC-7 (기존 행 백필, 경계):** 마이그레이션 후 기존 자산의 depreciated_months = 해당 자산의 상각이력(depreciation_entry) 개수.
- **AC-8 (정액 무영향, 회귀):** 정액 자산의 monthlyDepreciation·손상 override는 변경 없음(depreciatedMonths 증가하나 정액 분기는 미사용).
- **AC-9 (손상 상호작용, 경계):** 손상 인식된 정률 자산도 max(정률, 정액) 전환이 손상 반영 장부가액 기준으로 동작(정률은 override 미사용).

## 4. 제약 / 비기능
- 금액 BigDecimal scale 2, DOWN(기존 정률과 동일 반올림). 잔존 하한 가드 유지.
- 마이그레이션 forward-only. 백필은 기존 depreciation_entry 집계.
- 외과적: 정률 분기 + depreciatedMonths만 추가. 정액·손상·환입 경로 무변경.

## 5. 경계 / Do-Not
- ✅: 정률 max(정률,정액) 전환·depreciatedMonths·V2017·완전상각 테스트.
- ⚠️ 먼저 물어봐: 정액 로직 변경, 상각률 자동산정, 스케줄 전면 재계산.
- 🚫: 정액/손상 override 경로 변경, 과대상각(잔존 미만), 기존 정률 초기구간 상각액 변경, 시크릿 커밋.

## 6. Open Questions
- (없음 — max(정률,정액) 자동전환·depreciatedMonths 추적·잔존≥0 일반 적용으로 확정.)

---

## 7. 기술 접근 (HOW)
**도메인 (`FixedAsset.java`):**
- 신규 필드 `depreciatedMonths INT NOT NULL DEFAULT 0`. `applyDepreciation`에서 `depreciatedMonths++`(누계상각 갱신과 함께).
- `monthlyDepreciation()` 정률 분기 변경:
  ```
  BigDecimal db = bookValue × decliningAnnualRate / 12 (DOWN)
  int remaining = max(usefulLifeMonths − depreciatedMonths, 1)
  BigDecimal sl = (bookValue − residualValue) / remaining (DOWN)
  raw = db.max(sl)
  ```
  기존 `min(raw, depreciableRemaining)` 가드 유지(말기 정확·과대상각 방지). 정액 분기·override 무변경.
- **정액 자산 영향 없음**: 정액 분기는 depreciatedMonths 미사용(상수/override). depreciatedMonths 증가만 발생(무해).
- **손상 상호작용**: 정률은 override 미사용 → 손상 후 bookValue 하락분이 db·sl 양쪽에 자동 반영. 별도 처리 불필요.

**마이그레이션 V2017__declining_full_depreciation.sql:**
- `ALTER TABLE finance.fixed_asset ADD COLUMN depreciated_months INT NOT NULL DEFAULT 0;`
- 백필: `UPDATE finance.fixed_asset fa SET depreciated_months = (SELECT count(*) FROM finance.depreciation_entry de WHERE de.fixed_asset_id = fa.id AND de.deleted_at IS NULL);`

**영향 파일:** `FixedAsset.java`(도메인·필드·정률 분기·applyDepreciation), `V2017__…sql`. 서비스(DepreciationPostingService)는 monthlyDepreciation·applyDepreciation 호출만 하므로 무변경. 프론트 무변경(장부가액·상각 표시 그대로).

**테스트 전략(AC↔):**
- 단위(FixedAssetTest): AC-1 초기 정률 불변(기존 declining_firstMonth·decreasesEachPeriod 그대로 GREEN), AC-2 전환 시점 정액액, AC-3 N개월 루프 상각 후 장부=잔존(완전상각, 잔존0), AC-4 잔존>0 완전상각, AC-5 과대상각 금지, AC-6 depreciatedMonths 증가.
- 통합(DepreciationPostingIntegrationTest 또는 신규): 정률 자산 등록→여러 기간 상각→말기 장부가액=잔존. AC-8 정액 회귀·AC-9 손상 정률 상호작용.
- 백필(AC-7): @DataJpaTest 또는 통합에서 마이그레이션 후 depreciated_months 검증(기존 상각이력 개수와 일치).

## 8. 태스크 (test-first, 한 기능=한 브랜치 feature/declining-full-depreciation=한 PR)
| # | 태스크 | AC | 대상 | 검증(exit 0) | 의존 |
|---|---|---|---|---|---|
| 0 | 스펙 docs/specs/declining-full-depreciation.md 커밋 | — | docs/specs | 파일 존재 | — |
| 1 | FixedAsset: depreciatedMonths 필드·applyDepreciation 증가·정률 max(정률,정액) 전환 + V2017(컬럼·백필) | AC-1~6,8,9 | FixedAsset, V2017 | `cd backend && ./gradlew check`(정률 회귀·완전상각 단위 포함) | #0 |
| 2 | 통합테스트: 정률 완전상각(말기 장부=잔존)·정액 회귀·손상 정률 상호작용·백필 | AC-3,4,7,8,9 | DepreciationPostingIntegrationTest 등 | `./gradlew check` | #1 |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 단위(초기 정률 불변·전환·완전상각·잔존>0·과대상각 금지) + 통합(정률 N개월 완전상각·정액 회귀·손상 정률·백필) green + **기존 정률/정액/손상/환입 회귀 없음**.
2. 프론트 변경 없음(선택: `npm run build`만 확인).
3. 수동(docker-compose): 정률 자산(1,000만·내용연수 12월·잔존 0·율 0.5) 등록 → 12개월 상각 → 장부가액 0(완전상각)·초기 몇 개월 정률액 확인.
4. 게이트: 태스크별 `/feature-add`(TDD) → 전 GREEN → 사용자 확인 → `/feature-merge`(한 PR).
