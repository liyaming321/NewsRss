# 新闻 RSS 订阅、存储与阅读网站 V1 设计方案

## 1. 产品定位

本项目定位为一个面向高频资讯阅读者的 RSS 聚合、归档与沉浸式阅读工具。

核心目标：

- 接入多个 RSS/Atom 源，统一抓取、解析、入库与去重。
- 支持不同 RSS 源使用不同解析模板，解决字段命名、正文结构、图片提取、发布时间格式不一致的问题。
- 提供清晰、专业、略带科技感的阅读界面，既适合快速扫读，也适合长文沉浸阅读。
- 为后续扩展智能摘要、关键词推荐、全文搜索、站点健康监控预留架构空间。

技术栈建议：

- 后端：Spring Boot 3 + Java 17 + PostgreSQL + Flyway/Liquibase + Spring Scheduler。
- 前端：Vue 3 + TypeScript + Vite + Naive UI + Pinia + Vue Router。
- 数据库：PostgreSQL，建议开启 `pg_trgm` 或后续接入全文搜索能力。
- RSS 解析：优先使用成熟库，例如 Rome Tools 或 Feed4j，再叠加自定义解析模板。

## 2. V1 功能范围

### 2.1 订阅源管理

功能：

- 新增 RSS 源：输入 URL，自动探测标题、站点链接、图标、默认语言。
- 编辑 RSS 源：名称、分组、标签、抓取频率、启用状态、解析模板。
- 手动刷新单个源。
- 批量启用、停用、刷新。
- 显示源健康状态：正常、解析异常、网络异常、连续失败、已停用。
- 展示最近抓取时间、最近成功时间、失败次数、文章总数。

关键点：

- RSS 源本身与解析模板解耦，一个模板可以复用给多个相似源。
- 源支持“默认解析”和“自定义模板解析”两种模式。

### 2.2 RSS 解析模板

模板用途：

- 针对不同源配置字段映射、正文提取规则、封面图提取规则、时间格式、去重策略。

V1 模板能力：

- 字段映射：
  - 标题：`title`
  - 链接：`link`
  - 作者：`author`、`dc:creator`
  - 摘要：`description`、`summary`
  - 正文：`content:encoded`、`description`
  - 发布时间：`pubDate`、`published`、`updated`
  - 封面图：`media:thumbnail`、`media:content`、正文首图
- 时间解析：
  - 默认自动解析常见 RSS/Atom 时间格式。
  - 支持为特殊源配置自定义时间格式。
- 内容清洗：
  - 移除脚本、广告块、空标签。
  - 图片链接规范化。
  - 可选保留原始 HTML。
- 去重策略：
  - 优先使用 GUID。
  - 其次使用原文链接。
  - 最后使用 `source_id + title + published_at` 生成指纹。
- 预览调试：
  - 输入 RSS URL 后抓取前 5 条。
  - 展示“原始字段”和“模板解析结果”对比。
  - 保存模板前可以看到标题、时间、正文、封面图是否解析正确。

建议模板配置结构：

```json
{
  "name": "通用 RSS 模板",
  "fieldMappings": {
    "title": ["title"],
    "url": ["link"],
    "author": ["author", "dc:creator"],
    "summary": ["description", "summary"],
    "content": ["content:encoded", "description"],
    "publishedAt": ["pubDate", "published", "updated"],
    "coverImage": ["media:thumbnail", "media:content", "content:firstImage"]
  },
  "cleanupRules": {
    "removeSelectors": [".ad", "script", "style"],
    "keepOriginalHtml": true
  },
  "deduplicateBy": ["guid", "url", "fingerprint"]
}
```

### 2.3 文章抓取与存储

功能：

- 后台定时抓取所有启用订阅源。
- 支持手动刷新。
- 对文章做标准化入库。
- 保留原始条目数据，方便模板调整后重新解析。
- 支持已读、收藏、稍后读、归档。
- 支持按源、分组、标签、关键词筛选。

存储策略：

- `raw_payload` 保存原始 RSS item 数据。
- `content_html` 保存清洗后的正文。
- `summary` 保存摘要。
- `fingerprint` 用于兜底去重。
- 抓取日志独立保存，方便排查源异常。

### 2.4 阅读体验

V1 阅读能力：

- 信息流列表：标题、源、时间、摘要、封面图、阅读状态。
- 阅读详情：正文、原文链接、收藏、稍后读、标记已读。
- 快捷筛选：全部、未读、收藏、稍后读、今日更新。
- 源分组：科技、财经、设计、AI、开发、综合等。
- 阅读设置：字号、行宽、主题模式。
- 搜索：V1 可先做标题与摘要搜索，后续扩展全文搜索。

建议阅读交互：

- 点击文章后右侧打开阅读舱，列表仍保留在左侧。
- 支持键盘快捷键的空间预留，但 V1 页面不要用大量说明文字占空间。
- 已读文章降低透明度或标题权重。

### 2.5 仪表盘

首页建议做成“资讯驾驶舱”，不是普通列表页。

模块：

- 今日新增文章数。
- 未读文章数。
- 活跃订阅源数。
- 抓取失败源数。
- 今日热词或高频标签。
- 最近更新源列表。
- 源健康雷达：展示哪些源异常、延迟、长期无更新。

V1 可以先用真实统计 + 简单图表，后续接入趋势分析。

## 3. 页面与 UI 设计

### 3.1 整体视觉方向

关键词：

- 深色科技感。
- 信息密度高，但不拥挤。
- 层次清楚，适合长时间阅读。
- 类似“新闻控制台 + 沉浸阅读器”的结合。

推荐主色：

- 背景：近黑蓝灰 `#0B1020`
- 主面板：深石墨 `#111827`
- 辅助面板：冷灰 `#1F2937`
- 主强调色：电光青 `#22D3EE`
- 次强调色：酸性绿 `#A3E635`
- 警告色：琥珀 `#F59E0B`
- 错误色：珊瑚红 `#FB7185`
- 正文浅色：`#E5E7EB`
- 次级文本：`#9CA3AF`

注意：

- 不要整站只用蓝紫渐变，避免视觉疲劳。
- 酷炫感主要来自空间层次、发光边线、数据状态、动效细节，而不是堆装饰。
- 页面组件优先使用 Naive UI，再通过主题变量和少量 CSS 做产品气质。

### 3.2 信息架构

主导航：

- 首页驾驶舱
- 阅读
- 订阅源
- 解析模板
- 收藏
- 稍后读
- 抓取日志
- 设置

布局：

- 左侧窄导航栏，图标 + 简短文字。
- 顶部工具栏，包含全局搜索、刷新按钮、主题切换、抓取状态。
- 主区域根据页面切换。

### 3.3 首页驾驶舱

布局建议：

- 顶部为关键指标条：今日新增、未读、活跃源、异常源。
- 中部左侧为“实时信息流”，右侧为“源健康状态”。
- 底部为“最近抓取日志”和“热门标签”。

视觉细节：

- 指标卡使用细边框和轻微发光，不使用厚重卡片堆叠。
- 异常源使用红色状态点和轻微脉冲动效。
- 健康状态可以用环形进度或小型状态矩阵。

### 3.4 阅读页

推荐三栏布局：

- 左栏：源分组与过滤器。
- 中栏：文章列表。
- 右栏：阅读舱。

中栏文章卡片：

- 标题最多两行。
- 展示源名、发布时间、标签。
- 有封面图时显示缩略图，无图时用源图标或色块。
- 未读文章标题更亮，已读文章弱化。

右侧阅读舱：

- 顶部显示标题、来源、发布时间、原文链接按钮。
- 正文区域使用舒适行宽。
- 工具按钮：收藏、稍后读、已读、打开原文、归档。
- 底部展示相关文章或同源最新文章。

酷炫但实用的细节：

- 文章切换时右侧阅读舱有轻微滑入和淡入。
- 选中文章左侧显示电光色竖线。
- 未读数量使用小型发光徽标。

### 3.5 订阅源管理页

布局：

- 顶部操作区：新增源、批量刷新、导入 OPML、导出 OPML。
- 表格展示源列表。
- 右侧抽屉用于新增或编辑源。

表格字段：

- 源名称
- URL
- 分组
- 解析模板
- 状态
- 最近成功时间
- 失败次数
- 文章数
- 操作

状态展示：

- 正常：绿色点。
- 抓取中：青色旋转图标。
- 解析异常：琥珀色。
- 网络异常：红色。
- 停用：灰色。

新增源抽屉：

- RSS URL 输入框。
- 自动探测按钮。
- 分组选择。
- 抓取频率选择。
- 解析模板选择。
- 预览最近 5 条文章。

### 3.6 解析模板页

页面目标：

- 让用户能看懂“某个源为什么解析出来是这样”。
- 让模板配置可视化，而不是只编辑 JSON。

布局：

- 左侧：模板列表。
- 中间：模板配置表单。
- 右侧：解析预览。

配置区域：

- 基础信息：模板名、描述、适用源。
- 字段映射：目标字段 + 候选 RSS 字段。
- 正文提取：字段优先级、正文清洗规则。
- 图片提取：媒体字段、正文首图、默认图。
- 去重策略：GUID、URL、指纹。
- 时间策略：自动解析或自定义格式。

预览区域：

- 原始 item 字段树。
- 解析后的文章卡片。
- 字段命中提示，例如“标题命中 title”、“封面命中 media:thumbnail”。
- 异常提示，例如“发布时间无法解析，已使用抓取时间”。

### 3.7 抓取日志页

功能：

- 查看每次抓取任务的开始时间、结束时间、结果、错误信息。
- 支持按源、状态、时间筛选。
- 点击日志查看错误详情。

价值：

- 调试 RSS 源很重要，尤其是后续模板多了以后。
- 用户能快速定位是网络失败、XML 不合法，还是模板字段没有命中。

## 4. 后端模块设计

建议包结构：

```text
com.example.newsrss
├── article
│   ├── controller
│   ├── service
│   ├── repository
│   ├── domain
│   └── dto
├── feed
│   ├── controller
│   ├── service
│   ├── repository
│   ├── domain
│   └── dto
├── parser
│   ├── service
│   ├── template
│   └── dto
├── fetch
│   ├── scheduler
│   ├── service
│   └── domain
├── user
├── common
└── config
```

核心服务：

- `FeedService`：管理订阅源。
- `FeedFetchService`：抓取 RSS 内容。
- `FeedParserService`：使用解析模板标准化条目。
- `ArticleService`：文章查询、状态更新、收藏与归档。
- `ParserTemplateService`：模板保存、预览、验证。
- `FetchLogService`：抓取日志记录与查询。

## 5. PostgreSQL 核心表设计

V1 建议表：

- `rss_feed`：订阅源。
- `rss_parser_template`：解析模板。
- `rss_article`：文章主表。
- `rss_article_user_state`：用户阅读状态，方便后续多用户扩展。
- `rss_feed_fetch_log`：抓取日志。
- `rss_tag`：标签。
- `rss_article_tag`：文章标签关系。

关键字段建议：

`rss_feed`：

- `id`
- `name`
- `url`
- `site_url`
- `icon_url`
- `group_name`
- `parser_template_id`
- `fetch_interval_minutes`
- `status`
- `last_fetched_at`
- `last_success_at`
- `failure_count`
- `enabled`
- `created_at`
- `updated_at`

`rss_parser_template`：

- `id`
- `name`
- `description`
- `template_config`
- `enabled`
- `created_at`
- `updated_at`

`rss_article`：

- `id`
- `feed_id`
- `guid`
- `title`
- `source_url`
- `author`
- `summary`
- `content_html`
- `cover_image_url`
- `published_at`
- `fetched_at`
- `fingerprint`
- `raw_payload`
- `created_at`
- `updated_at`

`rss_feed_fetch_log`：

- `id`
- `feed_id`
- `status`
- `started_at`
- `finished_at`
- `fetched_count`
- `created_count`
- `updated_count`
- `error_message`
- `error_detail`

说明：

- 所有 SQL 建表文件需要包含字段注释。
- 后续如果要变更 SQL，按你的规则追加到新的变更 SQL 文件，不直接修改已有 SQL。

## 6. API 设计草案

订阅源：

- `GET /api/feeds`
- `POST /api/feeds`
- `PUT /api/feeds/{id}`
- `DELETE /api/feeds/{id}`
- `POST /api/feeds/{id}/refresh`
- `POST /api/feeds/detect`

解析模板：

- `GET /api/parser-templates`
- `POST /api/parser-templates`
- `PUT /api/parser-templates/{id}`
- `DELETE /api/parser-templates/{id}`
- `POST /api/parser-templates/preview`

文章：

- `GET /api/articles`
- `GET /api/articles/{id}`
- `PATCH /api/articles/{id}/read-state`
- `PATCH /api/articles/{id}/favorite`
- `PATCH /api/articles/{id}/read-later`
- `PATCH /api/articles/{id}/archive`

仪表盘：

- `GET /api/dashboard/summary`
- `GET /api/dashboard/feed-health`
- `GET /api/dashboard/recent-articles`

抓取日志：

- `GET /api/fetch-logs`
- `GET /api/fetch-logs/{id}`

## 7. 前端模块设计

建议目录：

```text
src
├── api
├── assets
├── components
│   ├── article
│   ├── feed
│   ├── parser-template
│   └── layout
├── composables
├── router
├── stores
├── theme
│   └── naive.ts
├── types
└── views
```

页面：

- `DashboardView.vue`
- `ReaderView.vue`
- `FeedsView.vue`
- `ParserTemplatesView.vue`
- `FavoritesView.vue`
- `ReadLaterView.vue`
- `FetchLogsView.vue`
- `SettingsView.vue`

Naive UI 组件建议：

- 布局：`NLayout`、`NLayoutSider`、`NLayoutHeader`
- 表格：`NDataTable`
- 表单：`NForm`、`NInput`、`NSelect`、`NSwitch`
- 抽屉：`NDrawer`
- 弹窗：`NModal`
- 状态：`NTag`、`NBadge`、`NProgress`
- 通知：`NNotificationProvider`

主题配置：

- 放在 `src/theme/naive.ts`。
- 定义深色主题覆盖色、边框、圆角、字体、按钮状态。
- 全局保持 8px 或更小圆角，工具类按钮使用图标。

## 8. 推荐 V1 开发顺序

1. 初始化后端 Spring Boot、数据库连接、迁移工具。
2. 初始化前端 Vue 3 + TypeScript + Vite + Naive UI。
3. 建立基础表：订阅源、模板、文章、日志。
4. 实现通用 RSS 抓取与默认解析。
5. 实现订阅源管理页面。
6. 实现文章列表与阅读舱。
7. 实现解析模板保存与预览。
8. 实现后台定时抓取和抓取日志。
9. 实现仪表盘统计。
10. 补充测试、异常处理、文档记录。

## 9. V1 验收标准

功能验收：

- 可以新增至少 3 个不同 RSS 源。
- 可以定时抓取并存储文章。
- 可以通过模板调整字段映射，并预览解析结果。
- 可以阅读文章、标记已读、收藏、稍后读。
- 可以查看订阅源状态和抓取日志。

工程验收：

- 后端接口可运行。
- 前端页面可运行。
- 数据库迁移脚本包含字段注释。
- 关键逻辑有单元测试或集成测试。
- docs 目录有每次逻辑变更记录。

UI 验收：

- 深色专业风格统一。
- 阅读页三栏布局清晰。
- 异常状态有明显但不刺眼的提示。
- 模板预览能直观看到原始字段和解析结果。
- 移动端至少能正常查看文章列表和阅读详情。

## 10. 后续增强方向

- OPML 导入导出。
- 全文搜索。
- AI 摘要和关键词提取。
- 阅读偏好推荐。
- 多用户账号体系。
- WebSocket 实时抓取状态。
- RSS 源市场或公共模板库。
- 针对无法提供全文的源接入网页正文抽取。
- PWA 离线阅读。

