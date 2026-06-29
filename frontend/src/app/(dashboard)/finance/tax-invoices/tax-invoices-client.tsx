'use client'
import { useMemo, useState, useTransition } from 'react'
import { toast } from 'sonner'
import { DownloadIcon, FileTextIcon, PlusIcon } from 'lucide-react'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { DatePicker } from '@/components/ui/date-picker'
import { Badge } from '@/components/ui/badge'
import { DataTable, type Column } from '@/components/ui/data-table'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { PageHeader } from '@/components/ui/page-header'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { EmptyState } from '@/components/ui/empty-state'
import { DetailSheet, DetailRow, DetailSection } from '@/components/ui/detail-sheet'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { downloadCsv } from '@/lib/csv'
import type { TaxInvoice, TaxInvoiceStatus, TaxType, ChargeType, ArInvoice } from '@/types/finance'
import { issueTaxInvoice, cancelTaxInvoice, getTaxInvoiceXml } from './actions'

const STATUS_LABEL: Record<TaxInvoiceStatus, string> = { ISSUED: '발행', CANCELLED: '취소' }
const STATUS_VARIANT: Record<TaxInvoiceStatus, 'default' | 'secondary'> = {
  ISSUED: 'default',
  CANCELLED: 'secondary',
}
const TAX_TYPE_LABEL: Record<TaxType, string> = {
  TAXABLE: '과세',
  ZERO_RATED: '영세율',
  EXEMPT: '면세',
}
const CHARGE_TYPE_LABEL: Record<ChargeType, string> = { CHARGE: '청구', RECEIPT: '영수' }

const won = (n: number) => n.toLocaleString('ko-KR')

export default function TaxInvoicesClient({
  data,
  issuableArInvoices,
  status,
}: {
  data: {
    content: TaxInvoice[]
    page: number
    size: number
    totalPages: number
    totalElements: number
  }
  issuableArInvoices: ArInvoice[]
  status: TaxInvoiceStatus | ''
}) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)
  const [isPending, startTransition] = useTransition()

  // 조회 조건(클라이언트 필터 — 로드된 페이지 대상)
  const [qStatus, setQStatus] = useState<string>(status)
  const [qBuyer, setQBuyer] = useState('')
  const [applied, setApplied] = useState<{ status: string; buyer: string }>({
    status,
    buyer: '',
  })

  const [detail, setDetail] = useState<TaxInvoice | null>(null)
  const [cancelTarget, setCancelTarget] = useState<TaxInvoice | null>(null)

  // 발행 다이얼로그
  const [issueOpen, setIssueOpen] = useState(false)
  const [arInvoiceId, setArInvoiceId] = useState('')
  const [writeDate, setWriteDate] = useState('')
  const [chargeType, setChargeType] = useState<ChargeType>('CHARGE')
  const [itemName, setItemName] = useState('')
  const [note, setNote] = useState('')

  const filtered = useMemo(
    () =>
      data.content.filter(
        (t) =>
          (!applied.status || t.status === applied.status) &&
          (!applied.buyer ||
            t.buyer.companyName.toLowerCase().includes(applied.buyer.toLowerCase())),
      ),
    [data.content, applied],
  )

  const onSearch = () => setApplied({ status: qStatus, buyer: qBuyer.trim() })
  const onReset = () => {
    setQStatus('')
    setQBuyer('')
    setApplied({ status: '', buyer: '' })
  }

  const openIssue = () => {
    setArInvoiceId('')
    setWriteDate('')
    setChargeType('CHARGE')
    setItemName('')
    setNote('')
    setIssueOpen(true)
  }

  const handleIssue = () => {
    if (!arInvoiceId) {
      toast.error('발행할 매출 인보이스를 선택하세요')
      return
    }
    startTransition(async () => {
      try {
        await issueTaxInvoice(Number(arInvoiceId), {
          writeDate: writeDate || null,
          chargeType,
          itemName: itemName.trim() || null,
          note: note.trim() || null,
        })
        toast.success('세금계산서가 발행되었습니다')
        setIssueOpen(false)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '발행 중 오류가 발생했습니다')
      }
    })
  }

  const handleCancel = (t: TaxInvoice) => {
    startTransition(async () => {
      try {
        await cancelTaxInvoice(t.id)
        toast.success('세금계산서가 취소되었습니다')
        setCancelTarget(null)
        setDetail(null)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '취소 중 오류가 발생했습니다')
      }
    })
  }

  const handleDownloadXml = (t: TaxInvoice) => {
    startTransition(async () => {
      try {
        const xml = await getTaxInvoiceXml(t.id)
        const blob = new Blob([xml], { type: 'application/xml;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `${t.issueNo ?? `tax-invoice-${t.id}`}.xml`
        document.body.appendChild(a)
        a.click()
        a.remove()
        URL.revokeObjectURL(url)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : 'XML 다운로드에 실패했습니다')
      }
    })
  }

  const exportExcel = () => {
    downloadCsv(
      '세금계산서',
      ['발행번호', '작성일', '공급받는자', '품목', '공급가액', '세액', '합계', '상태'],
      filtered.map((t) => [
        t.issueNo ?? '',
        t.writeDate,
        t.buyer.companyName,
        t.itemName,
        t.supplyAmount,
        t.vatAmount,
        t.totalAmount,
        STATUS_LABEL[t.status],
      ]),
    )
  }

  const columns: Column<TaxInvoice>[] = [
    {
      key: 'issueNo',
      header: '발행번호',
      sortable: true,
      sortValue: (t) => t.issueNo ?? '',
      cell: (t) => <span className="font-mono text-sm">{t.issueNo ?? '—'}</span>,
    },
    {
      key: 'writeDate',
      header: '작성일',
      sortable: true,
      sortValue: (t) => t.writeDate,
      cell: (t) => <span className="text-sm">{t.writeDate}</span>,
    },
    {
      key: 'buyer',
      header: '공급받는자',
      sortable: true,
      sortValue: (t) => t.buyer.companyName,
      cell: (t) => <span className="font-medium">{t.buyer.companyName}</span>,
    },
    {
      key: 'taxType',
      header: '과세구분',
      cell: (t) => (
        <span className="text-sm text-muted-foreground">{TAX_TYPE_LABEL[t.taxType]}</span>
      ),
    },
    {
      key: 'supplyAmount',
      header: '공급가액',
      align: 'right',
      sortable: true,
      sortValue: (t) => t.supplyAmount,
      cell: (t) => <span className="font-mono text-sm">{won(t.supplyAmount)}</span>,
      footer: (rows) => (
        <span className="font-mono">{won(rows.reduce((s, r) => s + r.supplyAmount, 0))}</span>
      ),
    },
    {
      key: 'vatAmount',
      header: '세액',
      align: 'right',
      sortable: true,
      sortValue: (t) => t.vatAmount,
      cell: (t) => <span className="font-mono text-sm">{won(t.vatAmount)}</span>,
      footer: (rows) => (
        <span className="font-mono">{won(rows.reduce((s, r) => s + r.vatAmount, 0))}</span>
      ),
    },
    {
      key: 'totalAmount',
      header: '합계',
      align: 'right',
      sortable: true,
      sortValue: (t) => t.totalAmount,
      cell: (t) => <span className="font-mono text-sm">{won(t.totalAmount)}</span>,
      footer: (rows) => (
        <span className="font-mono">{won(rows.reduce((s, r) => s + r.totalAmount, 0))}</span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (t) => STATUS_LABEL[t.status],
      cell: (t) => <Badge variant={STATUS_VARIANT[t.status]}>{STATUS_LABEL[t.status]}</Badge>,
    },
  ]

  return (
    <div className="p-5">
      <PageHeader
        title="세금계산서"
        description="매출 인보이스에서 전자세금계산서를 발행하고 국세청 표준 XML을 내려받습니다"
        className="mb-4"
      >
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openIssue}>
            <PlusIcon />
            세금계산서 발행
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(!v || v === 'ALL' ? '' : v)}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="ISSUED">발행</SelectItem>
                <SelectItem value="CANCELLED">취소</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="공급받는자">
            <Input
              value={qBuyer}
              onChange={(e) => setQBuyer(e.target.value)}
              placeholder="상호 검색"
              className="h-8 w-40"
            />
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(t) => t.id}
          onRowClick={(t) => setDetail(t)}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              icon={FileTextIcon}
              title="발행된 세금계산서가 없습니다"
              description={canWrite ? '우측 상단에서 세금계산서를 발행하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/finance/tax-invoices"
        />
      </div>

      {/* 상세 */}
      <DetailSheet
        open={detail !== null}
        onOpenChange={(o) => !o && setDetail(null)}
        title={detail ? `세금계산서 ${detail.issueNo ?? ''}` : ''}
        description={
          detail ? `${STATUS_LABEL[detail.status]} · ${TAX_TYPE_LABEL[detail.taxType]}` : undefined
        }
        footer={
          detail && (
            <div className="flex w-full justify-between gap-2">
              <Button
                variant="outline"
                onClick={() => detail && handleDownloadXml(detail)}
                disabled={isPending || detail.status !== 'ISSUED'}
              >
                <DownloadIcon />
                XML 다운로드
              </Button>
              {canWrite && detail.status === 'ISSUED' && (
                <Button
                  variant="destructive"
                  onClick={() => setCancelTarget(detail)}
                  disabled={isPending}
                >
                  발행 취소
                </Button>
              )}
            </div>
          )
        }
      >
        {detail && (
          <>
            <DetailSection title="기본 정보">
              <dl>
                <DetailRow label="발행번호">{detail.issueNo ?? '—'}</DetailRow>
                <DetailRow label="작성일자">{detail.writeDate}</DetailRow>
                <DetailRow label="청구/영수">{CHARGE_TYPE_LABEL[detail.chargeType]}</DetailRow>
                <DetailRow label="과세구분">{TAX_TYPE_LABEL[detail.taxType]}</DetailRow>
                <DetailRow label="품목">{detail.itemName}</DetailRow>
              </dl>
            </DetailSection>

            <DetailSection title="공급자">
              <dl>
                <DetailRow label="상호">{detail.supplier.companyName}</DetailRow>
                <DetailRow label="사업자번호">{detail.supplier.businessNo ?? '—'}</DetailRow>
                <DetailRow label="대표자">{detail.supplier.representative ?? '—'}</DetailRow>
                <DetailRow label="주소">{detail.supplier.address ?? '—'}</DetailRow>
                <DetailRow label="업태/종목">
                  {[detail.supplier.businessType, detail.supplier.businessItem]
                    .filter(Boolean)
                    .join(' / ') || '—'}
                </DetailRow>
              </dl>
            </DetailSection>

            <DetailSection title="공급받는자">
              <dl>
                <DetailRow label="상호">{detail.buyer.companyName}</DetailRow>
                <DetailRow label="사업자번호">{detail.buyer.businessNo ?? '—'}</DetailRow>
                <DetailRow label="대표자">{detail.buyer.representative ?? '—'}</DetailRow>
                <DetailRow label="주소">{detail.buyer.address ?? '—'}</DetailRow>
                <DetailRow label="업태/종목">
                  {[detail.buyer.businessType, detail.buyer.businessItem]
                    .filter(Boolean)
                    .join(' / ') || '—'}
                </DetailRow>
              </dl>
            </DetailSection>

            <DetailSection title="금액">
              <dl>
                <DetailRow label="공급가액">
                  <span className="font-mono">{won(detail.supplyAmount)}</span>
                </DetailRow>
                <DetailRow label="세액">
                  <span className="font-mono">{won(detail.vatAmount)}</span>
                </DetailRow>
                <DetailRow label="합계">
                  <span className="font-mono font-semibold">{won(detail.totalAmount)}</span>
                </DetailRow>
              </dl>
            </DetailSection>
          </>
        )}
      </DetailSheet>

      {/* 발행 다이얼로그 */}
      <Dialog open={issueOpen} onOpenChange={setIssueOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>세금계산서 발행</DialogTitle>
          </DialogHeader>
          <div className="grid gap-3 py-2">
            <div className="grid gap-1.5">
              <label className="text-xs font-medium text-muted-foreground">매출 인보이스 *</label>
              <Select value={arInvoiceId} onValueChange={(v) => setArInvoiceId(v ?? '')}>
                <SelectTrigger className="h-9 w-full">
                  <SelectValue placeholder="승인·완납된 매출 인보이스 선택" />
                </SelectTrigger>
                <SelectContent>
                  {issuableArInvoices.length === 0 && (
                    <div className="px-3 py-2 text-sm text-muted-foreground">
                      발행 가능한 인보이스가 없습니다
                    </div>
                  )}
                  {issuableArInvoices.map((inv) => (
                    <SelectItem key={inv.id} value={String(inv.id)}>
                      {inv.invoiceNo} · {inv.customerName} · {won(inv.totalAmount)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="grid gap-1.5">
                <label className="text-xs font-medium text-muted-foreground">작성일자</label>
                <DatePicker value={writeDate} onChange={setWriteDate} />
              </div>
              <div className="grid gap-1.5">
                <label className="text-xs font-medium text-muted-foreground">청구/영수</label>
                <Select
                  value={chargeType}
                  onValueChange={(v) => v && setChargeType(v as ChargeType)}
                >
                  <SelectTrigger className="h-9 w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CHARGE">청구</SelectItem>
                    <SelectItem value="RECEIPT">영수</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid gap-1.5">
              <label className="text-xs font-medium text-muted-foreground">품목명</label>
              <Input
                value={itemName}
                onChange={(e) => setItemName(e.target.value)}
                placeholder="미입력 시 기본값(상품)"
                className="h-9"
              />
            </div>
            <div className="grid gap-1.5">
              <label className="text-xs font-medium text-muted-foreground">비고</label>
              <Textarea value={note} onChange={(e) => setNote(e.target.value)} rows={2} />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleIssue} disabled={isPending}>
              발행
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 취소 확인 */}
      <Dialog open={cancelTarget !== null} onOpenChange={(o) => !o && setCancelTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>세금계산서 발행 취소</DialogTitle>
          </DialogHeader>
          {cancelTarget && (
            <p className="py-2 text-sm text-muted-foreground">
              <strong>{cancelTarget.issueNo}</strong> ({cancelTarget.buyer.companyName})를
              취소하시겠습니까? 취소 후 동일 매출 인보이스로 재발행할 수 있습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => cancelTarget && handleCancel(cancelTarget)}
              disabled={isPending}
            >
              발행 취소
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
