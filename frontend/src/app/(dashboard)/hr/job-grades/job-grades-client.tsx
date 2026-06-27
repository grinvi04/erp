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
import { createJobGrade, updateJobGrade, deleteJobGrade } from './actions'
import type { JobGrade } from '@/types/hr'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; grade: JobGrade }
  | { type: 'delete'; grade: JobGrade }

interface Props {
  jobGrades: JobGrade[]
}

const fmt = (v: number | null) => (v == null ? '—' : v.toLocaleString('ko-KR'))

export default function JobGradesClient({ jobGrades }: Props) {
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [gradeOrder, setGradeOrder] = useState('0')
  const [minSalary, setMinSalary] = useState('')
  const [maxSalary, setMaxSalary] = useState('')

  const openCreate = () => {
    setCode('')
    setName('')
    setGradeOrder('0')
    setMinSalary('')
    setMaxSalary('')
    setDialog({ type: 'create' })
  }

  const openEdit = (grade: JobGrade) => {
    setName(grade.name)
    setGradeOrder(String(grade.gradeOrder))
    setMinSalary(grade.minSalary != null ? String(grade.minSalary) : '')
    setMaxSalary(grade.maxSalary != null ? String(grade.maxSalary) : '')
    setDialog({ type: 'edit', grade })
  }

  const close = () => setDialog({ type: 'none' })

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 직급명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createJobGrade({
          code: code.trim(),
          name: name.trim(),
          gradeOrder: Number(gradeOrder),
          minSalary: minSalary ? Number(minSalary) : null,
          maxSalary: maxSalary ? Number(maxSalary) : null,
        })
        toast.success('직급이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (grade: JobGrade) => {
    if (!name.trim()) {
      toast.error('직급명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateJobGrade(grade.id, {
          name: name.trim(),
          gradeOrder: Number(gradeOrder),
          minSalary: minSalary ? Number(minSalary) : null,
          maxSalary: maxSalary ? Number(maxSalary) : null,
          version: grade.version,
        })
        toast.success('직급 정보가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (grade: JobGrade) => {
    startTransition(async () => {
      try {
        await deleteJobGrade(grade.id)
        toast.success('직급이 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<JobGrade>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (g) => g.code,
      cell: (g) => <span className="font-mono text-sm">{g.code}</span>,
    },
    {
      key: 'name',
      header: '직급명',
      sortable: true,
      sortValue: (g) => g.name,
      cell: (g) => <span className="font-medium">{g.name}</span>,
    },
    {
      key: 'gradeOrder',
      header: '순서',
      align: 'right',
      sortable: true,
      sortValue: (g) => g.gradeOrder,
      cell: (g) => <span className="text-sm text-muted-foreground">{g.gradeOrder}</span>,
    },
    {
      key: 'minSalary',
      header: '최소 급여',
      align: 'right',
      sortable: true,
      sortValue: (g) => g.minSalary,
      cell: (g) => <span className="text-sm text-muted-foreground">{fmt(g.minSalary)}</span>,
    },
    {
      key: 'maxSalary',
      header: '최대 급여',
      align: 'right',
      sortable: true,
      sortValue: (g) => g.maxSalary,
      cell: (g) => <span className="text-sm text-muted-foreground">{fmt(g.maxSalary)}</span>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (grade) => (
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="icon-xs" onClick={() => openEdit(grade)}>
            <PencilIcon />
            <span className="sr-only">수정</span>
          </Button>
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={() => setDialog({ type: 'delete', grade })}
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
      <PageHeader
        title="직급 관리"
        description="직급 체계와 급여 범위를 관리합니다"
        className="mb-6"
      >
        <Button onClick={openCreate}>
          <PlusIcon />새 직급
        </Button>
      </PageHeader>

      <DataTable
        data={jobGrades}
        columns={columns}
        getRowId={(g) => g.id}
        empty={<EmptyState title="등록된 직급이 없습니다" />}
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
              {dialog.type === 'create' ? '새 직급 등록' : '직급 정보 수정'}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            {dialog.type === 'create' && (
              <div className="grid gap-1.5">
                <Label htmlFor="grade-code">코드 *</Label>
                <Input
                  id="grade-code"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="예: G3"
                  maxLength={30}
                />
              </div>
            )}
            <div className="grid gap-1.5">
              <Label htmlFor="grade-name">직급명 *</Label>
              <Input
                id="grade-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="예: 3급"
                maxLength={100}
              />
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="grade-order">순서</Label>
              <Input
                id="grade-order"
                type="number"
                value={gradeOrder}
                onChange={(e) => setGradeOrder(e.target.value)}
                min={0}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label htmlFor="grade-min">최소 급여</Label>
                <Input
                  id="grade-min"
                  type="number"
                  value={minSalary}
                  onChange={(e) => setMinSalary(e.target.value)}
                  min={0}
                  placeholder="0"
                />
              </div>
              <div className="grid gap-1.5">
                <Label htmlFor="grade-max">최대 급여</Label>
                <Input
                  id="grade-max"
                  type="number"
                  value={maxSalary}
                  onChange={(e) => setMaxSalary(e.target.value)}
                  min={0}
                  placeholder="0"
                />
              </div>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => {
                if (dialog.type === 'create') handleCreate()
                else if (dialog.type === 'edit') handleUpdate(dialog.grade)
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
            <DialogTitle>직급 삭제</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground py-2">
            {dialog.type === 'delete' && (
              <>
                <strong>{dialog.grade.name}</strong> 직급을 삭제하시겠습니까?
                <br />
                해당 직급을 사용 중인 직원·계약이 있으면 삭제할 수 없습니다.
              </>
            )}
          </p>
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.grade)}
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
