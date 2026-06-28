'use server'
import { apiGet } from '@/lib/api'
import type { AuditLogDetail } from '@/types/audit'

// 감사 로그 상세(drill-in)용 단건 조회 — 변경 내역(before/after) 포함, 읽기전용.
export async function getAuditLogDetail(id: number): Promise<AuditLogDetail> {
  return apiGet<AuditLogDetail>(`/api/audit/logs/${id}`)
}
