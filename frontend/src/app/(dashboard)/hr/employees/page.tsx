import { apiGetPage } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PaginationBar } from '@/components/ui/pagination-bar'
import type { Employee, EmployeeStatus } from '@/types/hr'
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

export const metadata = { title: '직원 관리 | ERP' }

export default async function EmployeesPage(props: {
  searchParams: Promise<{ page?: string; size?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)

  const data: PageResponse<Employee> = await apiGetPage<Employee>(
    `/api/hr/employees?page=${page}&size=${size}`
  )

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">직원 관리</h1>
        <p className="text-sm text-gray-500 mt-1">조직 내 직원 정보를 관리합니다</p>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>사번</TableHead>
              <TableHead>이름</TableHead>
              <TableHead>부서</TableHead>
              <TableHead>직위</TableHead>
              <TableHead>이메일</TableHead>
              <TableHead>입사일</TableHead>
              <TableHead>상태</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-gray-400 py-10">
                  등록된 직원이 없습니다
                </TableCell>
              </TableRow>
            )}
            {data.content.map((emp) => (
              <TableRow key={emp.id}>
                <TableCell className="font-mono text-sm">{emp.employeeNo}</TableCell>
                <TableCell>{emp.lastName}{emp.firstName}</TableCell>
                <TableCell>{emp.departmentName}</TableCell>
                <TableCell>{emp.positionName}</TableCell>
                <TableCell className="text-sm text-gray-600">{emp.workEmail}</TableCell>
                <TableCell className="text-sm">{emp.hireDate}</TableCell>
                <TableCell>
                  <Badge variant={STATUS_VARIANT[emp.status]}>
                    {STATUS_LABEL[emp.status]}
                  </Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/hr/employees"
        />
      </div>
    </div>
  )
}
