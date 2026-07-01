<script setup lang="ts">
import { RefreshCw } from '@lucide/vue'
import { NButton, NEmpty, NSpin, NTag, useMessage } from 'naive-ui'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchDashboardSummary,
  fetchFeedHealth,
  fetchRecentArticles,
  type ArticleListItemResponse,
  type DashboardSummaryResponse,
  type FeedHealthResponse,
} from '../api/dashboard'
import { fetchFetchLogs, type FetchLogResponse } from '../api/fetchLogs'

const message = useMessage()
const router = useRouter()
const loading = ref(false)
const summary = ref<DashboardSummaryResponse | null>(null)
const feedHealth = ref<FeedHealthResponse | null>(null)
const recentArticles = ref<ArticleListItemResponse[]>([])
const recentLogs = ref<FetchLogResponse[]>([])

const totalHealthCount = computed(() => {
  if (!feedHealth.value) {
    return 0
  }
  return feedHealth.value.unknown + feedHealth.value.healthy + feedHealth.value.warning + feedHealth.value.error
})

const metrics = computed(() => [
  {
    label: '今日新增',
    value: formatNumber(summary.value?.todayNewArticleCount ?? 0),
    hint: '按 UTC 今日入库统计',
    tone: 'cyan',
  },
  {
    label: '未读文章',
    value: formatNumber(summary.value?.unreadCount ?? 0),
    hint: `${formatNumber(summary.value?.articleCount ?? 0)} 篇文章总量`,
    tone: 'lime',
  },
  {
    label: '活跃订阅源',
    value: formatNumber(summary.value?.enabledFeedCount ?? 0),
    hint: `${formatNumber(summary.value?.feedCount ?? 0)} 个源已接入`,
    tone: 'amber',
  },
  {
    label: '异常源',
    value: formatNumber((feedHealth.value?.warning ?? 0) + (feedHealth.value?.error ?? 0)),
    hint: `${formatNumber(summary.value?.failedFetchLogCount ?? 0)} 条失败日志`,
    tone: 'coral',
  },
])

const pipelineSteps = computed(() => [
  {
    index: '01',
    title: '抓取',
    detail: `${formatNumber(summary.value?.enabledFeedCount ?? 0)} 源轮询`,
  },
  {
    index: '02',
    title: '解析',
    detail: `${formatNumber(recentLogs.value.filter((log) => log.status !== 'FAILED').length)} 次成功`,
  },
  {
    index: '03',
    title: '去重',
    detail: `${formatNumber(recentLogs.value.reduce((sum, log) => sum + log.duplicateCount, 0))} 条重复`,
  },
  {
    index: '04',
    title: '入库',
    detail: `新增 ${formatNumber(recentLogs.value.reduce((sum, log) => sum + log.newCount, 0))}`,
  },
])

const healthRows = computed(() => [
  { label: '健康', value: feedHealth.value?.healthy ?? 0, tone: 'success' },
  { label: '警告', value: feedHealth.value?.warning ?? 0, tone: 'amber' },
  { label: '错误', value: feedHealth.value?.error ?? 0, tone: 'coral' },
  { label: '未知', value: feedHealth.value?.unknown ?? 0, tone: 'cyan' },
])

const hotKeywords = computed(() => {
  const stopWords = new Set([
    'the',
    'and',
    'for',
    'with',
    'from',
    'this',
    'that',
    'you',
    'your',
    'are',
    'was',
    'rss',
    'http',
    'https',
    'href',
    'url',
    'article',
    'comments',
    'points',
  ])
  const counter = new Map<string, number>()
  recentArticles.value.forEach((article) => {
    const text = `${article.title} ${sanitizePlainText(article.summary ?? '')}`
    text
      .split(/[\s,，.。:：;；!?！？()[\]{}"'“”‘’/\\|]+/)
      .map((word) => word.trim())
      .filter((word) => word.length >= 3)
      .filter((word) => !/[<>=]/.test(word))
      .filter((word) => !/^https?$/i.test(word))
      .filter((word) => !/^(www\.|[a-z0-9-]+\.(com|org|net|dev|io|ai))/i.test(word))
      .filter((word) => !stopWords.has(word.toLowerCase()))
      .forEach((word) => counter.set(word, (counter.get(word) ?? 0) + 1))
  })

  return Array.from(counter.entries())
    .sort((left, right) => right[1] - left[1])
    .slice(0, 8)
    .map(([word], index) => ({ word, tone: ['cyan', 'lime', 'amber', 'violet', 'coral'][index % 5] }))
})

/**
 * 加载驾驶舱需要的全部数据。
 */
async function loadDashboard() {
  loading.value = true
  try {
    const [summaryData, healthData, articlesData, logsPage] = await Promise.all([
      fetchDashboardSummary(),
      fetchFeedHealth(),
      fetchRecentArticles(4),
      fetchFetchLogs(0, 5),
    ])
    summary.value = summaryData
    feedHealth.value = healthData
    recentArticles.value = articlesData
    recentLogs.value = logsPage.items
  } catch (error) {
    message.error(error instanceof Error ? error.message : '驾驶舱数据加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到阅读页并打开指定文章详情。
 */
function openArticleInReader(article: ArticleListItemResponse) {
  void router.push({
    name: 'reader',
    query: {
      articleId: String(article.id),
      feedId: String(article.feedId),
    },
  })
}

/**
 * 格式化数字，避免大数字难以扫读。
 */
function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value)
}

/**
 * 格式化文章或日志时间。
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
 * 计算健康状态进度条比例。
 */
function getHealthPercent(value: number) {
  if (totalHealthCount.value <= 0) {
    return 0
  }
  return Math.max(4, Math.round((value / totalHealthCount.value) * 100))
}

/**
 * 获取日志状态中文文案。
 */
function getLogStatusLabel(status: string) {
  const labels: Record<string, string> = {
    SUCCESS: '成功',
    PARTIAL: '部分成功',
    FAILED: '失败',
    RUNNING: '运行中',
  }
  return labels[status] ?? status
}

/**
 * 获取日志状态色调。
 */
function getLogTone(status: string) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'PARTIAL') {
    return 'amber'
  }
  if (status === 'FAILED') {
    return 'coral'
  }
  return 'cyan'
}

/**
 * 将 RSS 摘要中的 HTML 和 URL 清洗成适合卡片展示的纯文本。
 */
function sanitizePlainText(value: string | null) {
  if (!value) {
    return ''
  }
  const withoutHtml = value
    .replace(/<[^>]*>/g, ' ')
    .replace(/https?:\/\/\S+/g, ' ')
    .replace(/Article URL:/gi, ' ')
    .replace(/Comments URL:/gi, ' ')
    .replace(/Points:\s*\d+/gi, ' ')
    .replace(/#\s*Comments:\s*\d+/gi, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
  return withoutHtml.replace(/\s+/g, ' ').trim()
}

onMounted(loadDashboard)
</script>

<template>
  <section class="dashboard-view">
    <header class="dashboard-toolbar">
      <div>
        <h3>实时概览</h3>
        <p>聚合订阅源、文章、抓取日志和健康状态。</p>
      </div>
      <NButton secondary type="primary" :loading="loading" @click="loadDashboard">
        <template #icon>
          <RefreshCw :size="16" />
        </template>
        刷新
      </NButton>
    </header>

    <NSpin :show="loading">
      <div class="metric-grid">
        <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="metric.tone">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <em>{{ metric.hint }}</em>
        </article>
      </div>

      <section class="pipeline-card" aria-label="RSS 抓取管线">
        <div>
          <h3>RSS 抓取管线</h3>
          <p>请求、解析、去重、入库。</p>
        </div>
        <ol class="pipeline-steps">
          <li v-for="step in pipelineSteps" :key="step.index">
            <span>{{ step.index }}</span>
            <b>{{ step.title }}</b>
            <em>{{ step.detail }}</em>
          </li>
        </ol>
        <div class="pipeline-alert" :class="{ calm: (feedHealth?.warning ?? 0) + (feedHealth?.error ?? 0) === 0 }">
          <span class="dot" :class="(feedHealth?.warning ?? 0) + (feedHealth?.error ?? 0) === 0 ? 'success' : 'amber'"></span>
          {{ (feedHealth?.warning ?? 0) + (feedHealth?.error ?? 0) === 0 ? '所有源状态平稳' : `${(feedHealth?.warning ?? 0) + (feedHealth?.error ?? 0)} 个源需要关注` }}
        </div>
      </section>

      <div class="dashboard-layout">
        <section class="panel stream-panel">
          <div class="panel-title-row">
            <div>
              <h3>实时信息流</h3>
              <p>最近 4 条入库文章。</p>
            </div>
            <NTag :bordered="false" type="info">API</NTag>
          </div>

          <NEmpty v-if="recentArticles.length === 0" description="暂无最近文章" />
          <article
            v-for="(article, index) in recentArticles"
            v-else
            :key="article.id"
            class="article-row"
            :class="{ selected: index === 0 }"
            role="link"
            tabindex="0"
            @click="openArticleInReader(article)"
            @keydown.enter="openArticleInReader(article)"
            @keydown.space.prevent="openArticleInReader(article)"
          >
            <span class="unread-dot" :class="index % 3 === 1 ? 'lime' : index % 3 === 2 ? 'amber' : ''"></span>
            <div>
              <h4>{{ article.title }}</h4>
              <p>{{ article.feedName }} · {{ formatDateTime(article.fetchedAt) }}</p>
            </div>
          </article>
        </section>

        <section class="panel health-panel">
          <h3>源健康雷达</h3>
          <p>按订阅源健康状态聚合统计。</p>
          <div v-for="row in healthRows" :key="row.label" class="health-row" :class="row.tone">
            <span>{{ row.label }}</span>
            <strong>{{ row.value }}</strong>
            <i :style="{ '--health-percent': `${getHealthPercent(row.value)}%` }"></i>
          </div>
        </section>

        <section class="panel tag-panel">
          <h3>今日热词</h3>
          <div v-if="hotKeywords.length > 0" class="tag-cloud">
            <span v-for="keyword in hotKeywords" :key="keyword.word" class="topic-tag" :class="keyword.tone">
              {{ keyword.word }}
            </span>
          </div>
          <p>基于最近文章标题和摘要的轻量统计，后续可替换为后端关键词服务。</p>
        </section>

        <section class="panel log-panel">
          <h3>最近抓取</h3>
          <NEmpty v-if="recentLogs.length === 0" description="暂无抓取日志" />
          <div v-for="log in recentLogs" v-else :key="log.id" class="log-row">
            <span class="dot" :class="getLogTone(log.status)"></span>
            <b>{{ log.feedName }}</b>
            <em>{{ getLogStatusLabel(log.status) }} · 新增 {{ log.newCount }} · {{ formatDateTime(log.startedAt) }}</em>
          </div>
        </section>
      </div>
    </NSpin>
  </section>
</template>

<style scoped>
.dashboard-view {
  display: grid;
  gap: 12px;
}

.dashboard-toolbar,
.pipeline-card,
.panel,
.metric-card {
  border: 1px solid rgb(36 48 68 / 0.95);
  border-radius: 8px;
  background: linear-gradient(180deg, rgb(17 24 39 / 0.96), rgb(15 23 42 / 0.96));
  box-shadow: 0 18px 40px rgb(0 0 0 / 0.18);
}

.dashboard-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 12px 16px;
}

.dashboard-toolbar h3,
.pipeline-card h3,
.panel h3,
.article-row h4 {
  margin: 0;
  letter-spacing: 0;
}

.dashboard-toolbar h3,
.pipeline-card h3,
.panel h3 {
  color: #e5e7eb;
  font-size: 16px;
}

.dashboard-toolbar p,
.pipeline-card p,
.panel p {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.55;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 12px;
}

.metric-card {
  position: relative;
  min-height: 82px;
  overflow: hidden;
  padding: 12px 14px;
}

.metric-card::after {
  position: absolute;
  right: 18px;
  bottom: 14px;
  width: 42px;
  height: 50px;
  border-radius: 8px;
  background: currentColor;
  opacity: 0.12;
  content: "";
}

.metric-card span,
.metric-card em {
  display: block;
  color: #94a3b8;
  font-size: 13px;
  font-style: normal;
}

.metric-card strong {
  display: block;
  margin-top: 4px;
  color: #e5e7eb;
  font-size: 26px;
  line-height: 1.15;
}

.metric-card em {
  margin-top: 4px;
  color: currentColor;
  font-size: 12px;
  font-weight: 800;
}

.pipeline-card {
  display: grid;
  grid-template-columns: 190px 1fr 190px;
  align-items: center;
  gap: 12px;
  border-color: rgb(34 211 238 / 0.22);
  background:
    linear-gradient(90deg, rgb(34 211 238 / 0.08), transparent 44%),
    rgb(15 23 42 / 0.88);
  padding: 10px 14px;
}

.pipeline-steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.pipeline-steps li {
  position: relative;
  min-height: 48px;
  border: 1px solid rgb(51 65 85 / 0.55);
  border-radius: 8px;
  background: rgb(17 24 39 / 0.72);
  padding: 7px 10px;
}

.pipeline-steps li:not(:last-child)::after {
  position: absolute;
  top: 24px;
  right: -10px;
  width: 10px;
  height: 1px;
  background: rgb(34 211 238 / 0.45);
  content: "";
}

.pipeline-steps span,
.pipeline-steps em {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-style: normal;
}

.pipeline-steps b {
  display: block;
  margin-top: 1px;
  color: #e5e7eb;
  font-size: 13px;
}

.pipeline-alert {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 38px;
  border: 1px solid rgb(245 158 11 / 0.32);
  border-radius: 8px;
  background: rgb(245 158 11 / 0.08);
  color: #f59e0b;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 800;
}

.pipeline-alert.calm {
  border-color: rgb(163 230 53 / 0.3);
  background: rgb(163 230 53 / 0.08);
  color: #a3e635;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(360px, 1.1fr) repeat(3, minmax(220px, 1fr));
  gap: 12px;
}

.panel {
  padding: 12px 14px;
}

.panel-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.article-row {
  position: relative;
  display: grid;
  grid-template-columns: 10px 1fr;
  gap: 12px;
  min-height: 50px;
  margin-top: 8px;
  border: 1px solid rgb(36 48 68 / 0.58);
  border-radius: 8px;
  background: rgb(21 28 46 / 0.55);
  padding: 8px 10px;
}

.article-row.selected {
  border-color: rgb(34 211 238 / 0.55);
  background: rgb(34 211 238 / 0.1);
  box-shadow: inset 3px 0 0 #22d3ee;
}

.article-row h4 {
  display: -webkit-box;
  overflow: hidden;
  color: #e5e7eb;
  font-size: 12px;
  line-height: 1.34;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-row p {
  margin: 3px 0 0;
  color: #94a3b8;
  font-size: 11px;
}

.unread-dot,
.dot {
  display: inline-block;
  flex: 0 0 auto;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 12px currentColor;
}

.unread-dot {
  margin-top: 7px;
  color: #22d3ee;
}

.dot {
  color: #22d3ee;
}

.health-row {
  display: grid;
  grid-template-columns: 72px 40px 1fr;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  color: #a3e635;
  font-size: 13px;
}

.health-row span {
  color: #e5e7eb;
}

.health-row i {
  display: block;
  height: 8px;
  border-radius: 99px;
  background:
    linear-gradient(90deg, currentColor var(--health-percent), transparent var(--health-percent)),
    #0f172a;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
  min-width: 0;
}

.topic-tag {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 24px;
  border: 1px solid currentColor;
  border-radius: 6px;
  background: color-mix(in srgb, currentColor 12%, transparent);
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.35;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.tag-panel p {
  margin-top: 12px;
}

.log-row {
  display: grid;
  grid-template-columns: 8px 1fr;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 13px;
}

.log-row b {
  overflow: hidden;
  color: #e5e7eb;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-row em {
  grid-column: 2;
  overflow: hidden;
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cyan {
  color: #22d3ee;
}

.lime,
.success {
  color: #a3e635;
}

.amber {
  color: #f59e0b;
}

.coral {
  color: #fb7185;
}

.violet {
  color: #8b5cf6;
}

@media (max-width: 1180px) {
  .metric-grid,
  .pipeline-card,
  .dashboard-layout {
    grid-template-columns: 1fr;
  }

  .pipeline-steps {
    grid-template-columns: repeat(2, 1fr);
  }

  .pipeline-steps li::after {
    display: none;
  }

  .stream-panel {
    grid-row: auto;
  }
}
</style>
