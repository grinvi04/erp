import { beforeEach, describe, expect, it, vi } from 'vitest'

const { apiPost, revalidatePath } = vi.hoisted(() => ({
  apiPost: vi.fn(),
  revalidatePath: vi.fn(),
}))

vi.mock('@/lib/api', () => ({ apiPost }))
vi.mock('next/cache', () => ({ revalidatePath }))

import { rejectInboxItem } from './actions'

describe('rejectInboxItem', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('허용된 결재 유형만 해당 반려 API로 전달한다', async () => {
    await rejectInboxItem('STOCK_MOVEMENT', 17, '수량 재확인')

    expect(apiPost).toHaveBeenCalledWith('/api/inventory/movements/17/reject', {
      comment: '수량 재확인',
    })
    expect(revalidatePath).toHaveBeenCalledWith('/approvals')
  })

  it('객체 프로토타입 이름을 결재 유형으로 실행하지 않는다', async () => {
    await expect(rejectInboxItem('toString', 17, '거부')).rejects.toThrow(
      '인박스 인라인 반려를 지원하지 않는 결재 유형입니다',
    )

    expect(apiPost).not.toHaveBeenCalled()
    expect(revalidatePath).not.toHaveBeenCalled()
  })
})
