<script setup lang="ts">
import {
  AlertTriangle,
  Clock,
  FileWarning,
  Filter,
  RefreshCw,
  Search,
  ServerCrash,
} from '@lucide/vue'
import {
  NButton,
  NDataTable,
  NDatePicker,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NInput,
  NPagination,
  NSelect,
  NSpin,
  NTag,
  useMessage,
  type DataTableColumns,
  type SelectOption,
} from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { fetchFeeds, type FeedResponse } from '../api/feeds'
import {
  fetchFetchLogDetail,
  fetchFetchLogs,
  type FetchLogResponse,
  type FetchLogQuery,
} from '../api/fetchLogs'

const message = useMessage()
const logs = ref<FetchLogResponse[]>([])
const feeds = ref<FeedResponse[]>([])
const loading = ref(false)
const detailLoading = ref(false)
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const totalPages = ref(0)
const selectedLog = ref<FetchLogResponse | null>(null)
const detailVisible = ref(false)
const searchKeyword = ref('')
const selectedFeedId = ref<number | null>(null)
const selectedStatus = ref<string | null>(null)
const selectedTimeRange = ref<[number, number] | null>(null)

const statusOptions: SelectOption[] = [
  { label: '全部状态', value: 'ALL' },
  { label: '成功', value: 'SUCCESS' },
  { label: '部分成功', value: 'PARTIAL' },
  { label: '失败', value: 'FAILED' },
  { label: '运行中', value: 'RUNNING' },
]

const feedOptions = computed<SelectOption[]>(() => [
  { label: '全部订阅源', value: 'ALL' },
  ...feeds.value.map((feed) => ({ label: feed.feedName, value: feed.id })),
])

const filteredLogs = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return logs.value
  }
  return logs.value.filter((log) => {
    return [
      log.feedName,
      log.requestUrl,
      log.status,
      log.errorMessage ?? '',
      classifyFailure(log).label,
    ].join(' ').toLowerCase().includes(keyword)
  })
})

const statusStats = computed(() => [
  { key: 'SUCCESS', label: '成功', value: logs.value.filter((log) => log.status === 'SUCCESS').length, tone: 'success' },
  { key: 'PARTIAL', label: '部分', value: logs.value.filter((log) => log.status === 'PARTIAL').length, tone: 'warning' },
  { key: 'FAILED', label: '失败', value: logs.value.filter((log) => log.status === 'FAILED').length, tone: 'error' },
  { key: 'RUNNING', label: '运行中', value: logs.value.filter((log) => log.status === 'RUNNING').length, tone: 'info' },
])

const currentPageStart = computed(() => {
  if (totalElements.value === 0) {
    return 0
  }
  return (currentPage.value - 1) * pageSize.value + 1
})

const currentPageEnd = computed(() => Math.min(currentPage.value * pageSize.value, totalElements.value))

const columns = computed<DataTableColumns<FetchLogResponse>>(() => [
  {
    title: '状态',
    key: 'status',
    width: 104,
    render: (row) => h(NTag, { bordered: false, type: getStatusTagType(row.status) }, () => getStatusLabel(row.status)),
  },
  {
    title: '失败类型',
    key: 'failureType',
    width: 124,
    render: (row) => {
      const failure = classifyFailure(row)
      return h(NTag, { bordered: false, type: failure.tone }, () => failure.label)
    },
  },
  {
    title: '订阅源',
    key: 'feedName',
    minWidth: 220,
    ellipsis: { tooltip: true },
  },
  {
    title: '数量',
    key: 'counts',
    width: 180,
    render: (row) => `${row.fetchedCount} / 新增 ${row.newCount} / 重复 ${row.duplicateCount}`,
  },
  {
    title: '失败',
    key: 'failedCount',
    width: 76,
  },
  {
    title: '耗时',
    key: 'durationMs',
    width: 92,
    render: (row) => row.durationMs == null ? '-' : `${row.durationMs}ms`,
  },
  {
    title: '开始时间',
    key: 'startedAt',
    width: 170,
    render: (row) => formatDateTime(row.startedAt),
  },
  {
    title: '错误信息',
    key: 'errorMessage',
    minWidth: 260,
    ellipsis: { tooltip: true },
    render: (row) => row.errorMessage || '-',
  },
  {
    title: '操作',
    key: 'actions',
    width: 96,
    render: (row) => h(NButton, { size: 'small', secondary: true, onClick: () => openLogDetail(row) }, () => '详情'),
  },
])

/**
 * 加载抓取日志和订阅源选项。
 */
async function loadFetchLogPage() {
  loading.value = true
  try {
    const [logPage, feedPage] = await Promise.all([
      fetchFetchLogs(currentPage.value - 1, pageSize.value, buildQuery()),
      fetchFeeds(0, 100),
    ])
    logs.value = logPage.items
    totalElements.value = logPage.totalElements
    totalPages.value = logPage.totalPages
    feeds.value = feedPage.items
  } catch (error) {
    message.error(error instanceof Error ? error.message : '抓取日志查询失败')
  } finally {
    loading.value = false
  }
}

/**
 * 筛选条件变化后回到第一页并重新查询。
 */
function reloadFirstPage() {
  currentPage.value = 1
  void loadFetchLogPage()
}

/**
 * 切换日志页码。
 */
function changePage(page: number) {
  currentPage.value = page
  void loadFetchLogPage()
}

/**
 * 切换每页条数。
 */
function changePageSize(size: number) {
  pageSize.value = size
  currentPage.value = 1
  void loadFetchLogPage()
}

/**
 * 打开抓取日志详情抽屉。
 */
async function openLogDetail(log: FetchLogResponse) {
  detailVisible.value = true
  detailLoading.value = true
  selectedLog.value = log
  try {
    selectedLog.value = await fetchFetchLogDetail(log.id)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '抓取日志详情查询失败')
  } finally {
    detailLoading.value = false
  }
}

/**
 * 重置筛选条件。
 */
function resetFilters() {
  selectedFeedId.value = null
  selectedStatus.value = null
  selectedTimeRange.value = null
  searchKeyword.value = ''
  reloadFirstPage()
}

/**
 * 构造日志查询参数。
 */
function buildQuery(): FetchLogQuery {
  return {
    feedId: selectedFeedId.value,
    status: selectedStatus.value && selectedStatus.value !== 'ALL' ? selectedStatus.value : null,
    startedFrom: selectedTimeRange.value ? new Date(selectedTimeRange.value[0]).toISOString() : null,
    startedTo: selectedTimeRange.value ? new Date(selectedTimeRange.value[1]).toISOString() : null,
  }
}

/**
 * 判断日志失败类型，便于人工快速定位问题。
 */
function classifyFailure(log: FetchLogResponse) {
  const messageText = `${log.errorMessage ?? ''} ${log.errorStack ?? ''}`.toLowerCase()
  if (log.status === 'SUCCESS') {
    return { label: '正常', tone: 'success' as const, icon: Clock }
  }
  if (messageText.includes('timeout') || messageText.includes('handshake') || messageText.includes('unknownhost') || messageText.includes('connect') || messageText.includes('remote host')) {
    return { label: '网络失败', tone: 'error' as const, icon: ServerCrash }
  }
  if (log.status === 'PARTIAL' || messageText.includes('字段缺失') || messageText.includes('未命中') || messageText.includes('template')) {
    return { label: '模板字段', tone: 'warning' as const, icon: FileWarning }
  }
  if (messageText.includes('parse') || messageText.includes('解析') || messageText.includes('xml') || messageText.includes('rss')) {
    return { label: '解析失败', tone: 'error' as const, icon: AlertTriangle }
  }
  if (log.status === 'FAILED') {
    return { label: '抓取失败', tone: 'error' as const, icon: AlertTriangle }
  }
  return { label: '运行中', tone: 'info' as const, icon: Clock }
}

/**
 * 获取状态标签类型。
 */
function getStatusTagType(status: string) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'PARTIAL') {
    return 'warning'
  }
  if (status === 'FAILED') {
    return 'error'
  }
  return 'info'
}

/**
 * 获取状态中文文案。
 */
function getStatusLabel(status: string) {
  const labels: Record<string, string> = {
    SUCCESS: '成功',
    PARTIAL: '部分',
    FAILED: '失败',
    RUNNING: '运行中',
  }
  return labels[status] ?? status
}

/**
 * 格式化接口返回时间。
 */
function formatDateTime(value: string | null) {
  if (!value) {
    return '-'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

onMounted(loadFetchLogPage)
</script>

<template>
  <section class="fetch-log-view">
    <header class="view-toolbar">
      <div>
        <h3>抓取日志排障台</h3>
        <p>共 {{ totalElements }} 条日志，当前第 {{ currentPage }} / {{ totalPages || 1 }} 页，展示 {{ currentPageStart }}-{{ currentPageEnd }} 条。</p>
      </div>
      <NButton secondary type="primary" :loading="loading" @click="loadFetchLogPage">
        <template #icon>
          <RefreshCw :size="16" />
        </template>
        刷新
      </NButton>
    </header>

    <section class="status-strip" aria-label="抓取状态概览">
      <article v-for="item in statusStats" :key="item.key" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="filter-panel">
      <NInput v-model:value="searchKeyword" clearable placeholder="搜索源、URL、错误信息">
        <template #prefix>
          <Search :size="16" />
        </template>
      </NInput>
      <NSelect
        v-model:value="selectedFeedId"
        clearable
        :options="feedOptions"
        placeholder="全部订阅源"
        :fallback-option="false"
        @update:value="reloadFirstPage"
      />
      <NSelect
        v-model:value="selectedStatus"
        clearable
        :options="statusOptions"
        placeholder="全部状态"
        :fallback-option="false"
        @update:value="reloadFirstPage"
      />
      <NDatePicker
        v-model:value="selectedTimeRange"
        clearable
        type="datetimerange"
        @update:value="reloadFirstPage"
      />
      <NButton secondary @click="resetFilters">
        <template #icon>
          <Filter :size="16" />
        </template>
        重置
      </NButton>
    </section>

    <section class="log-table-panel">
      <NSpin :show="loading">
        <NEmpty v-if="filteredLogs.length === 0" description="暂无匹配日志" />
        <NDataTable
          v-else
          remote
          :columns="columns"
          :data="filteredLogs"
          :bordered="false"
          :single-line="false"
          :row-key="(row) => row.id"
          :scroll-x="1320"
        />
        <div v-if="totalElements > 0" class="pagination-row">
          <span>每页 {{ pageSize }} 条</span>
          <NPagination
            :page="currentPage"
            :page-size="pageSize"
            :item-count="totalElements"
            show-size-picker
            :page-sizes="[10, 20, 50, 100]"
            @update:page="changePage"
            @update:page-size="changePageSize"
          />
        </div>
      </NSpin>
    </section>

    <NDrawer v-model:show="detailVisible" :width="560" placement="right">
      <NDrawerContent title="抓取日志详情" closable>
        <NSpin :show="detailLoading">
          <NEmpty v-if="!selectedLog" description="请选择一条日志" />
          <div v-else class="detail-body">
            <section class="detail-hero">
              <component :is="classifyFailure(selectedLog).icon" :size="22" />
              <div>
                <h3>{{ selectedLog.feedName }}</h3>
                <p>{{ selectedLog.requestUrl }}</p>
              </div>
              <NTag :bordered="false" :type="classifyFailure(selectedLog).tone">
                {{ classifyFailure(selectedLog).label }}
              </NTag>
            </section>

            <div class="detail-grid">
              <div><span>状态</span><b>{{ getStatusLabel(selectedLog.status) }}</b></div>
              <div><span>HTTP</span><b>{{ selectedLog.httpStatus ?? '-' }}</b></div>
              <div><span>抓取</span><b>{{ selectedLog.fetchedCount }}</b></div>
              <div><span>新增</span><b>{{ selectedLog.newCount }}</b></div>
              <div><span>重复</span><b>{{ selectedLog.duplicateCount }}</b></div>
              <div><span>失败</span><b>{{ selectedLog.failedCount }}</b></div>
              <div><span>耗时</span><b>{{ selectedLog.durationMs == null ? '-' : `${selectedLog.durationMs}ms` }}</b></div>
              <div><span>开始</span><b>{{ formatDateTime(selectedLog.startedAt) }}</b></div>
            </div>

            <section class="detail-section">
              <h4>错误信息</h4>
              <pre>{{ selectedLog.errorMessage || '无错误信息。' }}</pre>
            </section>

            <section class="detail-section">
              <h4>错误详情</h4>
              <pre>{{ selectedLog.errorStack || '无错误详情。' }}</pre>
            </section>

            <section class="detail-section">
              <h4>原始响应样本</h4>
              <pre>{{ selectedLog.rawResponseSample || '无原始响应样本。' }}</pre>
            </section>
          </div>
        </NSpin>
      </NDrawerContent>
    </NDrawer>
  </section>
</template>

<style scoped>
.fetch-log-view {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.view-toolbar,
.filter-panel,
.log-table-panel,
.status-strip article {
  border: 1px solid #243044;
  border-radius: 8px;
  background: linear-gradient(180deg, rgb(17 24 39 / 0.96), rgb(15 23 42 / 0.92));
  box-shadow: 0 18px 40px rgb(0 0 0 / 0.18);
}

.view-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px;
}

.view-toolbar h3 {
  margin: 0;
  color: #e5e7eb;
  font-size: 18px;
}

.view-toolbar p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 16px;
}

.status-strip article {
  min-height: 88px;
  padding: 14px 16px;
}

.status-strip span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.status-strip strong {
  display: block;
  margin-top: 8px;
  color: #e5e7eb;
  font-size: 30px;
  line-height: 1;
}

.success {
  color: #a3e635;
}

.warning {
  color: #f59e0b;
}

.error {
  color: #fb7185;
}

.info {
  color: #22d3ee;
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 150px 320px auto;
  gap: 12px;
  padding: 14px;
}

.log-table-panel {
  min-width: 0;
  overflow-x: auto;
  padding: 16px;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px solid rgb(36 48 68 / 0.72);
  margin-top: 14px;
  padding-top: 14px;
}

.pagination-row span {
  color: #94a3b8;
  font-size: 12px;
}

.detail-body {
  display: grid;
  gap: 16px;
}

.detail-hero {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  border: 1px solid rgb(36 48 68 / 0.82);
  border-radius: 8px;
  background: rgb(15 23 42 / 0.72);
  padding: 14px;
}

.detail-hero h3 {
  margin: 0;
  color: #e5e7eb;
  font-size: 17px;
}

.detail-hero p {
  margin: 5px 0 0;
  overflow: hidden;
  color: #94a3b8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.detail-grid div {
  border: 1px solid rgb(36 48 68 / 0.65);
  border-radius: 8px;
  background: rgb(15 23 42 / 0.58);
  padding: 10px 12px;
}

.detail-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.detail-grid b {
  display: block;
  margin-top: 5px;
  color: #e5e7eb;
}

.detail-section h4 {
  margin: 0 0 8px;
  color: #e5e7eb;
}

.detail-section pre {
  max-height: 220px;
  overflow: auto;
  border: 1px solid #243044;
  border-radius: 8px;
  background: #0b1120;
  color: #cbd5e1;
  padding: 12px;
  white-space: pre-wrap;
}

@media (max-width: 1120px) {
  .filter-panel {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .view-toolbar,
  .filter-panel,
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .status-strip,
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
