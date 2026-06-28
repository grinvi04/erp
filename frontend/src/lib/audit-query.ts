import 'server-only'

// 화면 필터(yyyy-MM-dd 등)를 백엔드 /api/audit/logs 쿼리 문자열로 변환한다.
// 페이지 조회와 CSV 내보내기 프록시가 같은 변환을 공유한다(단일 출처).
export function auditBackendQuery(params: {
  page?: string | number
  size?: string | number
  entityType?: string
  action?: string
  performedBy?: string
  from?: string // yyyy-MM-dd
  to?: string // yyyy-MM-dd
}): string {
  const sp = new URLSearchParams()
  if (params.page !== undefined && params.page !== '') sp.set('page', String(params.page))
  if (params.size !== undefined && params.size !== '') sp.set('size', String(params.size))
  if (params.entityType) sp.set('entityType', params.entityType)
  if (params.action) sp.set('action', params.action)
  if (params.performedBy) sp.set('performedBy', params.performedBy)
  // 백엔드는 LocalDateTime을 받는다 — 날짜를 그날의 시작/끝 시각으로 확장한다.
  if (params.from) sp.set('from', `${params.from}T00:00:00`)
  if (params.to) sp.set('to', `${params.to}T23:59:59`)
  return sp.toString()
}
