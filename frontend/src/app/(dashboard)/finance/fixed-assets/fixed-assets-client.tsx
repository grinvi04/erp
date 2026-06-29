'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PlayIcon, BanIcon, HistoryIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DatePicker } from '@/components/ui/date-picker'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
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
import { FilterBar, FilterField } from '@/components/ui/filter-bar'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import {
  createFixedAsset,
  disposeFixedAsset,
  runDepreciation,
  updateDepreciationAccounts,
  getDepreciationHistory,
} from './actions'
import type {
  Account,
  DepreciationAccounts,
  DepreciationEntry,
  DepreciationMethod,
  FiscalPeriod,
  FixedAsset,
  FixedAssetStatus,
} from '@/types/finance'
import type { PageResponse } from '@/types/api'

const METHOD_LABEL: Record<DepreciationMethod, string> = {
  STRAIGHT_LINE: '정액법',
  DECLINING_BALANCE: '정률법',
}
const STATUS_LABEL: Record<FixedAssetStatus, string> = {
  ACTIVE: '가동',
  DISPOSED: '처분',
}
const STATUS_VARIANT: Record<FixedAssetStatus, 'default' | 'secondary'> = {
  ACTIVE: 'default',
  DISPOSED: 'secondary',
}
const NONE = 'NONE'

function won(n: number) {
  return n.toLocaleString('ko-KR')
}

type DialogState =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'dispose'; asset: FixedAsset }
  | { type: 'run' }
  | { type: 'history'; asset: FixedAsset }

interface Props {
  data: PageResponse<FixedAsset>
  accounts: Account[]
  depreciationAccounts: DepreciationAccounts
  periods: FiscalPeriod[]
}

export default function FixedAssetsClient({
  data,
  accounts,
  depreciationAccounts,
  periods,
}: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.FINANCE_WRITE)
  const canSetting = can(PERM.FINANCE_SETTING_WRITE)
  const selectableAccounts = accounts.filter((a) => !a.isSummary && a.isActive)
  const openPeriods = periods.filter((p) => p.status === 'OPEN')

  const [dialog, setDialog] = useState<DialogState>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  // 등록 폼
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [acquisitionDate, setAcquisitionDate] = useState('')
  const [acquisitionCost, setAcquisitionCost] = useState('')
  const [residualValue, setResidualValue] = useState('0')
  const [usefulLifeMonths, setUsefulLifeMonths] = useState('')
  const [method, setMethod] = useState<DepreciationMethod>('STRAIGHT_LINE')
  const [decliningAnnualRate, setDecliningAnnualRate] = useState('')
  const [assetAccountId, setAssetAccountId] = useState('')

  const openCreate = () => {
    setCode('')
    setName('')
    setAcquisitionDate('')
    setAcquisitionCost('')
    setResidualValue('0')
    setUsefulLifeMonths('')
    setMethod('STRAIGHT_LINE')
    setDecliningAnnualRate('')
    setAssetAccountId('')
    setDialog({ type: 'create' })
  }

  const handleCreate = () => {
    if (
      !code.trim() ||
      !name.trim() ||
      !acquisitionDate ||
      Number(acquisitionCost) <= 0 ||
      Number(usefulLifeMonths) <= 0 ||
      !assetAccountId
    ) {
      toast.error('필수 항목을 모두 입력해주세요')
      return
    }
    if (method === 'DECLINING_BALANCE' && Number(decliningAnnualRate) <= 0) {
      toast.error('정률법은 연상각률을 입력해주세요')
      return
    }
    startTransition(async () => {
      try {
        await createFixedAsset({
          code: code.trim(),
          name: name.trim(),
          acquisitionDate,
          acquisitionCost: Number(acquisitionCost),
          residualValue: Number(residualValue) || 0,
          usefulLifeMonths: Number(usefulLifeMonths),
          method,
          decliningAnnualRate: method === 'DECLINING_BALANCE' ? Number(decliningAnnualRate) : null,
          assetAccountId: Number(assetAccountId),
        })
        toast.success('고정자산이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  // 처분 폼
  const [disposalDate, setDisposalDate] = useState('')
  const [proceeds, setProceeds] = useState('0')
  const [proceedsAccountId, setProceedsAccountId] = useState('')

  const openDispose = (asset: FixedAsset) => {
    setDisposalDate(new Date().toISOString().slice(0, 10))
    setProceeds('0')
    setProceedsAccountId('')
    setDialog({ type: 'dispose', asset })
  }

  const handleDispose = (asset: FixedAsset) => {
    if (!disposalDate) {
      toast.error('처분일을 입력해주세요')
      return
    }
    if (Number(proceeds) > 0 && !proceedsAccountId) {
      toast.error('처분 대가가 있으면 대가 수령 계정을 선택해주세요')
      return
    }
    startTransition(async () => {
      try {
        await disposeFixedAsset(asset.id, {
          disposalDate,
          proceeds: Number(proceeds) || 0,
          proceedsAccountId: proceedsAccountId ? Number(proceedsAccountId) : null,
        })
        toast.success('자산이 처분되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '처분 중 오류가 발생했습니다')
      }
    })
  }

  // 월 상각 실행
  const [runPeriodId, setRunPeriodId] = useState('')
  const openRun = () => {
    setRunPeriodId(openPeriods.length > 0 ? String(openPeriods[0].id) : '')
    setDialog({ type: 'run' })
  }
  const handleRun = () => {
    if (!runPeriodId) {
      toast.error('회계기간을 선택해주세요')
      return
    }
    startTransition(async () => {
      try {
        const result = await runDepreciation(Number(runPeriodId))
        toast.success(
          `상각 처리 완료 — ${result.processedCount}건 처리, ${result.skippedCount}건 건너뜀, 총 ${won(result.totalAmount)}원`,
        )
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '상각 처리 중 오류가 발생했습니다')
      }
    })
  }

  // 상각 이력
  const [history, setHistory] = useState<DepreciationEntry[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const openHistory = (asset: FixedAsset) => {
    setHistory([])
    setHistoryLoading(true)
    setDialog({ type: 'history', asset })
    startTransition(async () => {
      try {
        setHistory(await getDepreciationHistory(asset.id))
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '이력 조회 중 오류가 발생했습니다')
      } finally {
        setHistoryLoading(false)
      }
    })
  }

  // 감가상각·처분 계정 설정
  const [expenseAcc, setExpenseAcc] = useState(
    depreciationAccounts.depreciationExpenseAccountId != null
      ? String(depreciationAccounts.depreciationExpenseAccountId)
      : '',
  )
  const [accumulatedAcc, setAccumulatedAcc] = useState(
    depreciationAccounts.accumulatedDepreciationAccountId != null
      ? String(depreciationAccounts.accumulatedDepreciationAccountId)
      : '',
  )
  const [gainAcc, setGainAcc] = useState(
    depreciationAccounts.disposalGainAccountId != null
      ? String(depreciationAccounts.disposalGainAccountId)
      : '',
  )
  const [lossAcc, setLossAcc] = useState(
    depreciationAccounts.disposalLossAccountId != null
      ? String(depreciationAccounts.disposalLossAccountId)
      : '',
  )
  const handleSaveAccounts = () => {
    startTransition(async () => {
      try {
        await updateDepreciationAccounts({
          depreciationExpenseAccountId: expenseAcc ? Number(expenseAcc) : null,
          accumulatedDepreciationAccountId: accumulatedAcc ? Number(accumulatedAcc) : null,
          disposalGainAccountId: gainAcc ? Number(gainAcc) : null,
          disposalLossAccountId: lossAcc ? Number(lossAcc) : null,
        })
        toast.success('감가상각·처분 계정이 저장되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '저장 중 오류가 발생했습니다')
      }
    })
  }

  const accountSelect = (value: string, onChange: (v: string) => void, placeholder = '미설정') => (
    <Select
      value={value || NONE}
      disabled={!canSetting}
      onValueChange={(v) => onChange(!v || v === NONE ? '' : v)}
    >
      <SelectTrigger className="h-8 w-full">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value={NONE}>미설정</SelectItem>
        {selectableAccounts.map((a) => (
          <SelectItem key={a.id} value={String(a.id)}>
            {a.code} {a.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )

  // 조회 조건 — 상태 필터(현재 페이지 기준).
  const [qStatus, setQStatus] = useState('')
  const [applied, setApplied] = useState({ status: '' })
  const onSearch = () => setApplied({ status: qStatus })
  const onReset = () => {
    setQStatus('')
    setApplied({ status: '' })
  }
  const filtered = data.content.filter((a) => {
    if (applied.status && a.status !== applied.status) return false
    return true
  })

  const columns: Column<FixedAsset>[] = [
    {
      key: 'code',
      header: '자산코드',
      sortable: true,
      sortValue: (a) => a.code,
      cell: (a) => <span className="font-mono text-sm">{a.code}</span>,
    },
    {
      key: 'name',
      header: '자산명',
      sortable: true,
      sortValue: (a) => a.name,
      cell: (a) => <span className="font-medium">{a.name}</span>,
    },
    {
      key: 'acquisitionDate',
      header: '취득일',
      sortable: true,
      sortValue: (a) => a.acquisitionDate,
      cell: (a) => <span className="text-sm">{a.acquisitionDate}</span>,
    },
    {
      key: 'acquisitionCost',
      header: '취득원가',
      align: 'right',
      sortable: true,
      sortValue: (a) => a.acquisitionCost,
      cell: (a) => <span className="font-mono text-sm">{won(a.acquisitionCost)}</span>,
      footer: (rows) => (
        <span className="font-mono">{won(rows.reduce((s, r) => s + r.acquisitionCost, 0))}</span>
      ),
    },
    {
      key: 'accumulatedDepreciation',
      header: '감가상각누계',
      align: 'right',
      sortable: true,
      sortValue: (a) => a.accumulatedDepreciation,
      cell: (a) => (
        <span className="font-mono text-sm text-muted-foreground">
          {won(a.accumulatedDepreciation)}
        </span>
      ),
    },
    {
      key: 'bookValue',
      header: '장부가액',
      align: 'right',
      sortable: true,
      sortValue: (a) => a.bookValue,
      cell: (a) => <span className="font-mono text-sm">{won(a.bookValue)}</span>,
      footer: (rows) => (
        <span className="font-mono">{won(rows.reduce((s, r) => s + r.bookValue, 0))}</span>
      ),
    },
    {
      key: 'method',
      header: '상각방법',
      sortable: true,
      sortValue: (a) => METHOD_LABEL[a.method],
      cell: (a) => <span className="text-sm">{METHOD_LABEL[a.method]}</span>,
    },
    {
      key: 'status',
      header: '상태',
      sortable: true,
      sortValue: (a) => STATUS_LABEL[a.status],
      cell: (a) => <Badge variant={STATUS_VARIANT[a.status]}>{STATUS_LABEL[a.status]}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-24',
      cell: (a) => (
        <div className="flex justify-end gap-1">
          <Button
            variant="ghost"
            size="icon-xs"
            title="상각이력"
            onClick={() => openHistory(a)}
            disabled={isPending}
          >
            <HistoryIcon className="text-muted-foreground" />
          </Button>
          {canWrite && a.status === 'ACTIVE' && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="처분"
              onClick={() => openDispose(a)}
              disabled={isPending}
            >
              <BanIcon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="p-5">
      <PageHeader
        title="고정자산"
        description="자산 대장·월 감가상각·처분을 관리합니다"
        className="mb-4"
      >
        {canWrite && (
          <Button variant="outline" onClick={openRun}>
            <PlayIcon />월 상각 실행
          </Button>
        )}
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 자산
          </Button>
        )}
      </PageHeader>

      {/* 감가상각·처분 계정 설정 */}
      <div className="bg-card rounded-lg border p-5 mb-4">
        <div className="mb-1 text-sm font-medium text-foreground">감가상각·처분 계정</div>
        <p className="text-xs text-muted-foreground mb-4">
          월 상각 시 (차)감가상각비·(대)감가상각누계액으로, 처분 시 처분이익·처분손실로 자동 분개할
          계정입니다. 미설정이면 상각/처분 처리가 차단됩니다.
        </p>
        <FormGrid>
          <FormRow label="감가상각비 (비용)">{accountSelect(expenseAcc, setExpenseAcc)}</FormRow>
          <FormRow label="감가상각누계액 (자산차감)">
            {accountSelect(accumulatedAcc, setAccumulatedAcc)}
          </FormRow>
          <FormRow label="유형자산처분이익 (수익)">{accountSelect(gainAcc, setGainAcc)}</FormRow>
          <FormRow label="유형자산처분손실 (비용)">{accountSelect(lossAcc, setLossAcc)}</FormRow>
        </FormGrid>
        {canSetting && (
          <div className="mt-4 flex justify-end">
            <Button onClick={handleSaveAccounts} disabled={isPending}>
              저장
            </Button>
          </div>
        )}
      </div>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="상태">
            <Select
              value={qStatus || 'ALL'}
              onValueChange={(v) => setQStatus(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {(Object.keys(STATUS_LABEL) as FixedAssetStatus[]).map((s) => (
                  <SelectItem key={s} value={s}>
                    {STATUS_LABEL[s]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(a) => a.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 고정자산이 없습니다"
              description={canWrite ? '우측 상단에서 새 자산을 등록하세요.' : undefined}
            />
          }
        />
        <PaginationBar
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          basePath="/finance/fixed-assets"
        />
      </div>

      {/* 등록 Dialog */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>새 고정자산 등록</DialogTitle>
          </DialogHeader>
          <FormGrid className="py-2">
            <FormRow label="자산코드" required>
              <Input
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="FA-2025-001"
                className="h-8"
              />
            </FormRow>
            <FormRow label="자산명" required>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="업무용 노트북"
                className="h-8"
              />
            </FormRow>
            <FormRow label="취득일" required>
              <DatePicker value={acquisitionDate} onChange={setAcquisitionDate} />
            </FormRow>
            <FormRow label="취득원가" required>
              <Input
                type="number"
                min={0.01}
                step={0.01}
                value={acquisitionCost}
                onChange={(e) => setAcquisitionCost(e.target.value)}
                placeholder="0"
                className="h-8"
              />
            </FormRow>
            <FormRow label="잔존가치">
              <Input
                type="number"
                min={0}
                step={0.01}
                value={residualValue}
                onChange={(e) => setResidualValue(e.target.value)}
                placeholder="0"
                className="h-8"
              />
            </FormRow>
            <FormRow label="내용연수(월)" required>
              <Input
                type="number"
                min={1}
                step={1}
                value={usefulLifeMonths}
                onChange={(e) => setUsefulLifeMonths(e.target.value)}
                placeholder="60"
                className="h-8"
              />
            </FormRow>
            <FormRow label="상각방법" required>
              <Select
                value={method}
                onValueChange={(v) => setMethod((v ?? 'STRAIGHT_LINE') as DepreciationMethod)}
              >
                <SelectTrigger className="h-8 w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(METHOD_LABEL) as DepreciationMethod[]).map((m) => (
                    <SelectItem key={m} value={m}>
                      {METHOD_LABEL[m]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FormRow>
            {method === 'DECLINING_BALANCE' && (
              <FormRow label="연상각률" required>
                <Input
                  type="number"
                  min={0}
                  max={1}
                  step={0.0001}
                  value={decliningAnnualRate}
                  onChange={(e) => setDecliningAnnualRate(e.target.value)}
                  placeholder="0.45"
                  className="h-8"
                />
              </FormRow>
            )}
            <FormRow label="유형자산 계정" required span>
              <Select value={assetAccountId} onValueChange={(v) => setAssetAccountId(v ?? '')}>
                <SelectTrigger className="h-8 w-full">
                  <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                  {selectableAccounts.map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>
                      {a.code} {a.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FormRow>
          </FormGrid>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 처분 Dialog */}
      <Dialog
        open={dialog.type === 'dispose'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>자산 처분</DialogTitle>
          </DialogHeader>
          {dialog.type === 'dispose' && (
            <div className="grid gap-4 py-2">
              <div className="text-sm text-muted-foreground">
                <strong>{dialog.asset.name}</strong> — {dialog.asset.code}
                <br />
                장부가액: <strong>{won(dialog.asset.bookValue)}원</strong>
              </div>
              <div className="grid gap-1.5">
                <label className="text-sm">처분일 *</label>
                <DatePicker value={disposalDate} onChange={setDisposalDate} />
              </div>
              <div className="grid gap-1.5">
                <label className="text-sm">처분 대가 (매각액, 폐기는 0)</label>
                <Input
                  type="number"
                  min={0}
                  step={0.01}
                  value={proceeds}
                  onChange={(e) => setProceeds(e.target.value)}
                />
              </div>
              <div className="grid gap-1.5">
                <label className="text-sm">대가 수령 계정 (현금·미수금)</label>
                <Select
                  value={proceedsAccountId || NONE}
                  onValueChange={(v) => setProceedsAccountId(!v || v === NONE ? '' : v)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="미선택" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NONE}>미선택</SelectItem>
                    {selectableAccounts.map((a) => (
                      <SelectItem key={a.id} value={String(a.id)}>
                        {a.code} {a.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <p className="text-xs text-muted-foreground">
                장부가액과 처분 대가의 차액이 처분이익(대가&gt;장부) 또는 처분손실(대가&lt;장부)로
                자동 분개됩니다.
              </p>
            </div>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'dispose' && handleDispose(dialog.asset)}
              disabled={isPending}
            >
              처분 처리
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 월 상각 실행 Dialog */}
      <Dialog
        open={dialog.type === 'run'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>월 감가상각 실행</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="grid gap-1.5">
              <label className="text-sm">대상 회계기간 *</label>
              <Select value={runPeriodId} onValueChange={(v) => setRunPeriodId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="OPEN 회계기간 선택" />
                </SelectTrigger>
                <SelectContent>
                  {openPeriods.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.startDate} ~ {p.endDate}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {openPeriods.length === 0 && (
                <p className="text-xs text-destructive">OPEN 상태인 회계기간이 없습니다.</p>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              가동 중인 모든 자산에 당월 상각을 처리하고 (차)감가상각비·(대)감가상각누계액 분개를
              생성합니다. 이미 처리된 자산·기간은 건너뜁니다(멱등).
            </p>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleRun} disabled={isPending || openPeriods.length === 0}>
              상각 실행
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 상각 이력 Dialog */}
      <Dialog
        open={dialog.type === 'history'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>
              감가상각 이력{dialog.type === 'history' ? ` — ${dialog.asset.name}` : ''}
            </DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>회계기간 ID</TableHead>
                  <TableHead className="text-right">상각액</TableHead>
                  <TableHead className="text-right">분개</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {historyLoading && (
                  <TableRow>
                    <TableCell colSpan={3} className="text-center text-muted-foreground py-8">
                      불러오는 중…
                    </TableCell>
                  </TableRow>
                )}
                {!historyLoading && history.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={3} className="text-center text-muted-foreground py-8">
                      상각 이력이 없습니다
                    </TableCell>
                  </TableRow>
                )}
                {history.map((h) => (
                  <TableRow key={h.id}>
                    <TableCell className="font-mono text-sm">{h.fiscalPeriodId}</TableCell>
                    <TableCell className="text-right font-mono text-sm">{won(h.amount)}</TableCell>
                    <TableCell className="text-right font-mono text-sm text-muted-foreground">
                      {h.journalEntryId ? `#${h.journalEntryId}` : '-'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <DialogFooter showCloseButton />
        </DialogContent>
      </Dialog>
    </div>
  )
}
