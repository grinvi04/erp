'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, PencilIcon, BanIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { PaginationBar } from '@/components/ui/pagination-bar'
import { createItem, updateItem, deactivateItem } from './actions'
import type { Item, ItemCategory, Uom, CostMethod } from '@/types/inventory'
import type { PageResponse } from '@/types/api'

const COST_METHOD_LABEL: Record<CostMethod, string> = {
  FIFO: 'FIFO (선입선출)', LIFO: 'LIFO (후입선출)',
  WEIGHTED_AVG: '총평균법', STANDARD: '표준원가',
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
}

function fmtNum(n: number) { return n.toLocaleString('ko-KR') }

export default function ItemsClient({ data, categories, uoms }: Props) {
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
    setSku(''); setName(''); setDescription(''); setCategoryId(''); setUomId('')
    setCostMethod('STANDARD'); setStandardCost('0'); setReorderPoint('0')
    setReorderQty('0'); setMinStock('0'); setMaxStock('0')
    setLotTracked(false); setSerialTracked(false)
    setDialog({ type: 'create' })
  }

  const openEdit = (item: Item) => {
    setName(item.name); setDescription(item.description ?? '')
    setCategoryId(item.categoryId != null ? String(item.categoryId) : '')
    setUomId(String(item.uomId)); setCostMethod(item.costMethod)
    setStandardCost(String(item.standardCost)); setReorderPoint(String(item.reorderPoint))
    setReorderQty(String(item.reorderQty)); setMinStock(String(item.minStock))
    setMaxStock(String(item.maxStock)); setLotTracked(item.lotTracked)
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
    if (!name.trim()) { toast.error('품목명은 필수입니다'); return false }
    if (!uomId) { toast.error('단위(UOM)를 선택해주세요'); return false }
    return true
  }

  const handleCreate = () => {
    if (!sku.trim()) { toast.error('SKU는 필수입니다'); return }
    if (!validate()) return
    startTransition(async () => {
      try {
        await createItem({ sku: sku.trim(), ...buildPayload() })
        toast.success('품목이 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (item: Item) => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await updateItem(item.id, buildPayload())
        toast.success('품목이 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDeactivate = (item: Item) => {
    startTransition(async () => {
      try {
        await deactivateItem(item.id)
        toast.success('품목이 비활성화되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다') }
    })
  }

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
            <SelectTrigger className="w-full"><SelectValue placeholder="없음" /></SelectTrigger>
            <SelectContent>
              {categories.map((c) => (
                <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-1.5">
          <Label>단위(UOM) *</Label>
          <Select value={uomId} onValueChange={(v) => setUomId(v ?? '')}>
            <SelectTrigger className="w-full"><SelectValue placeholder="선택" /></SelectTrigger>
            <SelectContent>
              {uoms.map((u) => (
                <SelectItem key={u.id} value={String(u.id)}>{u.code} — {u.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>원가 계산법 *</Label>
          <Select value={costMethod} onValueChange={(v) => setCostMethod((v ?? 'STANDARD') as CostMethod)}>
            <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
            <SelectContent>
              {(Object.keys(COST_METHOD_LABEL) as CostMethod[]).map((k) => (
                <SelectItem key={k} value={k}>{COST_METHOD_LABEL[k]}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-1.5">
          <Label>표준원가</Label>
          <Input type="number" min={0} step={0.01} value={standardCost}
            onChange={(e) => setStandardCost(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-4">
        <div className="grid gap-1.5">
          <Label>재주문점</Label>
          <Input type="number" min={0} step={0.01} value={reorderPoint}
            onChange={(e) => setReorderPoint(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>재주문량</Label>
          <Input type="number" min={0} step={0.01} value={reorderQty}
            onChange={(e) => setReorderQty(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label>최소재고</Label>
          <Input type="number" min={0} step={0.01} value={minStock}
            onChange={(e) => setMinStock(e.target.value)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>최대재고</Label>
          <Input type="number" min={0} step={0.01} value={maxStock}
            onChange={(e) => setMaxStock(e.target.value)} />
        </div>
      </div>
      <div className="flex gap-6">
        <label className="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" checked={lotTracked} onChange={(e) => setLotTracked(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300" />
          <span className="text-sm">LOT 추적</span>
        </label>
        <label className="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" checked={serialTracked} onChange={(e) => setSerialTracked(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300" />
          <span className="text-sm">시리얼 추적</span>
        </label>
      </div>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">품목 관리</h1>
          <p className="text-sm text-gray-500 mt-1">재고 품목 마스터를 관리합니다</p>
        </div>
        <Button onClick={openCreate}><PlusIcon />새 품목</Button>
      </div>

      <div className="bg-white rounded-lg border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>SKU</TableHead>
              <TableHead>품목명</TableHead>
              <TableHead>분류</TableHead>
              <TableHead>단위</TableHead>
              <TableHead>원가법</TableHead>
              <TableHead className="text-right">표준원가</TableHead>
              <TableHead>LOT</TableHead>
              <TableHead>시리얼</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-20" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={10} className="text-center text-gray-400 py-10">
                  등록된 품목이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((item) => (
              <TableRow key={item.id}>
                <TableCell className="font-mono text-sm">{item.sku}</TableCell>
                <TableCell className="font-medium">{item.name}</TableCell>
                <TableCell className="text-sm text-gray-600">{item.categoryName ?? '—'}</TableCell>
                <TableCell className="text-sm text-gray-600">{item.uomCode}</TableCell>
                <TableCell className="text-sm text-gray-500">{item.costMethod}</TableCell>
                <TableCell className="text-right font-mono text-sm">
                  {fmtNum(item.standardCost)}
                </TableCell>
                <TableCell className="text-center text-sm">{item.lotTracked ? '●' : '○'}</TableCell>
                <TableCell className="text-center text-sm">{item.serialTracked ? '●' : '○'}</TableCell>
                <TableCell>
                  <Badge variant={item.active ? 'default' : 'secondary'}>
                    {item.active ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(item)}>
                      <PencilIcon />
                    </Button>
                    {item.active && (
                      <Button
                        variant="ghost" size="icon-xs" title="비활성화"
                        onClick={() => setDialog({ type: 'deactivate', item })}
                      >
                        <BanIcon className="text-destructive" />
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationBar
          page={data.page} totalPages={data.totalPages}
          totalElements={data.totalElements} size={data.size}
          basePath="/inventory/items"
        />
      </div>

      {/* Create Dialog */}
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>새 품목 등록</DialogTitle></DialogHeader>
          {itemForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={dialog.type === 'edit'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              품목 수정{dialog.type === 'edit' && ` — ${dialog.item.sku}`}
            </DialogTitle>
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
      <Dialog open={dialog.type === 'deactivate'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>품목 비활성화</DialogTitle></DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.item.sku} {dialog.item.name}</strong>을(를) 비활성화하시겠습니까?
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
