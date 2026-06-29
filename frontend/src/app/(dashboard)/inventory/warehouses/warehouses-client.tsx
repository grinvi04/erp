'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { DataTable, type Column } from '@/components/ui/data-table'
import { PageHeader } from '@/components/ui/page-header'
import { EmptyState } from '@/components/ui/empty-state'
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { createWarehouse, updateWarehouse, deactivateWarehouse } from './actions'
import type { Warehouse } from '@/types/inventory'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; wh: Warehouse }
  | { type: 'deactivate'; wh: Warehouse }

interface Props {
  warehouses: Warehouse[]
}

export default function WarehousesClient({ warehouses }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.INVENTORY_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')

  const openCreate = () => {
    setCode('')
    setName('')
    setAddress('')
    setDialog({ type: 'create' })
  }

  const openEdit = (wh: Warehouse) => {
    setName(wh.name)
    setAddress(wh.address ?? '')
    setDialog({ type: 'edit', wh })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim()) {
      toast.error('코드와 창고명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createWarehouse({
          code: code.trim(),
          name: name.trim(),
          address: address.trim() || null,
        })
        toast.success('창고가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (wh: Warehouse) => {
    if (!name.trim()) {
      toast.error('창고명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateWarehouse(wh.id, {
          version: wh.version,
          name: name.trim(),
          address: address.trim() || null,
        })
        toast.success('창고 정보가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDeactivate = (wh: Warehouse) => {
    startTransition(async () => {
      try {
        await deactivateWarehouse(wh.id)
        toast.success('창고가 비활성화되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Warehouse>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (w) => w.code,
      cell: (w) => <span className="font-mono text-sm">{w.code}</span>,
    },
    {
      key: 'name',
      header: '창고명',
      sortable: true,
      sortValue: (w) => w.name,
      cell: (w) => <span className="font-medium">{w.name}</span>,
    },
    {
      key: 'address',
      header: '주소',
      cell: (w) => (
        <span className="block max-w-xs truncate text-sm text-muted-foreground">
          {w.address ?? '—'}
        </span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (w) => (w.active ? 0 : 1),
      cell: (w) => (
        <Badge variant={w.active ? 'default' : 'secondary'}>{w.active ? '활성' : '비활성'}</Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (wh) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(wh)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && wh.active && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="비활성화"
              onClick={() => setDialog({ type: 'deactivate', wh })}
            >
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 페이지 데이터 기준 필터.
  const [qStatus, setQStatus] = useState('')
  const [qKeyword, setQKeyword] = useState('')
  const [applied, setApplied] = useState({ status: '', keyword: '' })
  const onSearch = () => setApplied({ status: qStatus, keyword: qKeyword })
  const onReset = () => {
    setQStatus('')
    setQKeyword('')
    setApplied({ status: '', keyword: '' })
  }
  const filtered = warehouses.filter((w) => {
    if (applied.status === 'active' && !w.active) return false
    if (applied.status === 'inactive' && w.active) return false
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      const hay = `${w.code} ${w.name} ${w.address ?? ''}`.toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `창고_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '창고명', '주소', '상태'],
      filtered.map((w) => [w.code, w.name, w.address ?? '', w.active ? '활성' : '비활성']),
    )

  return (
    <div className="p-5">
      <PageHeader title="창고 관리" description="물류 창고 정보를 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 창고
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="active">활성</SelectItem>
                <SelectItem value="inactive">비활성</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="검색어">
            <Input
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
              placeholder="코드·창고명·주소"
              className="h-8 w-48"
            />
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(w) => w.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 창고가 없습니다"
              description={canWrite ? '우측 상단에서 새 창고를 등록하세요.' : undefined}
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
            <DialogTitle>새 창고 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="코드" required>
                <Input
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="WH-001"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="창고명" required>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="서울 물류센터"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="주소" span>
                <Textarea
                  rows={2}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="서울특별시 ..."
                />
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
            <DialogTitle>창고 수정{dialog.type === 'edit' && ` — ${dialog.wh.code}`}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="창고명" required span>
                <Input value={name} onChange={(e) => setName(e.target.value)} className="h-8" />
              </FormRow>
              <FormRow label="주소" span>
                <Textarea rows={2} value={address} onChange={(e) => setAddress(e.target.value)} />
              </FormRow>
            </FormGrid>
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
      <Dialog
        open={dialog.type === 'deactivate'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>창고 비활성화</DialogTitle>
          </DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.wh.code} {dialog.wh.name}
              </strong>
              을(를) 비활성화하시겠습니까?
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
