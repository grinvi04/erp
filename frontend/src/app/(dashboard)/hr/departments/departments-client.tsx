'use client'
import { useState, useTransition, useMemo } from 'react'
import { toast } from 'sonner'
import { PlusIcon, PencilIcon, Trash2Icon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
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
  // 부모 재지정 시 순환을 막기 위해 자기 자신과 모든 하위 부서를 후보에서 제외한다(백엔드도 거부).
  const childrenByParent = useMemo(() => {
    const map = new Map<number, Department[]>()
    for (const d of departments) {
      if (d.parentId != null) {
        const list = map.get(d.parentId) ?? []
        list.push(d)
        map.set(d.parentId, list)
      }
    }
    return map
  }, [departments])
  const collectSelfAndDescendants = (rootId: number): Set<number> => {
    const result = new Set<number>()
    const stack = [rootId]
    while (stack.length > 0) {
      const id = stack.pop() as number
      if (result.has(id)) continue
      result.add(id)
      for (const child of childrenByParent.get(id) ?? []) stack.push(child.id)
    }
    return result
  }

  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState<string>('')
  const [sortOrder, setSortOrder] = useState('0')
  const [active, setActive] = useState(true)

  const openCreate = () => {
    setCode('')
    setName('')
    setParentId('')
    setSortOrder('0')
    setActive(true)
    setDialog({ type: 'create' })
  }

  const openEdit = (dept: Department) => {
    setName(dept.name)
    setSortOrder(String(dept.sortOrder))
    setParentId(dept.parentId != null ? String(dept.parentId) : '')
    setActive(dept.active)
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
          parentId: parentId ? Number(parentId) : null,
          active,
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

  // 수정 다이얼로그의 상위 부서 후보 — 순환 방지를 위해 자기 자신·하위 부서 제외.
  const parentOptions =
    dialog.type === 'edit'
      ? departments.filter((d) => !collectSelfAndDescendants(dialog.dept.id).has(d.id))
      : departments

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 데이터 기준 필터.
  const [qKeyword, setQKeyword] = useState('')
  const [qParent, setQParent] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [applied, setApplied] = useState({ keyword: '', parent: '', status: '' })
  const onSearch = () => setApplied({ keyword: qKeyword, parent: qParent, status: qStatus })
  const onReset = () => {
    setQKeyword('')
    setQParent('')
    setQStatus('')
    setApplied({ keyword: '', parent: '', status: '' })
  }
  const filtered = departments.filter((dept) => {
    if (applied.parent && String(dept.parentId ?? '') !== applied.parent) return false
    if (applied.status && (dept.active ? 'ACTIVE' : 'INACTIVE') !== applied.status) return false
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      if (!dept.code.toLowerCase().includes(kw) && !dept.name.toLowerCase().includes(kw))
        return false
    }
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `부서_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '부서명', '상위 부서', '정렬순서', '상태'],
      filtered.map((dept) => [
        dept.code,
        dept.name,
        (dept.parentId != null ? deptById.get(dept.parentId)?.name : '') ?? '',
        dept.sortOrder,
        dept.active ? '활성' : '비활성',
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader title="부서 관리" description="조직 부서 구조를 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        <Button onClick={openCreate}>
          <PlusIcon />새 부서
        </Button>
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="부서명/코드">
            <Input
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
              placeholder="검색어"
              className="h-8 w-44"
            />
          </FilterField>
          <FilterField label="상위 부서">
            <Select
              value={qParent || 'ALL'}
              onValueChange={(v) => setQParent(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {departments.map((d) => (
                  <SelectItem key={d.id} value={String(d.id)}>
                    {d.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-28">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="ACTIVE">활성</SelectItem>
                <SelectItem value="INACTIVE">비활성</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(dept) => dept.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={<EmptyState title="등록된 부서가 없습니다" />}
        />
      </div>

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
            <FormGrid>
              {dialog.type === 'create' && (
                <FormRow label="코드" required>
                  <Input
                    id="dept-code"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder="예: DEV_FRONTEND"
                    maxLength={30}
                    className="h-8"
                  />
                </FormRow>
              )}
              <FormRow label="부서명" required>
                <Input
                  id="dept-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="예: 프론트엔드 개발팀"
                  maxLength={100}
                  className="h-8"
                />
              </FormRow>
              <FormRow label="상위 부서">
                <Select value={parentId} onValueChange={(v) => setParentId(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue placeholder="상위 부서 없음" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">상위 부서 없음</SelectItem>
                    {parentOptions.map((d) => (
                      <SelectItem key={d.id} value={String(d.id)}>
                        {d.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="정렬 순서">
                <Input
                  id="dept-sort"
                  type="number"
                  value={sortOrder}
                  onChange={(e) => setSortOrder(e.target.value)}
                  min={0}
                  className="h-8"
                />
              </FormRow>
              {dialog.type === 'edit' && (
                <FormRow label="활성" span>
                  <div className="flex items-center gap-2">
                    <Checkbox
                      id="dept-active"
                      checked={active}
                      onCheckedChange={(checked) => setActive(checked === true)}
                    />
                    <Label htmlFor="dept-active" className="text-xs font-normal">
                      해제 시 비활성화
                    </Label>
                  </div>
                </FormRow>
              )}
            </FormGrid>
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
