'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
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
import { fetchContracts, createContract } from './actions'
import type { Contract, ContractType, Employee, Position, JobGrade } from '@/types/hr'

const CONTRACT_TYPE_LABEL: Record<ContractType, string> = {
  REGULAR: '정규직',
  CONTRACT: '계약직',
  PART_TIME: '파트타임',
  INTERN: '인턴',
  DISPATCH: '파견',
}
const CONTRACT_TYPES = Object.keys(CONTRACT_TYPE_LABEL) as ContractType[]

interface Props {
  employees: Employee[]
  positions: Position[]
  jobGrades: JobGrade[]
}

const fmt = (v: number | null) => (v == null ? '—' : v.toLocaleString('ko-KR'))

export default function ContractsClient({ employees, positions, jobGrades }: Props) {
  const [empId, setEmpId] = useState<string>('')
  const [contracts, setContracts] = useState<Contract[]>([])
  const [loading, setLoading] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [isPending, startTransition] = useTransition()

  // Create form
  const [contractType, setContractType] = useState<string>('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [baseSalary, setBaseSalary] = useState('')
  const [positionId, setPositionId] = useState<string>('')
  const [jobGradeId, setJobGradeId] = useState<string>('')
  const [note, setNote] = useState('')

  const loadContracts = (id: number) => {
    setLoading(true)
    startTransition(async () => {
      try {
        const list = await fetchContracts(id)
        setContracts(list)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '계약 조회 중 오류가 발생했습니다')
        setContracts([])
      } finally {
        setLoading(false)
      }
    })
  }

  const onSelectEmployee = (v: string | null) => {
    setEmpId(v ?? '')
    if (v) loadContracts(Number(v))
    else setContracts([])
  }

  const openCreate = () => {
    setContractType('')
    setStartDate('')
    setEndDate('')
    setBaseSalary('')
    setPositionId('')
    setJobGradeId('')
    setNote('')
    setCreateOpen(true)
  }

  const handleCreate = () => {
    if (!empId) {
      toast.error('먼저 직원을 선택해주세요')
      return
    }
    if (!contractType || !startDate || !positionId) {
      toast.error('계약 유형·시작일·직위는 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createContract(Number(empId), {
          contractType: contractType as ContractType,
          startDate,
          endDate: endDate || null,
          baseSalary: baseSalary ? Number(baseSalary) : null,
          positionId: Number(positionId),
          jobGradeId: jobGradeId ? Number(jobGradeId) : null,
          note: note || null,
        })
        toast.success('근로 계약이 등록되었습니다')
        setCreateOpen(false)
        loadContracts(Number(empId))
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Contract>[] = [
    {
      key: 'contractType',
      header: '유형',
      cell: (c) => <Badge variant="secondary">{CONTRACT_TYPE_LABEL[c.contractType]}</Badge>,
    },
    {
      key: 'startDate',
      header: '시작일',
      sortable: true,
      sortValue: (c) => c.startDate,
      cell: (c) => <span className="text-sm">{c.startDate}</span>,
    },
    {
      key: 'endDate',
      header: '종료일',
      sortable: true,
      sortValue: (c) => c.endDate,
      cell: (c) => <span className="text-sm text-muted-foreground">{c.endDate ?? '—'}</span>,
    },
    {
      key: 'positionName',
      header: '직위',
      cell: (c) => <span className="text-sm">{c.positionName}</span>,
    },
    {
      key: 'jobGradeName',
      header: '직급',
      cell: (c) => <span className="text-sm">{c.jobGradeName ?? '—'}</span>,
    },
    {
      key: 'baseSalary',
      header: '기본급',
      align: 'right',
      sortable: true,
      sortValue: (c) => c.baseSalary,
      cell: (c) => <span className="text-sm text-muted-foreground">{fmt(c.baseSalary)}</span>,
    },
    {
      key: 'note',
      header: '비고',
      cell: (c) => <span className="text-sm text-muted-foreground">{c.note ?? '—'}</span>,
    },
  ]

  return (
    <div className="p-6">
      <PageHeader
        title="근로 계약"
        description="직원별 근로 계약 이력을 조회·등록합니다"
        className="mb-6"
      >
        <Button onClick={openCreate} disabled={!empId}>
          <PlusIcon />새 계약
        </Button>
      </PageHeader>

      <div className="mb-4 max-w-sm">
        <Label className="mb-1.5 block">직원 선택</Label>
        <Select value={empId} onValueChange={onSelectEmployee}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="직원을 선택하세요" />
          </SelectTrigger>
          <SelectContent>
            {employees.map((e) => (
              <SelectItem key={e.id} value={String(e.id)}>
                {e.fullName} ({e.employeeNo})
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DataTable
        data={empId ? contracts : []}
        columns={columns}
        getRowId={(c) => c.id}
        loading={!!empId && loading}
        empty={
          <EmptyState
            title={empId ? '등록된 계약이 없습니다' : '직원을 선택하면 계약 이력이 표시됩니다'}
          />
        }
      />

      {/* Create Dialog */}
      <Dialog
        open={createOpen}
        onOpenChange={(o) => {
          if (!o) setCreateOpen(false)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 근로 계약</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>계약 유형 *</Label>
              <Select value={contractType} onValueChange={(v) => setContractType(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                  {CONTRACT_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {CONTRACT_TYPE_LABEL[t]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>시작일 *</Label>
                <Input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
              <div className="grid gap-1.5">
                <Label>종료일</Label>
                <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
              </div>
            </div>
            <div className="grid gap-1.5">
              <Label>직위 *</Label>
              <Select value={positionId} onValueChange={(v) => setPositionId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="직위 선택" />
                </SelectTrigger>
                <SelectContent>
                  {positions.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>직급</Label>
              <Select value={jobGradeId} onValueChange={(v) => setJobGradeId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="직급 선택 (선택)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">선택 안 함</SelectItem>
                  {jobGrades.map((g) => (
                    <SelectItem key={g.id} value={String(g.id)}>
                      {g.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>기본급</Label>
              <Input
                type="number"
                value={baseSalary}
                onChange={(e) => setBaseSalary(e.target.value)}
                min={0}
                placeholder="0"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>비고</Label>
              <Textarea value={note} onChange={(e) => setNote(e.target.value)} rows={2} />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
