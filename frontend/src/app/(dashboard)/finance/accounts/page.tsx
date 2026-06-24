import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Account, AccountType } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const TYPE_LABEL: Record<AccountType, string> = {
  ASSET: '자산',
  LIABILITY: '부채',
  EQUITY: '자본',
  REVENUE: '수익',
  EXPENSE: '비용',
}

export const metadata = { title: '계정과목 | ERP' }

export default async function AccountsPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Account> = await apiGetPage<Account>(
    `/api/finance/accounts?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">계정과목</h1>
        <p className="text-sm text-gray-500 mt-1">회계 계정과목 체계를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>계정과목명</TableHead>
              <TableHead>유형</TableHead>
              <TableHead>집계계정</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-gray-400 py-10">
                  등록된 계정과목이 없습니다
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
                <TableCell className="text-sm text-gray-500">
                  {acc.isSummary ? 'Y' : 'N'}
                </TableCell>
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
          basePath="/finance/accounts"
        />
      </div>
    </div>
  )
}
