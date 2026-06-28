'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
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
import { createItem, updateItem, deactivateItem } from './actions'
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

  const itemForm = (
    <div className="grid gap-4 py-2">
      <div className="grid grid-cols-2 gap-4">
        {dialog.type === 'create' && (
          <div className="grid gap-1.5">
            <Label>SKU *</Label>
            <Input value={sku} onChange={(e) => setSku(e.target.value)} placeholder="ITEM-001" />
          </div>
        )}
        <div className="grid gap-1.5">
          <Label>품목명 *</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="제품명" />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label>설명</Label>
        <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>분류</Label>
          <Select value={categoryId} onValueChange={(v) => setCategoryId(v ?? '')}>
            <SelectTrigger className="w-full">
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
        </div>
        <div className="grid gap-1.5">
          <Label>단위(UOM) *</Label>
          <Select value={uomId} onValueChange={(v) => setUomId(v ?? '')}>
            <SelectTrigger className="w-full">
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
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>원가 계산법 *</Label>
          <Select
            value={costMethod}
            onValueChange={(v) => setCostMethod((v ?? 'STANDARD') as CostMethod)}
          >
            <SelectTrigger className="w-full">
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
        </div>
        <div className="grid gap-1.5">
          <Label>표준원가</Label>
          <Input
            type="number"
            min={0}
            step={0.01}
            value={standardCost}
            onChange={(e) => setStandardCost(e.target.value)}
          />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-4">
        <div className="grid gap-1.5">
          <Label>재주문점</Label>
          <Input
            type="number"
            min={0}
            step={0.01}
            value={reorderPoint}
            onChange={(e) => setReorderPoint(e.target.value)}
          />
        </div>
        <div className="grid gap-1.5">
          <Label>재주문량</Label>
          <Input
            type="number"
            min={0}
            step={0.01}
            value={reorderQty}
            onChange={(e) => setReorderQty(e.target.value)}
          />
        </div>
        <div className="grid gap-1.5">
          <Label>최소재고</Label>
          <Input
            type="number"
            min={0}
            step={0.01}
            value={minStock}
            onChange={(e) => setMinStock(e.target.value)}
          />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>최대재고</Label>
          <Input
            type="number"
            min={0}
            step={0.01}
            value={maxStock}
            onChange={(e) => setMaxStock(e.target.value)}
          />
        </div>
      </div>
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
    <div className="p-6">
      <PageHeader title="품목 관리" description="재고 품목 마스터를 관리합니다" className="mb-6">
        <SearchInput placeholder="이름·코드 검색" className="w-64" />
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 품목
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <DataTable
          data={data.content}
          columns={columns}
          getRowId={(i) => i.id}
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
