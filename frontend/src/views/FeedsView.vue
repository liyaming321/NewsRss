<script setup lang="ts">
import {
  AlertTriangle,
  Download,
  Plus,
  Power,
  RefreshCw,
  RotateCw,
  Rss,
  Save,
  Search,
  Trash2,
  Upload,
  WandSparkles,
} from '@lucide/vue'
import {
  NAlert,
  NButton,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NSelect,
  NSpace,
  NSpin,
  NSwitch,
  NTag,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type SelectOption,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { fetchDictionaryItems, type DictionaryResponse } from '../api/dictionaries'
import {
  createFeed,
  deleteFeed,
  detectFeed,
  fetchFeeds,
  refreshFeed,
  updateFeed,
  type FeedDetectResponse,
  type FeedRequest,
  type FeedResponse,
} from '../api/feeds'
import { fetchParserTemplates, type ParserTemplateResponse } from '../api/parserTemplates'

type FeedFormModel = FeedRequest
type HealthTone = 'success' | 'warning' | 'error' | 'info' | 'default'

const message = useMessage()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const saving = ref(false)
const detecting = ref(false)
const refreshingId = ref<number | null>(null)
const feeds = ref<FeedResponse[]>([])
const totalFeeds = ref(0)
const parserTemplates = ref<ParserTemplateResponse[]>([])
const feedCategories = ref<DictionaryResponse[]>([])
const searchKeyword = ref('')
const drawerVisible = ref(false)
const deleteModalVisible = ref(false)
const selectedFeed = ref<FeedResponse | null>(null)
const detectedFeed = ref<FeedDetectResponse | null>(null)
const formModel = reactive<FeedFormModel>(createEmptyFeedForm())

const templateOptions = computed<SelectOption[]>(() => [
  { label: '默认解析', value: 'default' },
  ...parserTemplates.value
    .filter((template) => template.enabled)
    .map((template) => ({
      label: `${template.templateName} · ${template.templateCode}`,
      value: template.id,
    })),
])

const categoryOptions = computed<SelectOption[]>(() => feedCategories.value
  .filter((category) => category.enabled)
  .map((category) => ({
    label: category.itemLabel,
    value: category.itemCode,
  })))

const categoryLabelMap = computed(() => new Map(
  feedCategories.value.map((category) => [category.itemCode, category.itemLabel]),
))

const filteredFeeds = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return feeds.value
  }
  return feeds.value.filter((feed) => {
    return [
      feed.feedName,
      feed.feedUrl,
      getCategoryLabel(feed.category),
      feed.parserTemplateCode ?? '',
      feed.healthStatus,
    ].join(' ').toLowerCase().includes(keyword)
  })
})

const healthStats = computed(() => {
  const counters = {
    healthy: 0,
    warning: 0,
    error: 0,
    unknown: 0,
    disabled: 0,
  }
  feeds.value.forEach((feed) => {
    if (!feed.enabled) {
      counters.disabled += 1
      return
    }
    if (feed.healthStatus === 'HEALTHY') {
      counters.healthy += 1
      return
    }
    if (feed.healthStatus === 'WARNING') {
      counters.warning += 1
      return
    }
    if (feed.healthStatus === 'ERROR') {
      counters.error += 1
      return
    }
    counters.unknown += 1
  })
  return [
    { key: 'healthy', label: '健康', value: counters.healthy, tone: 'success' as const },
    { key: 'warning', label: '警告', value: counters.warning, tone: 'warning' as const },
    { key: 'error', label: '异常', value: counters.error, tone: 'error' as const },
    { key: 'unknown', label: '未知', value: counters.unknown, tone: 'info' as const },
    { key: 'disabled', label: '停用', value: counters.disabled, tone: 'default' as const },
  ]
})

const riskItems = computed(() => {
  const risks: Array<{ label: string; tone: HealthTone }> = []
  if (!formModel.feedUrl.trim()) {
    risks.push({ label: 'RSS 地址为空，无法保存。', tone: 'error' })
  } else if (!/^https?:\/\//i.test(formModel.feedUrl.trim())) {
    risks.push({ label: '地址不是 HTTP/HTTPS URL，探测可能失败。', tone: 'warning' })
  }
  if (!detectedFeed.value) {
    risks.push({ label: '尚未自动探测，建议先确认标题和条目数量。', tone: 'warning' })
  }
  if (!formModel.parserTemplateId) {
    risks.push({ label: '未绑定解析模板，将使用默认解析逻辑。', tone: 'info' })
  }
  if (formModel.fetchIntervalMinutes < 15) {
    risks.push({ label: '抓取频率较高，可能增加远端压力。', tone: 'warning' })
  }
  if (!formModel.enabled) {
    risks.push({ label: '当前配置为停用，保存后不会自动抓取。', tone: 'info' })
  }
  return risks
})

const columns = computed<DataTableColumns<FeedResponse>>(() => [
  {
    title: '订阅源',
    key: 'feedName',
    minWidth: 280,
    render: (feed) => h('div', { class: 'feed-name-cell' }, [
      h('b', feed.feedName),
      h('span', feed.feedUrl),
    ]),
  },
  {
    title: '健康',
    key: 'healthStatus',
    width: 112,
    render: (feed) => h(NTag, { bordered: false, type: getHealthTagType(feed) }, () => getHealthLabel(feed)),
  },
  {
    title: '分类',
    key: 'category',
    width: 120,
    render: (feed) => getCategoryLabel(feed.category),
  },
  {
    title: '解析模板',
    key: 'parserTemplateCode',
    width: 150,
    render: (feed) => feed.parserTemplateCode || '默认解析',
  },
  {
    title: '频率',
    key: 'fetchIntervalMinutes',
    width: 110,
    render: (feed) => `${feed.fetchIntervalMinutes} 分钟`,
  },
  {
    title: '文章',
    key: 'articleCount',
    width: 90,
    render: (feed) => formatNumber(feed.articleCount),
  },
  {
    title: '最近成功',
    key: 'lastSuccessAt',
    width: 140,
    render: (feed) => formatDateTime(feed.lastSuccessAt),
  },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    render: (feed) => h(NSpace, { size: 8, wrap: false }, () => [
      h(NButton, { size: 'small', secondary: true, onClick: () => openEditDrawer(feed) }, () => '编辑'),
      h(NButton, {
        size: 'small',
        secondary: true,
        type: feed.enabled ? 'warning' : 'success',
        onClick: () => toggleFeedEnabled(feed),
      }, {
        icon: () => h(Power, { size: 14 }),
        default: () => feed.enabled ? '停用' : '启用',
      }),
      h(NButton, {
        size: 'small',
        secondary: true,
        type: 'primary',
        loading: refreshingId.value === feed.id,
        onClick: () => refreshSingleFeed(feed),
      }, {
        icon: () => h(RotateCw, { size: 14 }),
        default: () => '刷新',
      }),
      h(NButton, { size: 'small', secondary: true, type: 'error', onClick: () => openDeleteModal(feed) }, {
        icon: () => h(Trash2, { size: 14 }),
        default: () => '删除',
      }),
    ]),
  },
])

/**
 * 创建空白订阅源表单。
 */
function createEmptyFeedForm(): FeedFormModel {
  return {
    feedName: '',
    feedUrl: '',
    category: '',
    iconUrl: '',
    parserTemplateId: null,
    fetchIntervalMinutes: 60,
    enabled: true,
  }
}

/**
 * 加载订阅源和解析模板数据。
 */
async function loadFeedsPage() {
  loading.value = true
  try {
    const [feedPage, templates, categories] = await Promise.all([
      fetchFeeds(0, 80),
      fetchParserTemplates(),
      fetchDictionaryItems('FEED_CATEGORY', true),
    ])
    feeds.value = feedPage.items
    totalFeeds.value = feedPage.totalElements
    parserTemplates.value = templates
    feedCategories.value = categories
  } catch (error) {
    message.error(error instanceof Error ? error.message : '订阅源数据加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 打开新增订阅源抽屉。
 */
function openCreateDrawer() {
  selectedFeed.value = null
  detectedFeed.value = null
  Object.assign(formModel, createEmptyFeedForm())
  drawerVisible.value = true
}

/**
 * 打开编辑订阅源抽屉。
 */
function openEditDrawer(feed: FeedResponse) {
  selectedFeed.value = feed
  detectedFeed.value = null
  Object.assign(formModel, {
    feedName: feed.feedName,
    feedUrl: feed.feedUrl,
    category: feed.category ?? '',
    iconUrl: feed.iconUrl ?? '',
    parserTemplateId: feed.parserTemplateId,
    fetchIntervalMinutes: feed.fetchIntervalMinutes,
    enabled: feed.enabled,
  })
  drawerVisible.value = true
}

/**
 * 自动探测 RSS 源，并把元数据回填到表单。
 */
async function detectCurrentFeed() {
  if (!formModel.feedUrl.trim()) {
    message.warning('请先输入 RSS 地址')
    return
  }
  detecting.value = true
  try {
    const result = await detectFeed(formModel.feedUrl.trim())
    detectedFeed.value = result
    if (!formModel.feedName.trim() && result.title) {
      formModel.feedName = result.title
    }
    message.success(`探测成功，发现 ${result.itemCount} 条条目`)
  } catch (error) {
    detectedFeed.value = null
    message.error(error instanceof Error ? error.message : '自动探测失败，请检查 RSS 地址')
  } finally {
    detecting.value = false
  }
}

/**
 * 保存新增或编辑后的订阅源。
 */
async function saveFeed() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = normalizeFeedRequest(formModel)
    if (selectedFeed.value) {
      await updateFeed(selectedFeed.value.id, payload)
      message.success('订阅源已更新')
    } else {
      await createFeed(payload)
      message.success('订阅源已新增')
    }
    drawerVisible.value = false
    await loadFeedsPage()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '订阅源保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 切换订阅源启用状态。
 */
async function toggleFeedEnabled(feed: FeedResponse) {
  try {
    await updateFeed(feed.id, normalizeFeedRequest({
      feedName: feed.feedName,
      feedUrl: feed.feedUrl,
      category: feed.category,
      iconUrl: feed.iconUrl,
      parserTemplateId: feed.parserTemplateId,
      fetchIntervalMinutes: feed.fetchIntervalMinutes,
      enabled: !feed.enabled,
    }))
    message.success(feed.enabled ? '订阅源已停用' : '订阅源已启用')
    await loadFeedsPage()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '订阅源状态更新失败')
  }
}

/**
 * 手动刷新单个订阅源。
 */
async function refreshSingleFeed(feed: FeedResponse) {
  refreshingId.value = feed.id
  try {
    const result = await refreshFeed(feed.id)
    if (result.success) {
      message.success(`刷新完成：新增 ${result.newCount}，重复 ${result.duplicateCount}`)
    } else {
      message.error(result.errorMessage || '刷新失败')
    }
    await loadFeedsPage()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '订阅源刷新失败')
  } finally {
    refreshingId.value = null
  }
}

/**
 * 打开删除确认弹窗。
 */
function openDeleteModal(feed: FeedResponse) {
  selectedFeed.value = feed
  deleteModalVisible.value = true
}

/**
 * 确认删除订阅源。
 */
async function confirmDeleteFeed() {
  if (!selectedFeed.value) {
    return
  }
  saving.value = true
  try {
    await deleteFeed(selectedFeed.value.id)
    message.success('订阅源已删除')
    deleteModalVisible.value = false
    await loadFeedsPage()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '订阅源删除失败')
  } finally {
    saving.value = false
  }
}

/**
 * 归一化保存请求，避免把空字符串传给后端。
 */
function normalizeFeedRequest(model: FeedFormModel): FeedRequest {
  return {
    feedName: model.feedName.trim(),
    feedUrl: model.feedUrl.trim(),
    category: normalizeOptionalText(model.category),
    iconUrl: normalizeOptionalText(model.iconUrl),
    parserTemplateId: model.parserTemplateId,
    fetchIntervalMinutes: model.fetchIntervalMinutes,
    enabled: model.enabled,
  }
}

/**
 * 将可选文本统一转为空值或去空白字符串。
 */
function normalizeOptionalText(value: string | null) {
  if (!value || !value.trim()) {
    return null
  }
  return value.trim()
}

/**
 * 获取订阅源分类展示名称，避免把内部编码直接暴露给用户。
 */
function getCategoryLabel(categoryCode: string | null) {
  if (!categoryCode) {
    return '未分组'
  }
  return categoryLabelMap.value.get(categoryCode) ?? categoryCode
}

/**
 * 格式化日期时间。
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
    hour12: false,
  }).format(new Date(value))
}

/**
 * 格式化数字。
 */
function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value)
}

/**
 * 获取健康状态标签文案。
 */
function getHealthLabel(feed: FeedResponse) {
  if (!feed.enabled) {
    return '停用'
  }
  const labels: Record<string, string> = {
    HEALTHY: '健康',
    WARNING: '警告',
    ERROR: '异常',
    UNKNOWN: '未知',
  }
  return labels[feed.healthStatus] ?? feed.healthStatus
}

/**
 * 获取健康状态标签样式。
 */
function getHealthTagType(feed: FeedResponse) {
  if (!feed.enabled) {
    return 'default'
  }
  if (feed.healthStatus === 'HEALTHY') {
    return 'success'
  }
  if (feed.healthStatus === 'WARNING') {
    return 'warning'
  }
  if (feed.healthStatus === 'ERROR') {
    return 'error'
  }
  return 'info'
}

/**
 * 获取风险提示标签样式。
 */
function getRiskTagType(tone: HealthTone) {
  return tone === 'default' ? undefined : tone
}

onMounted(loadFeedsPage)

watch(() => formModel.feedUrl, () => {
  detectedFeed.value = null
})
</script>

<template>
  <section class="feeds-view">
    <header class="feeds-toolbar">
      <div>
        <h3>订阅源矩阵</h3>
        <p>{{ totalFeeds }} 个源接入，按健康、模板和抓取状态管理。</p>
      </div>
      <div class="toolbar-actions">
        <NButton secondary @click="message.info('OPML 导入入口已预留，后续接入文件解析')">
          <template #icon>
            <Upload :size="16" />
          </template>
          OPML 导入
        </NButton>
        <NButton secondary @click="message.info('OPML 导出入口已预留，后续接入导出接口')">
          <template #icon>
            <Download :size="16" />
          </template>
          OPML 导出
        </NButton>
        <NButton secondary type="primary" :loading="loading" @click="loadFeedsPage">
          <template #icon>
            <RefreshCw :size="16" />
          </template>
          刷新
        </NButton>
        <NButton class="create-feed-button" type="primary" @click="openCreateDrawer">
          <template #icon>
            <Plus :size="16" />
          </template>
          新增订阅源
        </NButton>
      </div>
    </header>

    <section class="health-strip" aria-label="订阅源健康概览">
      <article v-for="item in healthStats" :key="item.key" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="feed-console">
      <div class="table-tools">
        <NInput v-model:value="searchKeyword" clearable placeholder="搜索名称、URL、分类、模板">
          <template #prefix>
            <Search :size="16" />
          </template>
        </NInput>
        <NTag :bordered="false" type="info">API</NTag>
      </div>

      <NSpin :show="loading">
        <NEmpty v-if="filteredFeeds.length === 0" description="暂无订阅源">
          <template #extra>
            <NButton type="primary" @click="openCreateDrawer">
              <template #icon>
                <Plus :size="16" />
              </template>
              新增订阅源
            </NButton>
          </template>
        </NEmpty>
        <NDataTable
          v-else
          :columns="columns"
          :data="filteredFeeds"
          :pagination="{ pageSize: 12 }"
          :scroll-x="1460"
          size="small"
        />
      </NSpin>
    </section>

    <NDrawer v-model:show="drawerVisible" :width="520" placement="right">
      <NDrawerContent :title="selectedFeed ? '编辑订阅源' : '新增订阅源'" closable>
        <div class="drawer-body">
          <NForm ref="formRef" :model="formModel" label-placement="top">
            <NFormItem
              label="RSS 地址"
              path="feedUrl"
              :rule="{ required: true, message: '请输入 RSS 地址', trigger: ['blur', 'input'] }"
            >
              <div class="detect-line">
                <NInput v-model:value="formModel.feedUrl" placeholder="https://example.com/feed.xml" />
                <NButton secondary type="primary" :loading="detecting" @click="detectCurrentFeed">
                  <template #icon>
                    <WandSparkles :size="16" />
                  </template>
                  探测
                </NButton>
              </div>
            </NFormItem>

            <NAlert v-if="detectedFeed" type="success" :bordered="false" class="detect-card">
              <template #icon>
                <Rss :size="18" />
              </template>
              <b>{{ detectedFeed.title || '未命名 Feed' }}</b>
              <span>{{ detectedFeed.siteUrl || detectedFeed.feedUrl }}</span>
              <em>{{ detectedFeed.language || '未知语言' }} · {{ detectedFeed.itemCount }} 条条目</em>
            </NAlert>

            <NFormItem
              label="订阅源名称"
              path="feedName"
              :rule="{ required: true, message: '请输入订阅源名称', trigger: ['blur', 'input'] }"
            >
              <NInput v-model:value="formModel.feedName" placeholder="例如：The GitHub Blog" />
            </NFormItem>

            <div class="form-grid">
              <NFormItem label="分组">
                <NSelect
                  v-model:value="formModel.category"
                  clearable
                  :options="categoryOptions"
                  placeholder="请选择订阅源分类"
                  :fallback-option="false"
                />
              </NFormItem>
              <NFormItem label="抓取频率">
                <NInputNumber v-model:value="formModel.fetchIntervalMinutes" :min="1" :max="1440" :step="5">
                  <template #suffix>分钟</template>
                </NInputNumber>
              </NFormItem>
            </div>

            <NFormItem label="解析模板">
              <NSelect
                v-model:value="formModel.parserTemplateId"
                clearable
                :options="templateOptions"
                placeholder="默认解析"
                :fallback-option="false"
              />
            </NFormItem>

            <NFormItem label="图标 URL">
              <NInput v-model:value="formModel.iconUrl" placeholder="可选，用于后续展示源头像" />
            </NFormItem>

            <NFormItem label="启用抓取">
              <NSwitch v-model:value="formModel.enabled">
                <template #checked>启用</template>
                <template #unchecked>停用</template>
              </NSwitch>
            </NFormItem>
          </NForm>

          <section class="risk-panel">
            <div>
              <AlertTriangle :size="17" />
              <b>保存前风险</b>
            </div>
            <NTag
              v-for="risk in riskItems"
              :key="risk.label"
              :bordered="false"
              :type="getRiskTagType(risk.tone)"
            >
              {{ risk.label }}
            </NTag>
            <NTag v-if="riskItems.length === 0" :bordered="false" type="success">配置看起来稳定，可以保存。</NTag>
          </section>
        </div>

        <template #footer>
          <NSpace justify="space-between" class="drawer-footer">
            <NButton secondary @click="drawerVisible = false">取消</NButton>
            <NButton type="primary" :loading="saving" @click="saveFeed">
              <template #icon>
                <Save :size="16" />
              </template>
              保存
            </NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>

    <NModal v-model:show="deleteModalVisible" preset="dialog" title="删除订阅源" positive-text="删除" negative-text="取消" :loading="saving" @positive-click="confirmDeleteFeed">
      <p class="delete-copy">
        删除后会移除该源及关联文章数据。当前选择：{{ selectedFeed?.feedName || '-' }}
      </p>
    </NModal>
  </section>
</template>

<style scoped>
.feeds-view {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.feeds-toolbar,
.feed-console,
.health-strip article {
  border: 1px solid rgb(36 48 68 / 0.95);
  border-radius: 8px;
  background: linear-gradient(180deg, rgb(17 24 39 / 0.96), rgb(15 23 42 / 0.96));
  box-shadow: 0 18px 40px rgb(0 0 0 / 0.18);
}

.feeds-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
}

.feeds-toolbar h3 {
  margin: 0;
  color: #e5e7eb;
  font-size: 18px;
  letter-spacing: 0;
}

.feeds-toolbar p {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.create-feed-button {
  min-width: 124px;
  font-weight: 800;
}

.health-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 16px;
}

.health-strip article {
  position: relative;
  min-height: 92px;
  overflow: hidden;
  padding: 15px 16px;
}

.health-strip article::after {
  position: absolute;
  right: 16px;
  bottom: 14px;
  width: 42px;
  height: 54px;
  border-radius: 8px;
  background: currentColor;
  opacity: 0.13;
  content: "";
}

.health-strip span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.health-strip strong {
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

.default {
  color: #94a3b8;
}

.feed-console {
  min-width: 0;
  overflow-x: auto;
  padding: 18px;
}

.table-tools {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

:deep(.n-data-table) {
  --n-td-color: rgb(17 24 39 / 0.72);
  --n-th-color: #0f172a;
}

:deep(.n-data-table-th) {
  font-weight: 800;
}

.feed-name-cell {
  display: grid;
  gap: 5px;
}

.feed-name-cell b {
  color: #e5e7eb;
  font-size: 13px;
}

.feed-name-cell span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-body {
  display: grid;
  gap: 16px;
}

.detect-line {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: 100%;
}

.detect-card {
  margin-bottom: 4px;
}

.detect-card b,
.detect-card span,
.detect-card em {
  display: block;
}

.detect-card span,
.detect-card em {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 14px;
}

.risk-panel {
  display: grid;
  gap: 10px;
  border: 1px solid rgb(245 158 11 / 0.24);
  border-radius: 8px;
  background: rgb(245 158 11 / 0.07);
  padding: 14px;
}

.risk-panel div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f59e0b;
}

.drawer-footer {
  width: 100%;
}

.delete-copy {
  margin: 0;
  color: #cbd5e1;
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .feeds-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .health-strip {
    grid-template-columns: repeat(3, minmax(120px, 1fr));
  }
}

@media (max-width: 760px) {
  .table-tools,
  .detect-line,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .health-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
