'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon, DownloadIcon, UploadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { SearchInput } from '@/components/ui/search-input'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { BulkImportDialog } from '@/components/ui/bulk-import-dialog'
import { createItem, updateItem, deactivateItem, importItemsCsv, getItemTemplate } from './actions'
import type { Item, ItemCategory, Uom, CostMethod } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

const COST_METHOD_LABEL: Record<CostMethod, string> = {
  FIFO: '선입선출(FIFO)',
  LIFO: '후입선출(LIFO)',
  WEIGHTED_AVG: '총평균법',
  STANDARD: '표준원가',
}

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; item: Item }
  | { type: 'deactivate'; item: Item }

interface Props {
  data: PageResponse<Item>
  categories: ItemCategory[]
  uoms: Uom[]
  keyword: string
}

function fmtNum(n: number) {
  return n.toLocaleString('ko-KR')
}

export default function ItemsClient({ data, categories, uoms, keyword }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.INVENTORY_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [importOpen, setImportOpen] = useState(false)
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [sku, setSku] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [uomId, setUomId] = useState('')
  const [costMethod, setCostMethod] = useState<CostMethod>('STANDARD')
  const [standardCost, setStandardCost] = useState('')
  const [reorderPoint, setReorderPoint] = useState('')
  const [reorderQty, setReorderQty] = useState('')
  const [minStock, setMinStock] = useState('')
  const [maxStock, setMaxStock] = useState('')
  const [lotTracked, setLotTracked] = useState(false)
  const [serialTracked, setSerialTracked] = useState(false)

  const openCreate = () => {
    setSku('')
    setName('')
    setDescription('')
    setCategoryId('')
    setUomId('')
    setCostMethod('STANDARD')
    setStandardCost('0')
    setReorderPoint('0')
    setReorderQty('0')
    setMinStock('0')
    setMaxStock('0')
    setLotTracked(false)
    setSerialTracked(false)
    setDialog({ type: 'create' })
  }

  const openEdit = (item: Item) => {
    setName(item.name)
    setDescription(item.description ?? '')
    setCategoryId(item.categoryId != null ? String(item.categoryId) : '')
    setUomId(String(item.uomId))
    setCostMethod(item.costMethod)
    setStandardCost(String(item.standardCost))
    setReorderPoint(String(item.reorderPoint))
    setReorderQty(String(item.reorderQty))
    setMinStock(String(item.minStock))
    setMaxStock(String(item.maxStock))
    setLotTracked(item.lotTracked)
    setSerialTracked(item.serialTracked)
    setDialog({ type: 'edit', item })
  }

  const buildPayload = () => ({
    name: name.trim(),
    description: description.trim() || null,
    categoryId: categoryId ? Number(categoryId) : null,
    uomId: Number(uomId),
    costMethod,
    standardCost: Number(standardCost) || 0,
    reorderPoint: Number(reorderPoint) || 0,
    reorderQty: Number(reorderQty) || 0,
    minStock: Number(minStock) || 0,
    maxStock: Number(maxStock) || 0,
    lotTracked,
    serialTracked,
  })

  const validate = () => {
    if (!name.trim()) {
      toast.error('품목명은 필수입니다')
      return false
    }
    if (!uomId) {
      toast.error('단위(UOM)를 선택해주세요')
      return false
    }
    return true
  }

  const handleCreate = () => {
    if (!sku.trim()) {
      toast.error('SKU는 필수입니다')
      return
    }
    if (!validate()) return
    startTransition(async () => {
      try {
        await createItem({ sku: sku.trim(), ...buildPayload() })
        toast.success('품목이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (item: Item) => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await updateItem(item.id, { version: item.version, ...buildPayload() })
        toast.success('품목이 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDeactivate = (item: Item) => {
    startTransition(async () => {
      try {
        await deactivateItem(item.id)
        toast.success('품목이 비활성화되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Item>[] = [
    {
      key: 'sku',
      header: 'SKU',
      sortable: true,
      sortValue: (i) => i.sku,
      cell: (i) => <span className="font-mono text-sm">{i.sku}</span>,
    },
    {
      key: 'name',
      header: '품목명',
      sortable: true,
      sortValue: (i) => i.name,
      cell: (i) => <span className="font-medium">{i.name}</span>,
    },
    {
      key: 'category',
      header: '분류',
      cell: (i) => <span className="text-sm text-muted-foreground">{i.categoryName ?? '—'}</span>,
    },
    {
      key: 'uom',
      header: '단위',
      cell: (i) => <span className="text-sm text-muted-foreground">{i.uomCode}</span>,
    },
    {
      key: 'costMethod',
      header: '원가법',
      cell: (i) => <span className="text-sm text-muted-foreground">{i.costMethod}</span>,
    },
    {
      key: 'standardCost',
      header: '표준원가',
      align: 'right',
      sortable: true,
      sortValue: (i) => i.standardCost,
      cell: (i) => <span className="font-mono text-sm">{fmtNum(i.standardCost)}</span>,
    },
    {
      key: 'lot',
      header: 'LOT',
      align: 'center',
      cell: (i) => <span className="text-sm">{i.lotTracked ? '●' : '○'}</span>,
    },
    {
      key: 'serial',
      header: '시리얼',
      align: 'center',
      cell: (i) => <span className="text-sm">{i.serialTracked ? '●' : '○'}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (i) => (i.active ? 0 : 1),
      cell: (i) => (
        <Badge variant={i.active ? 'default' : 'secondary'}>{i.active ? '활성' : '비활성'}</Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (item) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(item)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && item.active && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="비활성화"
              onClick={() => setDialog({ type: 'deactivate', item })}
            >
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qCategory, setQCategory] = useState('')
  const [qMethod, setQMethod] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [applied, setApplied] = useState({ category: '', method: '', status: '' })
  const onSearch = () => setApplied({ category: qCategory, method: qMethod, status: qStatus })
  const onReset = () => {
    setQCategory('')
    setQMethod('')
    setQStatus('')
    setApplied({ category: '', method: '', status: '' })
  }
  const filtered = data.content.filter((i) => {
    if (applied.category && String(i.categoryId ?? '') !== applied.category) return false
    if (applied.method && i.costMethod !== applied.method) return false
    if (applied.status && (applied.status === 'ACTIVE') !== i.active) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `품목_${new Date().toISOString().slice(0, 10)}`,
      ['SKU', '품목명', '분류', '단위', '원가법', '표준원가', 'LOT', '시리얼', '상태'],
      filtered.map((i) => [
        i.sku,
        i.name,
        i.categoryName ?? '',
        i.uomCode,
        i.costMethod,
        i.standardCost,
        i.lotTracked ? 'Y' : 'N',
        i.serialTracked ? 'Y' : 'N',
        i.active ? '활성' : '비활성',
      ]),
    )

  const itemForm = (
    <div className="grid gap-4 py-2">
      <FormGrid>
        {dialog.type === 'create' && (
          <FormRow label="SKU" required>
            <Input
              value={sku}
              onChange={(e) => setSku(e.target.value)}
              placeholder="ITEM-001"
              className="h-8"
            />
          </FormRow>
        )}
        <FormRow label="품목명" required>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="제품명"
            className="h-8"
          />
        </FormRow>
        <FormRow label="설명" span>
          <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
        </FormRow>
        <FormRow label="분류">
          <Select value={categoryId} onValueChange={(v) => setCategoryId(v ?? '')}>
            <SelectTrigger className="h-8 w-full">
              <SelectValue placeholder="없음" />
            </SelectTrigger>
            <SelectContent>
              {categories.map((c) => (
                <SelectItem key={c.id} value={String(c.id)}>
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="단위(UOM)" required>
          <Select value={uomId} onValueChange={(v) => setUomId(v ?? '')}>
            <SelectTrigger className="h-8 w-full">
              <SelectValue placeholder="선택" />
            </SelectTrigger>
            <SelectContent>
              {uoms.map((u) => (
                <SelectItem key={u.id} value={String(u.id)}>
                  {u.code} — {u.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="원가 계산법" required>
          <Select
            value={costMethod}
            onValueChange={(v) => setCostMethod((v ?? 'STANDARD') as CostMethod)}
          >
            <SelectTrigger className="h-8 w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(COST_METHOD_LABEL) as CostMethod[]).map((k) => (
                <SelectItem key={k} value={k}>
                  {COST_METHOD_LABEL[k]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormRow>
        <FormRow label="표준원가">
          <Input
            type="number"
            min={0}
            step={0.01}
            value={standardCost}
            onChange={(e) => setStandardCost(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="재주문점">
          <Input
            type="number"
            min={0}
            step={0.01}
            value={reorderPoint}
            onChange={(e) => setReorderPoint(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="재주문량">
          <Input
            type="number"
            min={0}
            step={0.01}
            value={reorderQty}
            onChange={(e) => setReorderQty(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="최소재고">
          <Input
            type="number"
            min={0}
            step={0.01}
            value={minStock}
            onChange={(e) => setMinStock(e.target.value)}
            className="h-8"
          />
        </FormRow>
        <FormRow label="최대재고">
          <Input
            type="number"
            min={0}
            step={0.01}
            value={maxStock}
            onChange={(e) => setMaxStock(e.target.value)}
            className="h-8"
          />
        </FormRow>
      </FormGrid>
      <div className="flex gap-6">
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={lotTracked}
            onChange={(e) => setLotTracked(e.target.checked)}
            className="h-4 w-4 rounded border-input"
          />
          <span className="text-sm">LOT 추적</span>
        </label>
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={serialTracked}
            onChange={(e) => setSerialTracked(e.target.checked)}
            className="h-4 w-4 rounded border-input"
          />
          <span className="text-sm">시리얼 추적</span>
        </label>
      </div>
    </div>
  )

  return (
    <div className="p-5">
      <PageHeader title="품목 관리" description="재고 품목 마스터를 관리합니다" className="mb-4">
        <SearchInput placeholder="이름·코드 검색" className="w-64" />
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button variant="outline" onClick={() => setImportOpen(true)}>
            <UploadIcon />
            엑셀 업로드
          </Button>
        )}
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 품목
          </Button>
        )}
      </PageHeader>

      <BulkImportDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        title="품목 대량 업로드"
        templateFilename="item-template.csv"
        uploadAction={importItemsCsv}
        templateAction={getItemTemplate}
      />

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="분류">
            <Select
              value={qCategory || 'ALL'}
              onValueChange={(v) => setQCategory(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {categories.map((c) => (
                  <SelectItem key={c.id} value={String(c.id)}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="원가법">
            <Select
              value={qMethod || 'ALL'}
              onValueChange={(v) => setQMethod(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {(Object.keys(COST_METHOD_LABEL) as CostMethod[]).map((k) => (
                  <SelectItem key={k} value={k}>
                    {COST_METHOD_LABEL[k]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="ACTIVE">활성</SelectItem>
                <SelectItem value="INACTIVE">비활성</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(i) => i.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 품목이 없습니다"
              description={canWrite ? '우측 상단에서 새 품목을 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/inventory/items"
          searchParams={keyword ? { keyword } : undefined}
        />
      </div>

      {/* Create Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>새 품목 등록</DialogTitle>
          </DialogHeader>
          {itemForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog
        open={dialog.type === 'edit'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>품목 수정{dialog.type === 'edit' && ` — ${dialog.item.sku}`}</DialogTitle>
          </DialogHeader>
          {itemForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.item)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Deactivate Dialog */}
      <Dialog
        open={dialog.type === 'deactivate'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>품목 비활성화</DialogTitle>
          </DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.item.sku} {dialog.item.name}
              </strong>
              을(를) 비활성화하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'deactivate' && handleDeactivate(dialog.item)}
              disabled={isPending}
            >
              비활성화
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
