'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, DownloadIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DatePicker } from '@/components/ui/date-picker'
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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { fetchContracts, createContract } from './actions'
import type { Contract, ContractType, Employee, Position, JobGrade } from '@/types/hr'

const CONTRACT_TYPE_LABEL: Record<ContractType, string> = {
  REGULAR: '정규직',
  CONTRACT: '계약직',
  PART_TIME: '시간제',
  INTERN: '인턴',
  DISPATCH: '파견직',
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

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 선택 직원의 계약 데이터 기준 필터.
  const [qType, setQType] = useState('')
  const [qPosition, setQPosition] = useState('')
  const [qFrom, setQFrom] = useState('')
  const [qTo, setQTo] = useState('')
  const [applied, setApplied] = useState({ type: '', position: '', from: '', to: '' })
  const onSearch = () => setApplied({ type: qType, position: qPosition, from: qFrom, to: qTo })
  const onReset = () => {
    setQType('')
    setQPosition('')
    setQFrom('')
    setQTo('')
    setApplied({ type: '', position: '', from: '', to: '' })
  }
  const filtered = (empId ? contracts : []).filter((c) => {
    if (applied.type && c.contractType !== applied.type) return false
    if (applied.position && String(c.positionId) !== applied.position) return false
    if (applied.from && c.startDate < applied.from) return false
    if (applied.to && c.startDate > applied.to) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `근로계약_${new Date().toISOString().slice(0, 10)}`,
      ['유형', '시작일', '종료일', '직위', '직급', '기본급', '비고'],
      filtered.map((c) => [
        CONTRACT_TYPE_LABEL[c.contractType],
        c.startDate,
        c.endDate ?? '',
        c.positionName,
        c.jobGradeName ?? '',
        c.baseSalary ?? '',
        c.note ?? '',
      ]),
    )

  return (
    <div className="p-5">
      <PageHeader
        title="근로 계약"
        description="직원별 근로 계약 이력을 조회·등록합니다"
        className="mb-4"
      >
        <Button variant="outline" onClick={exportExcel} disabled={!empId}>
          <DownloadIcon />
          엑셀
        </Button>
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

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="시작일">
            <Input
              type="date"
              value={qFrom}
              onChange={(e) => setQFrom(e.target.value)}
              className="h-8 w-36"
            />
            <span className="text-muted-foreground">~</span>
            <Input
              type="date"
              value={qTo}
              onChange={(e) => setQTo(e.target.value)}
              className="h-8 w-36"
            />
          </FilterField>
          <FilterField label="계약유형">
            <Select value={qType || 'ALL'} onValueChange={(v) => setQType(v === 'ALL' ? '' : (v ?? ''))}>
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {CONTRACT_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {CONTRACT_TYPE_LABEL[t]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="직위">
            <Select
              value={qPosition || 'ALL'}
              onValueChange={(v) => setQPosition(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {positions.map((p) => (
                  <SelectItem key={p.id} value={String(p.id)}>
                    {p.name}
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
          loading={!!empId && loading}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title={empId ? '등록된 계약이 없습니다' : '직원을 선택하면 계약 이력이 표시됩니다'}
            />
          }
        />
      </div>

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
            <FormGrid>
              <FormRow label="계약 유형" required span>
                <Select value={contractType} onValueChange={(v) => setContractType(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
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
              </FormRow>
              <FormRow label="시작일" required>
                <DatePicker value={startDate} onChange={setStartDate} />
              </FormRow>
              <FormRow label="종료일">
                <DatePicker value={endDate} onChange={setEndDate} />
              </FormRow>
              <FormRow label="직위" required>
                <Select value={positionId} onValueChange={(v) => setPositionId(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
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
              </FormRow>
              <FormRow label="직급">
                <Select value={jobGradeId} onValueChange={(v) => setJobGradeId(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
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
              </FormRow>
              <FormRow label="기본급" span>
                <Input
                  type="number"
                  value={baseSalary}
                  onChange={(e) => setBaseSalary(e.target.value)}
                  min={0}
                  placeholder="0"
                  className="h-8"
                />
              </FormRow>
            </FormGrid>
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
