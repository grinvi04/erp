'use client'
import { useState, useTransition } from 'react'
import { toast } from 'sonner'
import { PlusIcon, PencilIcon, Trash2Icon, SearchIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  createRole,
  updateRole,
  deleteRole,
  getUserRoles,
  getAccessProfile,
  assignRole,
  unassignRole,
  setAccessProfile,
} from './actions'
import type { AccessProfile, DataScope, Role } from '@/types/iam'

function groupByModule(catalog: string[]): Record<string, string[]> {
  const groups: Record<string, string[]> = {}
  for (const p of catalog) {
    const mod = p.split(':')[0]
    const list = groups[mod] ?? (groups[mod] = [])
    list.push(p)
  }
  return groups
}

type RoleDialog = { mode: 'none' } | { mode: 'create' } | { mode: 'edit'; role: Role }

export default function IamClient({
  roles,
  catalog,
  canWrite,
}: {
  roles: Role[]
  catalog: string[]
  canWrite: boolean
}) {
  const [isPending, startTransition] = useTransition()
  const [dialog, setDialog] = useState<RoleDialog>({ mode: 'none' })
  const [form, setForm] = useState({
    code: '',
    name: '',
    description: '',
    permissions: new Set<string>(),
  })
  const grouped = groupByModule(catalog)

  function openCreate() {
    setForm({ code: '', name: '', description: '', permissions: new Set() })
    setDialog({ mode: 'create' })
  }
  function openEdit(role: Role) {
    setForm({
      code: role.code,
      name: role.name,
      description: role.description ?? '',
      permissions: new Set(role.permissions),
    })
    setDialog({ mode: 'edit', role })
  }
  function close() {
    setDialog({ mode: 'none' })
  }

  function togglePerm(p: string) {
    setForm((f) => {
      const permissions = new Set(f.permissions)
      if (permissions.has(p)) permissions.delete(p)
      else permissions.add(p)
      return { ...f, permissions }
    })
  }

  function saveRole() {
    const permissions = [...form.permissions]
    startTransition(async () => {
      try {
        if (dialog.mode === 'create') {
          await createRole({
            code: form.code,
            name: form.name,
            description: form.description || null,
            permissions,
          })
          toast.success('역할이 생성되었습니다')
        } else if (dialog.mode === 'edit') {
          await updateRole(dialog.role.id, {
            name: form.name,
            description: form.description || null,
            permissions,
          })
          toast.success('역할이 수정되었습니다')
        }
        close()
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '저장 중 오류가 발생했습니다')
      }
    })
  }

  function removeRole(role: Role) {
    if (!window.confirm(`역할 '${role.name}'을(를) 삭제할까요?`)) return
    startTransition(async () => {
      try {
        await deleteRole(role.id)
        toast.success('역할이 삭제되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '삭제 중 오류가 발생했습니다')
      }
    })
  }

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-semibold text-foreground">역할·권한 관리</h1>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>역할</CardTitle>
          {canWrite && (
            <Button onClick={openCreate} size="sm">
              <PlusIcon />새 역할
            </Button>
          )}
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>코드</TableHead>
                <TableHead>이름</TableHead>
                <TableHead>권한 수</TableHead>
                <TableHead className="text-right">작업</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {roles.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-sm text-muted-foreground py-8">
                    역할이 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                roles.map((role) => (
                  <TableRow key={role.id}>
                    <TableCell className="font-mono text-xs">{role.code}</TableCell>
                    <TableCell>{role.name}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{role.permissions.length}</Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      {canWrite && (
                        <div className="flex justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon-xs"
                            title="수정"
                            onClick={() => openEdit(role)}
                            disabled={isPending}
                          >
                            <PencilIcon />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon-xs"
                            title="삭제"
                            onClick={() => removeRole(role)}
                            disabled={isPending}
                          >
                            <Trash2Icon className="text-destructive" />
                          </Button>
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

      <UserAccessPanel roles={roles} canWrite={canWrite} />

      <Dialog
        open={dialog.mode !== 'none'}
        onOpenChange={(o) => {
          if (!o) close()
        }}
      >
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{dialog.mode === 'create' ? '새 역할' : '역할 수정'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-1.5">
                <Label>코드</Label>
                <Input
                  value={form.code}
                  disabled={dialog.mode === 'edit'}
                  onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
                  placeholder="HR_MANAGER"
                />
              </div>
              <div className="grid gap-1.5">
                <Label>이름</Label>
                <Input
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="인사 관리자"
                />
              </div>
            </div>
            <div className="grid gap-1.5">
              <Label>설명</Label>
              <Input
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              />
            </div>
            <div className="grid gap-2">
              <Label>권한</Label>
              <div className="max-h-64 overflow-y-auto space-y-3 border rounded-md p-3">
                {Object.entries(grouped).map(([mod, perms]) => (
                  <div key={mod}>
                    <div className="text-xs font-semibold text-muted-foreground uppercase mb-1">
                      {mod}
                    </div>
                    <div className="grid grid-cols-2 gap-1">
                      {perms.map((p) => (
                        <label key={p} className="flex items-center gap-2 text-sm">
                          <input
                            type="checkbox"
                            checked={form.permissions.has(p)}
                            onChange={() => togglePerm(p)}
                          />
                          <span className="font-mono text-xs">{p}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={close}>
              취소
            </Button>
            <Button
              onClick={saveRole}
              disabled={isPending || !form.name || (dialog.mode === 'create' && !form.code)}
            >
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function UserAccessPanel({ roles, canWrite }: { roles: Role[]; canWrite: boolean }) {
  const [isPending, startTransition] = useTransition()
  const [sub, setSub] = useState('')
  const [loaded, setLoaded] = useState<{
    sub: string
    roleIds: Set<number>
    profile: AccessProfile | null
  } | null>(null)
  const [scope, setScope] = useState<DataScope>('ALL')
  const [deptId, setDeptId] = useState('')
  const [limit, setLimit] = useState('')

  function load() {
    const target = sub.trim()
    if (!target) return
    startTransition(async () => {
      try {
        const [userRoles, profile] = await Promise.all([
          getUserRoles(target),
          getAccessProfile(target),
        ])
        setLoaded({ sub: target, roleIds: new Set(userRoles.map((r) => r.id)), profile })
        setScope(profile?.dataScope ?? 'ALL')
        setDeptId(profile?.departmentId != null ? String(profile.departmentId) : '')
        setLimit(profile?.approvalLimit != null ? String(profile.approvalLimit) : '')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '조회에 실패했습니다')
      }
    })
  }

  function toggleRole(roleId: number, has: boolean) {
    if (!loaded) return
    startTransition(async () => {
      try {
        if (has) await unassignRole(loaded.sub, roleId)
        else await assignRole(loaded.sub, roleId)
        const next = new Set(loaded.roleIds)
        if (has) next.delete(roleId)
        else next.add(roleId)
        setLoaded({ ...loaded, roleIds: next })
        toast.success('역할 배정이 변경되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '변경에 실패했습니다')
      }
    })
  }

  function saveProfile() {
    if (!loaded) return
    startTransition(async () => {
      try {
        await setAccessProfile(loaded.sub, {
          dataScope: scope,
          departmentId: deptId.trim() ? Number(deptId) : null,
          approvalLimit: limit.trim() ? Number(limit) : null,
        })
        toast.success('접근 프로파일이 저장되었습니다')
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '저장에 실패했습니다')
      }
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>사용자 접근 관리</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex gap-2 items-end">
          <div className="grid gap-1.5 flex-1 max-w-md">
            <Label>사용자 ID (Keycloak sub)</Label>
            <Input
              value={sub}
              onChange={(e) => setSub(e.target.value)}
              placeholder="예: 3f2a8c..."
            />
          </div>
          <Button variant="outline" onClick={load} disabled={isPending || !sub.trim()}>
            <SearchIcon />
            조회
          </Button>
        </div>

        {loaded && (
          <div className="space-y-5 border-t pt-4">
            <div>
              <div className="text-sm font-medium mb-2">
                역할 배정 — <span className="font-mono text-xs">{loaded.sub}</span>
              </div>
              <div className="grid grid-cols-2 gap-1">
                {roles.map((role) => {
                  const has = loaded.roleIds.has(role.id)
                  return (
                    <label key={role.id} className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={has}
                        disabled={!canWrite || isPending}
                        onChange={() => toggleRole(role.id, has)}
                      />
                      <span>
                        {role.name}{' '}
                        <span className="text-muted-foreground font-mono text-xs">
                          ({role.code})
                        </span>
                      </span>
                    </label>
                  )
                })}
              </div>
            </div>

            <div>
              <div className="text-sm font-medium mb-2">
                접근 프로파일 (데이터 스코프·전결 한도)
              </div>
              <div className="grid grid-cols-3 gap-3 max-w-2xl">
                <div className="grid gap-1.5">
                  <Label>데이터 스코프</Label>
                  <Select value={scope} onValueChange={(v) => setScope((v as DataScope) ?? 'ALL')}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">전사(ALL)</SelectItem>
                      <SelectItem value="DEPARTMENT">부서+하위(DEPARTMENT)</SelectItem>
                      <SelectItem value="SELF">본인(SELF)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-1.5">
                  <Label>부서 ID</Label>
                  <Input
                    value={deptId}
                    onChange={(e) => setDeptId(e.target.value)}
                    placeholder="(선택)"
                  />
                </div>
                <div className="grid gap-1.5">
                  <Label>전결 한도(원)</Label>
                  <Input
                    value={limit}
                    onChange={(e) => setLimit(e.target.value)}
                    placeholder="(선택)"
                  />
                </div>
              </div>
              {canWrite && (
                <Button className="mt-3" size="sm" onClick={saveProfile} disabled={isPending}>
                  프로파일 저장
                </Button>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
