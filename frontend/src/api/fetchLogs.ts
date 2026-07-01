import { requestApiData } from './client'

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FetchLogResponse {
  id: number
  feedId: number
  feedName: string
  startedAt: string
  finishedAt: string | null
  status: string
  requestUrl: string
  httpStatus: number | null
  fetchedCount: number
  newCount: number
  duplicateCount: number
  failedCount: number
  durationMs: number | null
  errorMessage: string | null
  errorStack: string | null
  rawResponseSample: string | null
  createdAt: string
}

export interface FetchLogQuery {
  feedId?: number | null
  status?: string | null
  startedFrom?: string | null
  startedTo?: string | null
}

/**
 * 查询抓取日志分页，用于抓取日志页面展示最近任务执行结果。
 */
export async function fetchFetchLogs(page = 0, size = 20, query: FetchLogQuery = {}): Promise<PageResponse<FetchLogResponse>> {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  })
  if (query.feedId != null) {
    searchParams.set('feedId', String(query.feedId))
  }
  if (query.status) {
    searchParams.set('status', query.status)
  }
  if (query.startedFrom) {
    searchParams.set('startedFrom', query.startedFrom)
  }
  if (query.startedTo) {
    searchParams.set('startedTo', query.startedTo)
  }
  return requestApiData<PageResponse<FetchLogResponse>>(`/api/fetch-logs?${searchParams.toString()}`)
}

/**
 * 查询抓取日志详情。
 */
export async function fetchFetchLogDetail(id: number): Promise<FetchLogResponse> {
  return requestApiData<FetchLogResponse>(`/api/fetch-logs/${id}`)
}
