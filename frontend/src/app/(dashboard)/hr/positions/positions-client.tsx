'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
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
import { createPosition, updatePosition, deletePosition } from './actions'
import type { Position } from '@/types/hr'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; position: Position }
  | { type: 'delete'; position: Position }

interface Props {
  positions: Position[]
}

export default function PositionsClient({ positions }: Props) {
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [levelOrder, setLevelOrder] = useState('0')

  const openCreate = () => {
    setCode('')
    setName('')
    setLevelOrder('0')
    setDialog({ type: 'create' })
  }

  const openEdit = (position: Position) => {
    setName(position.name)
    setLevelOrder(String(position.levelOrder))
    setDialog({ type: 'edit', position })
  }

  const close = () => setDialog({ type: 'none' })

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 직위명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createPosition({
          code: code.trim(),
          name: name.trim(),
          levelOrder: Number(levelOrder),
        })
        toast.success('직위가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (position: Position) => {
    if (!name.trim()) {
      toast.error('직위명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updatePosition(position.id, {
          name: name.trim(),
          levelOrder: Number(levelOrder),
          version: position.version,
        })
        toast.success('직위 정보가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (position: Position) => {
    startTransition(async () => {
      try {
        await deletePosition(position.id)
        toast.success('직위가 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Position>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (p) => p.code,
      cell: (p) => <span className="font-mono text-sm">{p.code}</span>,
    },
    {
      key: 'name',
      header: '직위명',
      sortable: true,
      sortValue: (p) => p.name,
      cell: (p) => <span className="font-medium">{p.name}</span>,
    },
    {
      key: 'levelOrder',
      header: '레벨',
      align: 'right',
      sortable: true,
      sortValue: (p) => p.levelOrder,
      cell: (p) => <span className="text-sm text-muted-foreground">{p.levelOrder}</span>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (position) => (
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="icon-xs" onClick={() => openEdit(position)}>
            <PencilIcon />
            <span className="sr-only">수정</span>
          </Button>
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={() => setDialog({ type: 'delete', position })}
          >
            <Trash2Icon className="text-destructive" />
            <span className="sr-only">삭제</span>
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader title="직위 관리" description="직위(직책) 체계를 관리합니다" className="mb-6">
        <Button onClick={openCreate}>
          <PlusIcon />새 직위
        </Button>
      </PageHeader>

      <DataTable
        data={positions}
        columns={columns}
        getRowId={(p) => p.id}
        empty={<EmptyState title="등록된 직위가 없습니다" />}
      />

      {/* Create / Edit Dialog */}
      <Dialog
        open={dialog.type === 'create' || dialog.type === 'edit'}
        onOpenChange={(open) => {
          if (!open) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {dialog.type === 'create' ? '새 직위 등록' : '직위 정보 수정'}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            {dialog.type === 'create' && (
              <div className="grid gap-1.5">
                <Label htmlFor="pos-code">코드 *</Label>
                <Input
                  id="pos-code"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="예: MANAGER"
                  maxLength={30}
                />
              </div>
            )}
            <div className="grid gap-1.5">
              <Label htmlFor="pos-name">직위명 *</Label>
              <Input
                id="pos-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="예: 부장"
                maxLength={100}
              />
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="pos-level">레벨 순서</Label>
              <Input
                id="pos-level"
                type="number"
                value={levelOrder}
                onChange={(e) => setLevelOrder(e.target.value)}
                min={0}
              />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => {
                if (dialog.type === 'create') handleCreate()
                else if (dialog.type === 'edit') handleUpdate(dialog.position)
              }}
              disabled={isPending}
            >
              {dialog.type === 'create' ? '등록' : '저장'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog
        open={dialog.type === 'delete'}
        onOpenChange={(open) => {
          if (!open) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>직위 삭제</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground py-2">
            {dialog.type === 'delete' && (
              <>
                <strong>{dialog.position.name}</strong> 직위를 삭제하시겠습니까?
                <br />
                해당 직위를 사용 중인 직원·계약이 있으면 삭제할 수 없습니다.
              </>
            )}
          </p>
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.position)}
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
