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
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
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

const fmt = (v: number | null) =>
  v == null ? '—' : v.toLocaleString('ko-KR')

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
    setContractType(''); setStartDate(''); setEndDate(''); setBaseSalary('')
    setPositionId(''); setJobGradeId(''); setNote('')
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

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">근로 계약</h1>
          <p className="text-sm text-muted-foreground mt-1">직원별 근로 계약 이력을 조회·등록합니다</p>
        </div>
        <Button onClick={openCreate} disabled={!empId}>
          <PlusIcon />
          새 계약
        </Button>
      </div>

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

      <div className="bg-card rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>유형</TableHead>
              <TableHead>시작일</TableHead>
              <TableHead>종료일</TableHead>
              <TableHead>직위</TableHead>
              <TableHead>직급</TableHead>
              <TableHead className="text-right">기본급</TableHead>
              <TableHead>비고</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {!empId && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground py-10">
                  직원을 선택하면 계약 이력이 표시됩니다
                </TableCell>
              </TableRow>
            )}
            {empId && loading && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground py-10">
                  불러오는 중...
                </TableCell>
              </TableRow>
            )}
            {empId && !loading && contracts.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground py-10">
                  등록된 계약이 없습니다
                </TableCell>
              </TableRow>
            )}
            {empId && !loading && contracts.map((c) => (
              <TableRow key={c.id}>
                <TableCell>
                  <Badge variant="secondary">{CONTRACT_TYPE_LABEL[c.contractType]}</Badge>
                </TableCell>
                <TableCell className="text-sm">{c.startDate}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{c.endDate ?? '—'}</TableCell>
                <TableCell className="text-sm">{c.positionName}</TableCell>
                <TableCell className="text-sm">{c.jobGradeName ?? '—'}</TableCell>
                <TableCell className="text-right text-sm text-muted-foreground">
                  {fmt(c.baseSalary)}
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">{c.note ?? '—'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Create Dialog */}
      <Dialog open={createOpen} onOpenChange={(o) => { if (!o) setCreateOpen(false) }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 근로 계약</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>계약 유형 *</Label>
              <Select value={contractType} onValueChange={(v) => setContractType(v ?? '')}>
                <SelectTrigger className="w-full"><SelectValue placeholder="선택" /></SelectTrigger>
                <SelectContent>
                  {CONTRACT_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>{CONTRACT_TYPE_LABEL[t]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>시작일 *</Label>
                <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
              </div>
              <div className="grid gap-1.5">
                <Label>종료일</Label>
                <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
              </div>
            </div>
            <div className="grid gap-1.5">
              <Label>직위 *</Label>
              <Select value={positionId} onValueChange={(v) => setPositionId(v ?? '')}>
                <SelectTrigger className="w-full"><SelectValue placeholder="직위 선택" /></SelectTrigger>
                <SelectContent>
                  {positions.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>직급</Label>
              <Select value={jobGradeId} onValueChange={(v) => setJobGradeId(v ?? '')}>
                <SelectTrigger className="w-full"><SelectValue placeholder="직급 선택 (선택)" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="">선택 안 함</SelectItem>
                  {jobGrades.map((g) => (
                    <SelectItem key={g.id} value={String(g.id)}>{g.name}</SelectItem>
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
            <Button onClick={handleCreate} disabled={isPending}>등록</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
