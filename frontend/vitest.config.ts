import { defineConfig } from 'vitest/config'

// 순수 로직(lib 유틸) 단위 테스트 — node 환경. 프레젠테이셔널/통합은 Playwright e2e + /qa가 커버.
export default defineConfig({
  test: {
    include: ['src/**/*.test.ts'],
    environment: 'node',
  },
})
