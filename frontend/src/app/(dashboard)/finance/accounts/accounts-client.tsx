'use client'
import { useState, useTransition, useMemo } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, BanIcon, DownloadIcon } from 'lucide-react'
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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { downloadCsv } from '@/lib/csv'
import { createAccount, updateAccount, deactivateAccount } from './actions'
import type { Account, AccountType, NormalBalance } from '@/types/finance'

const TYPE_LABEL: Record<AccountType, string> = {
  ASSET: '자산',
  LIABILITY: '부채',
  EQUITY: '자본',
  REVENUE: '수익',
  EXPENSE: '비용',
}
const NORMAL_LABEL: Record<NormalBalance, string> = { DEBIT: '차변', CREDIT: '대변' }

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; acc: Account }
  | { type: 'deactivate'; acc: Account }

interface Props {
  accounts: Account[]
}

export default function AccountsClient({ accounts }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)
  const accountById = useMemo(() => new Map(accounts.map((a) => [a.id, a])), [accounts])

  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [accountType, setAccountType] = useState<string>('')
  const [normalBalance, setNormalBalance] = useState<string>('')
  const [parentId, setParentId] = useState<string>('')
  const [isSummary, setIsSummary] = useState(false)

  const openCreate = () => {
    setCode('')
    setName('')
    setAccountType('')
    setNormalBalance('')
    setParentId('')
    setIsSummary(false)
    setDialog({ type: 'create' })
  }

  const openEdit = (acc: Account) => {
    setName(acc.name)
    setIsSummary(acc.isSummary)
    setDialog({ type: 'edit', acc })
  }

  const handleCreate = () => {
    if (!code.trim() || !name.trim() || !accountType || !normalBalance) {
      toast.error('필수 항목을 모두 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        await createAccount({
          code: code.trim(),
          name: name.trim(),
          accountType: accountType as AccountType,
          normalBalance: normalBalance as NormalBalance,
          parentId: parentId ? Number(parentId) : null,
          isSummary,
        })
        toast.success('계정과목이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (acc: Account) => {
    if (!name.trim()) {
      toast.error('계정과목명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateAccount(acc.id, { name: name.trim(), isSummary, version: acc.version })
        toast.success('계정과목이 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDeactivate = (acc: Account) => {
    startTransition(async () => {
      try {
        await deactivateAccount(acc.id)
        toast.success('계정과목이 비활성화되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '비활성화 중 오류가 발생했습니다')
      }
    })
  }

  const columns: Column<Account>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (acc) => acc.code,
      cell: (acc) => <span className="font-mono text-sm">{acc.code}</span>,
    },
    {
      key: 'name',
      header: '계정과목명',
      sortable: true,
      sortValue: (acc) => acc.name,
      cell: (acc) => <span className="font-medium">{acc.name}</span>,
    },
    {
      key: 'type',
      header: '유형',
      sortable: true,
      sortValue: (acc) => TYPE_LABEL[acc.accountType],
      cell: (acc) => <Badge variant="secondary">{TYPE_LABEL[acc.accountType]}</Badge>,
    },
    {
      key: 'normalBalance',
      header: '대차구분',
      cell: (acc) => (
        <span className="text-sm text-muted-foreground">{NORMAL_LABEL[acc.normalBalance]}</span>
      ),
    },
    {
      key: 'parent',
      header: '상위',
      cell: (acc) => {
        const parent = acc.parentId != null ? accountById.get(acc.parentId) : undefined
        return (
          <span className="text-sm text-muted-foreground font-mono">
            {parent ? `${parent.code} ${parent.name}` : '—'}
          </span>
        )
      },
    },
    {
      key: 'isSummary',
      header: '집계',
      cell: (acc) => (
        <span className="text-sm text-muted-foreground">{acc.isSummary ? 'Y' : 'N'}</span>
      ),
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (acc) => (acc.isActive ? 0 : 1),
      cell: (acc) => (
        <Badge variant={acc.isActive ? 'default' : 'secondary'}>
          {acc.isActive ? '활성' : '비활성'}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (acc) => (
        <div className="flex justify-end gap-1">
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(acc)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && acc.isActive && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="비활성화"
              onClick={() => setDialog({ type: 'deactivate', acc })}
            >
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 현재 데이터 기준 필터.
  const [qType, setQType] = useState('')
  const [qNormal, setQNormal] = useState('')
  const [qStatus, setQStatus] = useState('')
  const [applied, setApplied] = useState({ type: '', normal: '', status: '' })
  const onSearch = () => setApplied({ type: qType, normal: qNormal, status: qStatus })
  const onReset = () => {
    setQType('')
    setQNormal('')
    setQStatus('')
    setApplied({ type: '', normal: '', status: '' })
  }
  const filtered = accounts.filter((acc) => {
    if (applied.type && acc.accountType !== applied.type) return false
    if (applied.normal && acc.normalBalance !== applied.normal) return false
    if (applied.status && String(acc.isActive ? 'ACTIVE' : 'INACTIVE') !== applied.status) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `계정과목_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '계정과목명', '유형', '대차구분', '상위', '집계', '상태'],
      filtered.map((acc) => {
        const parent = acc.parentId != null ? accountById.get(acc.parentId) : undefined
        return [
          acc.code,
          acc.name,
          TYPE_LABEL[acc.accountType],
          NORMAL_LABEL[acc.normalBalance],
          parent ? `${parent.code} ${parent.name}` : '',
          acc.isSummary ? 'Y' : 'N',
          acc.isActive ? '활성' : '비활성',
        ]
      }),
    )

  return (
    <div className="p-5">
      <PageHeader title="계정과목" description="회계 계정과목 체계를 관리합니다" className="mb-4">
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 계정과목
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="유형">
            <Select value={qType || 'ALL'} onValueChange={(v) => setQType(v === 'ALL' ? '' : (v ?? ''))}>
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {(Object.keys(TYPE_LABEL) as AccountType[]).map((k) => (
                  <SelectItem key={k} value={k}>
                    {TYPE_LABEL[k]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="대차구분">
            <Select
              value={qNormal || 'ALL'}
              onValueChange={(v) => setQNormal(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-28">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {(Object.keys(NORMAL_LABEL) as NormalBalance[]).map((k) => (
                  <SelectItem key={k} value={k}>
                    {NORMAL_LABEL[k]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-28">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="ACTIVE">활성</SelectItem>
                <SelectItem value="INACTIVE">비활성</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(acc) => acc.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 계정과목이 없습니다"
              description={canWrite ? '우측 상단에서 새 계정과목을 등록하세요.' : undefined}
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
            <DialogTitle>새 계정과목 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="코드" required>
                <Input
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="1101"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="유형" required>
                <Select value={accountType} onValueChange={(v) => setAccountType(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue placeholder="선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(TYPE_LABEL).map(([k, v]) => (
                      <SelectItem key={k} value={k}>
                        {v}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="계정과목명" required span>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="현금및현금성자산"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="대차구분" required>
                <Select value={normalBalance} onValueChange={(v) => setNormalBalance(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue placeholder="선택" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DEBIT">차변</SelectItem>
                    <SelectItem value="CREDIT">대변</SelectItem>
                  </SelectContent>
                </Select>
              </FormRow>
              <FormRow label="상위 계정과목">
                <Select value={parentId} onValueChange={(v) => setParentId(v ?? '')}>
                  <SelectTrigger className="h-8 w-full">
                    <SelectValue placeholder="없음" />
                  </SelectTrigger>
                  <SelectContent>
                    {accounts
                      .filter((a) => a.isSummary && a.isActive)
                      .map((a) => (
                        <SelectItem key={a.id} value={String(a.id)}>
                          {a.code} {a.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </FormRow>
            </FormGrid>
            <div className="flex items-center gap-2">
              <input
                id="isSummary"
                type="checkbox"
                checked={isSummary}
                onChange={(e) => setIsSummary(e.target.checked)}
                className="h-4 w-4 rounded border-input"
              />
              <Label htmlFor="isSummary">집계 계정 (하위 계정 합산용)</Label>
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
            <DialogTitle>
              계정과목 수정{dialog.type === 'edit' && ` — ${dialog.acc.code}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="계정과목명" required span>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="h-8"
                />
              </FormRow>
            </FormGrid>
            <div className="flex items-center gap-2">
              <input
                id="isSummaryEdit"
                type="checkbox"
                checked={isSummary}
                onChange={(e) => setIsSummary(e.target.checked)}
                className="h-4 w-4 rounded border-input"
              />
              <Label htmlFor="isSummaryEdit">집계 계정</Label>
            </div>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.acc)}
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
            <DialogTitle>계정과목 비활성화</DialogTitle>
          </DialogHeader>
          {dialog.type === 'deactivate' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.acc.code} {dialog.acc.name}
              </strong>
              을(를) 비활성화하시겠습니까? 하위 계정이 있으면 비활성화할 수 없습니다.
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'deactivate' && handleDeactivate(dialog.acc)}
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
