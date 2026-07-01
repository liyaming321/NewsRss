import { requestApiData } from './client'
import type { PageResponse } from './fetchLogs'

export interface ArticleStateResponse {
  read: boolean
  favorite: boolean
  readLater: boolean
  archived: boolean
  readAt: string | null
  favoritedAt: string | null
  readLaterAt: string | null
  archivedAt: string | null
}

export interface ArticleListItemResponse {
  id: number
  feedId: number
  feedName: string
  title: string
  articleUrl: string
  summary: string | null
  author: string | null
  publishedAt: string | null
  fetchedAt: string
  coverImageUrl: string | null
  readingMinutes: number | null
  wordCount: number | null
  state: ArticleStateResponse
}

export interface ArticleResponse extends ArticleListItemResponse {
  guid: string | null
  canonicalUrl: string | null
  contentHtml: string | null
  rawPayload: unknown
  parseTrace: unknown
  customFields: Record<string, unknown>
}

export type ArticleFlagType = 'read-state' | 'favorite' | 'read-later' | 'archive'
export type ArticleFilterType = 'all' | 'unread' | 'favorite' | 'readLater' | 'today'

export interface ArticleFeedStatResponse {
  feedId: number
  feedName: string
  count: number
}

export interface ArticleStatsResponse {
  totalCount: number
  unreadCount: number
  favoriteCount: number
  readLaterCount: number
  todayCount: number
  feedStats: ArticleFeedStatResponse[]
}

/**
 * 分页查询文章列表。
 */
export async function fetchArticles(
  feedId: number | null,
  page = 0,
  size = 30,
  filter: ArticleFilterType = 'all',
  keyword = '',
): Promise<PageResponse<ArticleListItemResponse>> {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  })
  if (feedId != null) {
    searchParams.set('feedId', String(feedId))
  }
  if (filter !== 'all') {
    searchParams.set('filter', filter)
  }
  if (keyword.trim()) {
    searchParams.set('keyword', keyword.trim())
  }

  return requestApiData<PageResponse<ArticleListItemResponse>>(`/api/articles?${searchParams.toString()}`)
}

/**
 * 查询文章统计。
 */
export async function fetchArticleStats(
  feedId: number | null,
  filter: ArticleFilterType = 'all',
  keyword = '',
): Promise<ArticleStatsResponse> {
  const searchParams = new URLSearchParams()
  if (feedId != null) {
    searchParams.set('feedId', String(feedId))
  }
  if (filter !== 'all') {
    searchParams.set('filter', filter)
  }
  if (keyword.trim()) {
    searchParams.set('keyword', keyword.trim())
  }

  const query = searchParams.toString()
  return requestApiData<ArticleStatsResponse>(query ? `/api/articles/stats?${query}` : '/api/articles/stats')
}

/**
 * 查询文章详情。
 */
export async function fetchArticleDetail(id: number): Promise<ArticleResponse> {
  return requestApiData<ArticleResponse>(`/api/articles/${id}`)
}

/**
 * 更新文章布尔状态。
 */
export async function updateArticleFlag(id: number, type: ArticleFlagType, value: boolean): Promise<ArticleStateResponse> {
  return requestApiData<ArticleStateResponse>(`/api/articles/${id}/${type}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ value }),
  })
}
