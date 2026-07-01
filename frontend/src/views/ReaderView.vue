<script setup lang="ts">
import {
  Archive,
  BookOpenCheck,
  ExternalLink,
  Heart,
  RefreshCw,
  Search,
  Star,
} from '@lucide/vue'
import { NButton, NEmpty, NInput, NPagination, NSpin, NTag, useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchArticleDetail,
  fetchArticleStats,
  fetchArticles,
  updateArticleFlag,
  type ArticleFeedStatResponse,
  type ArticleListItemResponse,
  type ArticleResponse,
  type ArticleStateResponse,
  type ArticleStatsResponse,
} from '../api/articles'

type FilterKey = 'all' | 'unread' | 'favorite' | 'readLater' | 'today'
type FontMode = 'compact' | 'normal' | 'large'
type WidthMode = 'normal' | 'wide'

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loadingList = ref(false)
const loadingDetail = ref(false)
const updatingState = ref(false)
const articles = ref<ArticleListItemResponse[]>([])
const totalArticles = ref(0)
const articleStats = ref<ArticleStatsResponse | null>(null)
const currentPage = ref(1)
const pageSize = ref(40)
const selectedArticle = ref<ArticleResponse | null>(null)
const selectedArticleId = ref<number | null>(null)
const activeFilter = ref<FilterKey>('all')
const activeFeedId = ref<number | null>(null)
const searchKeyword = ref('')
const fontMode = ref<FontMode>('normal')
const widthMode = ref<WidthMode>('normal')
const routeTargetArticleId = ref<number | null>(null)

const filterItems = computed(() => [
  { key: 'all' as const, label: '全部文章', count: articleStats.value?.totalCount ?? totalArticles.value },
  { key: 'unread' as const, label: '未读', count: articleStats.value?.unreadCount ?? 0 },
  { key: 'favorite' as const, label: '收藏', count: articleStats.value?.favoriteCount ?? 0 },
  { key: 'readLater' as const, label: '稍后读', count: articleStats.value?.readLaterCount ?? 0 },
  { key: 'today' as const, label: '今日更新', count: articleStats.value?.todayCount ?? 0 },
])

const pageTotal = computed(() => Math.max(1, Math.ceil(totalArticles.value / pageSize.value)))

const sourceTotalCount = computed(() => {
  const groupTotal = feedGroups.value.reduce((total, group) => total + group.count, 0)
  return groupTotal || totalArticles.value
})

const articleListTitle = computed(() => {
  if (activeFeedId.value != null) {
    return feedGroups.value.find((group) => group.feedId === activeFeedId.value)?.feedName ?? '源文章'
  }
  const labelMap: Record<FilterKey, string> = {
    all: '全部文章',
    unread: '未读文章',
    favorite: '收藏文章',
    readLater: '稍后读',
    today: '今日更新',
  }
  return labelMap[activeFilter.value]
})

const articleListDescription = computed(() => {
  const start = totalArticles.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1
  const end = Math.min(currentPage.value * pageSize.value, totalArticles.value)
  const pageText = totalArticles.value > pageSize.value ? `第 ${start}-${end} 篇 / 共 ${totalArticles.value} 篇` : `共 ${totalArticles.value} 篇`
  return `${pageText}，当前页 ${articles.value.length} 篇`
})

const feedGroups = computed<ArticleFeedStatResponse[]>(() => articleStats.value?.feedStats ?? [])

const relatedArticles = computed(() => {
  if (!selectedArticle.value) {
    return []
  }
  return articles.value
    .filter((article) => article.feedId === selectedArticle.value?.feedId && article.id !== selectedArticle.value.id)
    .slice(0, 3)
})

/**
 * 从路由查询参数中读取阅读筛选模式。
 */
function getFilterFromRoute(): FilterKey {
  const routeFilter = route.query.filter
  if (routeFilter === 'unread' || routeFilter === 'favorite' || routeFilter === 'readLater' || routeFilter === 'today') {
    return routeFilter
  }
  return 'all'
}

/**
 * 从路由查询参数中读取目标文章主键。
 */
function getArticleIdFromRoute() {
  const routeArticleId = Number(route.query.articleId)
  return Number.isSafeInteger(routeArticleId) && routeArticleId > 0 ? routeArticleId : null
}

/**
 * 从路由查询参数中读取订阅源筛选。
 */
function getFeedIdFromRoute() {
  const routeFeedId = Number(route.query.feedId)
  return Number.isSafeInteger(routeFeedId) && routeFeedId > 0 ? routeFeedId : null
}

/**
 * 生成阅读页查询参数，保留当前筛选和目标文章。
 */
function buildReaderQuery(overrides: {
  filter?: FilterKey
  feedId?: number | null
  articleId?: number | null
} = {}) {
  const filter = overrides.filter ?? activeFilter.value
  const feedId = overrides.feedId === undefined ? activeFeedId.value : overrides.feedId
  const articleId = overrides.articleId === undefined ? routeTargetArticleId.value : overrides.articleId
  const query: Record<string, string> = {}
  if (filter !== 'all') {
    query.filter = filter
  }
  if (feedId != null) {
    query.feedId = String(feedId)
  }
  if (articleId != null) {
    query.articleId = String(articleId)
  }
  return query
}

/**
 * 切换阅读筛选，并把筛选状态同步到地址栏，方便刷新后保留上下文。
 */
function changeActiveFilter(filter: FilterKey) {
  currentPage.value = 1
  selectedArticle.value = null
  selectedArticleId.value = null
  routeTargetArticleId.value = null
  void router.replace({ name: 'reader', query: buildReaderQuery({ filter, articleId: null }) })
}

/**
 * 切换订阅源筛选并重新加载文章。
 */
function changeActiveFeed(feedId: number | null) {
  activeFeedId.value = feedId
  currentPage.value = 1
  selectedArticle.value = null
  selectedArticleId.value = null
  routeTargetArticleId.value = null
  void router.replace({ name: 'reader', query: buildReaderQuery({ feedId, articleId: null }) })
  void loadArticles()
}

/**
 * 加载文章列表，并默认选中第一篇文章。
 */
async function loadArticles() {
  loadingList.value = true
  try {
    const [page, stats] = await Promise.all([
      fetchArticles(activeFeedId.value, currentPage.value - 1, pageSize.value, activeFilter.value, searchKeyword.value),
      fetchArticleStats(activeFeedId.value, activeFilter.value, searchKeyword.value),
    ])
    articles.value = page.items
    totalArticles.value = page.totalElements
    articleStats.value = stats
    const firstArticle = page.items[0]
    if (routeTargetArticleId.value != null) {
      await selectArticle(routeTargetArticleId.value, false)
      return
    }
    if (firstArticle && (selectedArticleId.value == null || !page.items.some((article) => article.id === selectedArticleId.value))) {
      await selectArticle(firstArticle.id)
    } else if (!firstArticle) {
      selectedArticle.value = null
      selectedArticleId.value = null
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '文章列表加载失败')
  } finally {
    loadingList.value = false
  }
}

/**
 * 切换文章分页并重新加载全源文章。
 */
function changePage(page: number) {
  currentPage.value = page
  selectedArticle.value = null
  selectedArticleId.value = null
  routeTargetArticleId.value = null
  void router.replace({ name: 'reader', query: buildReaderQuery({ articleId: null }) })
  void loadArticles()
}

/**
 * 选择并加载文章详情。
 */
async function selectArticle(id: number, syncRoute = true) {
  selectedArticleId.value = id
  loadingDetail.value = true
  try {
    selectedArticle.value = await fetchArticleDetail(id)
    if (syncRoute) {
      routeTargetArticleId.value = id
      void router.replace({ name: 'reader', query: buildReaderQuery({ articleId: id }) })
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '文章详情加载失败')
  } finally {
    loadingDetail.value = false
  }
}

/**
 * 更新当前文章状态，并同步列表状态。
 */
async function toggleArticleFlag(type: 'read-state' | 'favorite' | 'read-later' | 'archive', value: boolean) {
  if (!selectedArticle.value) {
    return
  }
  updatingState.value = true
  try {
    const state = await updateArticleFlag(selectedArticle.value.id, type, value)
    applyArticleState(selectedArticle.value.id, state)
    await loadArticles()
    message.success('文章状态已更新')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '文章状态更新失败')
  } finally {
    updatingState.value = false
  }
}

/**
 * 在详情和列表中同步文章状态。
 */
function applyArticleState(articleId: number, state: ArticleStateResponse) {
  if (selectedArticle.value?.id === articleId) {
    selectedArticle.value = { ...selectedArticle.value, state }
  }
  articles.value = articles.value.map((article) => (article.id === articleId ? { ...article, state } : article))
}

/**
 * 打开当前文章原文。
 */
function openOriginalArticle() {
  if (!selectedArticle.value) {
    return
  }
  window.open(selectedArticle.value.articleUrl, '_blank', 'noopener,noreferrer')
}

/**
 * 格式化时间。
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
 * 清洗摘要中的 RSS 平台 HTML 和 URL。
 */
function sanitizePlainText(value: string | null) {
  if (!value) {
    return ''
  }
  return value
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
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * 判断正文是否只剩 RSS 平台元数据。
 */
function isMetadataOnlyContent(value: string) {
  const text = sanitizePlainText(value)
  return !text || /^(Article URL|Comments URL|Points|# Comments|[:\d\s])+$/i.test(text)
}

/**
 * 转义兜底正文，避免把纯文本当作 HTML 注入。
 */
function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 获取文章列表摘要。
 */
function getArticleSummary(article: ArticleListItemResponse) {
  const summary = sanitizePlainText(article.summary)
  return summary || `${article.feedName} · ${article.readingMinutes ?? 1} 分钟阅读`
}

/**
 * 获取详情正文 HTML。
 */
function getContentHtml(article: ArticleResponse) {
  if (article.contentHtml && article.contentHtml.trim() && !isMetadataOnlyContent(article.contentHtml)) {
    return article.contentHtml
  }
  const summary = sanitizePlainText(article.summary)
  return `<p>${escapeHtml(summary || '当前文章没有解析到正文，可打开原文继续阅读。')}</p>`
}

/**
 * 获取速读摘要。
 */
function getQuickSummary(article: ArticleResponse) {
  const summary = sanitizePlainText(article.summary)
  if (summary) {
    return summary
  }
  return `来自 ${article.feedName}，预计阅读 ${article.readingMinutes ?? 1} 分钟。`
}

watch(() => route.query.filter, () => {
  const nextFilter = getFilterFromRoute()
  if (activeFilter.value === nextFilter) {
    return
  }
  activeFilter.value = nextFilter
  currentPage.value = 1
  selectedArticle.value = null
  selectedArticleId.value = null
  void loadArticles()
})

watch(() => [route.query.articleId, route.query.feedId], async () => {
  const nextArticleId = getArticleIdFromRoute()
  const nextFeedId = getFeedIdFromRoute()
  const feedChanged = activeFeedId.value !== nextFeedId
  activeFeedId.value = nextFeedId
  routeTargetArticleId.value = nextArticleId
  currentPage.value = 1
  if (feedChanged) {
    await loadArticles()
    return
  }
  if (nextArticleId != null && selectedArticleId.value !== nextArticleId) {
    await selectArticle(nextArticleId, false)
  }
})

watch(searchKeyword, () => {
  currentPage.value = 1
  selectedArticle.value = null
  selectedArticleId.value = null
  void loadArticles()
})

onMounted(() => {
  activeFilter.value = getFilterFromRoute()
  activeFeedId.value = getFeedIdFromRoute()
  routeTargetArticleId.value = getArticleIdFromRoute()
  void loadArticles()
})
</script>

<template>
  <section class="reader-view">
    <header class="reader-toolbar">
      <NInput v-model:value="searchKeyword" clearable placeholder="搜索标题、源、摘要">
        <template #prefix>
          <Search :size="16" />
        </template>
      </NInput>
      <NButton secondary type="primary" :loading="loadingList" @click="loadArticles">
        <template #icon>
          <RefreshCw :size="16" />
        </template>
        刷新
      </NButton>
    </header>

    <div class="reader-layout">
      <aside class="panel filters">
        <h3>过滤器</h3>
        <button
          v-for="filter in filterItems"
          :key="filter.key"
          class="filter-button"
          :class="{ active: activeFilter === filter.key }"
          type="button"
          @click="changeActiveFilter(filter.key)"
        >
          <span>{{ filter.label }}</span>
          <b>{{ filter.count }}</b>
        </button>

        <hr />
        <h4>源分组</h4>
        <button class="source-line" :class="{ active: activeFeedId == null }" type="button" @click="changeActiveFeed(null)">
          <span class="dot cyan"></span>
          全部源
          <em>{{ sourceTotalCount }}</em>
        </button>
        <button
          v-for="group in feedGroups"
          :key="group.feedId"
          class="source-line"
          :class="{ active: activeFeedId === group.feedId }"
          type="button"
          @click="changeActiveFeed(group.feedId)"
        >
          <span class="dot success"></span>
          {{ group.feedName }}
          <em>{{ group.count }}</em>
        </button>
      </aside>

      <section class="panel article-list">
        <div class="panel-title-row">
          <div>
            <h3>{{ articleListTitle }}</h3>
            <p>{{ articleListDescription }}</p>
          </div>
          <NTag :bordered="false" type="info">API</NTag>
        </div>

        <NSpin :show="loadingList">
          <NEmpty v-if="articles.length === 0" description="暂无匹配文章" />
          <article
            v-for="article in articles"
            v-else
            :key="article.id"
            class="article-row"
            :class="{ selected: selectedArticleId === article.id, read: article.state.read }"
            @click="selectArticle(article.id)"
          >
            <span class="unread-dot" :class="{ muted: article.state.read }"></span>
            <div>
              <h4>{{ article.title }}</h4>
              <p>{{ article.feedName }} · {{ formatDateTime(article.fetchedAt) }} · {{ article.state.read ? '已读' : '未读' }}</p>
              <small>{{ getArticleSummary(article) }}</small>
            </div>
          </article>
        </NSpin>
        <NPagination
          v-if="totalArticles > pageSize"
          v-model:page="currentPage"
          class="article-pagination"
          :page-count="pageTotal"
          :page-size="pageSize"
          simple
          @update:page="changePage"
        />
      </section>

      <article class="panel reader-detail" :class="[fontMode, widthMode]">
        <NSpin :show="loadingDetail" class="reader-detail-spin">
          <div v-if="!selectedArticle" class="reader-empty-state">
            <NEmpty description="请选择一篇文章" />
          </div>
          <template v-else>
            <div class="reader-detail-shell">
              <div class="reader-scroll">
                <div class="tag-row">
                  <NTag :bordered="false" type="info">{{ selectedArticle.feedName }}</NTag>
                  <NTag :bordered="false" :type="selectedArticle.state.read ? 'default' : 'success'">
                    {{ selectedArticle.state.read ? '已读' : '未读' }}
                  </NTag>
                  <NTag v-if="selectedArticle.state.favorite" :bordered="false" type="error">收藏</NTag>
                  <NTag v-if="selectedArticle.state.readLater" :bordered="false" type="warning">稍后读</NTag>
                </div>

                <h2>{{ selectedArticle.title }}</h2>
                <p class="reader-meta">
                  {{ formatDateTime(selectedArticle.publishedAt ?? selectedArticle.fetchedAt) }}
                  · {{ selectedArticle.author || '未知作者' }}
                  · 预计阅读 {{ selectedArticle.readingMinutes ?? 1 }} 分钟
                </p>

                <div class="reader-tools" aria-label="阅读工具">
                  <NButton size="small" secondary :type="fontMode === 'compact' ? 'primary' : 'default'" @click="fontMode = 'compact'">A-</NButton>
                  <NButton size="small" secondary :type="fontMode === 'large' ? 'primary' : 'default'" @click="fontMode = 'large'">A+</NButton>
                  <NButton size="small" secondary :type="widthMode === 'normal' ? 'primary' : 'default'" @click="widthMode = 'normal'">窄行宽</NButton>
                  <NButton size="small" secondary :type="widthMode === 'wide' ? 'primary' : 'default'" @click="widthMode = 'wide'">宽行宽</NButton>
                </div>

                <hr />

                <section class="insight-box">
                  <b>速读摘要</b>
                  <p>{{ getQuickSummary(selectedArticle) }}</p>
                </section>

                <section class="content-body" v-html="getContentHtml(selectedArticle)"></section>

                <section class="related-box">
                  <h3>同源最新</h3>
                  <button v-for="article in relatedArticles" :key="article.id" type="button" @click="selectArticle(article.id)">
                    {{ article.title }}
                  </button>
                  <p v-if="relatedArticles.length === 0">暂无同源文章。</p>
                </section>
              </div>

              <div class="action-row">
                <NButton secondary :type="selectedArticle.state.favorite ? 'error' : 'default'" :loading="updatingState" @click="toggleArticleFlag('favorite', !selectedArticle.state.favorite)">
                  <template #icon>
                    <Heart :size="16" />
                  </template>
                  收藏
                </NButton>
                <NButton secondary :type="selectedArticle.state.readLater ? 'warning' : 'default'" :loading="updatingState" @click="toggleArticleFlag('read-later', !selectedArticle.state.readLater)">
                  <template #icon>
                    <Star :size="16" />
                  </template>
                  稍后读
                </NButton>
                <NButton secondary :type="selectedArticle.state.read ? 'success' : 'default'" :loading="updatingState" @click="toggleArticleFlag('read-state', !selectedArticle.state.read)">
                  <template #icon>
                    <BookOpenCheck :size="16" />
                  </template>
                  {{ selectedArticle.state.read ? '已读' : '标记已读' }}
                </NButton>
                <NButton secondary :type="selectedArticle.state.archived ? 'primary' : 'default'" :loading="updatingState" @click="toggleArticleFlag('archive', !selectedArticle.state.archived)">
                  <template #icon>
                    <Archive :size="16" />
                  </template>
                  归档
                </NButton>
                <NButton secondary type="primary" @click="openOriginalArticle">
                  <template #icon>
                    <ExternalLink :size="16" />
                  </template>
                  打开原文
                </NButton>
              </div>
            </div>
          </template>
        </NSpin>
      </article>
    </div>
  </section>
</template>

<style scoped>
.reader-view {
  display: grid;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}

.reader-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 12px;
}

.reader-layout {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-columns: 190px minmax(300px, 380px) minmax(420px, 1fr);
  gap: 12px;
}

.panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid rgb(36 48 68 / 0.95);
  border-radius: 8px;
  background: linear-gradient(180deg, rgb(17 24 39 / 0.96), rgb(15 23 42 / 0.96));
  box-shadow: 0 18px 40px rgb(0 0 0 / 0.18);
}

.filters,
.article-list {
  overflow-y: auto;
  padding: 12px;
}

.reader-detail {
  border-color: rgb(34 211 238 / 0.26);
  display: grid;
  overflow: hidden;
}

.reader-detail-spin,
.reader-detail-spin :deep(.n-spin-content) {
  min-height: 0;
  height: 100%;
}

.reader-empty-state,
.reader-detail-shell {
  height: 100%;
  min-height: 0;
}

.reader-empty-state {
  display: grid;
  place-items: center;
}

.reader-detail-shell {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
}

.reader-scroll {
  min-height: 0;
  overflow-y: auto;
  padding: 12px 12px 18px;
}

.filters h3,
.article-list h3,
.reader-detail h2,
.related-box h3 {
  margin: 0;
  letter-spacing: 0;
}

.filters h3 {
  margin-bottom: 12px;
  color: #e5e7eb;
  font-size: 17px;
}

.filter-button,
.source-line {
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid rgb(36 48 68 / 0.55);
  border-radius: 8px;
  background: rgb(21 28 46 / 0.5);
  color: #94a3b8;
  cursor: pointer;
}

.filter-button {
  justify-content: space-between;
  min-height: 34px;
  margin-bottom: 8px;
  padding: 0 10px;
  font-weight: 700;
}

.filter-button.active,
.source-line.active {
  border-color: rgb(34 211 238 / 0.48);
  background: rgb(34 211 238 / 0.13);
  color: #e5e7eb;
}

.filters hr,
.reader-detail hr {
  height: 1px;
  margin: 14px 0;
  border: 0;
  background: #243044;
}

.filters h4 {
  margin: 0 0 12px;
  color: #94a3b8;
  font-size: 13px;
}

.source-line {
  gap: 10px;
  min-height: 32px;
  margin-top: 8px;
  padding: 0 10px;
  font-size: 13px;
  text-align: left;
}

.source-line em {
  margin-left: auto;
  color: #64748b;
  font-style: normal;
}

.panel-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.panel-title-row h3 {
  color: #e5e7eb;
  font-size: 18px;
}

.panel-title-row p {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.article-row {
  display: grid;
  grid-template-columns: 10px 1fr;
  gap: 12px;
  min-height: 76px;
  margin-top: 10px;
  border: 1px solid rgb(36 48 68 / 0.58);
  border-radius: 8px;
  background: rgb(21 28 46 / 0.55);
  cursor: pointer;
  padding: 10px 12px;
}

.article-row.selected {
  border-color: rgb(34 211 238 / 0.55);
  background: rgb(34 211 238 / 0.1);
  box-shadow: inset 3px 0 0 #22d3ee;
}

.article-row.read {
  opacity: 0.72;
}

.article-pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.article-row h4 {
  margin: 0;
  overflow: hidden;
  color: #e5e7eb;
  font-size: 15px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-row p,
.article-row small {
  display: block;
  margin: 4px 0 0;
  overflow: hidden;
  color: #94a3b8;
  font-size: 12px;
  text-overflow: ellipsis;
}

.article-row small {
  display: -webkit-box;
  color: #64748b;
  line-clamp: 1;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
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

.unread-dot.muted {
  color: #64748b;
}

.dot.cyan {
  color: #22d3ee;
}

.dot.success {
  color: #a3e635;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reader-detail h2 {
  margin-top: 14px;
  color: #e5e7eb;
  font-size: 23px;
  line-height: 1.35;
}

.reader-meta {
  margin: 10px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.reader-tools,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.action-row {
  align-items: center;
  margin-top: 0;
  border-top: 1px solid rgb(34 211 238 / 0.2);
  background: linear-gradient(180deg, rgb(15 23 42 / 0.94), rgb(8 47 73 / 0.42));
  padding: 12px;
}

.insight-box,
.related-box {
  max-width: 640px;
  border: 1px solid rgb(163 230 53 / 0.28);
  border-radius: 8px;
  background: rgb(163 230 53 / 0.07);
  padding: 12px;
}

.insight-box b,
.related-box h3 {
  display: block;
  color: #a3e635;
  font-size: 13px;
}

.insight-box p {
  margin: 8px 0 0;
  color: #e5e7eb;
  font-size: 13px;
  line-height: 1.65;
}

.content-body {
  max-width: 640px;
  color: #e5e7eb;
  font-size: 15px;
  line-height: 1.75;
}

.reader-detail.compact .content-body {
  font-size: 14px;
  line-height: 1.7;
}

.reader-detail.large .content-body {
  font-size: 17px;
  line-height: 1.9;
}

.reader-detail.wide .content-body,
.reader-detail.wide .insight-box,
.reader-detail.wide .related-box {
  max-width: 100%;
}

.content-body :deep(p),
.content-body :deep(li) {
  margin: 14px 0;
}

.content-body :deep(a) {
  color: #22d3ee;
  word-break: break-word;
}

.content-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.content-body :deep(blockquote) {
  margin: 18px 0;
  border: 1px solid rgb(34 211 238 / 0.35);
  border-radius: 8px;
  background: #0f172a;
  color: #22d3ee;
  padding: 18px 20px;
  font-weight: 800;
}

.related-box {
  margin-top: 18px;
  border-color: rgb(51 65 85 / 0.8);
  background: rgb(15 23 42 / 0.72);
}

.related-box h3 {
  color: #e5e7eb;
}

.related-box button {
  display: block;
  width: 100%;
  margin-top: 8px;
  border: 0;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  line-height: 1.5;
  padding: 0;
  text-align: left;
}

.related-box p {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .reader-layout {
    grid-template-columns: minmax(220px, 280px) 1fr;
  }

  .reader-detail {
    grid-column: 1 / -1;
    max-height: none;
  }
}

@media (max-width: 760px) {
  .reader-toolbar,
  .reader-layout {
    grid-template-columns: 1fr;
  }

  .filters {
    order: 1;
  }

  .article-list {
    order: 2;
    max-height: 420px;
  }

  .reader-detail {
    order: 3;
  }
}
</style>
