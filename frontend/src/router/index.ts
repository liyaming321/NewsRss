import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import FetchLogsView from '../views/FetchLogsView.vue'
import FeedsView from '../views/FeedsView.vue'
import ParserTemplatesView from '../views/ParserTemplatesView.vue'
import ReaderView from '../views/ReaderView.vue'
import SettingsView from '../views/SettingsView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'dashboard',
    component: DashboardView,
    meta: {
      title: '资讯驾驶舱',
      description: '聚合抓取、阅读状态和源健康的一屏总览',
    },
  },
  {
    path: '/reader',
    name: 'reader',
    component: ReaderView,
    meta: {
      title: '全部文章',
      description: '汇总所有订阅源文章，并在页内筛选未读、收藏和稍后读',
    },
  },
  {
    path: '/feeds',
    name: 'feeds',
    component: FeedsView,
    meta: {
      title: '订阅源管理',
      description: '接入、分组、模板绑定和健康状态管理',
    },
  },
  {
    path: '/parser-templates',
    name: 'parser-templates',
    component: ParserTemplatesView,
    meta: {
      title: '解析模板实验室',
      description: '用字段映射和预览调试解决 RSS 源结构差异',
    },
  },
  {
    path: '/fetch-logs',
    name: 'fetch-logs',
    component: FetchLogsView,
    meta: {
      title: '抓取日志',
      description: '追踪抓取任务、失败原因和模板异常',
    },
  },
  {
    path: '/settings',
    name: 'settings',
    component: SettingsView,
    meta: {
      title: '设置',
      description: '管理阅读偏好、抓取策略和系统配置',
    },
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
