'use server'
import { apiPost, apiPut, apiDelete } from '@/lib/api'
import { revalidatePath } from 'next/cache'

export interface PipelineStagePayload {
  name: string
  stageOrder: number
  probability: number
  isClosedWon: boolean
  isClosedLost: boolean
}

export async function createPipelineStage(data: PipelineStagePayload): Promise<void> {
  await apiPost('/api/crm/pipeline-stages', data)
  revalidatePath('/crm/pipeline-stages')
}

export async function updatePipelineStage(id: number, data: PipelineStagePayload): Promise<void> {
  await apiPut(`/api/crm/pipeline-stages/${id}`, data)
  revalidatePath('/crm/pipeline-stages')
}

export async function deletePipelineStage(id: number): Promise<void> {
  await apiDelete(`/api/crm/pipeline-stages/${id}`)
  revalidatePath('/crm/pipeline-stages')
}
