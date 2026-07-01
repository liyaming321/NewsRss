export interface ApiEnvelope<T> {
  success: boolean
  message: string
  data: T
  checkedAt?: string
  timestamp?: string
}

interface ApiErrorEnvelope {
  message?: string
  details?: string[]
}

/**
 * 请求 JSON 接口，并统一处理 HTTP 错误。
 */
export async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  const responseText = await response.text()

  if (!response.ok) {
    throw new Error(parseErrorMessage(responseText, response.status))
  }

  if (!responseText.trim()) {
    throw new Error('接口返回空响应')
  }

  return JSON.parse(responseText) as T
}

/**
 * 请求统一响应结构接口，并返回 data 字段。
 */
export async function requestApiData<T>(url: string, init?: RequestInit): Promise<T> {
  const payload = await requestJson<ApiEnvelope<T>>(url, init)
  if (!payload.success) {
    throw new Error(payload.message || '接口请求失败')
  }

  return payload.data
}

/**
 * 从错误响应体中提取可读错误信息。
 */
function parseErrorMessage(responseText: string, status: number) {
  if (!responseText.trim()) {
    return `接口请求失败，状态码：${status}`
  }
  try {
    const payload = JSON.parse(responseText) as ApiErrorEnvelope
    if (payload.details?.length) {
      return `${payload.message || '接口请求失败'}：${payload.details.join('；')}`
    }
    return payload.message || `接口请求失败，状态码：${status}`
  } catch {
    return `接口请求失败，状态码：${status}`
  }
}
