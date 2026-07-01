import { requestApiData } from './client'
import type { ArticleListItemResponse, ArticleStateResponse } from './articles'

export interface DashboardSummaryResponse {
  feedCount: number
  enabledFeedCount: number
  articleCount: number
  todayNewArticleCount: number
  unreadCount: number
  favoriteCount: number
  readLaterCount: number
  failedFetchLogCount: number
}

export interface FeedHealthResponse {
  unknown: number
  healthy: number
  warning: number
  error: number
}

export type { ArticleListItemResponse, ArticleStateResponse }

/**
 * 查询首页驾驶舱摘要指标。
 */
export async function fetchDashboardSummary(): Promise<DashboardSummaryResponse> {
  return requestApiData<DashboardSummaryResponse>('/api/dashboard/summary')
}

/**
 * 查询订阅源健康状态统计。
 */
export async function fetchFeedHealth(): Promise<FeedHealthResponse> {
  return requestApiData<FeedHealthResponse>('/api/dashboard/feed-health')
}

/**
 * 查询最近文章列表。
 */
export async function fetchRecentArticles(limit = 8): Promise<ArticleListItemResponse[]> {
  const searchParams = new URLSearchParams({
    limit: String(limit),
  })
  return requestApiData<ArticleListItemResponse[]>(`/api/dashboard/recent-articles?${searchParams.toString()}`)
}
