import { apiGet } from '@/lib/api'
import type { PipelineStage } from '@/types/crm'
import PipelineStagesClient from './pipeline-stages-client'

export const metadata = { title: '파이프라인 단계 | ERP' }

export default async function PipelineStagesPage() {
  const stages = await apiGet<PipelineStage[]>('/api/crm/pipeline-stages')
  return <PipelineStagesClient stages={stages} />
}
