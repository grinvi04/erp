import { apiGet } from '@/lib/api'
import type { ItemCategory } from '@/types/inventory'
import ItemCategoriesClient from './item-categories-client'

export const metadata = { title: '품목분류 관리 | ERP' }

export default async function ItemCategoriesPage() {
  const categories = await apiGet<ItemCategory[]>('/api/inventory/item-categories')
  return <ItemCategoriesClient categories={Array.isArray(categories) ? categories : []} />
}
