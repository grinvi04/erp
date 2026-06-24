import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Opportunity } from '@/types/crm'
import type { PageResponse } from '@/types/api'

function formatAmount(amount: number | null, currency: string) {
  if (amount === null) return '—'
  return `${currency} ${amount.toLocaleString('ko-KR')}`
}

export const metadata = { title: '영업 기회 | ERP' }

export default async function OpportunitiesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Opportunity> = await apiGetPage<Opportunity>(
    `/api/crm/opportunities?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">영업 기회</h1>
        <p className="text-sm text-gray-500 mt-1">영업 파이프라인을 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>기회명</TableHead>
              <TableHead>고객사</TableHead>
              <TableHead>단계</TableHead>
              <TableHead className="text-right">금액</TableHead>
              <TableHead className="text-right">확률(%)</TableHead>
              <TableHead>예상 종결일</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-gray-400 py-10">
                  등록된 영업 기회가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((opp) => (
              <TableRow key={opp.id}>
                <TableCell className="font-medium">{opp.name}</TableCell>
                <TableCell className="text-sm text-gray-700">{opp.accountName}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{opp.stageName}</Badge>
                </TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatAmount(opp.amount, opp.currency)}
                </TableCell>
                <TableCell className="text-right text-sm">{opp.probability}%</TableCell>
                <TableCell className="text-sm text-gray-600">
                  {opp.closeDate ?? '—'}
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
          basePath="/crm/opportunities"
        />
      </div>
    </div>
  )
}
