export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'TERMINATED';

export interface Department {
  id: number;
  code: string;
  name: string;
  parentId: number | null;
  headEmployeeId: number | null;
  isActive: boolean;
  createdAt: string;
}

export interface Employee {
  id: number;
  employeeNo: string;
  lastName: string;
  firstName: string;
  workEmail: string;
  phone: string | null;
  departmentId: number;
  departmentName: string;
  positionId: number;
  positionName: string;
  jobGradeId: number | null;
  jobGradeName: string | null;
  status: EmployeeStatus;
  hireDate: string;
  terminationDate: string | null;
  baseSalary: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface LeaveRequest {
  id: number;
  employeeId: number;
  employeeName: string;
  policyId: number;
  policyName: string;
  startDate: string;
  endDate: string;
  days: number;
  reason: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}
