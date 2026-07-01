import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
  }),
  actions: {
    /**
     * 切换侧边栏收起状态，方便后续适配窄屏布局。
     */
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
  },
})
