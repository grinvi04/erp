import { test, expect } from '@playwright/test'

// 인증된 세션(주입된 더미 쿠키)에서 보호 라우트가 /login으로 튕기지 않고 렌더되는지 검증.
// 백엔드 미기동이므로 데이터 fetch는 graceful 실패한다:
//  - safeGet 사용 페이지(대시보드·분석·재무제표·결재함)는 빈값으로 화면 골격을 정상 렌더.
//  - apiGet 직접 사용 페이지는 (dashboard)/error.tsx 경계로 폴백하되, 레이아웃(사이드바·
//    헤더)은 유지된다 → 인증 게이트 통과 + 인증 셸 렌더를 단언한다.
// 실제 백엔드 데이터 렌더는 backend-integration.spec.ts(E2E_BACKEND 게이트)에서 검증한다.

test.describe('인증된 사용자 — 렌더 스모크', () => {
  test('대시보드(/)가 /login으로 리다이렉트되지 않고 렌더된다', async ({ page }) => {
    await page.goto('/')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '대시보드', level: 1 })).toBeVisible()
    // 4개 모듈 요약 카드 헤딩.
    await expect(page.getByRole('heading', { name: '인사(HR)' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '재무(Finance)' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'CRM' })).toBeVisible()
  })

  test('사이드바 1차 네비게이션(결재함·분석)이 보인다', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('link', { name: '결재함' })).toBeVisible()
    await expect(page.getByRole('link', { name: '분석' })).toBeVisible()
  })

  test('사이드바 모듈 네비게이션 항목이 보인다', async ({ page }) => {
    await page.goto('/')
    // 각 모듈 그룹의 대표 하위 링크.
    await expect(page.getByRole('link', { name: '직원', exact: true })).toBeVisible()
    await expect(page.getByRole('link', { name: '재무제표' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'FX 설정' })).toBeVisible()
    await expect(page.getByRole('link', { name: '영업 기회' })).toBeVisible()
  })

  test('헤더에 세션 사용자·로그아웃이 표시된다', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByText('e2e@test.local')).toBeVisible()
    await expect(page.getByRole('button', { name: '로그아웃' })).toBeVisible()
  })

  test('결재함(/approvals)이 인증 상태로 렌더된다', async ({ page }) => {
    await page.goto('/approvals')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '결재함', level: 1 })).toBeVisible()
  })

  test('분석(/analytics)이 인증 상태로 렌더된다', async ({ page }) => {
    await page.goto('/analytics')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '분석', level: 1 })).toBeVisible()
    // safeGet 폴백(빈값)이어도 섹션 골격은 렌더된다.
    await expect(page.getByText('영업 파이프라인 분포')).toBeVisible()
  })

  test('재무제표(/finance/reports)가 인증 상태로 렌더된다', async ({ page }) => {
    await page.goto('/finance/reports')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: '재무제표', level: 1 })).toBeVisible()
    // 시산표·손익계산서·재무상태표 섹션 헤딩(safeGet 빈값에도 렌더).
    await expect(page.getByRole('heading', { name: '시산표' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '손익계산서' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '재무상태표' })).toBeVisible()
  })
})

test.describe('인증된 사용자 — 보호 라우트 인증 셸 렌더', () => {
  // apiGet 직접 호출 페이지: 백엔드 미기동 시 error.tsx로 폴백하지만, /login으로
  // 튕기지 않고 레이아웃(사이드바·헤더)이 유지됨을 단언한다(= 인증 게이트 통과).
  const shellRoutes = [
    '/finance/accounts',
    '/finance/journal-entries',
    '/finance/fx',
    '/hr/employees',
    '/inventory/movements',
    '/crm/opportunities',
    '/crm/accounts',
  ]

  for (const route of shellRoutes) {
    test(`${route}가 /login으로 튕기지 않고 인증 셸을 렌더한다`, async ({ page }) => {
      await page.goto(route)
      await expect(page).not.toHaveURL(/\/login/)
      // 사이드바 브랜드 — 레이아웃이 렌더되었음(인증된 셸).
      await expect(page.getByText('ERP System')).toBeVisible()
    })
  }
})
