import { test, expect } from '@playwright/test'

// 백엔드 통합 E2E — E2E_BACKEND 게이트로만 실행된다(playwright.config.ts).
// backend.setup.ts가 실 Keycloak 토큰으로 만든 세션으로, 실 백엔드(JWT 검증·RBAC·테넌트
// 필터)를 통과하는 데이터가 화면에 렌더되는지 검증한다.
//
// 핵심 단언 원리: apiGet 직접 호출 페이지는 백엔드가 401/오류면 error.tsx로 폴백한다.
// 따라서 "페이지 헤딩이 보인다 + 오류 폴백이 없다" = 백엔드가 유효 토큰으로 200을 반환했다.

test.describe('백엔드 통합 — 실 데이터 렌더', () => {
  test('대시보드가 모듈 요약을 오류 없이 표시한다', async ({ page }) => {
    await page.goto('/')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '대시보드', level: 1 })).toBeVisible()
    // 백엔드 200이면 요약 카드가 실패 안내("요약 정보를 불러오지 못했습니다")를 띄우지 않는다.
    await expect(page.getByText('요약 정보를 불러오지 못했습니다')).toHaveCount(0)
    // 모듈 카드 헤딩이 정상 노출.
    await expect(page.getByRole('heading', { name: '인사(HR)' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '재무(Finance)' })).toBeVisible()
  })

  test('계정과목(/finance/accounts)이 백엔드 데이터로 렌더된다', async ({ page }) => {
    await page.goto('/finance/accounts')
    await expect(page).not.toHaveURL(/\/login/)
    // apiGet 직접 호출 — 헤딩이 보이면 백엔드가 200을 반환한 것.
    await expect(page.getByRole('heading', { name: '계정과목', level: 1 })).toBeVisible()
    // 401/오류 시 나타나는 error.tsx 폴백이 없어야 한다.
    await expect(page.getByText('문제가 발생했습니다. 다시 시도해 주세요.')).toHaveCount(0)
  })

  test('직원 관리(/hr/employees)가 백엔드 데이터로 렌더된다', async ({ page }) => {
    await page.goto('/hr/employees')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '직원 관리', level: 1 })).toBeVisible()
    await expect(page.getByText('문제가 발생했습니다. 다시 시도해 주세요.')).toHaveCount(0)
  })

  test('재무제표(/finance/reports)가 백엔드 데이터로 렌더된다', async ({ page }) => {
    await page.goto('/finance/reports')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '재무제표', level: 1 })).toBeVisible()
    await expect(page.getByRole('heading', { name: '시산표' })).toBeVisible()
  })

  test('매입 인보이스(/finance/invoices)가 실 데이터로 크래시 없이 렌더된다', async ({ page }) => {
    // vendors 페이지네이션 응답을 배열로 캐스팅하던 버그(T0-1)로 vendors.filter 크래시 →
    // error.tsx 폴백이 떠 AP 인보이스 10건이 통째로 비가시였다. 회귀 가드.
    await page.goto('/finance/invoices')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '매입 인보이스', level: 1 })).toBeVisible()
    await expect(page.getByText('문제가 발생했습니다. 다시 시도해 주세요.')).toHaveCount(0)
    // 백엔드에 AP 인보이스가 존재하므로 표 본문 행이 1건 이상 보여야 한다.
    expect(await page.locator('table tbody tr').count()).toBeGreaterThanOrEqual(1)
  })

  test('매출 인보이스(/finance/ar-invoices)가 실 데이터로 크래시 없이 렌더된다', async ({
    page,
  }) => {
    // customers 페이지네이션 응답 배열캐스팅 버그(T0-1) 대칭 케이스. AR은 데이터 0건일 수
    // 있으므로 행수는 단언하지 않고 헤딩 노출 + 폴백 없음만 검증한다.
    await page.goto('/finance/ar-invoices')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '매출 인보이스', level: 1 })).toBeVisible()
    await expect(page.getByText('문제가 발생했습니다. 다시 시도해 주세요.')).toHaveCount(0)
  })
})
