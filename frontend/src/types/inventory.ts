export interface Item {
  id: number;
  sku: string;
  name: string;
  categoryId: number;
  categoryName: string;
  uomId: number;
  uomCode: string;
  isStockTracked: boolean;
  isActive: boolean;
  costPrice: number | null;
  salePrice: number | null;
  createdAt: string;
}

export interface Warehouse {
  id: number;
  code: string;
  name: string;
  address: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface StockMovement {
  id: number;
  movementNo: string;
  movementType: 'RECEIPT' | 'ISSUE' | 'TRANSFER' | 'ADJUSTMENT';
  itemId: number;
  itemName: string;
  warehouseId: number;
  warehouseName: string;
  quantity: number;
  unitCost: number | null;
  status: 'DRAFT' | 'CONFIRMED';
  movementDate: string;
  createdAt: string;
}

export interface StockBalance {
  itemId: number;
  itemName: string;
  sku: string;
  warehouseId: number;
  warehouseName: string;
  quantity: number;
  uomCode: string;
}
