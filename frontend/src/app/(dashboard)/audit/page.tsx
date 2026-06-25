import { apiGetPage } from '@/lib/api'
import { getMyPermissions, PERM } from '@/lib/permissions'
import type { PageResponse } from '@/types/api'
import type { AuditLog } from '@/types/audit'
import AuditClient from './audit-client'

export const metadata = { title: '감사 로그 | ERP' }

const ENTITY_TYPES = ['LEAVE_REQUEST', 'AP_INVOICE', 'EMPLOYEE'] as const

export default async function AuditPage(props: {
  searchParams: Promise<{ page?: string; size?: string; entityType?: string }>
}) {
  // 서버 측 권한 선검사 — audit:read 없이는 백엔드가 403을 내므로 깔끔한 안내로 대체한다.
  const perms = await getMyPermissions()
  if (!perms.includes(PERM.AUDIT_READ)) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold text-gray-900">감사 로그</h1>
        <p className="mt-4 text-sm text-gray-500">
          접근 권한이 없습니다. 감사 로그 조회에는 <code>audit:read</code> 권한이 필요합니다.
        </p>
      </div>
    )
  }

  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const entityType = sp.entityType && ENTITY_TYPES.includes(sp.entityType as (typeof ENTITY_TYPES)[number])
    ? sp.entityType
    : ''
  const typeFilter = entityType ? `&entityType=${encodeURIComponent(entityType)}` : ''

  const data = await apiGetPage<AuditLog>(`/api/audit/logs?page=${page}&size=${size}${typeFilter}`)

  return <AuditClient data={data as PageResponse<AuditLog>} entityType={entityType} />
}
