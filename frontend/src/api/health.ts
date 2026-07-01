import { requestJson } from './client'

export interface HealthResponse {
  applicationName: string
  version: string
  status: string
  checkedAt: string
}

/**
 * 请求后端健康检查接口，用于确认前后端基础联通能力。
 */
export async function fetchBackendHealth(): Promise<HealthResponse> {
  return requestJson<HealthResponse>('/api/health')
}
