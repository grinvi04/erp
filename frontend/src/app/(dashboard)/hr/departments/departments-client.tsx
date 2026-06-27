'use client'
import { useState, useTransition, useMemo } from 'react'
import { toast } from 'sonner'
import { PlusIcon, PencilIcon, Trash2Icon } from 'lucide-react'
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
import { createDepartment, updateDepartment, deleteDepartment } from './actions'
import type { Department } from '@/types/hr'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; dept: Department }
  | { type: 'delete'; dept: Department }

interface Props {
  departments: Department[]
}

export default function DepartmentsClient({ departments }: Props) {
  const deptById = useMemo(() => new Map(departments.map((d) => [d.id, d])), [departments])
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState<string>('')
  const [sortOrder, setSortOrder] = useState('0')

  const openCreate = () => {
    setCode('')
    setName('')
    setParentId('')
    setSortOrder('0')
    setDialog({ type: 'create' })
  }

  const openEdit = (dept: Department) => {
    setName(dept.name)
    setSortOrder(String(dept.sortOrder))
    setParentId('')
    setDialog({ type: 'edit', dept })
  }

  const close = () => setDialog({ type: 'none' })

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 부서명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createDepartment({
          code: code.trim(),
          name: name.trim(),
          parentId: parentId ? Number(parentId) : null,
          sortOrder: Number(sortOrder),
        })
        toast.success('부서가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (dept: Department) => {
    if (!name.trim()) {
      toast.error('부서명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateDepartment(dept.id, {
          name: name.trim(),
          sortOrder: Number(sortOrder),
          version: dept.version,
        })
        toast.success('부서 정보가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (dept: Department) => {
    startTransition(async () => {
      try {
        await deleteDepartment(dept.id)
        toast.success('부서가 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  // 부서명은 계층(depth) 들여쓰기로 트리 순서를 표현하므로 정렬 대상에서 제외.
  const columns: Column<Department>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (dept) => dept.code,
      cell: (dept) => <span className="font-mono text-sm">{dept.code}</span>,
    },
    {
      key: 'name',
      header: '부서명',
      cell: (dept) => (
        <span className="font-medium">
          {dept.depth > 0 && (
            <span className="text-muted-foreground mr-1">{'└'.padStart(dept.depth * 2)}</span>
          )}
          {dept.name}
        </span>
      ),
    },
    {
      key: 'parent',
      header: '상위 부서',
      cell: (dept) => {
        const parent = dept.parentId != null ? deptById.get(dept.parentId) : undefined
        return <span className="text-sm text-muted-foreground">{parent?.name ?? '—'}</span>
      },
    },
    {
      key: 'sortOrder',
      header: '정렬순서',
      align: 'right',
      sortable: true,
      sortValue: (dept) => dept.sortOrder,
      cell: (dept) => <span className="text-sm text-muted-foreground">{dept.sortOrder}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (dept) => (dept.active ? 0 : 1),
      cell: (dept) => (
        <Badge variant={dept.active ? 'default' : 'secondary'}>
          {dept.active ? '활성' : '비활성'}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (dept) => (
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="icon-xs" onClick={() => openEdit(dept)}>
            <PencilIcon />
            <span className="sr-only">수정</span>
          </Button>
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={() => setDialog({ type: 'delete', dept })}
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
      <PageHeader title="부서 관리" description="조직 부서 구조를 관리합니다" className="mb-6">
        <Button onClick={openCreate}>
          <PlusIcon />새 부서
        </Button>
      </PageHeader>

      <DataTable
        data={departments}
        columns={columns}
        getRowId={(dept) => dept.id}
        empty={<EmptyState title="등록된 부서가 없습니다" />}
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
              {dialog.type === 'create' ? '새 부서 등록' : '부서 정보 수정'}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            {dialog.type === 'create' && (
              <div className="grid gap-1.5">
                <Label htmlFor="dept-code">코드 *</Label>
                <Input
                  id="dept-code"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="예: DEV_FRONTEND"
                  maxLength={30}
                />
              </div>
            )}
            <div className="grid gap-1.5">
              <Label htmlFor="dept-name">부서명 *</Label>
              <Input
                id="dept-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="예: 프론트엔드 개발팀"
                maxLength={100}
              />
            </div>
            {dialog.type === 'create' && (
              <div className="grid gap-1.5">
                <Label>상위 부서</Label>
                <Select value={parentId} onValueChange={(v) => setParentId(v ?? '')}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="상위 부서 없음" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">상위 부서 없음</SelectItem>
                    {departments.map((d) => (
                      <SelectItem key={d.id} value={String(d.id)}>
                        {d.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
            <div className="grid gap-1.5">
              <Label htmlFor="dept-sort">정렬 순서</Label>
              <Input
                id="dept-sort"
                type="number"
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
                min={0}
              />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => {
                if (dialog.type === 'create') handleCreate()
                else if (dialog.type === 'edit') handleUpdate(dialog.dept)
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
            <DialogTitle>부서 삭제</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground py-2">
            {dialog.type === 'delete' && (
              <>
                <strong>{dialog.dept.name}</strong> 부서를 삭제하시겠습니까?
                <br />
                하위 부서 또는 소속 직원이 있으면 삭제할 수 없습니다.
              </>
            )}
          </p>
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.dept)}
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
