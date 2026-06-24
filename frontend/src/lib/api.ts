import { auth } from '@/lib/auth'
import type { ApiResponse, PageResponse } from '@/types/api'

// BACKEND_URL: server-only (Docker container-to-container). NEXT_PUBLIC_API_URL: browser-facing.
const API_BASE = process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

async function getHeaders(): Promise<HeadersInit> {
  const session = await auth()
  const headers: HeadersInit = { 'Content-Type': 'application/json' }
  if (session?.accessToken) {
    headers['Authorization'] = `Bearer ${session.accessToken}`
  }
  return headers
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const headers = await getHeaders()
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { ...headers, ...(options.headers ?? {}) },
    cache: 'no-store',
  })
  if (res.status === 204 || res.status === 205) {
    return { success: true } as ApiResponse<T>
  }
  const contentType = res.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) {
    throw new Error(`HTTP ${res.status} — JSON 응답이 아닙니다 (${contentType})`)
  }
  const body: ApiResponse<T> = await res.json()
  return body
}

/** 현재 로그인 사용자의 Keycloak subject(고유 ID)를 access token에서 추출한다. */
export async function getCurrentUserId(): Promise<string> {
  const session = await auth()
  if (!session?.accessToken) return ''
  try {
    const payload = JSON.parse(
      Buffer.from(session.accessToken.split('.')[1], 'base64').toString()
    )
    return payload.sub ?? ''
  } catch {
    return ''
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const body = await apiFetch<T>(path)
  if (!body.success || body.data === undefined) {
    throw new Error(body.error?.message ?? 'API error')
  }
  return body.data
}

export async function apiGetPage<T>(path: string): Promise<PageResponse<T>> {
  return apiGet<PageResponse<T>>(path)
}

export async function apiPost<T>(path: string, data: unknown): Promise<T> {
  const body = await apiFetch<T>(path, {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!body.success || body.data === undefined) {
    throw new Error(body.error?.message ?? 'API error')
  }
  return body.data
}

export async function apiPut<T>(path: string, data: unknown): Promise<T> {
  const body = await apiFetch<T>(path, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  if (!body.success || body.data === undefined) {
    throw new Error(body.error?.message ?? 'API error')
  }
  return body.data
}

export async function apiDelete(path: string): Promise<void> {
  const body = await apiFetch(path, { method: 'DELETE' })
  if (!body.success) {
    throw new Error(body.error?.message ?? 'DELETE 실패')
  }
}
