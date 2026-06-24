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
import { createWarehouse, updateWarehouse, deactivateWarehouse } from './actions'
import type { Warehouse } from '@/types/inventory'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; wh: Warehouse }
  | { type: 'deactivate'; wh: Warehouse }

interface Props { warehouses: Warehouse[] }

export default function WarehousesClient({ warehouses }: Props) {
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')

  const openCreate = () => {
    setCode(''); setName(''); setAddress('')
    setDialog({ type: 'create' })
  }

  const openEdit = (wh: Warehouse) => {
    setName(wh.name); setAddress(wh.address ?? '')
    setDialog({ type: 'edit', wh })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) { toast.error('코드와 창고명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await createWarehouse({
          code: code.trim(), name: name.trim(), address: address.trim() || null,
        })
        toast.success('창고가 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (wh: Warehouse) => {
    if (!name.trim()) { toast.error('창고명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await updateWarehouse(wh.id, { name: name.trim(), address: address.trim() || null })
        toast.success('창고 정보가 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDeactivate = (wh: Warehouse) => {
    startTransition(async () => {
      try {
        await deactivateWarehouse(wh.id)
        toast.success('창고가 비활성화되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다') }
    })
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">창고 관리</h1>
          <p className="text-sm text-gray-500 mt-1">물류 창고 정보를 관리합니다</p>
        </div>
        <Button onClick={openCreate}><PlusIcon />새 창고</Button>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>창고명</TableHead>
              <TableHead>주소</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-20" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {warehouses.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-gray-400 py-10">
                  등록된 창고가 없습니다
                </TableCell>
              </TableRow>
            )}
            {warehouses.map((wh) => (
              <TableRow key={wh.id}>
                <TableCell className="font-mono text-sm">{wh.code}</TableCell>
                <TableCell className="font-medium">{wh.name}</TableCell>
                <TableCell className="text-sm text-gray-500 max-w-xs truncate">
                  {wh.address ?? '—'}
                </TableCell>
                <TableCell>
                  <Badge variant={wh.active ? 'default' : 'secondary'}>
                    {wh.active ? '활성' : '비활성'}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(wh)}>
                      <PencilIcon />
                    </Button>
                    {wh.active && (
                      <Button
                        variant="ghost" size="icon-xs" title="비활성화"
                        onClick={() => setDialog({ type: 'deactivate', wh })}
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
      <Dialog open={dialog.type === 'create'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>새 창고 등록</DialogTitle></DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>코드 *</Label>
              <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="WH-001" />
            </div>
            <div className="grid gap-1.5">
              <Label>창고명 *</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="서울 물류센터" />
            </div>
            <div className="grid gap-1.5">
              <Label>주소</Label>
              <Textarea rows={2} value={address} onChange={(e) => setAddress(e.target.value)}
                placeholder="서울특별시 ..." />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={dialog.type === 'edit'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              창고 수정{dialog.type === 'edit' && ` — ${dialog.wh.code}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>창고명 *</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <div className="grid gap-1.5">
              <Label>주소</Label>
              <Textarea rows={2} value={address} onChange={(e) => setAddress(e.target.value)} />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.wh)}
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
          <DialogHeader><DialogTitle>창고 비활성화</DialogTitle></DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.wh.code} {dialog.wh.name}</strong>을(를) 비활성화하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'deactivate' && handleDeactivate(dialog.wh)}
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
