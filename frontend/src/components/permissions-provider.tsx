'use client'
import { createContext, useContext, useMemo } from 'react'

interface PermissionsContextValue {
  permissions: ReadonlySet<string>
  can: (permission: string) => boolean
}

const PermissionsContext = createContext<PermissionsContextValue>({
  permissions: new Set(),
  can: () => false,
})

export function PermissionsProvider({
  permissions,
  children,
}: {
  permissions: string[]
  children: React.ReactNode
}) {
  const value = useMemo<PermissionsContextValue>(() => {
    const set = new Set(permissions)
    return { permissions: set, can: (p) => set.has(p) }
  }, [permissions])
  return <PermissionsContext.Provider value={value}>{children}</PermissionsContext.Provider>
}

/** UI 노출 제어용 — 서버 검사가 항상 최종이다. */
export function usePermissions() {
  return useContext(PermissionsContext)
}
