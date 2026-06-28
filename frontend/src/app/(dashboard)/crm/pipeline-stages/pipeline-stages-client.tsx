'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
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
  createPipelineStage,
  updatePipelineStage,
  deletePipelineStage,
  type PipelineStagePayload,
} from './actions'
import type { PipelineStage } from '@/types/crm'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; stage: PipelineStage }
  | { type: 'delete'; stage: PipelineStage }

interface Props {
  stages: PipelineStage[]
}

export default function PipelineStagesClient({ stages }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.CRM_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [name, setName] = useState('')
  const [stageOrder, setStageOrder] = useState('')
  const [probability, setProbability] = useState('')
  const [isClosedWon, setIsClosedWon] = useState(false)
  const [isClosedLost, setIsClosedLost] = useState(false)

  const openCreate = () => {
    setName('')
    setStageOrder(String(stages.length + 1))
    setProbability('0')
    setIsClosedWon(false)
    setIsClosedLost(false)
    setDialog({ type: 'create' })
  }

  const openEdit = (stage: PipelineStage) => {
    setName(stage.name)
    setStageOrder(String(stage.stageOrder))
    setProbability(String(stage.probability))
    setIsClosedWon(stage.isClosedWon)
    setIsClosedLost(stage.isClosedLost)
    setDialog({ type: 'edit', stage })
  }

  const buildPayload = (): PipelineStagePayload => ({
    name: name.trim(),
    stageOrder: Number(stageOrder),
    probability: Number(probability) || 0,
    isClosedWon,
    isClosedLost,
  })

  const validate = (): boolean => {
    if (!name.trim()) {
      toast.error('단계명은 필수입니다')
      return false
    }
    const order = Number(stageOrder)
    if (!stageOrder || isNaN(order) || order < 1) {
      toast.error('단계 순서는 1 이상이어야 합니다')
      return false
    }
    const prob = Number(probability)
    if (isNaN(prob) || prob < 0 || prob > 100) {
      toast.error('확률은 0~100 사이여야 합니다')
      return false
    }
    if (isClosedWon && isClosedLost) {
      toast.error('성공·실패를 동시에 설정할 수 없습니다')
      return false
    }
    return true
  }

  const handleCreate = () => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await createPipelineStage(buildPayload())
        toast.success('단계가 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (stage: PipelineStage) => {
    if (!validate()) return
    startTransition(async () => {
      try {
        await updatePipelineStage(stage.id, { ...buildPayload(), version: stage.version })
        toast.success('단계가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (stage: PipelineStage) => {
    startTransition(async () => {
      try {
        await deletePipelineStage(stage.id)
        toast.success('단계가 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const stageForm = (
    <div className="grid gap-4 py-2">
      <div className="grid gap-1.5">
        <Label>단계명 *</Label>
        <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="예: 제안" />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-1.5">
          <Label>단계 순서 *</Label>
          <Input
            type="number"
            min={1}
            value={stageOrder}
            onChange={(e) => setStageOrder(e.target.value)}
          />
        </div>
        <div className="grid gap-1.5">
          <Label>기본 확률(%)</Label>
          <Input
            type="number"
            min={0}
            max={100}
            value={probability}
            onChange={(e) => setProbability(e.target.value)}
          />
        </div>
      </div>
      <div className="flex gap-6">
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={isClosedWon}
            onChange={(e) => {
              setIsClosedWon(e.target.checked)
              if (e.target.checked) setIsClosedLost(false)
            }}
            className="h-4 w-4 rounded border-input"
          />
          <span className="text-sm">성공 종결</span>
        </label>
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={isClosedLost}
            onChange={(e) => {
              setIsClosedLost(e.target.checked)
              if (e.target.checked) setIsClosedWon(false)
            }}
            className="h-4 w-4 rounded border-input"
          />
          <span className="text-sm">실패 종결</span>
        </label>
      </div>
    </div>
  )

  const columns: Column<PipelineStage>[] = [
    {
      key: 'stageOrder',
      header: '순서',
      headerClassName: 'w-20',
      sortable: true,
      sortValue: (stage) => stage.stageOrder,
      cell: (stage) => <span className="font-mono text-sm">{stage.stageOrder}</span>,
    },
    {
      key: 'name',
      header: '단계명',
      sortable: true,
      sortValue: (stage) => stage.name,
      cell: (stage) => <span className="font-medium">{stage.name}</span>,
    },
    {
      key: 'probability',
      header: '기본 확률',
      align: 'right',
      sortable: true,
      sortValue: (stage) => stage.probability,
      cell: (stage) => <span className="text-sm">{stage.probability}%</span>,
    },
    {
      key: 'closed',
      header: '종결',
      cell: (stage) => (
        <>
          {stage.isClosedWon && <Badge>성공</Badge>}
          {stage.isClosedLost && <Badge variant="destructive">실패</Badge>}
          {!stage.isClosedWon && !stage.isClosedLost && (
            <span className="text-sm text-muted-foreground">진행</span>
          )}
        </>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-20',
      cell: (stage) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <>
              <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(stage)}>
                <PencilIcon />
              </Button>
              <Button
                variant="ghost"
                size="icon-xs"
                title="삭제"
                onClick={() => setDialog({ type: 'delete', stage })}
              >
                <Trash2Icon className="text-destructive" />
              </Button>
            </>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="파이프라인 단계"
        description="영업 기회의 진행 단계를 정의합니다"
        className="mb-6"
      >
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 단계
          </Button>
        )}
      </PageHeader>

      <DataTable
        data={stages}
        columns={columns}
        getRowId={(stage) => stage.id}
        initialSort={{ key: 'stageOrder', dir: 'asc' }}
        empty={
          <EmptyState
            title="등록된 단계가 없습니다"
            description={canWrite ? '우측 상단에서 새 단계를 등록하세요.' : undefined}
          />
        }
      />

      {/* Create */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 단계 등록</DialogTitle>
          </DialogHeader>
          {stageForm}
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit */}
      <Dialog
        open={dialog.type === 'edit'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>단계 수정</DialogTitle>
          </DialogHeader>
          {stageForm}
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.stage)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete */}
      <Dialog
        open={dialog.type === 'delete'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>단계 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>{dialog.stage.name}</strong> 단계를 삭제하시겠습니까? 해당 단계를 사용하는
              영업 기회가 있으면 삭제할 수 없습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.stage)}
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
