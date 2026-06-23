import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { CrmAccount, AccountType } from '@/types/crm'
import type { PageResponse } from '@/types/api'

const TYPE_LABEL: Record<AccountType, string> = {
  PROSPECT: '잠재',
  CUSTOMER: '고객',
  PARTNER: '파트너',
  COMPETITOR: '경쟁사',
}

export const metadata = { title: '고객사 | ERP' }

export default async function CrmAccountsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<CrmAccount> = await apiGetPage<CrmAccount>(
    `/api/crm/accounts?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">고객사</h1>
        <p className="text-sm text-gray-500 mt-1">고객사 및 잠재 고객 정보를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>고객사명</TableHead>
              <TableHead>유형</TableHead>
              <TableHead>업종</TableHead>
              <TableHead>전화</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-gray-400 py-10">
                  등록된 고객사가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((acc) => (
              <TableRow key={acc.id}>
                <TableCell className="font-mono text-sm">{acc.code}</TableCell>
                <TableCell className="font-medium">{acc.name}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{TYPE_LABEL[acc.accountType]}</Badge>
                </TableCell>
                <TableCell className="text-sm text-gray-600">{acc.industry ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{acc.phone ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={acc.isActive ? 'default' : 'secondary'}>
                    {acc.isActive ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/crm/accounts"
        />
      </div>
    </div>
  )
}
