import { requestApiData } from './client'

export interface ParserTemplateRequest {
  templateCode: string
  templateName: string
  description: string | null
  fieldMapping: Record<string, string[]>
  customFieldMapping: Record<string, string[]>
  contentSelectors: string[]
  coverSelectors: string[]
  timeFormats: string[]
  cleanupRules: ParserTemplateCleanupRules
  enabled: boolean
}

export interface ParserTemplateCleanupRules {
  removeSelectors: string[]
  unwrapSelectors: string[]
  removeAttributes: string[]
}

export interface ParserTemplateResponse {
  id: number
  templateCode: string
  templateName: string
  description: string | null
  fieldMapping: Record<string, string[]>
  customFieldMapping: Record<string, string[]>
  contentSelectors: string[]
  coverSelectors: string[]
  timeFormats: string[]
  cleanupRules: ParserTemplateCleanupRules
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface ParserTemplatePreviewRequest {
  feedUrl: string
  templateId?: number | null
  template?: ParserTemplateRequest
  limit: number
}

export interface ParserTemplatePreviewResponse {
  feedUrl: string
  feedTitle: string
  templateCode: string
  previewLimit: number
  itemCount: number
  hitRate: number
  fieldHitRates: Record<string, number>
  warnings: string[]
  items: ParserTemplatePreviewItem[]
}

export interface ParserTemplateGenerateRequest {
  feedId?: number | null
  feedUrl?: string | null
  limit: number
  preferAi: boolean
}

export interface ParserTemplateGenerateResponse {
  feedId: number | null
  feedUrl: string
  feedTitle: string | null
  generator: string
  aiUsed: boolean
  fallbackUsed: boolean
  template: ParserTemplateRequest
  preview: ParserTemplatePreviewResponse
  samplePayloads: unknown[]
  warnings: string[]
}

export interface ParserTemplatePreviewItem {
  index: number
  rawPayload: unknown
  normalized: NormalizedPreviewArticle
  fieldHits: Record<string, FieldHit>
  customFieldHits: Record<string, FieldHit>
  warnings: string[]
}

export interface NormalizedPreviewArticle {
  guid: string | null
  articleUrl: string | null
  title: string | null
  summary: string | null
  contentHtml: string | null
  customFields: Record<string, unknown> | null
  author: string | null
  publishedAt: string | null
  coverImageUrl: string | null
  readingMinutes: number | null
  wordCount: number | null
}

export interface FieldHit {
  matched: boolean
  path: string | null
  value: string | null
  fallback: boolean
  message: string | null
}

/**
 * 查询解析模板列表。
 */
export async function fetchParserTemplates(): Promise<ParserTemplateResponse[]> {
  return requestApiData<ParserTemplateResponse[]>('/api/parser-templates')
}

/**
 * 创建解析模板。
 */
export async function createParserTemplate(request: ParserTemplateRequest): Promise<ParserTemplateResponse> {
  return requestApiData<ParserTemplateResponse>('/api/parser-templates', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

/**
 * 更新解析模板。
 */
export async function updateParserTemplate(id: number, request: ParserTemplateRequest): Promise<ParserTemplateResponse> {
  return requestApiData<ParserTemplateResponse>(`/api/parser-templates/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

/**
 * 删除解析模板。
 */
export async function deleteParserTemplate(id: number): Promise<void> {
  await requestApiData<void>(`/api/parser-templates/${id}`, {
    method: 'DELETE',
  })
}

/**
 * 启用或停用解析模板。
 */
export async function setParserTemplateEnabled(id: number, enabled: boolean): Promise<ParserTemplateResponse> {
  return requestApiData<ParserTemplateResponse>(`/api/parser-templates/${id}/enabled?enabled=${enabled}`, {
    method: 'PATCH',
  })
}

/**
 * 预览解析模板效果。
 */
export async function previewParserTemplate(request: ParserTemplatePreviewRequest): Promise<ParserTemplatePreviewResponse> {
  return requestApiData<ParserTemplatePreviewResponse>('/api/parser-templates/preview', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

/**
 * 从订阅源真实样本生成解析模板。
 */
export async function generateParserTemplateFromFeed(request: ParserTemplateGenerateRequest): Promise<ParserTemplateGenerateResponse> {
  return requestApiData<ParserTemplateGenerateResponse>('/api/parser-templates/generate-from-feed', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}
