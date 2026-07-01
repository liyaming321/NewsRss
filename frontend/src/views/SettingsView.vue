<script setup lang="ts">
import { Edit3, Plus, RefreshCw, Save, Trash2 } from '@lucide/vue'
import {
  NButton,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NSpace,
  NSwitch,
  NTabPane,
  NTabs,
  NTag,
  useMessage,
  type DataTableColumns,
  type FormInst,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  createDictionaryItem,
  deleteDictionaryItem,
  fetchDictionaryItems,
  updateDictionaryItem,
  type DictionaryRequest,
  type DictionaryResponse,
} from '../api/dictionaries'

type DictionaryFormModel = DictionaryRequest

const message = useMessage()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const deleteModalVisible = ref(false)
const selectedItem = ref<DictionaryResponse | null>(null)
const categoryItems = ref<DictionaryResponse[]>([])
const formModel = reactive<DictionaryFormModel>(createEmptyForm())

const enabledCount = computed(() => categoryItems.value.filter((item) => item.enabled).length)

const columns = computed<DataTableColumns<DictionaryResponse>>(() => [
  {
    title: '分类编码',
    key: 'itemCode',
    minWidth: 150,
    render: (item) => h('code', { class: 'dictionary-code-cell' }, item.itemCode),
  },
  {
    title: '分类名称',
    key: 'itemLabel',
    minWidth: 160,
    render: (item) => h('b', { class: 'dictionary-label-cell' }, item.itemLabel),
  },
  {
    title: '说明',
    key: 'description',
    minWidth: 220,
    render: (item) => item.description || '暂无说明',
  },
  {
    title: '排序',
    key: 'sortOrder',
    width: 90,
  },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render: (item) => h(NTag, { bordered: false, type: item.enabled ? 'success' : 'default' }, () => item.enabled ? '启用' : '停用'),
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render: (item) => h(NSpace, { size: 8, wrap: false }, () => [
      h(NButton, { size: 'small', secondary: true, onClick: () => openEditModal(item) }, {
        icon: () => h(Edit3, { size: 14 }),
        default: () => '编辑',
      }),
      h(NButton, {
        size: 'small',
        secondary: true,
        type: item.enabled ? 'warning' : 'success',
        onClick: () => toggleItemEnabled(item),
      }, () => item.enabled ? '停用' : '启用'),
      h(NButton, { size: 'small', secondary: true, type: 'error', onClick: () => openDeleteModal(item) }, {
        icon: () => h(Trash2, { size: 14 }),
        default: () => '删除',
      }),
    ]),
  },
])

/**
 * 创建空白分类表单。
 */
function createEmptyForm(): DictionaryFormModel {
  return {
    itemCode: '',
    itemLabel: '',
    description: null,
    sortOrder: 100,
    enabled: true,
  }
}

/**
 * 加载订阅源分类字典。
 */
async function loadSettings() {
  loading.value = true
  try {
    categoryItems.value = await fetchDictionaryItems('FEED_CATEGORY')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '分类字典加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 打开新增分类弹窗。
 */
function openCreateModal() {
  selectedItem.value = null
  Object.assign(formModel, createEmptyForm())
  modalVisible.value = true
}

/**
 * 打开编辑分类弹窗。
 */
function openEditModal(item: DictionaryResponse) {
  selectedItem.value = item
  Object.assign(formModel, {
    itemCode: item.itemCode,
    itemLabel: item.itemLabel,
    description: item.description,
    sortOrder: item.sortOrder,
    enabled: item.enabled,
  })
  modalVisible.value = true
}

/**
 * 保存分类字典项。
 */
async function saveItem() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = normalizeRequest(formModel)
    if (selectedItem.value) {
      await updateDictionaryItem(selectedItem.value.id, payload)
      message.success('分类已更新')
    } else {
      await createDictionaryItem(payload)
      message.success('分类已新增')
    }
    modalVisible.value = false
    await loadSettings()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '分类保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 切换分类启用状态。
 */
async function toggleItemEnabled(item: DictionaryResponse) {
  try {
    await updateDictionaryItem(item.id, {
      itemCode: item.itemCode,
      itemLabel: item.itemLabel,
      description: item.description,
      sortOrder: item.sortOrder,
      enabled: !item.enabled,
    })
    message.success(item.enabled ? '分类已停用' : '分类已启用')
    await loadSettings()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '分类状态更新失败')
  }
}

/**
 * 打开删除确认弹窗。
 */
function openDeleteModal(item: DictionaryResponse) {
  selectedItem.value = item
  deleteModalVisible.value = true
}

/**
 * 删除分类字典项。
 */
async function confirmDeleteItem() {
  if (!selectedItem.value) {
    return
  }
  saving.value = true
  try {
    await deleteDictionaryItem(selectedItem.value.id)
    message.success('分类已删除')
    deleteModalVisible.value = false
    await loadSettings()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '分类删除失败')
  } finally {
    saving.value = false
  }
}

/**
 * 归一化分类保存请求。
 */
function normalizeRequest(model: DictionaryFormModel): DictionaryRequest {
  return {
    itemCode: model.itemCode.trim(),
    itemLabel: model.itemLabel.trim(),
    description: normalizeOptionalText(model.description),
    sortOrder: model.sortOrder,
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

onMounted(loadSettings)
</script>

<template>
  <section class="settings-view">
    <header class="settings-toolbar">
      <div>
        <h3>系统设置</h3>
        <p>按设置项目分区维护系统配置，后续新增配置项时直接扩展独立页签。</p>
      </div>
    </header>

    <section class="settings-tabs-shell">
      <NTabs type="line" animated pane-class="settings-tab-pane">
        <NTabPane name="feed-category" tab="订阅源分类">
          <div class="tab-toolbar">
            <div>
              <h3>订阅源分类</h3>
              <p>用于订阅源新增和编辑时的分类下拉选项。</p>
            </div>
            <div class="toolbar-actions">
              <NButton secondary type="primary" :loading="loading" @click="loadSettings">
                <template #icon>
                  <RefreshCw :size="16" />
                </template>
                刷新
              </NButton>
              <NButton type="primary" @click="openCreateModal">
                <template #icon>
                  <Plus :size="16" />
                </template>
                新增分类
              </NButton>
            </div>
          </div>

          <section class="settings-summary">
            <article>
              <span>分类总数</span>
              <strong>{{ categoryItems.length }}</strong>
            </article>
            <article>
              <span>启用分类</span>
              <strong>{{ enabledCount }}</strong>
            </article>
            <article>
              <span>字典类型</span>
              <strong>FEED_CATEGORY</strong>
            </article>
          </section>

          <section class="settings-panel">
            <div class="panel-title-row">
              <div>
                <h3>分类字典</h3>
                <p>维护分类编码、展示名称、排序和启用状态。</p>
              </div>
              <NTag :bordered="false" type="info">API</NTag>
            </div>
            <NDataTable
              :columns="columns"
              :data="categoryItems"
              :loading="loading"
              :pagination="{ pageSize: 10 }"
              size="small"
            />
          </section>
        </NTabPane>
      </NTabs>
    </section>

    <NModal v-model:show="modalVisible" preset="card" :title="selectedItem ? '编辑分类' : '新增分类'" class="dictionary-modal">
      <NForm ref="formRef" :model="formModel" label-placement="top">
        <NFormItem
          label="分类编码"
          path="itemCode"
          :rule="{ required: true, message: '请输入分类编码', trigger: ['blur', 'input'] }"
        >
          <NInput v-model:value="formModel.itemCode" placeholder="例如：财经" />
        </NFormItem>
        <NFormItem
          label="分类名称"
          path="itemLabel"
          :rule="{ required: true, message: '请输入分类名称', trigger: ['blur', 'input'] }"
        >
          <NInput v-model:value="formModel.itemLabel" placeholder="例如：财经" />
        </NFormItem>
        <NFormItem label="说明">
          <NInput v-model:value="formModel.description" type="textarea" placeholder="可选，用于说明分类范围" />
        </NFormItem>
        <div class="form-grid">
          <NFormItem label="排序">
            <NInputNumber v-model:value="formModel.sortOrder" :min="0" :max="9999" />
          </NFormItem>
          <NFormItem label="启用">
            <NSwitch v-model:value="formModel.enabled">
              <template #checked>启用</template>
              <template #unchecked>停用</template>
            </NSwitch>
          </NFormItem>
        </div>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton secondary @click="modalVisible = false">取消</NButton>
          <NButton type="primary" :loading="saving" @click="saveItem">
            <template #icon>
              <Save :size="16" />
            </template>
            保存
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="deleteModalVisible" preset="dialog" title="删除分类" positive-text="删除" negative-text="取消" :loading="saving" @positive-click="confirmDeleteItem">
      <p class="delete-copy">
        删除后新增订阅源将无法再选择该分类。当前选择：{{ selectedItem?.itemLabel || '-' }}
      </p>
    </NModal>
  </section>
</template>

<style scoped>
.settings-view {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.settings-toolbar,
.settings-tabs-shell {
  border: 1px solid rgb(36 48 68 / 0.95);
  border-radius: 8px;
  background: linear-gradient(180deg, rgb(17 24 39 / 0.96), rgb(15 23 42 / 0.96));
  box-shadow: 0 18px 40px rgb(0 0 0 / 0.18);
}

.settings-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px;
}

.settings-toolbar h3,
.tab-toolbar h3,
.settings-panel h3 {
  margin: 0;
  color: #e5e7eb;
  font-size: 18px;
  letter-spacing: 0;
}

.settings-toolbar p,
.tab-toolbar p,
.settings-panel p {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.settings-tabs-shell {
  padding: 14px 16px 16px;
}

:deep(.settings-tab-pane) {
  padding-top: 14px;
}

.tab-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.settings-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.settings-summary article {
  padding: 12px 14px;
  border: 1px solid rgb(36 48 68 / 0.82);
  border-radius: 8px;
  background: rgb(15 23 42 / 0.58);
}

.settings-summary span {
  display: block;
  color: #94a3b8;
  font-size: 13px;
}

.settings-summary strong {
  display: block;
  margin-top: 6px;
  color: #e5e7eb;
  font-size: 24px;
}

.settings-panel {
  padding: 12px 0 0;
  border-top: 1px solid rgb(36 48 68 / 0.82);
}

.panel-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.dictionary-label-cell {
  color: #e5e7eb;
}

.dictionary-code-cell {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 3px 7px;
  overflow: hidden;
  border: 1px solid rgb(71 85 105 / 0.72);
  border-radius: 6px;
  background: rgb(15 23 42 / 0.78);
  color: #cbd5e1;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dictionary-modal {
  width: min(560px, calc(100vw - 32px));
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.delete-copy {
  margin: 0;
  color: #cbd5e1;
}

@media (max-width: 900px) {
  .settings-toolbar,
  .tab-toolbar,
  .settings-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .settings-toolbar,
  .tab-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
