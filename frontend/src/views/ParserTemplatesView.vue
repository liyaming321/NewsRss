<script setup lang="ts">
import {
  AlertTriangle,
  Beaker,
  Braces,
  CheckCircle2,
  Eye,
  FileJson,
  ListPlus,
  Sparkles,
  Plus,
  Power,
  RefreshCw,
  Save,
  Trash2,
  XCircle,
} from '@lucide/vue'
import {
  NButton,
  NDataTable,
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
  type SelectOption,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { fetchFeeds, type FeedResponse } from '../api/feeds'
import {
  createParserTemplate,
  deleteParserTemplate,
  fetchParserTemplates,
  generateParserTemplateFromFeed,
  previewParserTemplate,
  setParserTemplateEnabled,
  updateParserTemplate,
  type FieldHit,
  type ParserTemplatePreviewResponse,
  type ParserTemplateRequest,
  type ParserTemplateResponse,
} from '../api/parserTemplates'

type StandardField = 'guid' | 'articleUrl' | 'title' | 'summary' | 'author' | 'publishedAt' | 'contentHtml' | 'coverImageUrl'

interface MappingRow {
  field: StandardField
  label: string
  pathText: string
  required: boolean
}

interface CustomFieldRow {
  fieldName: string
  pathText: string
}

interface TemplateFormModel {
  templateCode: string
  templateName: string
  description: string
  enabled: boolean
  contentSelectors: string
  coverSelectors: string
  timeFormats: string
  removeSelectors: string
  unwrapSelectors: string
  removeAttributes: string
}

interface FieldTreeRow {
  path: string
  value: string
}

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const previewing = ref(false)
const generating = ref(false)
const deleting = ref(false)
const templates = ref<ParserTemplateResponse[]>([])
const feeds = ref<FeedResponse[]>([])
const selectedTemplateId = ref<number | null>(null)
const selectedPreviewIndex = ref(0)
const previewResult = ref<ParserTemplatePreviewResponse | null>(null)
const deleteModalVisible = ref(false)
const feedUrl = ref('https://rsshub.app/eastmoney/report/industry')
const selectedGenerateFeedId = ref<number | null>(null)
const preferAiGenerate = ref(true)
const generationSummary = ref<string | null>(null)
const previewLimit = ref(5)
const formModel = reactive<TemplateFormModel>(createEmptyForm())
const mappingRows = reactive<MappingRow[]>(createDefaultMappingRows())
const customFieldRows = reactive<CustomFieldRow[]>([])

const selectedTemplate = computed(() => templates.value.find((template) => template.id === selectedTemplateId.value) ?? null)

const feedOptions = computed<SelectOption[]>(() => feeds.value.map((feed) => ({
  label: `${feed.feedName} · ${feed.feedUrl}`,
  value: feed.id,
})))

/**
 * 选择订阅源后同步 RSS 地址输入框，方便用户确认本次生成使用的真实地址。
 */
function syncGenerateFeedUrl(feedId: string | number | null) {
  if (feedId == null) {
    return
  }
  const selectedFeed = feeds.value.find((feed) => String(feed.id) === String(feedId))
  if (selectedFeed) {
    feedUrl.value = selectedFeed.feedUrl
  }
}

/**
 * 监听选中项和订阅源列表，兼容下拉值类型差异与异步加载顺序。
 */
watch([selectedGenerateFeedId, feeds], ([feedId]) => {
  syncGenerateFeedUrl(feedId)
})

const selectedPreviewItem = computed(() => {
  if (!previewResult.value) {
    return null
  }
  return previewResult.value.items[selectedPreviewIndex.value] ?? previewResult.value.items[0] ?? null
})

const fieldTreeRows = computed(() => flattenRawPayload(selectedPreviewItem.value?.rawPayload ?? null).slice(0, 80))

const previewItemOptions = computed<SelectOption[]>(() => {
  if (!previewResult.value) {
    return []
  }
  return previewResult.value.items.map((item, index) => ({
    label: `条目 ${item.index} · ${item.normalized.title || '未命名文章'}`,
    value: index,
  }))
})

const riskItems = computed(() => {
  const risks: Array<{ label: string; tone: 'error' | 'warning' | 'info' | 'success' }> = []
  if (!formModel.templateCode.trim()) {
    risks.push({ label: '模板编码为空，无法保存。', tone: 'error' })
  }
  if (!formModel.templateName.trim()) {
    risks.push({ label: '模板名称为空，无法保存。', tone: 'error' })
  }
  if (!feedUrl.value.trim()) {
    risks.push({ label: '测试 RSS 地址为空，无法预览。', tone: 'error' })
  }
  const missingRequiredFields = mappingRows.filter((row) => row.required && !row.pathText.trim())
  if (missingRequiredFields.length > 0) {
    risks.push({ label: `必需字段未配置：${missingRequiredFields.map((row) => row.label).join('、')}`, tone: 'warning' })
  }
  if (!formModel.contentSelectors.trim()) {
    risks.push({ label: '未配置正文候选字段，会依赖默认正文路径兜底。', tone: 'info' })
  }
  if (!formModel.enabled) {
    risks.push({ label: '模板当前停用，保存后不能用于预览和绑定。', tone: 'warning' })
  }
  if (previewResult.value && previewResult.value.hitRate < 0.75) {
    risks.push({ label: `当前预览整体命中率 ${formatPercent(previewResult.value.hitRate)}，建议检查字段路径。`, tone: 'warning' })
  }
  if (previewResult.value?.warnings.length) {
    risks.push({ label: `预览返回 ${previewResult.value.warnings.length} 条异常提示。`, tone: 'warning' })
  }
  if (customFieldRows.length > 0) {
    risks.push({ label: `已配置 ${customFieldRows.length} 个自定义字段，会写入文章 customFields。`, tone: 'info' })
  }
  if (risks.length === 0) {
    risks.push({ label: '配置完整，预览结果稳定。', tone: 'success' })
  }
  return risks
})

const columns = computed<DataTableColumns<ParserTemplateResponse>>(() => [
  {
    title: '模板',
    key: 'templateName',
    minWidth: 230,
    render: (template) => h('div', { class: 'template-name-cell' }, [
      h('b', template.templateName),
      h('span', template.templateCode),
    ]),
  },
  {
    title: '状态',
    key: 'enabled',
    width: 92,
    render: (template) => h(NTag, { bordered: false, type: template.enabled ? 'success' : 'default' }, () => template.enabled ? '启用' : '停用'),
  },
  {
    title: '映射',
    key: 'fieldMapping',
    width: 92,
    render: (template) => `${Object.keys(template.fieldMapping ?? {}).length} 项`,
  },
  {
    title: '更新',
    key: 'updatedAt',
    width: 130,
    render: (template) => formatDateTime(template.updatedAt),
  },
  {
    title: '操作',
    key: 'actions',
    width: 230,
    render: (template) => h(NSpace, { size: 8, wrap: false }, () => [
      h(NButton, { size: 'small', secondary: true, onClick: () => selectTemplate(template) }, () => '编辑'),
      h(NButton, {
        size: 'small',
        secondary: true,
        type: template.enabled ? 'warning' : 'success',
        onClick: () => toggleTemplateEnabled(template),
      }, {
        icon: () => h(Power, { size: 14 }),
        default: () => template.enabled ? '停用' : '启用',
      }),
      h(NButton, { size: 'small', secondary: true, type: 'error', onClick: () => openDeleteModal(template) }, {
        icon: () => h(Trash2, { size: 14 }),
        default: () => '删除',
      }),
    ]),
  },
])

/**
 * 创建空白模板表单。
 */
function createEmptyForm(): TemplateFormModel {
  return {
    templateCode: 'custom-template',
    templateName: '自定义解析模板',
    description: '',
    enabled: true,
    contentSelectors: 'contents[0].value, description.value',
    coverSelectors: 'foreignMarkup.thumbnail.attributes.url, enclosures[0].url',
    timeFormats: 'yyyy-MM-dd HH:mm:ss',
    removeSelectors: '.ad, script, style',
    unwrapSelectors: 'article',
    removeAttributes: 'onclick, style',
  }
}

/**
 * 创建默认字段映射行，用户通过输入路径即可生成 JSON 配置。
 */
function createDefaultMappingRows(): MappingRow[] {
  return [
    { field: 'guid', label: 'GUID', pathText: 'uri', required: false },
    { field: 'articleUrl', label: '文章链接', pathText: 'foreignMarkup.articleUrl.value, link', required: true },
    { field: 'title', label: '标题', pathText: 'foreignMarkup.headline.value, title', required: true },
    { field: 'summary', label: '摘要', pathText: 'foreignMarkup.summary.value, description.value', required: false },
    { field: 'author', label: '作者', pathText: 'author, authors[0].name', required: false },
    { field: 'publishedAt', label: '发布时间', pathText: 'foreignMarkup.publishedText.value, publishedDate', required: false },
    { field: 'contentHtml', label: '正文 HTML', pathText: 'contents[0].value, description.value', required: false },
    { field: 'coverImageUrl', label: '封面图', pathText: 'foreignMarkup.thumbnail.attributes.url, enclosures[0].url', required: false },
  ]
}

/**
 * 加载模板列表、订阅源列表并默认选中第一条模板。
 */
async function loadInitialData() {
  loading.value = true
  try {
    const [templateItems, feedPage] = await Promise.all([
      fetchParserTemplates(),
      fetchFeeds(0, 100),
    ])
    templates.value = templateItems
    feeds.value = feedPage.items
    const firstTemplate = templates.value[0]
    if (!selectedTemplateId.value && firstTemplate) {
      selectTemplate(firstTemplate)
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '解析模板数据加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 仅刷新模板列表，避免保存后重置订阅源选择。
 */
async function loadTemplates() {
  loading.value = true
  try {
    templates.value = await fetchParserTemplates()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '解析模板加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 选择模板并把 JSON 配置回填为表单输入。
 */
function selectTemplate(template: ParserTemplateResponse) {
  selectedTemplateId.value = template.id
  previewResult.value = null
  selectedPreviewIndex.value = 0
  Object.assign(formModel, {
    templateCode: template.templateCode,
    templateName: template.templateName,
    description: template.description ?? '',
    enabled: template.enabled,
    contentSelectors: joinList(template.contentSelectors),
    coverSelectors: joinList(template.coverSelectors),
    timeFormats: joinList(template.timeFormats),
    removeSelectors: joinList(template.cleanupRules?.removeSelectors),
    unwrapSelectors: joinList(template.cleanupRules?.unwrapSelectors),
    removeAttributes: joinList(template.cleanupRules?.removeAttributes),
  })
  mappingRows.splice(0, mappingRows.length, ...createDefaultMappingRows().map((row) => ({
    ...row,
    pathText: joinList(template.fieldMapping?.[row.field]) || row.pathText,
  })))
  customFieldRows.splice(0, customFieldRows.length, ...toCustomFieldRows(template.customFieldMapping))
}

/**
 * 新建本地模板草稿。
 */
function createDraftTemplate() {
  selectedTemplateId.value = null
  previewResult.value = null
  selectedPreviewIndex.value = 0
  Object.assign(formModel, createEmptyForm())
  mappingRows.splice(0, mappingRows.length, ...createDefaultMappingRows())
  customFieldRows.splice(0, customFieldRows.length)
  generationSummary.value = null
}

/**
 * 保存模板，自动区分创建和更新。
 */
async function saveTemplate() {
  const request = buildTemplateRequest()
  if (!request.templateCode || !request.templateName) {
    message.warning('请填写模板编码和模板名称')
    return
  }
  saving.value = true
  try {
    const savedTemplate = selectedTemplateId.value
      ? await updateParserTemplate(selectedTemplateId.value, request)
      : await createParserTemplate(request)
    message.success(selectedTemplateId.value ? '模板已更新' : '模板已创建')
    await loadTemplates()
    selectTemplate(savedTemplate)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '模板保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 执行模板预览并展示字段命中结果。
 */
async function runPreview() {
  if (!feedUrl.value.trim()) {
    message.warning('请先输入测试 RSS 地址')
    return
  }
  previewing.value = true
  try {
    previewResult.value = await previewParserTemplate({
      feedUrl: feedUrl.value.trim(),
      template: buildTemplateRequest(),
      limit: previewLimit.value,
    })
    selectedPreviewIndex.value = 0
    message.success(`预览完成，整体命中率 ${formatPercent(previewResult.value.hitRate)}`)
  } catch (error) {
    previewResult.value = null
    message.error(error instanceof Error ? error.message : '模板预览失败')
  } finally {
    previewing.value = false
  }
}

/**
 * 从订阅源真实样本生成解析模板，并回填表单和预览结果。
 */
async function generateTemplateFromFeed() {
  if (!selectedGenerateFeedId.value && !feedUrl.value.trim()) {
    message.warning('请选择订阅源或输入 RSS 地址')
    return
  }
  generating.value = true
  try {
    const result = await generateParserTemplateFromFeed({
      feedId: selectedGenerateFeedId.value,
      feedUrl: selectedGenerateFeedId.value ? null : feedUrl.value.trim(),
      limit: previewLimit.value,
      preferAi: preferAiGenerate.value,
    })
    feedUrl.value = result.feedUrl
    applyGeneratedTemplate(result.template)
    previewResult.value = result.preview
    selectedPreviewIndex.value = 0
    generationSummary.value = `${result.generator} · ${result.aiUsed ? 'AI 已参与' : '本地生成'} · ${result.preview.itemCount} 条样本`
    result.warnings.forEach((warning) => message.warning(warning))
    message.success('解析模板已生成并回填')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '解析模板生成失败')
  } finally {
    generating.value = false
  }
}

/**
 * 启用或停用模板。
 */
async function toggleTemplateEnabled(template: ParserTemplateResponse) {
  try {
    const updatedTemplate = await setParserTemplateEnabled(template.id, !template.enabled)
    message.success(template.enabled ? '模板已停用' : '模板已启用')
    await loadTemplates()
    if (selectedTemplateId.value === template.id) {
      selectTemplate(updatedTemplate)
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '模板状态更新失败')
  }
}

/**
 * 打开删除确认框。
 */
function openDeleteModal(template: ParserTemplateResponse) {
  selectedTemplateId.value = template.id
  deleteModalVisible.value = true
}

/**
 * 删除当前模板。
 */
async function confirmDeleteTemplate() {
  if (!selectedTemplateId.value) {
    return
  }
  deleting.value = true
  try {
    await deleteParserTemplate(selectedTemplateId.value)
    message.success('模板已删除')
    deleteModalVisible.value = false
    selectedTemplateId.value = null
    createDraftTemplate()
    await loadTemplates()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '模板删除失败')
  } finally {
    deleting.value = false
  }
}

/**
 * 将可视化表单转换为后端模板请求。
 */
function buildTemplateRequest(): ParserTemplateRequest {
  const fieldMapping: Record<string, string[]> = {}
  mappingRows.forEach((row) => {
    const paths = splitList(row.pathText)
    if (paths.length > 0) {
      fieldMapping[row.field] = paths
    }
  })
  return {
    templateCode: formModel.templateCode.trim(),
    templateName: formModel.templateName.trim(),
    description: normalizeOptionalText(formModel.description),
    fieldMapping,
    customFieldMapping: buildCustomFieldMapping(),
    contentSelectors: splitList(formModel.contentSelectors),
    coverSelectors: splitList(formModel.coverSelectors),
    timeFormats: splitList(formModel.timeFormats),
    cleanupRules: {
      removeSelectors: splitList(formModel.removeSelectors),
      unwrapSelectors: splitList(formModel.unwrapSelectors),
      removeAttributes: splitList(formModel.removeAttributes),
    },
    enabled: formModel.enabled,
  }
}

/**
 * 回填生成后的模板配置。
 */
function applyGeneratedTemplate(template: ParserTemplateRequest) {
  selectedTemplateId.value = null
  Object.assign(formModel, {
    templateCode: template.templateCode,
    templateName: template.templateName,
    description: template.description ?? '',
    enabled: template.enabled,
    contentSelectors: joinList(template.contentSelectors),
    coverSelectors: joinList(template.coverSelectors),
    timeFormats: joinList(template.timeFormats),
    removeSelectors: joinList(template.cleanupRules?.removeSelectors),
    unwrapSelectors: joinList(template.cleanupRules?.unwrapSelectors),
    removeAttributes: joinList(template.cleanupRules?.removeAttributes),
  })
  mappingRows.splice(0, mappingRows.length, ...createDefaultMappingRows().map((row) => ({
    ...row,
    pathText: joinList(template.fieldMapping?.[row.field]) || row.pathText,
  })))
  customFieldRows.splice(0, customFieldRows.length, ...toCustomFieldRows(template.customFieldMapping))
}

/**
 * 添加一个自定义字段映射行。
 */
function addCustomFieldRow() {
  customFieldRows.push({ fieldName: '', pathText: '' })
}

/**
 * 删除一个自定义字段映射行。
 */
function removeCustomFieldRow(index: number) {
  customFieldRows.splice(index, 1)
}

/**
 * 构建自定义字段映射配置。
 */
function buildCustomFieldMapping() {
  const mapping: Record<string, string[]> = {}
  customFieldRows.forEach((row) => {
    const fieldName = row.fieldName.trim()
    const paths = splitList(row.pathText)
    if (fieldName && paths.length > 0) {
      mapping[fieldName] = paths
    }
  })
  return mapping
}

/**
 * 将自定义字段映射转换为可编辑行。
 */
function toCustomFieldRows(mapping: Record<string, string[]> | null | undefined): CustomFieldRow[] {
  return Object.entries(mapping ?? {}).map(([fieldName, paths]) => ({
    fieldName,
    pathText: joinList(paths),
  }))
}

/**
 * 将逗号或换行分隔文本转换为列表。
 */
function splitList(value: string | null | undefined) {
  if (!value) {
    return []
  }
  return value
    .split(/[\n,，]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

/**
 * 将列表展示为逗号分隔文本。
 */
function joinList(value: string[] | null | undefined) {
  return Array.isArray(value) ? value.join(', ') : ''
}

/**
 * 将可选文本归一为空值或去空白字符串。
 */
function normalizeOptionalText(value: string) {
  const normalizedValue = value.trim()
  return normalizedValue || null
}

/**
 * 扁平化原始 JSON，方便以字段树形式扫读。
 */
function flattenRawPayload(value: unknown, prefix = ''): FieldTreeRow[] {
  if (value == null) {
    return []
  }
  if (typeof value !== 'object') {
    return [{ path: prefix || '$', value: String(value) }]
  }
  if (Array.isArray(value)) {
    return value.flatMap((item, index) => flattenRawPayload(item, `${prefix}[${index}]`))
  }
  return Object.entries(value as Record<string, unknown>).flatMap(([key, item]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return flattenRawPayload(item, path)
  })
}

/**
 * 获取字段命中状态标签类型。
 */
function getFieldHitType(hit: FieldHit | undefined) {
  if (!hit?.matched) {
    return 'error'
  }
  return hit.fallback ? 'warning' : 'success'
}

/**
 * 获取字段命中文案。
 */
function getFieldHitLabel(hit: FieldHit | undefined) {
  if (!hit?.matched) {
    return '未命中'
  }
  return hit.fallback ? '兜底' : '命中'
}

/**
 * 安全格式化 JSON。
 */
function formatJson(value: unknown) {
  return JSON.stringify(value ?? {}, null, 2)
}

/**
 * 格式化百分比。
 */
function formatPercent(value: number) {
  return `${Math.round(value * 100)}%`
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
 * 清洗 HTML，避免预览卡片显示原始标签。
 */
function sanitizePlainText(value: string | null) {
  if (!value) {
    return '-'
  }
  return value
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim() || '-'
}

onMounted(loadInitialData)
</script>

<template>
  <section class="templates-view">
    <header class="templates-toolbar">
      <div>
        <h3>解析模板实验室</h3>
        <p>用字段路径配置解析模板，预览每个标准字段的命中来源。</p>
      </div>
      <div class="toolbar-actions">
        <NButton secondary :loading="loading" @click="loadTemplates">
          <template #icon>
            <RefreshCw :size="16" />
          </template>
          刷新
        </NButton>
        <NButton secondary type="primary" @click="createDraftTemplate">
          <template #icon>
            <Plus :size="16" />
          </template>
          新建草稿
        </NButton>
        <NButton type="primary" :loading="saving" @click="saveTemplate">
          <template #icon>
            <Save :size="16" />
          </template>
          保存模板
        </NButton>
      </div>
    </header>

    <div class="templates-layout">
      <aside class="panel template-list">
        <div class="panel-title-row">
          <div>
            <h3>模板列表</h3>
            <p>{{ templates.length }} 个模板</p>
          </div>
          <NTag :bordered="false" type="info">API</NTag>
        </div>
        <NSpin :show="loading">
          <NEmpty v-if="templates.length === 0" description="暂无解析模板" />
          <NDataTable
            v-else
            :columns="columns"
            :data="templates"
            :row-key="(row) => row.id"
            :row-class-name="(row) => row.id === selectedTemplateId ? 'selected-template-row' : ''"
            size="small"
          />
        </NSpin>
      </aside>

      <main class="template-workbench">
        <section class="panel ai-generate-panel">
          <div class="panel-title-row">
            <div>
              <h3>AI 生成模板</h3>
              <p>从订阅源拉取真实样本，生成标准字段和自定义字段映射。</p>
            </div>
            <NTag :bordered="false" :type="preferAiGenerate ? 'success' : 'info'">
              {{ preferAiGenerate ? 'DeepSeek 优先' : '本地生成' }}
            </NTag>
          </div>
          <div class="generate-grid">
            <NSelect
              v-model:value="selectedGenerateFeedId"
              clearable
              filterable
              :options="feedOptions"
              placeholder="选择已保存订阅源"
              @update:value="syncGenerateFeedUrl"
            />
            <NInput v-model:value="feedUrl" placeholder="或输入 RSS 地址" />
            <div class="generate-actions">
              <NInputNumber v-model:value="previewLimit" class="generate-limit" :min="1" :max="10" />
              <NSwitch v-model:value="preferAiGenerate">
                <template #checked>AI</template>
                <template #unchecked>本地</template>
              </NSwitch>
              <NButton type="primary" :loading="generating" @click="generateTemplateFromFeed">
                <template #icon>
                  <Sparkles :size="16" />
                </template>
                生成模板
              </NButton>
            </div>
          </div>
          <p v-if="generationSummary" class="generation-summary">{{ generationSummary }}</p>
        </section>

        <section class="panel editor-panel">
          <div class="panel-title-row">
            <div>
              <h3>字段映射</h3>
              <p>每格可输入多个候选路径，用逗号或换行分隔。</p>
            </div>
            <NTag :bordered="false" :type="formModel.enabled ? 'success' : 'default'">
              {{ formModel.enabled ? '启用' : '停用' }}
            </NTag>
          </div>

          <NForm label-placement="top" :show-feedback="false">
            <div class="form-grid">
              <NFormItem label="模板编码">
                <NInput v-model:value="formModel.templateCode" placeholder="例如：github-blog-template" />
              </NFormItem>
              <NFormItem label="模板名称">
                <NInput v-model:value="formModel.templateName" placeholder="例如：GitHub Blog 模板" />
              </NFormItem>
            </div>
            <NFormItem label="模板说明">
              <NInput v-model:value="formModel.description" type="textarea" placeholder="说明适用的 RSS 源和字段特点" />
            </NFormItem>
            <NFormItem label="启用状态">
              <NSwitch v-model:value="formModel.enabled">
                <template #checked>启用</template>
                <template #unchecked>停用</template>
              </NSwitch>
            </NFormItem>

            <div class="mapping-table">
              <article v-for="row in mappingRows" :key="row.field" class="mapping-row">
                <div>
                  <b>{{ row.label }}</b>
                  <span>{{ row.field }}{{ row.required ? ' · 必需' : '' }}</span>
                </div>
                <NInput v-model:value="row.pathText" type="textarea" autosize placeholder="例如：title, foreignMarkup.headline.value" />
              </article>
            </div>

            <div class="custom-field-editor">
              <div class="section-title-row">
                <div>
                  <h3>自定义字段</h3>
                  <p>标准字段之外的源特有信息会写入文章 customFields。</p>
                </div>
                <NButton size="small" secondary type="primary" @click="addCustomFieldRow">
                  <template #icon>
                    <Plus :size="14" />
                  </template>
                  添加字段
                </NButton>
              </div>
              <NEmpty v-if="customFieldRows.length === 0" description="暂无自定义字段映射" />
              <article v-for="(row, index) in customFieldRows" v-else :key="`${row.fieldName}-${index}`" class="custom-field-row">
                <NInput v-model:value="row.fieldName" placeholder="字段名，例如 commentsUrl" />
                <NInput v-model:value="row.pathText" type="textarea" autosize placeholder="候选路径，例如 foreignMarkup.commentsUrl.value" />
                <NButton tertiary type="error" @click="removeCustomFieldRow(index)">
                  <template #icon>
                    <Trash2 :size="15" />
                  </template>
                </NButton>
              </article>
            </div>

            <div class="form-grid triple">
              <NFormItem label="正文候选字段">
                <NInput v-model:value="formModel.contentSelectors" type="textarea" placeholder="contents[0].value" />
              </NFormItem>
              <NFormItem label="封面候选字段">
                <NInput v-model:value="formModel.coverSelectors" type="textarea" placeholder="enclosures[0].url" />
              </NFormItem>
              <NFormItem label="时间格式">
                <NInput v-model:value="formModel.timeFormats" type="textarea" placeholder="yyyy-MM-dd HH:mm:ss" />
              </NFormItem>
            </div>

            <div class="form-grid triple">
              <NFormItem label="移除选择器">
                <NInput v-model:value="formModel.removeSelectors" type="textarea" placeholder=".ad, script" />
              </NFormItem>
              <NFormItem label="解包选择器">
                <NInput v-model:value="formModel.unwrapSelectors" type="textarea" placeholder="article" />
              </NFormItem>
              <NFormItem label="移除属性">
                <NInput v-model:value="formModel.removeAttributes" type="textarea" placeholder="onclick, style" />
              </NFormItem>
            </div>
          </NForm>
        </section>

        <section class="panel preview-control">
          <div>
            <h3>测试 RSS 源</h3>
            <p>预览不会保存文章，只返回字段命中、标准化结果和异常提示。</p>
          </div>
          <div class="preview-inputs">
            <NInput v-model:value="feedUrl" placeholder="RSS 或 Atom 地址" />
            <NInputNumber v-model:value="previewLimit" :min="1" :max="10" />
            <NButton type="primary" :loading="previewing" @click="runPreview">
              <template #icon>
                <Beaker :size="16" />
              </template>
              模板预览
            </NButton>
          </div>
        </section>

        <section class="risk-panel">
          <div>
            <AlertTriangle :size="17" />
            <b>保存前风险</b>
          </div>
          <NTag v-for="risk in riskItems" :key="risk.label" :bordered="false" :type="risk.tone">
            {{ risk.label }}
          </NTag>
        </section>

        <section class="preview-grid">
          <article class="panel score-panel">
            <div class="panel-title-row">
              <div>
                <h3>命中率</h3>
                <p>{{ previewResult ? `${previewResult.feedTitle} · ${previewResult.itemCount} 条` : '等待预览结果' }}</p>
              </div>
              <strong>{{ previewResult ? formatPercent(previewResult.hitRate) : '--' }}</strong>
            </div>
            <div v-if="previewResult" class="hit-bars">
              <div v-for="(rate, field) in previewResult.fieldHitRates" :key="field" class="hit-bar">
                <span>{{ field }}</span>
                <i :style="{ '--hit-rate': `${Math.max(4, Math.round(rate * 100))}%` }"></i>
                <b>{{ formatPercent(rate) }}</b>
              </div>
            </div>
            <NEmpty v-else description="点击模板预览后展示命中率" />
          </article>

          <article class="panel normalized-panel">
            <div class="panel-title-row">
              <div>
                <h3>标准化文章结果</h3>
                <p>标题、链接、摘要、正文和元数据的最终输出。</p>
              </div>
              <NSelect v-if="previewItemOptions.length > 0" v-model:value="selectedPreviewIndex" :options="previewItemOptions" />
            </div>
            <NEmpty v-if="!selectedPreviewItem" description="暂无预览文章" />
            <div v-else class="normalized-card">
              <h4>{{ selectedPreviewItem.normalized.title || '未命名文章' }}</h4>
              <p>{{ selectedPreviewItem.normalized.articleUrl || '-' }}</p>
              <small>{{ sanitizePlainText(selectedPreviewItem.normalized.summary) }}</small>
              <div class="normalized-meta">
                <NTag :bordered="false" type="info">{{ selectedPreviewItem.normalized.author || '未知作者' }}</NTag>
                <NTag :bordered="false" type="success">{{ selectedPreviewItem.normalized.readingMinutes ?? 1 }} 分钟</NTag>
                <NTag :bordered="false" type="warning">{{ selectedPreviewItem.normalized.wordCount ?? 0 }} 字</NTag>
              </div>
              <div v-if="selectedPreviewItem.normalized.customFields && Object.keys(selectedPreviewItem.normalized.customFields).length > 0" class="custom-field-chips">
                <NTag v-for="(value, key) in selectedPreviewItem.normalized.customFields" :key="key" :bordered="false" type="info">
                  {{ key }}: {{ value }}
                </NTag>
              </div>
              <pre>{{ sanitizePlainText(selectedPreviewItem.normalized.contentHtml) }}</pre>
            </div>
          </article>

          <article class="panel hit-panel">
            <div class="panel-title-row">
              <div>
                <h3>字段命中路径</h3>
                <p>解释每个标准字段来自哪个原始路径。</p>
              </div>
              <Eye :size="18" />
            </div>
            <NEmpty v-if="!selectedPreviewItem" description="暂无字段命中结果" />
            <div v-else class="hit-list">
              <div v-for="row in mappingRows" :key="row.field" class="hit-row">
                <NTag :bordered="false" :type="getFieldHitType(selectedPreviewItem.fieldHits[row.field])">
                  {{ getFieldHitLabel(selectedPreviewItem.fieldHits[row.field]) }}
                </NTag>
                <div>
                  <b>{{ row.label }}</b>
                  <span>{{ selectedPreviewItem.fieldHits[row.field]?.path || '-' }}</span>
                  <em>{{ selectedPreviewItem.fieldHits[row.field]?.message || selectedPreviewItem.fieldHits[row.field]?.value || '-' }}</em>
                </div>
              </div>
            </div>
          </article>

          <article class="panel hit-panel">
            <div class="panel-title-row">
              <div>
                <h3>自定义字段命中</h3>
                <p>展示源特有字段进入 customFields 的路径。</p>
              </div>
              <Sparkles :size="18" />
            </div>
            <NEmpty v-if="!selectedPreviewItem || Object.keys(selectedPreviewItem.customFieldHits ?? {}).length === 0" description="暂无自定义字段命中" />
            <div v-else class="hit-list">
              <div v-for="(hit, fieldName) in selectedPreviewItem.customFieldHits" :key="fieldName" class="hit-row">
                <NTag :bordered="false" :type="getFieldHitType(hit)">
                  {{ getFieldHitLabel(hit) }}
                </NTag>
                <div>
                  <b>{{ fieldName }}</b>
                  <span>{{ hit.path || '-' }}</span>
                  <em>{{ hit.message || hit.value || '-' }}</em>
                </div>
              </div>
            </div>
          </article>

          <article class="panel raw-panel">
            <div class="panel-title-row">
              <div>
                <h3>原始字段树</h3>
                <p>展示当前条目的原始 payload 路径和值。</p>
              </div>
              <FileJson :size="18" />
            </div>
            <NEmpty v-if="fieldTreeRows.length === 0" description="暂无原始字段" />
            <div v-else class="field-tree">
              <div v-for="row in fieldTreeRows" :key="row.path">
                <code>{{ row.path }}</code>
                <span>{{ row.value }}</span>
              </div>
            </div>
          </article>

          <article class="panel warning-panel">
            <div class="panel-title-row">
              <div>
                <h3>异常提示</h3>
                <p>聚合预览级和条目级提示。</p>
              </div>
              <Braces :size="18" />
            </div>
            <NEmpty v-if="!previewResult" description="暂无异常提示" />
            <div v-else class="warning-list">
              <div v-for="warning in previewResult.warnings" :key="warning" class="warning-row">
                <XCircle :size="15" />
                <span>{{ warning }}</span>
              </div>
              <div v-for="warning in selectedPreviewItem?.warnings ?? []" :key="warning" class="warning-row item">
                <AlertTriangle :size="15" />
                <span>{{ warning }}</span>
              </div>
              <div v-if="previewResult.warnings.length === 0 && (selectedPreviewItem?.warnings.length ?? 0) === 0" class="warning-row calm">
                <CheckCircle2 :size="15" />
                <span>当前预览没有异常提示。</span>
              </div>
            </div>
          </article>

          <article class="panel json-panel">
            <div class="panel-title-row">
              <div>
                <h3>生成配置</h3>
                <p>由表单生成的模板 JSON，保存时会提交这份结构。</p>
              </div>
              <ListPlus :size="18" />
            </div>
            <pre>{{ formatJson(buildTemplateRequest()) }}</pre>
          </article>
        </section>
      </main>
    </div>

    <NModal v-model:show="deleteModalVisible" preset="dialog" title="删除解析模板" positive-text="删除" negative-text="取消" :loading="deleting" @positive-click="confirmDeleteTemplate">
      <p class="delete-copy">
        删除模板会影响已绑定该模板的订阅源，当前选择：{{ selectedTemplate?.templateName || '-' }}
      </p>
    </NModal>
  </section>
</template>

<style scoped>
.templates-view {
  display: grid;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}

.templates-toolbar,
.panel,
.risk-panel {
  border: 1px solid rgb(36 48 68 / 0.95);
  border-radius: 8px;
  background: linear-gradient(180deg, rgb(17 24 39 / 0.96), rgb(15 23 42 / 0.96));
  box-shadow: 0 18px 40px rgb(0 0 0 / 0.18);
}

.templates-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
}

.templates-toolbar h3,
.panel h3 {
  margin: 0;
  color: #e5e7eb;
  font-size: 18px;
  letter-spacing: 0;
}

.templates-toolbar p,
.panel p {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.55;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.templates-layout {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  gap: 12px;
}

.panel {
  min-width: 0;
  padding: 12px;
}

.template-list {
  min-height: 0;
  overflow: auto;
}

.template-workbench {
  display: grid;
  min-width: 0;
  min-height: 0;
  gap: 12px;
  overflow: auto;
  padding-right: 2px;
}

.panel-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.panel-title-row strong {
  color: #22d3ee;
  font-size: 28px;
  line-height: 1;
}

.template-name-cell {
  display: grid;
  gap: 5px;
}

.template-name-cell b {
  color: #e5e7eb;
}

.template-name-cell span {
  color: #64748b;
  font-size: 12px;
}

:deep(.selected-template-row td) {
  background: rgb(34 211 238 / 0.09) !important;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-grid.triple {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mapping-table {
  display: grid;
  gap: 8px;
  margin: 4px 0 12px;
}

.ai-generate-panel {
  border-color: rgb(34 211 238 / 0.32);
  background: linear-gradient(135deg, rgb(17 24 39 / 0.96), rgb(8 47 73 / 0.66));
}

.generate-grid {
  display: grid;
  grid-template-columns: minmax(220px, 0.9fr) minmax(260px, 1.1fr) auto;
  gap: 10px;
  align-items: start;
}

.generate-actions {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.generate-limit {
  width: 120px;
}

.generation-summary {
  margin: 10px 0 0;
  color: #22d3ee;
  font-size: 12px;
  font-weight: 700;
}

.mapping-row {
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 14px;
  align-items: start;
  border: 1px solid rgb(36 48 68 / 0.68);
  border-radius: 8px;
  background: rgb(15 23 42 / 0.72);
  padding: 10px;
}

.custom-field-editor {
  display: grid;
  gap: 10px;
  margin: 8px 0 14px;
  border: 1px solid rgb(34 211 238 / 0.2);
  border-radius: 8px;
  background: rgb(8 47 73 / 0.18);
  padding: 12px;
}

.section-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-title-row h3 {
  margin: 0;
  color: #e5e7eb;
  font-size: 15px;
}

.section-title-row p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.custom-field-row {
  display: grid;
  grid-template-columns: minmax(150px, 0.35fr) minmax(220px, 1fr) 36px;
  gap: 10px;
  align-items: start;
}

.mapping-row b,
.hit-row b {
  display: block;
  color: #e5e7eb;
  font-size: 13px;
}

.mapping-row span,
.hit-row span,
.hit-row em {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.preview-control {
  display: grid;
  grid-template-columns: 240px 1fr;
  align-items: end;
  gap: 12px;
}

.preview-inputs {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 90px auto;
  gap: 10px;
}

.risk-panel {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  border-color: rgb(245 158 11 / 0.24);
  background: rgb(245 158 11 / 0.07);
  padding: 10px 12px;
}

.risk-panel div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f59e0b;
  font-weight: 800;
}

.preview-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(360px, 1.2fr);
  gap: 12px;
}

.normalized-panel,
.json-panel {
  grid-column: span 1;
}

.raw-panel,
.warning-panel,
.hit-panel {
  min-height: 260px;
}

.hit-bars {
  display: grid;
  gap: 13px;
}

.hit-bar {
  display: grid;
  grid-template-columns: 110px 1fr 44px;
  align-items: center;
  gap: 12px;
}

.hit-bar span,
.hit-bar b {
  color: #94a3b8;
  font-size: 12px;
}

.hit-bar i {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #1e293b;
}

.hit-bar i::after {
  display: block;
  width: var(--hit-rate);
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #22d3ee, #a3e635);
  content: "";
}

.normalized-card {
  border: 1px solid rgb(36 48 68 / 0.68);
  border-radius: 8px;
  background: rgb(15 23 42 / 0.72);
  padding: 14px;
}

.normalized-card h4 {
  margin: 0;
  color: #e5e7eb;
  font-size: 18px;
  line-height: 1.45;
}

.normalized-card p,
.normalized-card small {
  display: block;
  margin-top: 7px;
  color: #94a3b8;
  line-height: 1.55;
}

.normalized-card pre,
.json-panel pre {
  max-height: 230px;
  overflow: auto;
  border: 1px solid #243044;
  border-radius: 8px;
  background: #0b1120;
  color: #cbd5e1;
  padding: 12px;
  white-space: pre-wrap;
}

.normalized-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.custom-field-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.hit-list,
.warning-list,
.field-tree {
  display: grid;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
}

.hit-row {
  display: grid;
  grid-template-columns: 70px 1fr;
  gap: 12px;
  border-bottom: 1px solid rgb(36 48 68 / 0.65);
  padding-bottom: 10px;
}

.field-tree div {
  display: grid;
  grid-template-columns: minmax(180px, 0.55fr) 1fr;
  gap: 12px;
  border-bottom: 1px solid rgb(36 48 68 / 0.55);
  padding-bottom: 8px;
}

.field-tree code {
  color: #22d3ee;
  font-size: 12px;
  white-space: normal;
  word-break: break-all;
}

.field-tree span {
  overflow: hidden;
  color: #cbd5e1;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #fb7185;
  font-size: 13px;
  line-height: 1.55;
}

.warning-row.item {
  color: #f59e0b;
}

.warning-row.calm {
  color: #a3e635;
}

.delete-copy {
  margin: 0;
  color: #cbd5e1;
  line-height: 1.7;
}

@media (max-width: 1280px) {
  .templates-layout,
  .preview-grid,
  .preview-control,
  .generate-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .templates-toolbar,
  .mapping-row {
    grid-template-columns: 1fr;
  }

  .templates-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid,
  .form-grid.triple,
  .preview-inputs,
  .custom-field-row,
  .field-tree div {
    grid-template-columns: 1fr;
  }
}
</style>
