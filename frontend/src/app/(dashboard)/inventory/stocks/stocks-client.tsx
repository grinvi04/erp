'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { PackageSearchIcon, DownloadIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import { Badge } from '@/components/ui/badge'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { downloadCsv } from '@/lib/csv'
import type { Warehouse, StockBalance } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

interface Props {
  warehouses: Warehouse[]
  warehouseId: string
  data: PageResponse<StockBalance> | null
}

function fmtNum(n: number) {
  return n.toLocaleString('ko-KR')
}

// 보유 수량을 품목 안전재고(minStock)·재주문점(reorderPoint)과 대조해 경고 수준을 산출.
// 임계값이 0(미설정)인 경우는 신호를 내지 않는다 — 동작 안 하는 표시 금지(정직성).
type StockLevel = 'critical' | 'reorder' | null
function stockLevel(s: StockBalance): StockLevel {
  if (s.minStock > 0 && s.qtyOnHand < s.minStock) return 'critical'
  if (s.reorderPoint > 0 && s.qtyOnHand <= s.reorderPoint) return 'reorder'
  return null
}

const LEVEL_LABEL: Record<'critical' | 'reorder' | 'normal', string> = {
  critical: '안전재고 미달',
  reorder: '재주문 필요',
  normal: '정상',
}

export default function StocksClient({ warehouses, warehouseId, data }: Props) {
  const router = useRouter()

  const onWarehouseChange = (val: string | null) => {
    const wId = val ?? ''
    if (wId) {
      router.push(`/inventory/stocks?warehouseId=${wId}`)
    } else {
      router.push('/inventory/stocks')
    }
  }

  const columns: Column<StockBalance>[] = [
    {
      key: 'sku',
      header: 'SKU',
      sortable: true,
      sortValue: (s) => s.itemSku,
      cell: (s) => <span className="font-mono text-sm">{s.itemSku}</span>,
    },
    {
      key: 'name',
      header: '품목명',
      sortable: true,
      sortValue: (s) => s.itemName,
      cell: (s) => <span className="font-medium">{s.itemName}</span>,
    },
    {
      key: 'location',
      header: '위치',
      cell: (s) => (
        <span className="text-sm text-muted-foreground">
          {s.locationCode}
          {s.locationName ? (
            <span className="text-muted-foreground"> — {s.locationName}</span>
          ) : null}
        </span>
      ),
    },
    {
      key: 'lotSerial',
      header: 'Lot / Serial',
      cell: (s) => (
        <span className="text-sm text-muted-foreground">{s.lotNo ?? s.serialNo ?? '—'}</span>
      ),
    },
    {
      key: 'qtyOnHand',
      header: '보유',
      align: 'right',
      sortable: true,
      sortValue: (s) => s.qtyOnHand,
      cell: (s) => {
        const lv = stockLevel(s)
        return (
          <span
            className={cn(
              'font-mono text-sm',
              lv === 'critical' && 'font-semibold text-destructive',
              lv === 'reorder' && 'font-medium text-warning',
            )}
          >
            {fmtNum(s.qtyOnHand)}
          </span>
        )
      },
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((sum, r) => sum + r.qtyOnHand, 0).toLocaleString('ko-KR')}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      cell: (s) => {
        const lv = stockLevel(s)
        if (lv === 'critical') return <Badge variant="destructive">안전재고 미달</Badge>
        if (lv === 'reorder') return <Badge variant="warning">재주문 필요</Badge>
        return <span className="text-sm text-muted-foreground">—</span>
      },
    },
    {
      key: 'unitCost',
      header: '단가',
      align: 'right',
      sortable: true,
      sortValue: (s) => s.unitCost,
      cell: (s) => <span className="font-mono text-sm">{fmtNum(s.unitCost)}</span>,
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qKeyword, setQKeyword] = useState('')
  const [qLocation, setQLocation] = useState('')
  const [qLevel, setQLevel] = useState('')
  const [applied, setApplied] = useState({ keyword: '', location: '', level: '' })
  const onSearch = () =>
    setApplied({ keyword: qKeyword.trim(), location: qLocation, level: qLevel })
  const onReset = () => {
    setQKeyword('')
    setQLocation('')
    setQLevel('')
    setApplied({ keyword: '', location: '', level: '' })
  }

  const rows = data?.content ?? []
  // 위치 옵션 — 현재 페이지 데이터에서 고유 추출.
  const locationOptions = Array.from(
    new Map(rows.map((s) => [s.locationCode, s.locationName])).entries(),
  )
  const filtered = rows.filter((s) => {
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      if (!s.itemSku.toLowerCase().includes(kw) && !s.itemName.toLowerCase().includes(kw))
        return false
    }
    if (applied.location && s.locationCode !== applied.location) return false
    if (applied.level) {
      const lv = stockLevel(s) ?? 'normal'
      if (lv !== applied.level) return false
    }
    return true
  })

  const exportExcel = () =>
    downloadCsv(
      `재고현황_${new Date().toISOString().slice(0, 10)}`,
      ['SKU', '품목명', '위치', 'Lot/Serial', '보유', '상태', '단가'],
      filtered.map((s) => [
        s.itemSku,
        s.itemName,
        s.locationName ? `${s.locationCode} — ${s.locationName}` : s.locationCode,
        s.lotNo ?? s.serialNo ?? '',
        s.qtyOnHand,
        LEVEL_LABEL[stockLevel(s) ?? 'normal'],
        s.unitCost,
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader
        title="재고 현황"
        description="창고별 품목 재고 보유 수량을 조회하고 안전재고 미달 품목을 식별합니다"
        className="mb-4"
      >
        <div className="w-64">
          <Select value={warehouseId} onValueChange={onWarehouseChange}>
            <SelectTrigger className="w-full">
              <SelectValue placeholder="창고 선택" />
            </SelectTrigger>
            <SelectContent>
              {warehouses
                .filter((w) => w.active)
                .map((w) => (
                  <SelectItem key={w.id} value={String(w.id)}>
                    {w.code} {w.name}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>
        <Button variant="outline" onClick={exportExcel} disabled={!warehouseId || !data}>
          <DownloadIcon />
          엑셀
        </Button>
      </PageHeader>

      {!warehouseId || !data ? (
        <div className="rounded-xl border border-border bg-card">
          <EmptyState
            icon={PackageSearchIcon}
            title="조회할 창고를 선택해주세요"
            className="py-20"
          />
        </div>
      ) : (
        <div className="space-y-3">
          <FilterBar onSearch={onSearch} onReset={onReset}>
            <FilterField label="검색어">
              <Input
                value={qKeyword}
                onChange={(e) => setQKeyword(e.target.value)}
                placeholder="SKU·품목명"
                className="h-8 w-44"
              />
            </FilterField>
            <FilterField label="위치">
              <Select
                value={qLocation || 'ALL'}
                onValueChange={(v) => setQLocation(v === 'ALL' ? '' : (v ?? ''))}
              >
                <SelectTrigger className="h-8 w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">전체</SelectItem>
                  {locationOptions.map(([code, name]) => (
                    <SelectItem key={code} value={code}>
                      {name ? `${code} — ${name}` : code}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FilterField>
            <FilterField label="상태">
              <Select
                value={qLevel || 'ALL'}
                onValueChange={(v) => setQLevel(v === 'ALL' ? '' : (v ?? ''))}
              >
                <SelectTrigger className="h-8 w-36">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">전체</SelectItem>
                  <SelectItem value="critical">안전재고 미달</SelectItem>
                  <SelectItem value="reorder">재주문 필요</SelectItem>
                  <SelectItem value="normal">정상</SelectItem>
                </SelectContent>
              </Select>
            </FilterField>
          </FilterBar>

          <DataTable
            data={filtered}
            columns={columns}
            getRowId={(s) => s.id}
            showTotals
            totalLabel={`총 ${filtered.length}건`}
            empty={<EmptyState icon={PackageSearchIcon} title="재고 내역이 없습니다" />}
          />
          <PaginationBar
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={data.size}
            basePath="/inventory/stocks"
            searchParams={{ warehouseId }}
          />
        </div>
      )}
    </div>
  )
}
