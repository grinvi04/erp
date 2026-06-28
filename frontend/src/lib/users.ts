import 'server-only'
import { safeGetArray } from '@/lib/api'

// 백엔드 com.erp.common.userdirectory.UserDisplayNameResponse 와 일치한다.
interface UserDisplayName {
  sub: string
  displayName: string
}

/**
 * sub(UUID) 집합 → 표시이름 맵. 서버 컴포넌트에서 호출한다(백엔드 /api/users/display-names).
 * 해소 실패·미존재 sub는 결과에서 누락되며 호출부가 fallback(앞 8자)으로 처리한다.
 */
export async function resolveUserNames(
  subs: Array<string | null | undefined>,
): Promise<Record<string, string>> {
  const unique = [...new Set(subs.filter((s): s is string => Boolean(s)))]
  if (unique.length === 0) return {}
  const rows = await safeGetArray<UserDisplayName>(
    `/api/users/display-names?subs=${unique.map(encodeURIComponent).join(',')}`,
  )
  return Object.fromEntries(rows.map((r) => [r.sub, r.displayName]))
}
