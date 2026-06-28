'use client'
import { useState, useTransition, useMemo } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, TrashIcon } from 'lucide-react'
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
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
import {
  createMovement,
  confirmMovement,
  submitMovement,
  approveMovement,
  cancelMovement,
  withdrawMovement,
  getLocationsByWarehouse,
} from './actions'
import type {
  Movement,
  MovementType,
  MovementStatus,
  Item,
  Warehouse,
  Location,
} from '@/types/inventory'
import type { PageResponse } from '@/types/api'

const TYPE_LABEL: Record<MovementType, string> = {
  RECEIPT: '입고',
  ISSUE: '출고',
  TRANSFER: '창고이동',
  ADJUSTMENT: '조정',
}
const STATUS_LABEL: Record<MovementStatus, string> = {
  DRAFT: '임시',
  PENDING_APPROVAL: '결재중',
  CONFIRMED: '확정',
  CANCELLED: '취소',
}
const STATUS_VARIANT: Record<MovementStatus, 'secondary' | 'default' | 'destructive' | 'outline'> =
  {
    DRAFT: 'secondary',
    PENDING_APPROVAL: 'outline',
    CONFIRMED: 'default',
    CANCELLED: 'destructive',
  }

interface LineRow {
  itemId: string
  fromLocationId: string
  toLocationId: string
  lotNo: string
  serialNo: string
  qty: string
  unitCost: string
}
const emptyLine = (): LineRow => ({
  itemId: '',
  fromLocationId: '',
  toLocationId: '',
  lotNo: '',
  serialNo: '',
  qty: '',
  unitCost: '0',
})

type DialogState = { type: 'none' } | { type: 'create' } | { type: 'cancel'; mv: Movement }

interface Props {
  data: PageResponse<Movement>
  items: Item[]
  warehouses: Warehouse[]
}

export default function MovementsClient({ data, items, warehouses }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.INVENTORY_WRITE)
  const canApprove = can(PERM.INVENTORY_MOVEMENT_APPROVE)
  const [dialog, setDialog] = useState<DialogState>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const [isLoadingLocations, setIsLoadingLocations] = useState(false)
  const close = () => setDialog({ type: 'none' })

  const [movementType, setMovementType] = useState<MovementType>('RECEIPT')
  const [movementDate, setMovementDate] = useState('')
  const [note, setNote] = useState('')
  const [lines, setLines] = useState<LineRow[]>([emptyLine()])
  const [dialogWarehouseId, setDialogWarehouseId] = useState('')
  const [dialogLocations, setDialogLocations] = useState<Location[]>([])
  // 창고간(TRANSFER) 이동: 출고/입고 창고를 서로 독립적으로 선택하므로 위치 목록을 양쪽 따로 유지한다.
  const [fromWarehouseId, setFromWarehouseId] = useState('')
  const [fromLocations, setFromLocations] = useState<Location[]>([])
  const [toWarehouseId, setToWarehouseId] = useState('')
  const [toLocations, setToLocations] = useState<Location[]>([])
  const [isLoadingFromLocations, setIsLoadingFromLocations] = useState(false)
  const [isLoadingToLocations, setIsLoadingToLocations] = useState(false)

  const isTransfer = movementType === 'TRANSFER'
  const activeItems = useMemo(() => items.filter((i) => i.active), [items])
  const activeLocations = useMemo(() => dialogLocations.filter((l) => l.active), [dialogLocations])
  const activeFromLocations = useMemo(() => fromLocations.filter((l) => l.active), [fromLocations])
  const activeToLocations = useMemo(() => toLocations.filter((l) => l.active), [toLocations])
  const warehouseOptions = warehouses
    .filter((w) => w.active)
    .map((w) => (
      <SelectItem key={w.id} value={String(w.id)}>
        {w.code} {w.name}
      </SelectItem>
    ))

  const itemOf = (line: LineRow): Item | undefined =>
    items.find((i) => String(i.id) === line.itemId)
  const anyLotTracked = useMemo(
    () => lines.some((l) => itemOf(l)?.lotTracked),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [lines, items],
  )
  const anySerialTracked = useMemo(
    () => lines.some((l) => itemOf(l)?.serialTracked),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [lines, items],
  )

  const onWarehouseChange = (val: string | null) => {
    const wId = val ?? ''
    setDialogWarehouseId(wId)
    setDialogLocations([])
    setLines((prev) => prev.map((l) => ({ ...l, fromLocationId: '', toLocationId: '' })))
    if (wId) {
      setIsLoadingLocations(true)
      getLocationsByWarehouse(Number(wId))
        .then((locs) => setDialogLocations(locs))
        .catch(() => {})
        .finally(() => setIsLoadingLocations(false))
    }
  }

  const onFromWarehouseChange = (val: string | null) => {
    const wId = val ?? ''
    setFromWarehouseId(wId)
    setFromLocations([])
    setLines((prev) => prev.map((l) => ({ ...l, fromLocationId: '' })))
    if (wId) {
      setIsLoadingFromLocations(true)
      getLocationsByWarehouse(Number(wId))
        .then((locs) => setFromLocations(locs))
        .catch(() => {})
        .finally(() => setIsLoadingFromLocations(false))
    }
  }

  const onToWarehouseChange = (val: string | null) => {
    const wId = val ?? ''
    setToWarehouseId(wId)
    setToLocations([])
    setLines((prev) => prev.map((l) => ({ ...l, toLocationId: '' })))
    if (wId) {
      setIsLoadingToLocations(true)
      getLocationsByWarehouse(Number(wId))
        .then((locs) => setToLocations(locs))
        .catch(() => {})
        .finally(() => setIsLoadingToLocations(false))
    }
  }

  const resetWarehouses = () => {
    setDialogWarehouseId('')
    setDialogLocations([])
    setFromWarehouseId('')
    setFromLocations([])
    setToWarehouseId('')
    setToLocations([])
  }

  const onMovementTypeChange = (v: string | null) => {
    const t = (v ?? 'RECEIPT') as MovementType
    setMovementType(t)
    setLines((prev) => prev.map((l) => ({ ...l, fromLocationId: '', toLocationId: '' })))
    resetWarehouses()
  }

  const openCreate = () => {
    setMovementType('RECEIPT')
    setMovementDate('')
    setNote('')
    setLines([emptyLine()])
    resetWarehouses()
    setDialog({ type: 'create' })
  }

  const addLine = () => setLines((prev) => [...prev, emptyLine()])
  const removeLine = (i: number) => setLines((prev) => prev.filter((_, idx) => idx !== i))
  const setLine = (i: number, field: keyof LineRow, val: string) =>
    setLines((prev) => prev.map((l, idx) => (idx === i ? { ...l, [field]: val } : l)))

  const handleCreate = () => {
    if (!movementDate) {
      toast.error('이동일은 필수입니다')
      return
    }
    if (lines.some((l) => !l.itemId || !l.qty || Number(l.qty) <= 0 || isNaN(Number(l.qty)))) {
      toast.error('모든 행에 품목과 수량을 입력해주세요')
      return
    }
    const needsFromRequired = movementType === 'ISSUE' || movementType === 'TRANSFER'
    const needsToRequired = movementType === 'RECEIPT' || movementType === 'TRANSFER'
    if (needsFromRequired && lines.some((l) => !l.fromLocationId)) {
      toast.error('출고 위치를 선택해주세요')
      return
    }
    if (needsToRequired && lines.some((l) => !l.toLocationId)) {
      toast.error('입고 위치를 선택해주세요')
      return
    }
    if (lines.some((l) => itemOf(l)?.lotTracked && !l.lotNo.trim())) {
      toast.error('로트 추적 품목은 로트 번호가 필수입니다')
      return
    }
    if (lines.some((l) => itemOf(l)?.serialTracked && !l.serialNo.trim())) {
      toast.error('시리얼 추적 품목은 시리얼 번호가 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createMovement({
          movementType,
          movementDate,
          referenceType: null,
          note: note.trim() || null,
          lines: lines.map((l) => ({
            itemId: Number(l.itemId),
            fromLocationId: l.fromLocationId ? Number(l.fromLocationId) : null,
            toLocationId: l.toLocationId ? Number(l.toLocationId) : null,
            lotNo: itemOf(l)?.lotTracked ? l.lotNo.trim() : null,
            serialNo: itemOf(l)?.serialTracked ? l.serialNo.trim() : null,
            qty: Number(l.qty),
            unitCost: Number(l.unitCost) || 0,
          })),
        })
        toast.success('재고 이동이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleConfirm = (mv: Movement) => {
    startTransition(async () => {
      try {
        await confirmMovement(mv.id)
        toast.success('이동이 확정되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '확정 중 오류가 발생했습니다')
      }
    })
  }

  const handleSubmit = (mv: Movement) => {
    startTransition(async () => {
      try {
        await submitMovement(mv.id)
        toast.success('결재 상신되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '상신 중 오류가 발생했습니다')
      }
    })
  }

  const handleApprove = (mv: Movement) => {
    startTransition(async () => {
      try {
        await approveMovement(mv.id)
        toast.success('승인·확정 처리되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '승인 중 오류가 발생했습니다')
      }
    })
  }

  const handleWithdraw = (mv: Movement) => {
    startTransition(async () => {
      try {
        await withdrawMovement(mv.id)
        toast.success('상신을 철회했습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '철회 중 오류가 발생했습니다')
      }
    })
  }

  const handleCancel = (mv: Movement) => {
    startTransition(async () => {
      try {
        await cancelMovement(mv.id)
        toast.success('이동이 취소되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '취소 중 오류가 발생했습니다')
      }
    })
  }

  const needsFrom =
    movementType === 'ISSUE' || movementType === 'TRANSFER' || movementType === 'ADJUSTMENT'
  const needsTo =
    movementType === 'RECEIPT' || movementType === 'TRANSFER' || movementType === 'ADJUSTMENT'

  const columns: Column<Movement>[] = [
    {
      key: 'movementNo',
      header: '이동번호',
      sortable: true,
      sortValue: (mv) => mv.movementNo,
      cell: (mv) => <span className="font-mono text-sm">{mv.movementNo}</span>,
    },
    {
      key: 'type',
      header: '유형',
      sortable: true,
      sortValue: (mv) => TYPE_LABEL[mv.movementType],
      cell: (mv) => <Badge variant="secondary">{TYPE_LABEL[mv.movementType]}</Badge>,
    },
    {
      key: 'item',
      header: '품목',
      cell: (mv) => (
        <span className="text-sm text-foreground">
          {mv.lines && mv.lines.length > 0
            ? mv.lines.length === 1
              ? mv.lines[0].itemName
              : `${mv.lines[0].itemName} 외 ${mv.lines.length - 1}건`
            : '—'}
        </span>
      ),
    },
    {
      key: 'movementDate',
      header: '이동일',
      sortable: true,
      sortValue: (mv) => mv.movementDate,
      cell: (mv) => <span className="text-sm">{mv.movementDate}</span>,
    },
    {
      key: 'note',
      header: '메모',
      cell: (mv) => (
        <span className="block max-w-xs truncate text-sm text-muted-foreground">
          {mv.note ?? '—'}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (mv) => STATUS_LABEL[mv.status],
      cell: (mv) => <Badge variant={STATUS_VARIANT[mv.status]}>{STATUS_LABEL[mv.status]}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-32',
      cell: (mv) => (
        <>
          {canWrite && mv.status === 'DRAFT' && (
            <div className="flex justify-end gap-1">
              {mv.movementType === 'ADJUSTMENT' ? (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleSubmit(mv)}
                  disabled={isPending}
                  title="결재상신"
                >
                  결재상신
                </Button>
              ) : (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleConfirm(mv)}
                  disabled={isPending}
                  title="확정"
                >
                  확정
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setDialog({ type: 'cancel', mv })}
                disabled={isPending}
                title="취소"
                className="text-destructive"
              >
                취소
              </Button>
            </div>
          )}
          {mv.status === 'PENDING_APPROVAL' && (canApprove || canWrite) && (
            <div className="flex justify-end gap-1">
              {canApprove && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleApprove(mv)}
                  disabled={isPending}
                  title="승인"
                >
                  승인
                </Button>
              )}
              {canWrite && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleWithdraw(mv)}
                  disabled={isPending}
                  title="철회"
                  className="text-destructive"
                >
                  철회
                </Button>
              )}
            </div>
          )}
        </>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="재고 이동"
        description="재고 입출고 및 이전 내역을 관리합니다"
        className="mb-6"
      >
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 이동 등록
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <DataTable
          data={data.content}
          columns={columns}
          getRowId={(mv) => mv.id}
          empty={<EmptyState title="재고 이동 내역이 없습니다" />}
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/inventory/movements"
        />
      </div>

      {/* Create Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-4xl">
          <DialogHeader>
            <DialogTitle>재고 이동 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div
              className={
                isTransfer
                  ? 'grid grid-cols-1 gap-4 sm:grid-cols-2'
                  : 'grid grid-cols-1 gap-4 sm:grid-cols-3'
              }
            >
              <div className="grid gap-1.5">
                <Label>유형 *</Label>
                <Select value={movementType} onValueChange={onMovementTypeChange}>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(Object.keys(TYPE_LABEL) as MovementType[]).map((t) => (
                      <SelectItem key={t} value={t}>
                        {TYPE_LABEL[t]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-1.5">
                <Label>이동일 *</Label>
                <Input
                  type="date"
                  value={movementDate}
                  onChange={(e) => setMovementDate(e.target.value)}
                />
              </div>
              {!isTransfer && (
                <div className="grid gap-1.5">
                  <Label>창고 (위치 조회용)</Label>
                  <Select value={dialogWarehouseId} onValueChange={onWarehouseChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="창고(선택)" />
                    </SelectTrigger>
                    <SelectContent>{warehouseOptions}</SelectContent>
                  </Select>
                </div>
              )}
            </div>
            {isTransfer && (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="grid gap-1.5">
                  <Label>출고 창고 *</Label>
                  <Select value={fromWarehouseId} onValueChange={onFromWarehouseChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="출고 창고 선택" />
                    </SelectTrigger>
                    <SelectContent>{warehouseOptions}</SelectContent>
                  </Select>
                </div>
                <div className="grid gap-1.5">
                  <Label>입고 창고 *</Label>
                  <Select value={toWarehouseId} onValueChange={onToWarehouseChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="입고 창고 선택" />
                    </SelectTrigger>
                    <SelectContent>{warehouseOptions}</SelectContent>
                  </Select>
                </div>
              </div>
            )}
            <div className="grid gap-1.5">
              <Label>메모</Label>
              <Textarea rows={2} value={note} onChange={(e) => setNote(e.target.value)} />
            </div>

            {/* Lines */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <Label>이동 행</Label>
                <Button variant="ghost" size="sm" onClick={addLine}>
                  <PlusIcon />행 추가
                </Button>
              </div>
              <div className="border rounded overflow-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/40">
                      <TableHead className="w-48">품목 *</TableHead>
                      {needsFrom && <TableHead className="w-36">출고 위치</TableHead>}
                      {needsTo && <TableHead className="w-36">입고 위치</TableHead>}
                      {anyLotTracked && <TableHead className="w-32">로트번호</TableHead>}
                      {anySerialTracked && <TableHead className="w-32">시리얼번호</TableHead>}
                      <TableHead className="text-right w-24">수량 *</TableHead>
                      <TableHead className="text-right w-28">단가</TableHead>
                      <TableHead className="w-8" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {lines.map((line, i) => (
                      <TableRow key={i}>
                        <TableCell className="py-1">
                          <Select
                            value={line.itemId}
                            onValueChange={(v) => setLine(i, 'itemId', v ?? '')}
                          >
                            <SelectTrigger className="w-full h-8 text-sm">
                              <SelectValue placeholder="품목 선택" />
                            </SelectTrigger>
                            <SelectContent>
                              {activeItems.map((item) => (
                                <SelectItem key={item.id} value={String(item.id)}>
                                  {item.sku} — {item.name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </TableCell>
                        {needsFrom && (
                          <TableCell className="py-1">
                            <Select
                              value={line.fromLocationId}
                              onValueChange={(v) => setLine(i, 'fromLocationId', v ?? '')}
                              disabled={isTransfer ? isLoadingFromLocations : isLoadingLocations}
                            >
                              <SelectTrigger className="w-full h-8 text-sm">
                                <SelectValue placeholder="위치 선택" />
                              </SelectTrigger>
                              <SelectContent>
                                {(isTransfer ? activeFromLocations : activeLocations).map((l) => (
                                  <SelectItem key={l.id} value={String(l.id)}>
                                    {l.code}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </TableCell>
                        )}
                        {needsTo && (
                          <TableCell className="py-1">
                            <Select
                              value={line.toLocationId}
                              onValueChange={(v) => setLine(i, 'toLocationId', v ?? '')}
                              disabled={isTransfer ? isLoadingToLocations : isLoadingLocations}
                            >
                              <SelectTrigger className="w-full h-8 text-sm">
                                <SelectValue placeholder="위치 선택" />
                              </SelectTrigger>
                              <SelectContent>
                                {(isTransfer ? activeToLocations : activeLocations).map((l) => (
                                  <SelectItem key={l.id} value={String(l.id)}>
                                    {l.code}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </TableCell>
                        )}
                        {anyLotTracked && (
                          <TableCell className="py-1">
                            {itemOf(line)?.lotTracked ? (
                              <Input
                                className="h-8 text-sm"
                                value={line.lotNo}
                                onChange={(e) => setLine(i, 'lotNo', e.target.value)}
                                placeholder="로트번호"
                              />
                            ) : (
                              <span className="text-sm text-muted-foreground">—</span>
                            )}
                          </TableCell>
                        )}
                        {anySerialTracked && (
                          <TableCell className="py-1">
                            {itemOf(line)?.serialTracked ? (
                              <Input
                                className="h-8 text-sm"
                                value={line.serialNo}
                                onChange={(e) => setLine(i, 'serialNo', e.target.value)}
                                placeholder="시리얼번호"
                              />
                            ) : (
                              <span className="text-sm text-muted-foreground">—</span>
                            )}
                          </TableCell>
                        )}
                        <TableCell className="py-1">
                          <Input
                            className="text-right h-8 text-sm font-mono"
                            type="number"
                            min={0.001}
                            step={0.001}
                            value={line.qty}
                            onChange={(e) => setLine(i, 'qty', e.target.value)}
                            placeholder="0"
                          />
                        </TableCell>
                        <TableCell className="py-1">
                          <Input
                            className="text-right h-8 text-sm font-mono"
                            type="number"
                            min={0}
                            step={0.01}
                            value={line.unitCost}
                            onChange={(e) => setLine(i, 'unitCost', e.target.value)}
                          />
                        </TableCell>
                        <TableCell className="py-1">
                          {lines.length > 1 && (
                            <Button variant="ghost" size="icon-xs" onClick={() => removeLine(i)}>
                              <TrashIcon className="text-destructive" />
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Cancel Confirmation Dialog */}
      <Dialog
        open={dialog.type === 'cancel'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>재고 이동 취소</DialogTitle>
          </DialogHeader>
          {dialog.type === 'cancel' && (
            <p className="text-sm text-muted-foreground py-2">
              이동번호 <strong>{dialog.mv.movementNo}</strong>을(를) 취소하시겠습니까? 이 작업은
              되돌릴 수 없습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'cancel' && handleCancel(dialog.mv)}
              disabled={isPending}
            >
              취소 확정
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
