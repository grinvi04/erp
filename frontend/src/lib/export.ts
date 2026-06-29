import 'server-only'
import { apiGetPage } from '@/lib/api'

const EXPORT_PAGE_SIZE = 100
export const MAX_EXPORT_ROWS = 5000

/**
 * 전체 데이터셋 내보내기 — 모든 페이지를 순회(size≤100, api-standards 상한 준수)하여 행을 누적한다.
 * 안전 상한(MAX_EXPORT_ROWS) 도달 시 truncated=true로 중단(조용한 잘림 금지 — 호출처가 사용자에게 경고).
 *
 * <p>server-only: 클라이언트 번들에 포함되지 않는다. 임의 경로 노출을 막기 위해 화면별 server action이
 * 고정 경로로만 호출한다(권한·테넌트 격리는 apiGetPage(BFF)가 경유).
 */
export async function fetchAllPages<T>(
  basePath: string,
): Promise<{ rows: T[]; truncated: boolean }> {
  const sep = basePath.includes('?') ? '&' : '?'
  const rows: T[] = []
  let page = 0
  let totalPages = 1
  while (page < totalPages) {
    const res = await apiGetPage<T>(`${basePath}${sep}page=${page}&size=${EXPORT_PAGE_SIZE}`)
    rows.push(...res.content)
    totalPages = res.totalPages
    if (rows.length >= MAX_EXPORT_ROWS) {
      return { rows: rows.slice(0, MAX_EXPORT_ROWS), truncated: true }
    }
    page += 1
  }
  return { rows, truncated: false }
}
