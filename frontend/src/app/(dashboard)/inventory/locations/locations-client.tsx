'use client'
import { useState, useTransition } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { createLocation, updateLocation, deactivateLocation } from './actions'
import type { Warehouse, Location, LocationType } from '@/types/inventory'

const NONE = '__none__'

const LOCATION_TYPE_LABEL: Record<LocationType, string> = {
  ZONE: 'ZONE (구역)',
  AISLE: 'AISLE (통로)',
  RACK: 'RACK (선반)',
  BIN: 'BIN (칸)',
}

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; loc: Location }
  | { type: 'deactivate'; loc: Location }

interface Props {
  warehouses: Warehouse[]
  selectedWarehouseId: string
  locations: Location[]
}

export default function LocationsClient({ warehouses, selectedWarehouseId, locations }: Props) {
  const router = useRouter()
  const { can } = usePermissions()
  const canWrite = can(PERM.INVENTORY_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')
  const [locationType, setLocationType] = useState<LocationType>('ZONE')

  const hasWarehouse = selectedWarehouseId !== ''

  const onWarehouseChange = (v: string | null) => {
    if (v == null) return
    router.push(`/inventory/locations?warehouseId=${v}`)
  }

  const openCreate = () => {
    setCode('')
    setName('')
    setParentId('')
    setLocationType('ZONE')
    setDialog({ type: 'create' })
  }

  const openEdit = (loc: Location) => {
    setCode(loc.code)
    setName(loc.name)
    setParentId(loc.parentId != null ? String(loc.parentId) : '')
    setLocationType(loc.locationType)
    setDialog({ type: 'edit', loc })
  }

  const handleCreate = () => {
    if (!hasWarehouse) {
      toast.error('창고를 먼저 선택해주세요')
      return
    }
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 로케이션명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createLocation({
          warehouseId: Number(selectedWarehouseId),
          code: code.trim(),
          name: name.trim(),
          parentId: parentId ? Number(parentId) : null,
          locationType,
        })
        toast.success('로케이션이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (loc: Location) => {
    if (!name.trim()) {
      toast.error('로케이션명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateLocation(loc.id, {
          version: loc.version,
          name: name.trim(),
          parentId: parentId ? Number(parentId) : null,
          locationType,
        })
        toast.success('로케이션이 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDeactivate = (loc: Location) => {
    startTransition(async () => {
      try {
        await deactivateLocation(loc.id)
        toast.success('로케이션이 비활성화되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  // 부모 선택지: 같은 창고의 로케이션 중 자기 자신 제외
  const parentOptions = (selfId?: number) => locations.filter((l) => l.id !== selfId)

  const locationForm = (selfId?: number) => (
    <div className="grid gap-4 py-2">
      {dialog.type === 'create' && (
        <div className="grid gap-1.5">
          <Label>코드 *</Label>
          <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="A-01-01" />
        </div>
      )}
      <div className="grid gap-1.5">
        <Label>로케이션명 *</Label>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="A구역 1번 선반"
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>유형 *</Label>
          <Select
            value={locationType}
            onValueChange={(v) => setLocationType((v ?? 'ZONE') as LocationType)}
          >
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(LOCATION_TYPE_LABEL) as LocationType[]).map((k) => (
                <SelectItem key={k} value={k}>
                  {LOCATION_TYPE_LABEL[k]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-1.5">
          <Label>상위 로케이션</Label>
          <Select
            value={parentId || NONE}
            onValueChange={(v) => setParentId(v === NONE ? '' : (v ?? ''))}
          >
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE}>없음 (최상위)</SelectItem>
              {parentOptions(selfId).map((l) => (
                <SelectItem key={l.id} value={String(l.id)}>
                  {l.code} — {l.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">로케이션 관리</h1>
          <p className="text-sm text-muted-foreground mt-1">
            창고 내 보관 위치(로케이션)를 관리합니다
          </p>
        </div>
        {canWrite && hasWarehouse && (
          <Button onClick={openCreate}>
            <PlusIcon />새 로케이션
          </Button>
        )}
      </div>

      <div className="mb-4 flex items-center gap-2">
        <Label className="text-sm text-muted-foreground">창고</Label>
        <Select
          value={selectedWarehouseId}
          onValueChange={onWarehouseChange}
          disabled={warehouses.length === 0}
        >
          <SelectTrigger className="w-64">
            <SelectValue placeholder="창고 선택" />
          </SelectTrigger>
          <SelectContent>
            {warehouses.map((wh) => (
              <SelectItem key={wh.id} value={String(wh.id)}>
                {wh.code} — {wh.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="bg-card rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>로케이션명</TableHead>
              <TableHead>유형</TableHead>
              <TableHead>상위 로케이션</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-20" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {!hasWarehouse && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground py-10">
                  창고를 먼저 등록해주세요
                </TableCell>
              </TableRow>
            )}
            {hasWarehouse && locations.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground py-10">
                  등록된 로케이션이 없습니다
                </TableCell>
              </TableRow>
            )}
            {locations.map((loc) => (
              <TableRow key={loc.id}>
                <TableCell className="font-mono text-sm">{loc.code}</TableCell>
                <TableCell className="font-medium">{loc.name}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{loc.locationType}</TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {loc.parentName ?? '—'}
                </TableCell>
                <TableCell>
                  <Badge variant={loc.active ? 'default' : 'secondary'}>
                    {loc.active ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    {canWrite && (
                      <Button
                        variant="ghost"
                        size="icon-xs"
                        title="수정"
                        onClick={() => openEdit(loc)}
                      >
                        <PencilIcon />
                      </Button>
                    )}
                    {canWrite && loc.active && (
                      <Button
                        variant="ghost"
                        size="icon-xs"
                        title="비활성화"
                        onClick={() => setDialog({ type: 'deactivate', loc })}
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
      </div>

      {/* Create Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 로케이션 등록</DialogTitle>
          </DialogHeader>
          {locationForm()}
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
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              로케이션 수정{dialog.type === 'edit' && ` — ${dialog.loc.code}`}
            </DialogTitle>
          </DialogHeader>
          {dialog.type === 'edit' && locationForm(dialog.loc.id)}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.loc)}
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
            <DialogTitle>로케이션 비활성화</DialogTitle>
          </DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.loc.code} {dialog.loc.name}
              </strong>
              을(를) 비활성화하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'deactivate' && handleDeactivate(dialog.loc)}
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
