'use client'

import { useState, useTransition } from 'react'
import { MailPlusIcon, RefreshCwIcon, UserRoundXIcon } from 'lucide-react'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { EmptyState } from '@/components/ui/empty-state'
import { FormGrid, FormRow } from '@/components/ui/form-grid'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { Role, TenantUser, TenantUserStatus } from '@/types/iam'
import { disableTenantUser, inviteTenantUser, reinviteTenantUser } from './actions'
import { statusLabel, upsertTenantUser } from './tenant-user-model'

const STATUS_VARIANT: Record<
  TenantUserStatus,
  'secondary' | 'default' | 'destructive' | 'outline'
> = {
  PENDING: 'secondary',
  ACTIVE: 'default',
  FAILED: 'destructive',
  DISABLED: 'outline',
}

type InviteForm = {
  email: string
  firstName: string
  lastName: string
  roleIds: Set<number>
}

const EMPTY_FORM: InviteForm = {
  email: '',
  firstName: '',
  lastName: '',
  roleIds: new Set(),
}

export default function TenantUserPanel({
  initialUsers,
  roles,
  canWrite,
}: {
  initialUsers: TenantUser[]
  roles: Role[]
  canWrite: boolean
}) {
  const [users, setUsers] = useState(initialUsers)
  const [isPending, startTransition] = useTransition()
  const [inviteOpen, setInviteOpen] = useState(false)
  const [form, setForm] = useState<InviteForm>(EMPTY_FORM)
  const [reinviteTarget, setReinviteTarget] = useState<TenantUser | null>(null)
  const [reinviteRoles, setReinviteRoles] = useState<Set<number>>(new Set())
  const [disableTarget, setDisableTarget] = useState<TenantUser | null>(null)

  function openInvite() {
    setForm({ ...EMPTY_FORM, roleIds: new Set() })
    setInviteOpen(true)
  }

  function toggleInviteRole(roleId: number) {
    setForm((current) => ({ ...current, roleIds: toggled(current.roleIds, roleId) }))
  }

  function openReinvite(user: TenantUser) {
    setReinviteRoles(new Set())
    setReinviteTarget(user)
  }

  function submitInvite() {
    startTransition(async () => {
      try {
        const user = await inviteTenantUser({
          email: form.email.trim(),
          firstName: form.firstName.trim() || null,
          lastName: form.lastName.trim() || null,
          requestKey: crypto.randomUUID(),
          roleIds: [...form.roleIds],
        })
        setUsers((current) => upsertTenantUser(current, user))
        setInviteOpen(false)
        toast.success('초대 메일을 보냈습니다')
      } catch (error) {
        toast.error(message(error, '초대를 보내지 못했습니다'))
      }
    })
  }

  function submitReinvite() {
    const target = reinviteTarget
    if (!target) return
    startTransition(async () => {
      try {
        const user = await reinviteTenantUser(target.id, [...reinviteRoles])
        setUsers((current) => upsertTenantUser(current, user))
        setReinviteTarget(null)
        toast.success('초대 메일을 다시 보냈습니다')
      } catch (error) {
        toast.error(message(error, '재초대를 보내지 못했습니다'))
      }
    })
  }

  function submitDisable() {
    const target = disableTarget
    if (!target) return
    startTransition(async () => {
      try {
        await disableTenantUser(target.id)
        setUsers((current) =>
          upsertTenantUser(current, { ...target, status: 'DISABLED', failureCode: null }),
        )
        setDisableTarget(null)
        toast.success('사용자 로그인을 중지했습니다')
      } catch (error) {
        toast.error(message(error, '사용자를 중지하지 못했습니다'))
      }
    })
  }

  return (
    <>
      <Card className="overflow-hidden">
        <CardHeader className="border-b bg-muted/30 sm:flex sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-1">
            <CardTitle>사용자 초대</CardTitle>
            <p className="text-sm text-muted-foreground">
              초대 메일 전송부터 로그인 중지까지 계정 상태를 관리합니다.
            </p>
          </div>
          {canWrite && (
            <Button size="sm" onClick={openInvite}>
              <MailPlusIcon /> 사용자 초대
            </Button>
          )}
        </CardHeader>
        <CardContent className="p-0">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b px-4 py-2.5 text-xs text-muted-foreground">
            <span className="font-medium text-foreground">계정 흐름</span>
            <LifecycleStep status="PENDING" />
            <span aria-hidden="true">→</span>
            <LifecycleStep status="ACTIVE" />
            <span aria-hidden="true">→</span>
            <LifecycleStep status="DISABLED" />
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>이메일</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>사용자 ID</TableHead>
                <TableHead className="text-right">작업</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="p-0">
                    <EmptyState
                      title="초대한 사용자가 없습니다"
                      description="첫 사용자를 초대해 업무 역할을 배정하세요."
                    />
                  </TableCell>
                </TableRow>
              ) : (
                users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">{user.email}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Badge variant={STATUS_VARIANT[user.status]}>
                          {statusLabel(user.status)}
                        </Badge>
                        {user.status === 'FAILED' && user.failureCode && (
                          <span className="text-xs text-destructive" title={user.failureCode}>
                            다시 초대할 수 있습니다
                          </span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="max-w-56 truncate font-mono text-xs text-muted-foreground">
                      {user.userId ?? '아직 발급되지 않음'}
                    </TableCell>
                    <TableCell className="text-right">
                      {canWrite && (
                        <div className="flex justify-end gap-1">
                          {(user.status === 'DISABLED' || user.status === 'FAILED') && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openReinvite(user)}
                              disabled={isPending}
                            >
                              <RefreshCwIcon /> 재초대
                            </Button>
                          )}
                          {user.status === 'ACTIVE' && (
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-destructive hover:text-destructive"
                              onClick={() => setDisableTarget(user)}
                              disabled={isPending}
                            >
                              <UserRoundXIcon /> 사용 중지
                            </Button>
                          )}
                        </div>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Dialog open={inviteOpen} onOpenChange={setInviteOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>사용자 초대</DialogTitle>
          </DialogHeader>
          <div className="space-y-5 py-2">
            <FormGrid>
              <FormRow label="이메일" required span>
                <Input
                  aria-label="이메일"
                  type="email"
                  autoComplete="email"
                  value={form.email}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, email: event.target.value }))
                  }
                  placeholder="name@company.com"
                />
              </FormRow>
              <FormRow label="이름">
                <Input
                  aria-label="이름"
                  autoComplete="given-name"
                  value={form.firstName}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, firstName: event.target.value }))
                  }
                />
              </FormRow>
              <FormRow label="성">
                <Input
                  aria-label="성"
                  autoComplete="family-name"
                  value={form.lastName}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, lastName: event.target.value }))
                  }
                />
              </FormRow>
            </FormGrid>
            <RolePicker roles={roles} selected={form.roleIds} onToggle={toggleInviteRole} />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setInviteOpen(false)} disabled={isPending}>
              취소
            </Button>
            <Button onClick={submitInvite} disabled={isPending || !form.email.trim()}>
              초대 메일 보내기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={reinviteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setReinviteTarget(null)
        }}
      >
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>사용자 재초대</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <p className="text-sm text-muted-foreground">
              <strong className="text-foreground">{reinviteTarget?.email}</strong> 계정을 다시
              활성화하고 새 초대 메일을 보냅니다. 이번에 적용할 역할을 선택하세요.
            </p>
            <RolePicker
              roles={roles}
              selected={reinviteRoles}
              onToggle={(roleId) => setReinviteRoles((current) => toggled(current, roleId))}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setReinviteTarget(null)} disabled={isPending}>
              취소
            </Button>
            <Button onClick={submitReinvite} disabled={isPending}>
              다시 초대하기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={disableTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDisableTarget(null)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>사용자 로그인 중지</DialogTitle>
          </DialogHeader>
          <p className="py-2 text-sm text-muted-foreground">
            <strong className="text-foreground">{disableTarget?.email}</strong> 사용자는 즉시
            로그인할 수 없게 되고 배정된 역할이 모두 해제됩니다.
          </p>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDisableTarget(null)} disabled={isPending}>
              취소
            </Button>
            <Button variant="destructive" onClick={submitDisable} disabled={isPending}>
              로그인 중지
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}

function RolePicker({
  roles,
  selected,
  onToggle,
}: {
  roles: Role[]
  selected: Set<number>
  onToggle: (roleId: number) => void
}) {
  return (
    <div className="space-y-2">
      <Label>업무 역할</Label>
      <div className="grid max-h-56 gap-2 overflow-y-auto rounded-md border p-3 sm:grid-cols-2">
        {roles.length === 0 ? (
          <p className="text-sm text-muted-foreground">배정할 수 있는 역할이 없습니다.</p>
        ) : (
          roles.map((role) => (
            <label key={role.id} className="flex min-w-0 items-start gap-2 text-sm">
              <input
                type="checkbox"
                checked={selected.has(role.id)}
                onChange={() => onToggle(role.id)}
                className="mt-0.5 shrink-0"
              />
              <span className="min-w-0">
                <span className="block font-medium">{role.name}</span>
                <span className="block truncate font-mono text-xs text-muted-foreground">
                  {role.code}
                </span>
              </span>
            </label>
          ))
        )}
      </div>
      <p className="text-xs text-muted-foreground">역할은 선택하지 않고 초대할 수도 있습니다.</p>
    </div>
  )
}

function LifecycleStep({ status }: { status: TenantUserStatus }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span
        className={`size-1.5 rounded-full ${status === 'ACTIVE' ? 'bg-primary' : 'bg-muted-foreground/50'}`}
        aria-hidden="true"
      />
      {statusLabel(status)}
    </span>
  )
}

function toggled(values: Set<number>, value: number): Set<number> {
  const next = new Set(values)
  if (next.has(value)) next.delete(value)
  else next.add(value)
  return next
}

function message(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
