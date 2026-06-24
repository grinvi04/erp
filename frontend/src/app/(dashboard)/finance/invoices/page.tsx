import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Invoice } from '@/types/finance'
import type { PageResponse } from '@/types/api'

type InvoiceStatus = Invoice['status']

const STATUS_LABEL: Record<InvoiceStatus, string> = {
  OPEN: '미납',
  PARTIAL: '부분납',
  PAID: '완납',
  VOID: '취소',
}
const STATUS_VARIANT: Record<InvoiceStatus, 'default' | 'secondary' | 'destructive'> = {
  OPEN: 'secondary',
  PARTIAL: 'secondary',
  PAID: 'default',
  VOID: 'destructive',
}

function formatAmount(n: number, currency: string) {
  return `${currency} ${n.toLocaleString('ko-KR')}`
}

export const metadata = { title: '매입 인보이스 | ERP' }

export default async function InvoicesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Invoice> = await apiGetPage<Invoice>(
    `/api/finance/invoices?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">매입 인보이스</h1>
        <p className="text-sm text-gray-500 mt-1">공급업체 인보이스 및 지급 현황을 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>인보이스번호</TableHead>
              <TableHead>공급업체</TableHead>
              <TableHead>인보이스일</TableHead>
              <TableHead>만기일</TableHead>
              <TableHead className="text-right">총금액</TableHead>
              <TableHead className="text-right">납부금액</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  등록된 인보이스가 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((inv) => (
              <TableRow key={inv.id}>
                <TableCell className="font-mono text-sm">{inv.invoiceNo}</TableCell>
                <TableCell className="font-medium">{inv.vendorName}</TableCell>
                <TableCell className="text-sm">{inv.invoiceDate}</TableCell>
                <TableCell className="text-sm">{inv.dueDate}</TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatAmount(inv.totalAmount, inv.currency)}
                </TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {formatAmount(inv.paidAmount, inv.currency)}
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[inv.status]}>
                    {STATUS_LABEL[inv.status]}
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
          basePath="/finance/invoices"
        />
      </div>
    </div>
  )
}
