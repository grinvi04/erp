'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { Label } from '@/components/ui/label'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { fetchLeaveBalances } from './actions'
import type { LeaveBalance, Employee } from '@/types/hr'

interface Props {
  employees: Employee[]
}

const CURRENT_YEAR = new Date().getFullYear()
const YEARS = Array.from({ length: 5 }, (_, i) => CURRENT_YEAR - i)

export default function LeaveBalancesClient({ employees }: Props) {
  const [empId, setEmpId] = useState<string>('')
  const [year, setYear] = useState<string>(String(CURRENT_YEAR))
  const [balances, setBalances] = useState<LeaveBalance[]>([])
  const [searched, setSearched] = useState(false)
  const [loading, setLoading] = useState(false)
  const [, startTransition] = useTransition()

  const load = (id: string, yr: string) => {
    if (!id) {
      setBalances([])
      setSearched(false)
      return
    }
    setLoading(true)
    startTransition(async () => {
      try {
        const list = await fetchLeaveBalances(Number(id), Number(yr))
        setBalances(list)
        setSearched(true)
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '잔여 조회 중 오류가 발생했습니다')
        setBalances([])
        setSearched(true)
      } finally {
        setLoading(false)
      }
    })
  }

  const onSelectEmployee = (v: string | null) => {
    const next = v ?? ''
    setEmpId(next)
    load(next, year)
  }

  const onSelectYear = (v: string | null) => {
    const next = v ?? String(CURRENT_YEAR)
    setYear(next)
    load(empId, next)
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">휴가 잔여</h1>
        <p className="text-sm text-gray-500 mt-1">직원별 연도 휴가 부여·사용·잔여 현황을 조회합니다</p>
      </div>

      <div className="mb-4 flex flex-wrap gap-4">
        <div className="w-72">
          <Label className="mb-1.5 block">직원</Label>
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
        <div className="w-40">
          <Label className="mb-1.5 block">연도</Label>
          <Select value={year} onValueChange={onSelectYear}>
            <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
            <SelectContent>
              {YEARS.map((y) => (
                <SelectItem key={y} value={String(y)}>{y}년</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="bg-white rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>휴가 정책</TableHead>
              <TableHead className="text-right">부여 일수</TableHead>
              <TableHead className="text-right">이월 일수</TableHead>
              <TableHead className="text-right">사용 일수</TableHead>
              <TableHead className="text-right">잔여 일수</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {!empId && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-gray-400 py-10">
                  직원을 선택하면 휴가 잔여가 표시됩니다
                </TableCell>
              </TableRow>
            )}
            {empId && loading && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-gray-400 py-10">
                  불러오는 중...
                </TableCell>
              </TableRow>
            )}
            {empId && !loading && searched && balances.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-gray-400 py-10">
                  해당 연도의 휴가 잔여 내역이 없습니다
                </TableCell>
              </TableRow>
            )}
            {empId && !loading && balances.map((b) => (
              <TableRow key={b.id}>
                <TableCell className="font-medium">{b.leavePolicyName}</TableCell>
                <TableCell className="text-right text-sm text-gray-600">{b.entitledDays}</TableCell>
                <TableCell className="text-right text-sm text-gray-600">{b.carryOverDays}</TableCell>
                <TableCell className="text-right text-sm text-gray-600">{b.usedDays}</TableCell>
                <TableCell className="text-right text-sm font-medium">{b.remainingDays}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
