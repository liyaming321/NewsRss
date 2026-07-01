<script setup lang="ts">
import {
  Activity,
  BookOpen,
  Database,
  FileCog,
  Home,
  Rss,
  Settings,
} from '@lucide/vue'
import { NButton, NLayout, NLayoutContent, NLayoutHeader, NLayoutSider, NTag, useMessage } from 'naive-ui'
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useBackendHealth } from '../../composables/useBackendHealth'
import type { NavigationItem } from '../../types/navigation'

const route = useRoute()
const message = useMessage()
const backendHealth = useBackendHealth()

const navigationItems: NavigationItem[] = [
  { key: 'dashboard', label: '驾驶舱', routeName: 'dashboard', icon: Home },
  { key: 'reader', label: '全部文章', routeName: 'reader', icon: BookOpen },
  { key: 'feeds', label: '订阅源', routeName: 'feeds', icon: Rss },
  { key: 'parser-templates', label: '解析模板', routeName: 'parser-templates', icon: FileCog },
  { key: 'fetch-logs', label: '抓取日志', routeName: 'fetch-logs', icon: Activity },
  { key: 'settings', label: '设置', routeName: 'settings', icon: Settings },
]

const pageTitle = computed(() => String(route.meta.title ?? 'NewsRss'))
const pageDescription = computed(() => String(route.meta.description ?? 'RSS 订阅阅读系统'))
const healthTagType = computed(() => {
  if (backendHealth.errorMessage.value) {
    return 'error'
  }
  return backendHealth.health.value?.status === 'UP' ? 'success' : 'info'
})

/**
 * 判断导航项是否匹配当前路由，主导航只保留一级功能入口。
 */
function isNavigationActive(item: NavigationItem) {
  return route.name === item.routeName
}

/**
 * 手动检查后端健康状态，并给出轻量反馈。
 */
async function checkBackendHealth() {
  await backendHealth.checkHealth()
  if (backendHealth.errorMessage.value) {
    message.error(backendHealth.errorMessage.value)
    return
  }
  message.success('后端健康检查通过')
}

onMounted(() => {
  void backendHealth.checkHealth()
})
</script>

<template>
  <NLayout class="app-shell" has-sider>
    <NLayoutSider class="app-sidebar" :width="248" bordered>
      <section class="brand-panel" aria-label="NewsRss 品牌">
        <div class="brand-mark">NR</div>
        <div>
          <h1>NewsRss</h1>
          <p>信息流控制台</p>
        </div>
      </section>

      <nav class="nav-list" aria-label="主导航">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.key"
          class="nav-item"
          :class="{ active: isNavigationActive(item) }"
          :to="{ name: item.routeName, query: item.query }"
        >
          <span class="nav-icon">
            <component :is="item.icon" :size="16" />
          </span>
          {{ item.label }}
        </RouterLink>
      </nav>

      <section class="sync-status" aria-label="同步状态">
        <div>
          <span class="status-dot"></span>
          同步运行中
        </div>
        <p>8 个源正在轮询<br />下一轮 04:32</p>
      </section>
    </NLayoutSider>

    <NLayout>
      <NLayoutHeader class="app-header" bordered>
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageDescription }}</p>
        </div>
        <div class="header-actions">
          <NTag :bordered="false" :type="healthTagType">后端 {{ backendHealth.statusLabel.value }}</NTag>
          <NButton secondary type="primary" :loading="backendHealth.loading.value" @click="checkBackendHealth">
            <template #icon>
              <Database :size="16" />
            </template>
            后端健康
          </NButton>
        </div>
      </NLayoutHeader>

      <NLayoutContent class="app-content">
        <RouterView />
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>

<style scoped>
.app-shell {
  height: 100vh;
  min-width: 0;
  overflow: hidden;
  background: transparent;
}

.app-sidebar {
  height: 100vh;
  overflow-y: auto;
  background: linear-gradient(180deg, #0d1324, #0a1020);
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 20px 36px;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border: 1px solid rgb(34 211 238 / 0.65);
  border-radius: 8px;
  background: rgb(34 211 238 / 0.16);
  color: #22d3ee;
  font-weight: 900;
}

.brand-panel h1 {
  margin: 0;
  color: #e5e7eb;
  font-size: 18px;
  line-height: 1.2;
}

.brand-panel p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.nav-list {
  display: grid;
  gap: 8px;
  padding: 0 16px;
}

.nav-item {
  display: flex;
  align-items: center;
  height: 42px;
  gap: 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #94a3b8;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 700;
}

.nav-item.active {
  border-color: rgb(34 211 238 / 0.45);
  background: rgb(34 211 238 / 0.12);
  color: #e5e7eb;
  box-shadow: inset 3px 0 0 #22d3ee;
}

.nav-icon {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 1px solid #243044;
  border-radius: 6px;
}

.nav-item.active .nav-icon {
  border-color: rgb(34 211 238 / 0.55);
  color: #22d3ee;
}

.sync-status {
  position: absolute;
  right: 20px;
  bottom: 32px;
  left: 20px;
  border: 1px solid rgb(34 211 238 / 0.24);
  border-radius: 8px;
  background: #0f172a;
  padding: 16px 18px;
}

.sync-status div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e5e7eb;
  font-weight: 700;
}

.sync-status p {
  margin: 12px 0 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.55;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a3e635;
  box-shadow: 0 0 12px #a3e635;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 76px;
  background: rgb(9 14 26 / 0.86);
  padding: 0 24px;
}

.app-header h2 {
  margin: 0;
  color: #e5e7eb;
  font-size: 20px;
}

.app-header p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-content {
  min-width: 0;
  height: calc(100vh - 76px);
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  background: #090e1a;
  padding: 16px 24px;
}
</style>
