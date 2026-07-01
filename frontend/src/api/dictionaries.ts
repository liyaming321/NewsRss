import { requestApiData } from './client'

export const FEED_CATEGORY_DICT_TYPE = 'FEED_CATEGORY'

export interface DictionaryResponse {
  id: number
  dictType: string
  itemCode: string
  itemLabel: string
  description: string | null
  sortOrder: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface DictionaryRequest {
  itemCode: string
  itemLabel: string
  description: string | null
  sortOrder: number
  enabled: boolean
}

/**
 * 查询系统字典项。
 */
export async function fetchDictionaryItems(dictType = FEED_CATEGORY_DICT_TYPE, enabled?: boolean): Promise<DictionaryResponse[]> {
  const searchParams = new URLSearchParams({
    dictType,
  })
  if (enabled !== undefined) {
    searchParams.set('enabled', String(enabled))
  }
  return requestApiData<DictionaryResponse[]>(`/api/dictionaries?${searchParams.toString()}`)
}

/**
 * 创建系统字典项。
 */
export async function createDictionaryItem(
  request: DictionaryRequest,
  dictType = FEED_CATEGORY_DICT_TYPE,
): Promise<DictionaryResponse> {
  const searchParams = new URLSearchParams({ dictType })
  return requestApiData<DictionaryResponse>(`/api/dictionaries?${searchParams.toString()}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

/**
 * 更新系统字典项。
 */
export async function updateDictionaryItem(id: number, request: DictionaryRequest): Promise<DictionaryResponse> {
  return requestApiData<DictionaryResponse>(`/api/dictionaries/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

/**
 * 删除系统字典项。
 */
export async function deleteDictionaryItem(id: number): Promise<void> {
  await requestApiData<void>(`/api/dictionaries/${id}`, {
    method: 'DELETE',
  })
}
