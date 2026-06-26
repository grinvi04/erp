'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { createUom, updateUom, deleteUom } from './actions'
import type { Uom } from '@/types/inventory'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; uom: Uom }
  | { type: 'delete'; uom: Uom }

interface Props { uoms: Uom[] }

export default function UomsClient({ uoms }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.INVENTORY_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')

  const openCreate = () => {
    setCode(''); setName('')
    setDialog({ type: 'create' })
  }

  const openEdit = (uom: Uom) => {
    setCode(uom.code); setName(uom.name)
    setDialog({ type: 'edit', uom })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) { toast.error('코드와 단위명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await createUom({ code: code.trim(), name: name.trim() })
        toast.success('단위가 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (uom: Uom) => {
    if (!name.trim()) { toast.error('단위명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await updateUom(uom.id, { code: uom.code, name: name.trim() })
        toast.success('단위가 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDelete = (uom: Uom) => {
    startTransition(async () => {
      try {
        await deleteUom(uom.id)
        toast.success('단위가 삭제되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다') }
    })
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">단위 관리</h1>
          <p className="text-sm text-gray-500 mt-1">측정 단위(UOM) 마스터를 관리합니다</p>
        </div>
        {canWrite && <Button onClick={openCreate}><PlusIcon />새 단위</Button>}
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>단위명</TableHead>
              <TableHead className="w-20" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {uoms.length === 0 && (
              <TableRow>
                <TableCell colSpan={3} className="text-center text-gray-400 py-10">
                  등록된 단위가 없습니다
                </TableCell>
              </TableRow>
            )}
            {uoms.map((uom) => (
              <TableRow key={uom.id}>
                <TableCell className="font-mono text-sm">{uom.code}</TableCell>
                <TableCell className="font-medium">{uom.name}</TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    {canWrite && (
                      <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(uom)}>
                        <PencilIcon />
                      </Button>
                    )}
                    {canWrite && (
                      <Button
                        variant="ghost" size="icon-xs" title="삭제"
                        onClick={() => setDialog({ type: 'delete', uom })}
                      >
                        <Trash2Icon className="text-destructive" />
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
          <DialogHeader><DialogTitle>새 단위 등록</DialogTitle></DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>코드 *</Label>
              <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="EA" />
            </div>
            <div className="grid gap-1.5">
              <Label>단위명 *</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="개" />
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
              단위 수정{dialog.type === 'edit' && ` — ${dialog.uom.code}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>코드</Label>
              <Input value={code} disabled />
            </div>
            <div className="grid gap-1.5">
              <Label>단위명 *</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.uom)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Dialog */}
      <Dialog open={dialog.type === 'delete'} onOpenChange={(o) => { if (!o) close() }}>
        <DialogContent>
          <DialogHeader><DialogTitle>단위 삭제</DialogTitle></DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.uom.code} {dialog.uom.name}</strong>을(를) 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.uom)}
              disabled={isPending}
            >
              삭제
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
