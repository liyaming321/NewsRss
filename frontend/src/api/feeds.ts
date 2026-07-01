import { requestApiData } from './client'
import type { PageResponse } from './fetchLogs'

export interface FeedResponse {
  id: number
  parserTemplateId: number | null
  parserTemplateCode: string | null
  feedName: string
  feedUrl: string
  siteUrl: string | null
  description: string | null
  language: string | null
  category: string | null
  iconUrl: string | null
  healthStatus: string
  enabled: boolean
  fetchIntervalMinutes: number
  lastFetchAt: string | null
  nextFetchAt: string | null
  lastSuccessAt: string | null
  lastFailureAt: string | null
  consecutiveFailureCount: number
  articleCount: number
  createdAt: string
  updatedAt: string
}

export interface FeedRequest {
  feedName: string
  feedUrl: string
  category: string | null
  iconUrl: string | null
  parserTemplateId: number | null
  fetchIntervalMinutes: number
  enabled: boolean
}

export interface FeedDetectResponse {
  feedUrl: string
  title: string | null
  siteUrl: string | null
  description: string | null
  language: string | null
  itemCount: number
}

export interface RssFetchResult {
  feedUrl: string
  success: boolean
  fetchedCount: number
  newCount: number
  duplicateCount: number
  failedCount: number
  errorMessage: string | null
}

/**
 * 分页查询订阅源列表。
 */
export async function fetchFeeds(page = 0, size = 50): Promise<PageResponse<FeedResponse>> {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  })
  return requestApiData<PageResponse<FeedResponse>>(`/api/feeds?${searchParams.toString()}`)
}

/**
 * 创建订阅源。
 */
export async function createFeed(request: FeedRequest): Promise<FeedResponse> {
  return requestApiData<FeedResponse>('/api/feeds', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

/**
 * 更新订阅源配置。
 */
export async function updateFeed(id: number, request: FeedRequest): Promise<FeedResponse> {
  return requestApiData<FeedResponse>(`/api/feeds/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

/**
 * 删除订阅源。
 */
export async function deleteFeed(id: number): Promise<void> {
  await requestApiData<void>(`/api/feeds/${id}`, {
    method: 'DELETE',
  })
}

/**
 * 手动刷新订阅源。
 */
export async function refreshFeed(id: number): Promise<RssFetchResult> {
  return requestApiData<RssFetchResult>(`/api/feeds/${id}/refresh`, {
    method: 'POST',
  })
}

/**
 * 探测订阅源元数据。
 */
export async function detectFeed(feedUrl: string): Promise<FeedDetectResponse> {
  return requestApiData<FeedDetectResponse>('/api/feeds/detect', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ feedUrl }),
  })
}
