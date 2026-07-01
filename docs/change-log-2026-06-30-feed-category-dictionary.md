# 订阅源分类字典变更记录

## 实现过程

- 新增 `system_dictionary` 系统字典表迁移，用 `FEED_CATEGORY` 作为订阅源分类字典类型，并从已有 `rss_feed.category` 初始化分类项。
- 新增系统字典后端能力，支持分类字典查询、新增、编辑、启停和删除。
- 在订阅源创建和更新流程中加入分类校验：分类为空时允许保存；分类非空时必须存在于 `FEED_CATEGORY` 且处于启用状态。
- 设置页新增“订阅源分类”管理界面，可以维护分类编码、分类名称、说明、排序和启用状态。
- 订阅源新增和编辑抽屉中的“分组”从手动输入改为下拉选择，只展示启用的订阅源分类。
- 为字典服务和订阅源分类校验补充单元测试，并更新订阅源服务测试依赖。

## 验证命令

```bash
mvn -f backend/pom.xml test
pnpm --dir frontend build
curl -sS 'http://127.0.0.1:8080/api/dictionaries?dictType=FEED_CATEGORY&enabled=true' | jq '.data | map({itemCode,itemLabel,enabled})'
curl -sS 'http://127.0.0.1:8080/api/feeds?page=0&size=5' | jq '.data.items | map({feedName,category})'
```

## 执行结果

- 后端测试通过：`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`。
- 前端构建通过：`vite build` 正常完成；仅保留 chunk 大小超过 500 kB 的构建提示。
- 字典接口返回启用分类：`东方财富网`、`研报`。
- 订阅源接口返回的现有订阅源分类保持正常：东方财富网相关源归属 `东方财富网`，行业研报归属 `研报`。
- Playwright 验证设置页分类表正常展示，截图：`output/playwright/settings-feed-category-dictionary.png`。
- Playwright 验证新增订阅源抽屉的分组下拉可以展开并显示 `东方财富网`、`研报`，截图：`output/playwright/feeds-category-select.png`。

## 遗留问题

- 当前删除分类字典项不会检查是否已有订阅源正在使用该分类；删除后历史订阅源仍保留原分类文本，但后续编辑保存时会因为分类不存在而校验失败。后续可以改成删除前做引用检查，或只允许停用分类。
- 订阅源分类当前保存的是字典编码，初始化数据中编码和名称都使用原中文分类；如果后续需要英文编码和中文名称分离，需要补一轮数据迁移和展示映射。

## 2026-06-30 23:23 UI 微调

### 实现过程

- 将设置页订阅源分类表格中的“分类名称”复合列拆分为“分类编码”和“分类名称”两列。
- 分类编码使用等宽字体和轻量边框展示，分类名称单独加粗展示，避免编码和名称挤在一个单元格里。

### 验证命令

```bash
pnpm --dir frontend build
```

### 执行结果

- 前端构建通过：`vite build` 正常完成；仅保留 chunk 大小超过 500 kB 的构建提示。
- Playwright 页面快照确认表头展示为 `分类编码 分类名称 说明 排序 状态 操作`。
- 截图：`output/playwright/settings-category-code-label-columns.png`。

### 遗留问题

- 本次只调整设置页表格展示，不涉及后端数据结构和订阅源保存逻辑。

## 2026-06-30 23:26 设置页页签化

### 实现过程

- 将系统设置页改为 `NTabs` 页签布局。
- 当前先提供“订阅源分类”页签，分类刷新、新增、统计卡片和分类字典表格全部收敛到该页签内。
- 外层设置页只保留系统设置说明，后续可以继续新增“抓取策略”“AI 配置”“阅读偏好”等独立页签，避免设置项目混在同一页面。

### 验证命令

```bash
pnpm --dir frontend build
```

### 执行结果

- 前端构建通过：`vite build` 正常完成；仅保留 chunk 大小超过 500 kB 的构建提示。
- Playwright 页面快照确认设置页存在“订阅源分类”页签，并且分类编码、分类名称、说明、排序、状态、操作列正常展示。
- 截图：`output/playwright/settings-tabs-feed-category.png`。

### 遗留问题

- 当前只有“订阅源分类”一个页签，后续新增设置项时再按业务模块补充对应页签和接口。

## 2026-06-30 23:33 订阅源分类名称展示

### 实现过程

- 在订阅源页新增分类编码到分类名称的前端映射。
- 订阅源列表“分类”列改为展示字典名称，避免直接暴露内部分类编码。
- 订阅源搜索索引同步改为使用分类名称，用户可以按可见分类名称搜索。
- 保存订阅源时仍提交分类编码，保持后端校验和数据结构不变。

### 验证命令

```bash
pnpm --dir frontend build
```

### 执行结果

- 前端构建通过：`vite build` 正常完成；仅保留 chunk 大小超过 500 kB 的构建提示。
- Playwright 页面快照确认订阅源列表分类列展示为字典名称，而不是 `dfcfw`、`yanbao` 等内部编码。
- 截图：`output/playwright/feeds-category-label-display.png`。

### 遗留问题

- 如果历史订阅源分类值没有对应字典项，页面会兜底显示原始分类值，后续可增加数据修复或后台引用检查。
