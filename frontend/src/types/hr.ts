export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'TERMINATED'
export type EmploymentType = 'REGULAR' | 'CONTRACT' | 'PART_TIME' | 'INTERN' | 'DISPATCH'
export type Gender = 'MALE' | 'FEMALE' | 'OTHER'
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface Department {
  id: number
  code: string
  name: string
  parentId: number | null
  depth: number
  sortOrder: number
  headEmployeeId: number | null
  active: boolean
}

export interface Position {
  id: number
  code: string
  name: string
  levelOrder: number
}

export interface JobGrade {
  id: number
  code: string
  name: string
  gradeOrder: number
  minSalary: number | null
  maxSalary: number | null
}

export interface Employee {
  id: number
  employeeNo: string
  lastName: string
  firstName: string
  fullName: string
  dateOfBirth: string | null
  gender: Gender | null
  phone: string | null
  personalEmail: string | null
  departmentId: number
  departmentName: string
  positionId: number
  positionName: string
  jobGradeId: number | null
  jobGradeName: string | null
  hireDate: string
  terminationDate: string | null
  employmentType: EmploymentType
  status: EmployeeStatus
  baseSalary: number | null
  workEmail: string
  managerId: number | null
}

export interface LeaveRequest {
  id: number
  employeeId: number
  employeeName: string
  leavePolicyId: number
  leavePolicyName: string
  startDate: string
  endDate: string
  requestedDays: number
  approvalStatus: ApprovalStatus
  reason: string | null
  approvalRequestId: number | null
}

export interface LeavePolicy {
  id: number
  code: string
  name: string
  leaveType: string
  annualDays: number
  carryOverDays: number
  requiresApproval: boolean
  minNoticeDays: number
}
