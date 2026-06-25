export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'RETURNED'

export interface ApprovalSummary {
  id: number
  entityType: string
  entityId: number
  title: string
  status: ApprovalStatus
  requesterId: string
  currentStep: number
  totalSteps: number
  currentStepName: string | null
  currentApproverId: string | null
  requestedAt: string
  completedAt: string | null
}
