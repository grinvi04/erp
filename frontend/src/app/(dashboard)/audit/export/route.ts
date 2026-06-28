import { apiGetRaw } from '@/lib/api'
import { auditBackendQuery } from '@/lib/audit-query'

// 현재 필터 조건의 감사 로그 CSV 다운로드 — BFF가 토큰을 주입해 백엔드 export를 프록시한다.
// (브라우저는 백엔드를 직접 호출할 수 없으므로 이 라우트가 토큰을 붙여 중계한다.)
export async function GET(request: Request) {
  const sp = new URL(request.url).searchParams
  const query = auditBackendQuery({
    entityType: sp.get('entityType') ?? undefined,
    action: sp.get('action') ?? undefined,
    performedBy: sp.get('performedBy') ?? undefined,
    from: sp.get('from') ?? undefined,
    to: sp.get('to') ?? undefined,
  })
  const res = await apiGetRaw(`/api/audit/logs/export${query ? `?${query}` : ''}`)
  if (!res.ok) {
    return new Response('감사 로그 내보내기에 실패했습니다.', { status: res.status })
  }
  const csv = await res.text()
  return new Response(csv, {
    status: 200,
    headers: {
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': 'attachment; filename="audit-logs.csv"',
    },
  })
}
