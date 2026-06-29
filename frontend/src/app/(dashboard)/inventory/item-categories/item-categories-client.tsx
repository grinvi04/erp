'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, Trash2Icon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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
import { createItemCategory, updateItemCategory, deleteItemCategory } from './actions'
import type { ItemCategory } from '@/types/inventory'

const NONE = '__none__'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; cat: ItemCategory }
  | { type: 'delete'; cat: ItemCategory }

interface Props {
  categories: ItemCategory[]
}

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
    setCode('')
    setName('')
    setParentId('')
    setDialog({ type: 'create' })
  }

  const openEdit = (cat: ItemCategory) => {
    setCode(cat.code)
    setName(cat.name)
    setParentId(cat.parentId != null ? String(cat.parentId) : '')
    setDialog({ type: 'edit', cat })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 분류명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createItemCategory({
          code: code.trim(),
          name: name.trim(),
          parentId: parentId ? Number(parentId) : null,
        })
        toast.success('품목분류가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (cat: ItemCategory) => {
    if (!name.trim()) {
      toast.error('분류명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateItemCategory(cat.id, {
          code: cat.code,
          name: name.trim(),
          parentId: parentId ? Number(parentId) : null,
          version: cat.version,
        })
        toast.success('품목분류가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (cat: ItemCategory) => {
    startTransition(async () => {
      try {
        await deleteItemCategory(cat.id)
        toast.success('품목분류가 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  // 부모 선택지: 자기 자신 제외 (순환 방지)
  const parentOptions = (selfId?: number) => categories.filter((c) => c.id !== selfId)

  const columns: Column<ItemCategory>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (c) => c.code,
      cell: (c) => <span className="font-mono text-sm">{c.code}</span>,
    },
    {
      key: 'name',
      header: '분류명',
      sortable: true,
      sortValue: (c) => c.name,
      cell: (c) => <span className="font-medium">{c.name}</span>,
    },
    {
      key: 'parent',
      header: '상위분류',
      cell: (c) => <span className="text-sm text-muted-foreground">{c.parentName ?? '—'}</span>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (cat) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(cat)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="삭제"
              onClick={() => setDialog({ type: 'delete', cat })}
            >
              <Trash2Icon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  const parentSelect = (selfId?: number) => (
    <Select
      value={parentId || NONE}
      onValueChange={(v) => setParentId(v === NONE ? '' : (v ?? ''))}
    >
      <SelectTrigger className="h-8 w-full">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value={NONE}>없음 (최상위)</SelectItem>
        {parentOptions(selfId).map((c) => (
          <SelectItem key={c.id} value={String(c.id)}>
            {c.code} — {c.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 로드된 데이터 기준 클라이언트 필터.
  const NO_PARENT = '__top__'
  const parentFilterOptions = Array.from(
    new Map(
      categories
        .filter((c) => c.parentId != null)
        .map((c) => [c.parentId as number, c.parentName ?? String(c.parentId)] as const),
    ).entries(),
  ).map(([id, label]) => ({ id, label }))
  const [qKeyword, setQKeyword] = useState('')
  const [qParent, setQParent] = useState('')
  const [applied, setApplied] = useState({ keyword: '', parent: '' })
  const onSearch = () => setApplied({ keyword: qKeyword, parent: qParent })
  const onReset = () => {
    setQKeyword('')
    setQParent('')
    setApplied({ keyword: '', parent: '' })
  }
  const filtered = categories.filter((c) => {
    if (applied.parent === NO_PARENT) {
      if (c.parentId != null) return false
    } else if (applied.parent && String(c.parentId ?? '') !== applied.parent) {
      return false
    }
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      if (!c.code.toLowerCase().includes(kw) && !c.name.toLowerCase().includes(kw)) return false
    }
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `품목분류_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '분류명', '상위분류'],
      filtered.map((c) => [c.code, c.name, c.parentName ?? '']),
    )

  return (
    <div className="p-5">
      <PageHeader title="품목분류 관리" description="품목 분류 체계를 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 분류
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="검색어">
            <Input
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
              placeholder="코드·분류명"
              className="h-8 w-48"
            />
          </FilterField>
          <FilterField label="상위분류">
            <Select
              value={qParent || 'ALL'}
              onValueChange={(v) => setQParent(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-44">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value={NO_PARENT}>없음 (최상위)</SelectItem>
                {parentFilterOptions.map((p) => (
                  <SelectItem key={p.id} value={String(p.id)}>
                    {p.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(c) => c.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 품목분류가 없습니다"
              description={canWrite ? '우측 상단에서 새 분류를 등록하세요.' : undefined}
            />
          }
        />
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
            <DialogTitle>새 품목분류 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="코드" required>
                <Input
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="CAT-001"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="분류명" required>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="전자제품"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="상위분류" span>
                {parentSelect()}
              </FormRow>
            </FormGrid>
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
            <DialogTitle>
              품목분류 수정{dialog.type === 'edit' && ` — ${dialog.cat.code}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="코드">
                <Input value={code} disabled className="h-8" />
              </FormRow>
              <FormRow label="분류명" required>
                <Input value={name} onChange={(e) => setName(e.target.value)} className="h-8" />
              </FormRow>
              {dialog.type === 'edit' && (
                <FormRow label="상위분류" span>
                  {parentSelect(dialog.cat.id)}
                </FormRow>
              )}
            </FormGrid>
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
      <Dialog
        open={dialog.type === 'delete'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>품목분류 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.cat.code} {dialog.cat.name}
              </strong>
              을(를) 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
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
