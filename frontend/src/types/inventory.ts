export type CostMethod = 'FIFO' | 'LIFO' | 'WEIGHTED_AVG' | 'STANDARD'

export interface ItemCategory {
  id: number
  code: string
  name: string
  parentId: number | null
  parentName: string | null
  version: number
}

export interface Uom {
  id: number
  code: string
  name: string
  version: number
}

export interface Warehouse {
  id: number
  code: string
  name: string
  address: string | null
  active: boolean
  version: number
}

export type LocationType = 'ZONE' | 'AISLE' | 'RACK' | 'BIN'

export interface Location {
  id: number
  warehouseId: number
  warehouseName: string
  code: string
  name: string
  parentId: number | null
  parentName: string | null
  locationType: LocationType
  active: boolean
  version: number
}

export interface Item {
  id: number
  sku: string
  name: string
  description: string | null
  categoryId: number | null
  categoryName: string | null
  uomId: number
  uomCode: string
  uomName: string
  costMethod: CostMethod
  standardCost: number
  reorderPoint: number
  reorderQty: number
  minStock: number
  maxStock: number
  lotTracked: boolean
  serialTracked: boolean
  active: boolean
  version: number
}

export type MovementType = 'RECEIPT' | 'ISSUE' | 'TRANSFER' | 'ADJUSTMENT'
export type MovementStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELLED'

export interface MovementLine {
  id: number
  lineNo: number
  itemId: number
  itemSku: string
  itemName: string
  fromLocationId: number | null
  fromLocationCode: string | null
  toLocationId: number | null
  toLocationCode: string | null
  lotNo: string | null
  serialNo: string | null
  qty: number
  unitCost: number
}

export interface Movement {
  id: number
  movementNo: string
  movementType: MovementType
  status: MovementStatus
  referenceType: string | null
  referenceId: number | null
  movementDate: string
  note: string | null
  lines: MovementLine[] | null
}

export interface StockBalance {
  id: number
  itemId: number
  itemSku: string
  itemName: string
  locationId: number
  locationCode: string
  locationName: string
  warehouseId: number
  warehouseName: string
  lotNo: string | null
  serialNo: string | null
  qtyOnHand: number
  qtyReserved: number
  unitCost: number
  reorderPoint: number
  minStock: number
}
