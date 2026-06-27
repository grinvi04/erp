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
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import { createUom, updateUom, deleteUom } from './actions'
import type { Uom } from '@/types/inventory'

type DialogMode =
  { type: 'none' } | { type: 'create' } | { type: 'edit'; uom: Uom } | { type: 'delete'; uom: Uom }

interface Props {
  uoms: Uom[]
}

export default function UomsClient({ uoms }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.INVENTORY_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')

  const openCreate = () => {
    setCode('')
    setName('')
    setDialog({ type: 'create' })
  }

  const openEdit = (uom: Uom) => {
    setCode(uom.code)
    setName(uom.name)
    setDialog({ type: 'edit', uom })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 단위명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createUom({ code: code.trim(), name: name.trim() })
        toast.success('단위가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (uom: Uom) => {
    if (!name.trim()) {
      toast.error('단위명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateUom(uom.id, { code: uom.code, name: name.trim(), version: uom.version })
        toast.success('단위가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (uom: Uom) => {
    startTransition(async () => {
      try {
        await deleteUom(uom.id)
        toast.success('단위가 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Uom>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (u) => u.code,
      cell: (u) => <span className="font-mono text-sm">{u.code}</span>,
    },
    {
      key: 'name',
      header: '단위명',
      sortable: true,
      sortValue: (u) => u.name,
      cell: (u) => <span className="font-medium">{u.name}</span>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (uom) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(uom)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="삭제"
              onClick={() => setDialog({ type: 'delete', uom })}
            >
              <Trash2Icon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="단위 관리"
        description="측정 단위(UOM) 마스터를 관리합니다"
        className="mb-6"
      >
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 단위
          </Button>
        )}
      </PageHeader>

      <DataTable
        data={uoms}
        columns={columns}
        getRowId={(u) => u.id}
        empty={
          <EmptyState
            title="등록된 단위가 없습니다"
            description={canWrite ? '우측 상단에서 새 단위를 등록하세요.' : undefined}
          />
        }
      />

      {/* Create Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 단위 등록</DialogTitle>
          </DialogHeader>
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
            <DialogTitle>단위 수정{dialog.type === 'edit' && ` — ${dialog.uom.code}`}</DialogTitle>
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
      <Dialog
        open={dialog.type === 'delete'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>단위 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.uom.code} {dialog.uom.name}
              </strong>
              을(를) 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
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
