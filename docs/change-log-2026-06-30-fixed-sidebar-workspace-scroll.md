# 固定侧栏和工作区滚动变更记录

## 实现过程

- 调整全局应用壳层：
  - `AppLayout.vue` 将 `.app-shell` 固定为 `100vh`，并隐藏外层溢出，避免页面整体被内容撑高。
  - `AppLayout.vue` 将 `.app-sidebar` 固定为视口高度，并允许侧栏自身在内容过多时内部滚动。
  - `AppLayout.vue` 将 `.app-content` 改为 `calc(100vh - 76px)`，右侧功能区域独立上下滚动。
- 优化阅读页和收藏入口：
  - `NavigationItem` 增加 `query` 字段，让收藏和稍后读可以复用阅读页并进入指定筛选状态。
  - `AppLayout.vue` 修正导航高亮逻辑，普通阅读筛选高亮“阅读”，收藏和稍后读分别高亮对应入口。
  - `ReaderView.vue` 支持从 `route.query.filter` 同步 `all`、`unread`、`favorite`、`readLater`、`today` 筛选。
  - `ReaderView.vue` 将阅读页面改为顶部工具条加三栏工作区，过滤器、文章列表和详情区在各自容器内滚动。
  - `ReaderView.vue` 压缩文章行、间距和面板内边距，减少 1366x768 下的首屏占用。
- 优化解析模板页：
  - `ParserTemplatesView.vue` 将页面改为顶部工具条加双栏工作区。
  - 模板列表和右侧模板工作台都增加高度约束和内部滚动。
  - 压缩工具条、面板、字段映射和预览区间距，避免长表单撑开页面主体。
- 本次未修改后端逻辑，未新增 SQL 和数据库迁移文件。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次布局调整。

### 阅读页一屏检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" open http://127.0.0.1:5173/reader --headed
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" resize 1366 768
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果摘要：

```json
{
  "url": "http://127.0.0.1:5173/reader",
  "innerHeight": 768,
  "bodyScrollHeight": 768,
  "documentScrollHeight": 768,
  "appContent": {
    "clientHeight": 692,
    "scrollHeight": 692,
    "overflowY": "auto"
  },
  "sidebar": {
    "clientHeight": 768,
    "scrollHeight": 768,
    "overflowY": "auto"
  }
}
```

截图路径：

- `output/playwright/reader-fit-1366x768.png`

### 收藏页入口检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" goto "http://127.0.0.1:5173/reader?filter=favorite"
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果摘要：

```json
{
  "url": "http://127.0.0.1:5173/reader?filter=favorite",
  "activeFilter": "收藏2",
  "activeNav": ["收藏"],
  "bodyScrollHeight": 768,
  "documentScrollHeight": 768,
  "innerHeight": 768
}
```

截图路径：

- `output/playwright/favorites-fit-1366x768.png`

### 未读筛选入口检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" goto "http://127.0.0.1:5173/reader?filter=unread"
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果摘要：

```json
{
  "url": "http://127.0.0.1:5173/reader?filter=unread",
  "activeFilter": "未读37",
  "activeNav": ["阅读"],
  "bodyScrollHeight": 768,
  "documentScrollHeight": 768,
  "innerHeight": 768
}
```

### 解析模板页滚动检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" goto http://127.0.0.1:5173/parser-templates
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果摘要：

```json
{
  "url": "http://127.0.0.1:5173/parser-templates",
  "bodyScrollHeight": 768,
  "documentScrollHeight": 768,
  "innerHeight": 768,
  "templateWorkbench": {
    "clientHeight": 570,
    "scrollHeight": 2089,
    "overflowY": "auto"
  },
  "templateList": {
    "clientHeight": 568,
    "scrollHeight": 568,
    "overflowY": "auto"
  }
}
```

截图路径：

- `output/playwright/parser-templates-fit-1366x768.png`

## 遗留问题

- 解析模板页右侧工作台内容较长，目前按用户要求保留在右侧内部滚动；如果后续希望进一步减少滚动长度，可以把字段映射、清洗规则、预览结果拆成标签页。
- 前端构建仍有 chunk 大小警告，建议后续按路由拆包。
