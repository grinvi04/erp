import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { JournalEntry, JournalStatus } from '@/types/finance'
import type { PageResponse } from '@/types/api'

const STATUS_LABEL: Record<JournalStatus, string> = { DRAFT: '임시', POSTED: '승인' }
const STATUS_VARIANT: Record<JournalStatus, 'secondary' | 'default'> = {
  DRAFT: 'secondary',
  POSTED: 'default',
}

function formatAmount(n: number) {
  return n.toLocaleString('ko-KR')
}

export const metadata = { title: '전표 | ERP' }

export default async function JournalEntriesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<JournalEntry> = await apiGetPage<JournalEntry>(
    `/api/finance/journal-entries?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">전표</h1>
        <p className="text-sm text-gray-500 mt-1">회계 전표를 조회하고 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>전표번호</TableHead>
              <TableHead>전표일자</TableHead>
              <TableHead>적요</TableHead>
              <TableHead className="text-right">차변</TableHead>
              <TableHead className="text-right">대변</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>작성자</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  등록된 전표가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((je) => (
              <TableRow key={je.id}>
                <TableCell className="font-mono text-sm">{je.entryNo}</TableCell>
                <TableCell className="text-sm">{je.entryDate}</TableCell>
                <TableCell className="max-w-xs truncate text-sm">{je.description}</TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatAmount(je.totalDebit)}
                </TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatAmount(je.totalCredit)}
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[je.status]}>
                    {STATUS_LABEL[je.status]}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-gray-600">{je.createdBy}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/finance/journal-entries"
        />
      </div>
    </div>
  )
}
