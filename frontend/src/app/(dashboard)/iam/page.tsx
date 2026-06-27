import { apiGet, getMyPermissions } from '@/lib/api'
import { PERM } from '@/lib/permissions'
import type { Role } from '@/types/iam'
import IamClient from './iam-client'

export const metadata = { title: '역할·권한 관리 | ERP' }

export default async function IamPage() {
  const perms = await getMyPermissions()
  if (!perms.includes(PERM.IAM_READ)) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold text-foreground">역할·권한 관리</h1>
        <p className="mt-4 text-sm text-muted-foreground">
          접근 권한이 없습니다. 관리에는 <code>iam:read</code>/<code>iam:write</code> 권한이 필요합니다.
        </p>
      </div>
    )
  }

  const [roles, catalog] = await Promise.all([
    apiGet<Role[]>('/api/iam/roles'),
    apiGet<string[]>('/api/iam/permissions'),
  ])

  const canWrite = perms.includes(PERM.IAM_WRITE)
  return <IamClient roles={roles} catalog={[...catalog].sort()} canWrite={canWrite} />
}
