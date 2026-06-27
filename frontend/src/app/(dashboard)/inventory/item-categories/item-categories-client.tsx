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
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { createItemCategory, updateItemCategory, deleteItemCategory } from './actions'
import type { ItemCategory } from '@/types/inventory'

const NONE = '__none__'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; cat: ItemCategory }
  | { type: 'delete'; cat: ItemCategory }

interface Props { categories: ItemCategory[] }

export default function ItemCategoriesClient({ categories }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.INVENTORY_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')

  const openCreate = () => {
    setCode(''); setName(''); setParentId('')
    setDialog({ type: 'create' })
  }

  const openEdit = (cat: ItemCategory) => {
    setCode(cat.code); setName(cat.name)
    setParentId(cat.parentId != null ? String(cat.parentId) : '')
    setDialog({ type: 'edit', cat })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) { toast.error('코드와 분류명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await createItemCategory({
          code: code.trim(), name: name.trim(),
          parentId: parentId ? Number(parentId) : null,
        })
        toast.success('품목분류가 등록되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다') }
    })
  }

  const handleUpdate = (cat: ItemCategory) => {
    if (!name.trim()) { toast.error('분류명은 필수입니다'); return }
    startTransition(async () => {
      try {
        await updateItemCategory(cat.id, {
          code: cat.code, name: name.trim(),
          parentId: parentId ? Number(parentId) : null,
          version: cat.version,
        })
        toast.success('품목분류가 수정되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다') }
    })
  }

  const handleDelete = (cat: ItemCategory) => {
    startTransition(async () => {
      try {
        await deleteItemCategory(cat.id)
        toast.success('품목분류가 삭제되었습니다')
        close()
      } catch (e) { toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다') }
    })
  }

  // 부모 선택지: 자기 자신 제외 (순환 방지)
  const parentOptions = (selfId?: number) =>
    categories.filter((c) => c.id !== selfId)

  const parentField = (selfId?: number) => (
    <div className="grid gap-1.5">
      <Label>상위분류</Label>
      <Select
        value={parentId || NONE}
        onValueChange={(v) => setParentId(v === NONE ? '' : (v ?? ''))}
      >
        <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
        <SelectContent>
          <SelectItem value={NONE}>없음 (최상위)</SelectItem>
          {parentOptions(selfId).map((c) => (
            <SelectItem key={c.id} value={String(c.id)}>{c.code} — {c.name}</SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">품목분류 관리</h1>
          <p className="text-sm text-gray-500 mt-1">품목 분류 체계를 관리합니다</p>
        </div>
        {canWrite && <Button onClick={openCreate}><PlusIcon />새 분류</Button>}
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>코드</TableHead>
              <TableHead>분류명</TableHead>
              <TableHead>상위분류</TableHead>
              <TableHead className="w-20" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {categories.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-gray-400 py-10">
                  등록된 품목분류가 없습니다
                </TableCell>
              </TableRow>
            )}
            {categories.map((cat) => (
              <TableRow key={cat.id}>
                <TableCell className="font-mono text-sm">{cat.code}</TableCell>
                <TableCell className="font-medium">{cat.name}</TableCell>
                <TableCell className="text-sm text-gray-600">{cat.parentName ?? '—'}</TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    {canWrite && (
                      <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(cat)}>
                        <PencilIcon />
                      </Button>
                    )}
                    {canWrite && (
                      <Button
                        variant="ghost" size="icon-xs" title="삭제"
                        onClick={() => setDialog({ type: 'delete', cat })}
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
          <DialogHeader><DialogTitle>새 품목분류 등록</DialogTitle></DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>코드 *</Label>
              <Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="CAT-001" />
            </div>
            <div className="grid gap-1.5">
              <Label>분류명 *</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="전자제품" />
            </div>
            {parentField()}
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
              품목분류 수정{dialog.type === 'edit' && ` — ${dialog.cat.code}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>코드</Label>
              <Input value={code} disabled />
            </div>
            <div className="grid gap-1.5">
              <Label>분류명 *</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            {dialog.type === 'edit' && parentField(dialog.cat.id)}
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.cat)}
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
          <DialogHeader><DialogTitle>품목분류 삭제</DialogTitle></DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-gray-600 py-2">
              <strong>{dialog.cat.code} {dialog.cat.name}</strong>을(를) 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.cat)}
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
