import { computed, ref } from 'vue'
import { fetchBackendHealth, type HealthResponse } from '../api/health'

/**
 * 管理后端健康检查状态，供应用壳顶部按钮复用。
 */
export function useBackendHealth() {
  const health = ref<HealthResponse | null>(null)
  const loading = ref(false)
  const errorMessage = ref('')

  const statusLabel = computed(() => {
    if (loading.value) {
      return '检查中'
    }
    if (errorMessage.value) {
      return '异常'
    }
    return health.value?.status === 'UP' ? '在线' : '未检查'
  })

  /**
   * 请求后端健康检查接口，并保存最近一次结果。
   */
  async function checkHealth() {
    loading.value = true
    errorMessage.value = ''
    try {
      health.value = await fetchBackendHealth()
    } catch (error) {
      health.value = null
      errorMessage.value = error instanceof Error ? error.message : '后端健康检查失败'
    } finally {
      loading.value = false
    }
  }

  return {
    health,
    loading,
    errorMessage,
    statusLabel,
    checkHealth,
  }
}
