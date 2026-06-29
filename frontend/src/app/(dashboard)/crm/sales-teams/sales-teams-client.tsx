'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { usePermissions } from '@/components/permissions-provider'
import { PERM } from '@/lib/permissions'
import { PlusIcon, PencilIcon, Trash2Icon, UsersIcon, XIcon, DownloadIcon } from 'lucide-react'
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
import {
  createSalesTeam,
  updateSalesTeam,
  deleteSalesTeam,
  addSalesTeamMember,
  removeSalesTeamMember,
} from './actions'
import type { SalesTeam } from '@/types/crm'

type DialogMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; team: SalesTeam }
  | { type: 'delete'; team: SalesTeam }
  | { type: 'members'; team: SalesTeam }

interface Props {
  teams: SalesTeam[]
}

export default function SalesTeamsClient({ teams }: Props) {
  const { can } = usePermissions()
  const canWrite = can(PERM.CRM_WRITE)
  const [dialog, setDialog] = useState<DialogMode>({ type: 'none' })
  const [isPending, startTransition] = useTransition()
  const close = () => setDialog({ type: 'none' })

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [memberInput, setMemberInput] = useState('')

  const openCreate = () => {
    setCode('')
    setName('')
    setDialog({ type: 'create' })
  }

  const openEdit = (team: SalesTeam) => {
    setName(team.name)
    setDialog({ type: 'edit', team })
  }

  const openMembers = (team: SalesTeam) => {
    setMemberInput('')
    setDialog({ type: 'members', team })
  }

  const handleCreate = () => {
    if (!code.trim()) {
      toast.error('코드는 필수입니다')
      return
    }
    if (!name.trim()) {
      toast.error('팀명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await createSalesTeam({ code: code.trim(), name: name.trim() })
        toast.success('영업팀이 등록되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '등록 중 오류가 발생했습니다')
      }
    })
  }

  const handleUpdate = (team: SalesTeam) => {
    if (!name.trim()) {
      toast.error('팀명은 필수입니다')
      return
    }
    startTransition(async () => {
      try {
        await updateSalesTeam(team.id, { name: name.trim(), version: team.version })
        toast.success('영업팀이 수정되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '수정 중 오류가 발생했습니다')
      }
    })
  }

  const handleDelete = (team: SalesTeam) => {
    startTransition(async () => {
      try {
        await deleteSalesTeam(team.id)
        toast.success('영업팀이 삭제되었습니다')
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  const handleAddMember = (team: SalesTeam) => {
    const userId = memberInput.trim()
    if (!userId) {
      toast.error('사용자 ID를 입력하세요')
      return
    }
    if (team.memberUserIds.includes(userId)) {
      toast.error('이미 추가된 멤버입니다')
      return
    }
    startTransition(async () => {
      try {
        await addSalesTeamMember(team.id, userId)
        toast.success('멤버가 추가되었습니다')
        setMemberInput('')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '멤버 추가 중 오류가 발생했습니다')
      }
    })
  }

  const handleRemoveMember = (team: SalesTeam, userId: string) => {
    startTransition(async () => {
      try {
        await removeSalesTeamMember(team.id, userId)
        toast.success('멤버가 제거되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '멤버 제거 중 오류가 발생했습니다')
      }
    })
  }

  // members 다이얼로그는 서버 액션 후 revalidate된 teams로 갱신된다 — 현재 팀을 다시 찾는다
  const membersTeam =
    dialog.type === 'members' ? (teams.find((t) => t.id === dialog.team.id) ?? dialog.team) : null

  const columns: Column<SalesTeam>[] = [
    {
      key: 'code',
      header: '코드',
      sortable: true,
      sortValue: (team) => team.code,
      cell: (team) => <span className="font-mono text-sm">{team.code}</span>,
    },
    {
      key: 'name',
      header: '팀명',
      sortable: true,
      sortValue: (team) => team.name,
      cell: (team) => <span className="font-medium">{team.name}</span>,
    },
    {
      key: 'members',
      header: '멤버',
      sortable: true,
      sortValue: (team) => team.memberUserIds.length,
      cell: (team) => <Badge variant="secondary">{team.memberUserIds.length}명</Badge>,
      footer: (rows) => (
        <span className="font-mono">
          {rows.reduce((s, r) => s + r.memberUserIds.length, 0).toLocaleString('ko-KR')}명
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      headerClassName: 'w-28',
      cell: (team) => (
        <div className="flex justify-end gap-1">
          <Button
            variant="ghost"
            size="icon-xs"
            title="멤버 관리"
            onClick={() => openMembers(team)}
          >
            <UsersIcon />
          </Button>
          {canWrite && (
            <Button variant="ghost" size="icon-xs" title="수정" onClick={() => openEdit(team)}>
              <PencilIcon />
            </Button>
          )}
          {canWrite && (
            <Button
              variant="ghost"
              size="icon-xs"
              title="삭제"
              onClick={() => setDialog({ type: 'delete', team })}
            >
              <Trash2Icon className="text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  // 조회 조건(한국 ERP) — 입력값(draft)과 적용값(applied) 분리. [조회]에 적용. 로드된 데이터 기준 필터.
  const [qKeyword, setQKeyword] = useState('')
  const [qMembers, setQMembers] = useState('')
  const [applied, setApplied] = useState({ keyword: '', members: '' })
  const onSearch = () => setApplied({ keyword: qKeyword, members: qMembers })
  const onReset = () => {
    setQKeyword('')
    setQMembers('')
    setApplied({ keyword: '', members: '' })
  }
  const filtered = teams.filter((team) => {
    if (applied.keyword) {
      const kw = applied.keyword.toLowerCase()
      if (!team.code.toLowerCase().includes(kw) && !team.name.toLowerCase().includes(kw))
        return false
    }
    if (applied.members === 'HAS' && team.memberUserIds.length === 0) return false
    if (applied.members === 'NONE' && team.memberUserIds.length > 0) return false
    return true
  })
  const exportExcel = () =>
    downloadCsv(
      `영업팀_${new Date().toISOString().slice(0, 10)}`,
      ['코드', '팀명', '멤버수'],
      filtered.map((team) => [team.code, team.name, team.memberUserIds.length]),
    )

  return (
    <div className="p-5">
      <PageHeader
        title="영업팀"
        description="영업팀과 팀 멤버(데이터 스코프)를 관리합니다"
        className="mb-4"
      >
        <Button variant="outline" onClick={exportExcel}>
          <DownloadIcon />
          엑셀
        </Button>
        {canWrite && (
          <Button onClick={openCreate}>
            <PlusIcon />새 영업팀
          </Button>
        )}
      </PageHeader>

      <div className="space-y-3">
        <FilterBar onSearch={onSearch} onReset={onReset}>
          <FilterField label="검색어">
            <Input
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
              placeholder="코드 · 팀명"
              className="h-8 w-44"
            />
          </FilterField>
          <FilterField label="멤버">
            <Select
              value={qMembers || 'ALL'}
              onValueChange={(v) => setQMembers(v === 'ALL' ? '' : (v ?? ''))}
            >
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                <SelectItem value="HAS">멤버 있음</SelectItem>
                <SelectItem value="NONE">멤버 없음</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
        </FilterBar>

        <DataTable
          data={filtered}
          columns={columns}
          getRowId={(team) => team.id}
          showTotals
          totalLabel={`총 ${filtered.length}건`}
          empty={
            <EmptyState
              title="등록된 영업팀이 없습니다"
              description={canWrite ? '우측 상단에서 새 영업팀을 등록하세요.' : undefined}
            />
          }
        />
      </div>

      {/* Create */}
      <Dialog
        open={dialog.type === 'create'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 영업팀 등록</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="코드" required>
                <Input
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="TEAM-001"
                  className="h-8"
                />
              </FormRow>
              <FormRow label="팀명" required>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="영업1팀"
                  className="h-8"
                />
              </FormRow>
            </FormGrid>
          </div>
          <DialogFooter showCloseButton>
            <Button onClick={handleCreate} disabled={isPending}>
              등록
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit */}
      <Dialog
        open={dialog.type === 'edit'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              영업팀 수정{dialog.type === 'edit' && ` — ${dialog.team.code}`}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <FormGrid>
              <FormRow label="팀명" required span>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="h-8"
                />
              </FormRow>
            </FormGrid>
          </div>
          <DialogFooter showCloseButton>
            <Button
              onClick={() => dialog.type === 'edit' && handleUpdate(dialog.team)}
              disabled={isPending}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete */}
      <Dialog
        open={dialog.type === 'delete'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>영업팀 삭제</DialogTitle>
          </DialogHeader>
          {dialog.type === 'delete' && (
            <p className="text-sm text-muted-foreground py-2">
              <strong>
                {dialog.team.code} {dialog.team.name}
              </strong>
              을(를) 삭제하시겠습니까?
            </p>
          )}
          <DialogFooter showCloseButton>
            <Button
              variant="destructive"
              onClick={() => dialog.type === 'delete' && handleDelete(dialog.team)}
              disabled={isPending}
            >
              삭제
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Members */}
      <Dialog
        open={dialog.type === 'members'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>멤버 관리{membersTeam && ` — ${membersTeam.name}`}</DialogTitle>
          </DialogHeader>
          {membersTeam && (
            <div className="grid gap-4 py-2">
              {canWrite && (
                <div className="grid gap-1.5">
                  <Label>사용자 ID 추가</Label>
                  <div className="flex gap-2">
                    <Input
                      value={memberInput}
                      onChange={(e) => setMemberInput(e.target.value)}
                      placeholder="userId"
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') handleAddMember(membersTeam)
                      }}
                    />
                    <Button onClick={() => handleAddMember(membersTeam)} disabled={isPending}>
                      <PlusIcon />
                      추가
                    </Button>
                  </div>
                </div>
              )}
              <div className="grid gap-1.5">
                <Label>멤버 ({membersTeam.memberUserIds.length}명)</Label>
                <div className="flex flex-col gap-1 max-h-64 overflow-y-auto">
                  {membersTeam.memberUserIds.length === 0 && (
                    <p className="text-sm text-muted-foreground py-2">멤버가 없습니다</p>
                  )}
                  {membersTeam.memberUserIds.map((userId) => (
                    <div
                      key={userId}
                      className="flex items-center justify-between rounded-md border px-3 py-1.5"
                    >
                      <span className="font-mono text-sm">{userId}</span>
                      {canWrite && (
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          title="제거"
                          onClick={() => handleRemoveMember(membersTeam, userId)}
                          disabled={isPending}
                        >
                          <XIcon className="text-destructive" />
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
          <DialogFooter showCloseButton />
        </DialogContent>
      </Dialog>
    </div>
  )
}
