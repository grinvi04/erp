export interface PipelineDistributionResponse {
  stageId: number
  stageName: string
  stageOrder: number
  count: number
  totalAmount: number
}

export interface LeadStatusCountResponse {
  status: 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'CONVERTED' | 'DISQUALIFIED'
  count: number
}

export interface MonthlyInvoiceResponse {
  month: number
  count: number
  totalAmount: number
}
