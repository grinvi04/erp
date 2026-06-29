export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: {
    code: string
    message: string
  }
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface BulkImportResult {
  totalRows: number
  importedCount: number
  errors: { rowNumber: number; message: string }[]
}
