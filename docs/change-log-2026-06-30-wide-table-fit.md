# 订阅源和抓取日志宽表适配变更记录

## 实现过程

- 修复订阅源页和抓取日志页右侧内容被截掉的问题：
  - `AppLayout.vue` 的 `.app-content` 增加 `min-width: 0`，允许内部页面在主布局中正常收缩。
  - `AppLayout.vue` 的 `.app-content` 增加 `overflow-x: hidden`，避免宽表把整个页面撑出视口。
  - `FeedsView.vue` 的 `.feeds-view` 增加 `min-width: 0`。
  - `FeedsView.vue` 的 `.feed-console` 增加 `min-width: 0` 和 `overflow-x: auto`。
  - `FetchLogsView.vue` 的 `.fetch-log-view` 增加 `min-width: 0`。
  - `FetchLogsView.vue` 的 `.log-table-panel` 增加 `min-width: 0` 和 `overflow-x: auto`。
  - 移除订阅源表格和抓取日志表格的右侧固定操作列，避免固定列在窄屏下被滚动容器裁切。
- 本次未修改后端逻辑，未新增 SQL 和数据库迁移文件。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次宽表适配。

### 订阅源页宽度检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" open http://127.0.0.1:5173/feeds
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" resize 1366 768
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果摘要：

```json
{
  "body": 1366,
  "viewport": 1366,
  "content": 1118,
  "feedConsole": 1070,
  "table": 1032,
  "overflow": false
}
```

截图路径：

- `output/playwright/feeds-table-fit.png`

### 抓取日志页宽度检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" open http://127.0.0.1:5173/fetch-logs
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" resize 1366 768
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果摘要：

```json
{
  "url": "http://127.0.0.1:5173/fetch-logs",
  "body": 1366,
  "viewport": 1366,
  "content": 1118,
  "logPanel": 1070,
  "table": 1036,
  "overflow": false
}
```

截图路径：

- `output/playwright/fetch-logs-table-fit.png`

## 遗留问题

- 如果后续继续增加表格列，建议优先减少列宽或用详情抽屉承载低频字段，避免重新引入横向裁切。
- 前端构建仍有 chunk 大小警告，建议后续按路由拆包。
