'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import {
  PlusIcon,
  PencilIcon,
  ArrowRightLeftIcon,
  TrendingUpIcon,
  LogOutIcon,
  LogInIcon,
  BanIcon,
} from 'lucide-react'
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
import { PaginationBar } from '@/components/ui/pagination-bar'
import { SearchInput } from '@/components/ui/search-input'
import {
  createEmployee,
  updateEmployee,
  transferEmployee,
  promoteEmployee,
  terminateEmployee,
  setEmployeeOnLeave,
  returnEmployeeFromLeave,
} from './actions'
import type {
  Employee,
  EmployeeStatus,
  EmploymentType,
  Gender,
  Department,
  Position,
  JobGrade,
} from '@/types/hr'
import type { PageResponse } from '@/types/api'

const STATUS_LABEL: Record<EmployeeStatus, string> = {
  ACTIVE: '재직',
  ON_LEAVE: '휴직',
  TERMINATED: '퇴직',
}
const STATUS_VARIANT: Record<EmployeeStatus, 'default' | 'secondary' | 'destructive'> = {
  ACTIVE: 'default',
  ON_LEAVE: 'secondary',
  TERMINATED: 'destructive',
}
const EMPLOYMENT_TYPE_LABEL: Record<EmploymentType, string> = {
  REGULAR: '정규직',
  CONTRACT: '계약직',
  PART_TIME: '파트타임',
  INTERN: '인턴',
  DISPATCH: '파견직',
}
const GENDER_LABEL: Record<Gender, string> = {
  MALE: '남성',
  FEMALE: '여성',
  OTHER: '기타',
}

type DialogState =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; emp: Employee }
  | { type: 'transfer'; emp: Employee }
  | { type: 'promote'; emp: Employee }
  | { type: 'terminate'; emp: Employee }

interface Props {
  data: PageResponse<Employee>
  departments: Department[]
  positions: Position[]
  jobGrades: JobGrade[]
  keyword: string
}

export default function EmployeesClient({
  data,
  departments,
  positions,
  jobGrades,
  keyword,
}: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.HR_EMPLOYEE_WRITE)
  const [dialog, setDialog] = useState<DialogState>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  // Create form state
  const [empNo, setEmpNo] = useState('')
  const [lastName, setLastName] = useState('')
  const [firstName, setFirstName] = useState('')
  const [dob, setDob] = useState('')
  const [gender, setGender] = useState<string>('')
  const [nationalId, setNationalId] = useState('')
  const [phone, setPhone] = useState('')
  const [personalEmail, setPersonalEmail] = useState('')
  const [deptId, setDeptId] = useState<string>('')
  const [posId, setPosId] = useState<string>('')
  const [gradeId, setGradeId] = useState<string>('')
  const [hireDate, setHireDate] = useState('')
  const [empType, setEmpType] = useState<string>('')
  const [workEmail, setWorkEmail] = useState('')
  const [baseSalary, setBaseSalary] = useState('')
  const [managerId, setManagerId] = useState('')
  const [userId, setUserId] = useState('')

  // Transfer form state
  const [transferDeptId, setTransferDeptId] = useState<string>('')
  const [transferPosId, setTransferPosId] = useState<string>('')

  // Promote form state
  const [promotePosId, setPromotePosId] = useState<string>('')
  const [promoteGradeId, setPromoteGradeId] = useState<string>('')
  const [promoteSalary, setPromoteSalary] = useState('')

  // Terminate form state
  const [terminationDate, setTerminationDate] = useState('')

  const openCreate = () => {
    setEmpNo('')
    setLastName('')
    setFirstName('')
    setDob('')
    setGender('')
    setNationalId('')
    setPhone('')
    setPersonalEmail('')
    setDeptId('')
    setPosId('')
    setGradeId('')
    setHireDate('')
    setEmpType('')
    setWorkEmail('')
    setBaseSalary('')
    setManagerId('')
    setUserId('')
    setDialog({ type: 'create' })
  }

  const openEdit = (emp: Employee) => {
    setLastName(emp.lastName)
    setFirstName(emp.firstName)
    setPhone(emp.phone ?? '')
    setPersonalEmail(emp.personalEmail ?? '')
    setWorkEmail(emp.workEmail)
    setBaseSalary(emp.baseSalary != null ? String(emp.baseSalary) : '')
    setManagerId(emp.managerId != null ? String(emp.managerId) : '')
    setUserId(emp.userId ?? '')
    setDialog({ type: 'edit', emp })
  }

  const openTransfer = (emp: Employee) => {
    setTransferDeptId(String(emp.departmentId))
    setTransferPosId(String(emp.positionId))
    setDialog({ type: 'transfer', emp })
  }

  const openPromote = (emp: Employee) => {
    setPromotePosId(String(emp.positionId))
    setPromoteGradeId(emp.jobGradeId != null ? String(emp.jobGradeId) : '')
    setPromoteSalary(emp.baseSalary != null ? String(emp.baseSalary) : '')
    setDialog({ type: 'promote', emp })
  }

  const openTerminate = (emp: Employee) => {
    setTerminationDate(new Date().toISOString().slice(0, 10))
    setDialog({ type: 'terminate', emp })
  }

  const handleCreate = () => {
    if (
      !empNo ||
      !lastName ||
      !firstName ||
      !deptId ||
      !posId ||
      !hireDate ||
      !empType ||
      !workEmail
    ) {
      toast.error('필수 항목을 모두 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        await createEmployee({
          employeeNo: empNo,
          lastName,
          firstName,
          dateOfBirth: dob || null,
          gender: (gender as Gender) || null,
          nationalId: nationalId || null,
          phone: phone || null,
          personalEmail: personalEmail || null,
          departmentId: Number(deptId),
          positionId: Number(posId),
          jobGradeId: gradeId ? Number(gradeId) : null,
          hireDate,
          employmentType: empType as EmploymentType,
          workEmail,
          baseSalary: baseSalary ? Number(baseSalary) : null,
          managerId: managerId ? Number(managerId) : null,
        })
        toast.success('직원이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (emp: Employee) => {
    if (!lastName || !firstName || !workEmail) {
      toast.error('성, 이름, 업무 이메일은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateEmployee(emp.id, {
          lastName,
          firstName,
          phone: phone || null,
          personalEmail: personalEmail || null,
          workEmail,
          baseSalary: baseSalary ? Number(baseSalary) : null,
          managerId: managerId ? Number(managerId) : null,
          userId: userId.trim() || null,
          version: emp.version,
        })
        toast.success('직원 정보가 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleTransfer = (emp: Employee) => {
    if (!transferDeptId || !transferPosId) {
      toast.error('부서와 직위를 선택해주세요')
      return
    }
    startTransition(async () => {
      try {
        await transferEmployee(emp.id, {
          departmentId: Number(transferDeptId),
          positionId: Number(transferPosId),
        })
        toast.success('발령 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '발령 중 오류가 발생했습니다')
      }
    })
  }

  const handlePromote = (emp: Employee) => {
    if (!promotePosId) {
      toast.error('직위를 선택해주세요')
      return
    }
    startTransition(async () => {
      try {
        await promoteEmployee(emp.id, {
          positionId: Number(promotePosId),
          jobGradeId: promoteGradeId ? Number(promoteGradeId) : null,
          baseSalary: promoteSalary ? Number(promoteSalary) : null,
        })
        toast.success('승진 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '승진 중 오류가 발생했습니다')
      }
    })
  }

  const handleTerminate = (emp: Employee) => {
    if (!terminationDate) {
      toast.error('퇴직일을 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        await terminateEmployee(emp.id, { terminationDate })
        toast.success('퇴직 처리되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '퇴직 처리 중 오류가 발생했습니다')
      }
    })
  }

  const handleOnLeave = (emp: Employee) => {
    startTransition(async () => {
      try {
        await setEmployeeOnLeave(emp.id)
        toast.success('휴직 처리되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '처리 중 오류가 발생했습니다')
      }
    })
  }

  const handleReturnFromLeave = (emp: Employee) => {
    startTransition(async () => {
      try {
        await returnEmployeeFromLeave(emp.id)
        toast.success('복직 처리되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '처리 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Employee>[] = [
    {
      key: 'employeeNo',
      header: '사번',
      sortable: true,
      sortValue: (emp) => emp.employeeNo,
      cell: (emp) => <span className="font-mono text-sm">{emp.employeeNo}</span>,
    },
    {
      key: 'fullName',
      header: '이름',
      sortable: true,
      sortValue: (emp) => emp.fullName,
      cell: (emp) => <span className="font-medium">{emp.fullName}</span>,
    },
    {
      key: 'departmentName',
      header: '부서',
      sortable: true,
      sortValue: (emp) => emp.departmentName,
      cell: (emp) => <span className="text-sm">{emp.departmentName}</span>,
    },
    {
      key: 'positionName',
      header: '직위',
      sortable: true,
      sortValue: (emp) => emp.positionName,
      cell: (emp) => <span className="text-sm">{emp.positionName}</span>,
    },
    {
      key: 'workEmail',
      header: '이메일',
      cell: (emp) => <span className="text-sm text-muted-foreground">{emp.workEmail}</span>,
    },
    {
      key: 'hireDate',
      header: '입사일',
      sortable: true,
      sortValue: (emp) => emp.hireDate,
      cell: (emp) => <span className="text-sm">{emp.hireDate}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (emp) => STATUS_LABEL[emp.status],
      cell: (emp) => <Badge variant={STATUS_VARIANT[emp.status]}>{STATUS_LABEL[emp.status]}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-40',
      cell: (emp) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(emp)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && emp.status !== 'TERMINATED' && (
            <>
              <Button variant="ghost" size="icon-xs" title="발령" onClick={() => openTransfer(emp)}>
                <ArrowRightLeftIcon />
              </Button>
              <Button variant="ghost" size="icon-xs" title="승진" onClick={() => openPromote(emp)}>
                <TrendingUpIcon />
              </Button>
              {emp.status === 'ACTIVE' && (
                <Button
                  variant="ghost"
                  size="icon-xs"
                  title="휴직"
                  onClick={() => handleOnLeave(emp)}
                  disabled={isPending}
                >
                  <LogOutIcon />
                </Button>
              )}
              {emp.status === 'ON_LEAVE' && (
                <Button
                  variant="ghost"
                  size="icon-xs"
                  title="복직"
                  onClick={() => handleReturnFromLeave(emp)}
                  disabled={isPending}
                >
                  <LogInIcon />
                </Button>
              )}
              <Button
                variant="ghost"
                size="icon-xs"
                title="퇴직"
                onClick={() => openTerminate(emp)}
              >
                <BanIcon className="text-destructive" />
              </Button>
            </>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="p-6">
      <PageHeader title="직원 관리" description="조직 내 직원 정보를 관리합니다" className="mb-6">
        <SearchInput placeholder="이름·코드 검색" className="w-64" />
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 직원
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <DataTable
          data={data.content}
          columns={columns}
          getRowId={(emp) => emp.id}
          empty={<EmptyState title="등록된 직원이 없습니다" />}
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/hr/employees"
          searchParams={keyword ? { keyword } : undefined}
        />
      </div>

      {/* Create Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>새 직원 등록</DialogTitle>
          </DialogHeader>
          <div className="grid grid-cols-2 gap-4 py-2 max-h-[60vh] overflow-y-auto pr-1">
            <div className="grid gap-1.5">
              <Label>사번 *</Label>
              <Input
                value={empNo}
                onChange={(e) => setEmpNo(e.target.value)}
                placeholder="EMP001"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>고용 형태 *</Label>
              <Select value={empType} onValueChange={(v) => setEmpType(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                  {(Object.entries(EMPLOYMENT_TYPE_LABEL) as [EmploymentType, string][]).map(
                    ([k, v]) => (
                      <SelectItem key={k} value={k}>
                        {v}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>성 *</Label>
              <Input
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="김"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>이름 *</Label>
              <Input
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="철수"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>업무 이메일 *</Label>
              <Input
                type="email"
                value={workEmail}
                onChange={(e) => setWorkEmail(e.target.value)}
                placeholder="cs.kim@company.com"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>입사일 *</Label>
              <Input type="date" value={hireDate} onChange={(e) => setHireDate(e.target.value)} />
            </div>
            <div className="grid gap-1.5">
              <Label>부서 *</Label>
              <Select value={deptId} onValueChange={(v) => setDeptId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                  {departments.map((d) => (
                    <SelectItem key={d.id} value={String(d.id)}>
                      {d.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>직위 *</Label>
              <Select value={posId} onValueChange={(v) => setPosId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
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
              <Select value={gradeId} onValueChange={(v) => setGradeId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="없음" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">없음</SelectItem>
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
                placeholder="0"
                min={0}
              />
            </div>
            <div className="grid gap-1.5">
              <Label>성별</Label>
              <Select value={gender} onValueChange={(v) => setGender(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택 안 함" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">선택 안 함</SelectItem>
                  {(Object.entries(GENDER_LABEL) as [Gender, string][]).map(([k, v]) => (
                    <SelectItem key={k} value={k}>
                      {v}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>생년월일</Label>
              <Input type="date" value={dob} onChange={(e) => setDob(e.target.value)} />
            </div>
            <div className="grid gap-1.5">
              <Label>연락처</Label>
              <Input
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="010-0000-0000"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>개인 이메일</Label>
              <Input
                type="email"
                value={personalEmail}
                onChange={(e) => setPersonalEmail(e.target.value)}
              />
            </div>
            <div className="grid gap-1.5">
              <Label>주민등록번호</Label>
              <Input value={nationalId} onChange={(e) => setNationalId(e.target.value)} />
            </div>
            <div className="grid gap-1.5">
              <Label>관리자 ID</Label>
              <Input
                type="number"
                value={managerId}
                onChange={(e) => setManagerId(e.target.value)}
                placeholder="직원 ID"
              />
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
            <DialogTitle>직원 정보 수정</DialogTitle>
          </DialogHeader>
          <div className="grid grid-cols-2 gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>성 *</Label>
              <Input value={lastName} onChange={(e) => setLastName(e.target.value)} />
            </div>
            <div className="grid gap-1.5">
              <Label>이름 *</Label>
              <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} />
            </div>
            <div className="grid gap-1.5 col-span-2">
              <Label>업무 이메일 *</Label>
              <Input
                type="email"
                value={workEmail}
                onChange={(e) => setWorkEmail(e.target.value)}
              />
            </div>
            <div className="grid gap-1.5">
              <Label>연락처</Label>
              <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
            </div>
            <div className="grid gap-1.5">
              <Label>개인 이메일</Label>
              <Input
                type="email"
                value={personalEmail}
                onChange={(e) => setPersonalEmail(e.target.value)}
              />
            </div>
            <div className="grid gap-1.5">
              <Label>기본급</Label>
              <Input
                type="number"
                value={baseSalary}
                onChange={(e) => setBaseSalary(e.target.value)}
                min={0}
              />
            </div>
            <div className="grid gap-1.5">
              <Label>관리자 ID</Label>
              <Input
                type="number"
                value={managerId}
                onChange={(e) => setManagerId(e.target.value)}
                placeholder="직원 ID"
              />
            </div>
            <div className="grid gap-1.5">
              <Label>로그인 계정 ID (Keycloak)</Label>
              <Input
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                placeholder="결재자 식별용 sub"
              />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.emp)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Transfer Dialog */}
      <Dialog
        open={dialog.type === 'transfer'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              발령{dialog.type === 'transfer' && ` — ${dialog.emp.fullName}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>부서 *</Label>
              <Select value={transferDeptId} onValueChange={(v) => setTransferDeptId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                  {departments.map((d) => (
                    <SelectItem key={d.id} value={String(d.id)}>
                      {d.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>직위 *</Label>
              <Select value={transferPosId} onValueChange={(v) => setTransferPosId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
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
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'transfer' && handleTransfer(dialog.emp)}
              disabled={isPending}
            >
              발령
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Promote Dialog */}
      <Dialog
        open={dialog.type === 'promote'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              승진{dialog.type === 'promote' && ` — ${dialog.emp.fullName}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>직위 *</Label>
              <Select value={promotePosId} onValueChange={(v) => setPromotePosId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="선택" />
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
              <Select value={promoteGradeId} onValueChange={(v) => setPromoteGradeId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="없음" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">없음</SelectItem>
                  {jobGrades.map((g) => (
                    <SelectItem key={g.id} value={String(g.id)}>
                      {g.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1.5">
              <Label>변경 기본급</Label>
              <Input
                type="number"
                value={promoteSalary}
                onChange={(e) => setPromoteSalary(e.target.value)}
                min={0}
              />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'promote' && handlePromote(dialog.emp)}
              disabled={isPending}
            >
              승진
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Terminate Dialog */}
      <Dialog
        open={dialog.type === 'terminate'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              퇴직 처리{dialog.type === 'terminate' && ` — ${dialog.emp.fullName}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <Label>퇴직일 *</Label>
              <Input
                type="date"
                value={terminationDate}
                onChange={(e) => setTerminationDate(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'terminate' && handleTerminate(dialog.emp)}
              disabled={isPending}
            >
              퇴직 처리
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
