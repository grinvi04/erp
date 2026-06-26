import { apiGet, apiGetPage } from '@/lib/api'
import type { Item, ItemCategory, Uom } from '@/types/inventory'
import type { PageResponse } from '@/types/api'
import ItemsClient from './items-client'

export const metadata = { title: '품목 관리 | ERP' }

export default async function ItemsPage(props: {
  searchParams: Promise<{ page?: string; size?: string; categoryId?: string; keyword?: string }>
}) {
  const sp = await props.searchParams
  const page = Number(sp.page ?? 0)
  const size = Number(sp.size ?? 20)
  const categoryFilter = sp.categoryId ? `&categoryId=${sp.categoryId}` : ''
  const keyword = sp.keyword?.trim() || ''
  const keywordQuery = keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''

  const [data, categories, uoms] = await Promise.all([
    apiGetPage<Item>(`/api/inventory/items?page=${page}&size=${size}${categoryFilter}${keywordQuery}`),
    apiGet<ItemCategory[]>('/api/inventory/item-categories'),
    apiGet<Uom[]>('/api/inventory/uoms'),
  ])

  return (
    <ItemsClient
      data={data as PageResponse<Item>}
      categories={categories}
      uoms={uoms}
      keyword={keyword}
    />
  )
}
