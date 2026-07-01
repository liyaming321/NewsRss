# 抓取日志分页变更记录

## 实现过程

- 优化 `frontend/src/views/FetchLogsView.vue`：
  - 增加远程分页状态：`currentPage`、`pageSize`、`totalPages`。
  - 日志查询从固定 `page=0,size=50` 改为按当前页码和每页条数请求。
  - 增加 Naive UI `NPagination`，支持页码切换和每页 `10/20/50/100` 条切换。
  - 按源、状态、时间筛选变化后自动回到第一页。
  - 重置筛选后自动回到第一页。
  - 顶部文案改为显示总数、当前页、总页数和当前页数据范围。
  - 分页区域增加分隔线和辅助文案，避免和表格内容混在一起。
- 后端分页接口已存在，本次未修改后端逻辑。
- 本次未新增 SQL 和数据库迁移文件。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次分页功能。

### 后端分页接口

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=1&size=10' | jq
```

执行结果摘要：

```json
{
  "success": true,
  "total": 36,
  "page": 1,
  "size": 10,
  "totalPages": 4,
  "items": 10
}
```

### 页面访问

```bash
curl -sS -o /tmp/newsrss-fetchlogs-page.html -w '%{http_code}' 'http://127.0.0.1:5173/fetch-logs'
```

执行结果：

- 返回 `200`。

### 浏览器截图

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" open http://127.0.0.1:5173/fetch-logs
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" screenshot --filename output/playwright/fetch-logs-pagination.png
```

执行结果：

- 抓取日志页显示分页控件。
- 顶部显示 `共 36 条日志，当前第 1 / 2 页，展示 1-20 条。`
- 截图路径：`output/playwright/fetch-logs-pagination.png`

## 遗留问题

- 状态统计目前统计当前页数据，不是全量筛选结果聚合；如需全局统计，可后续增加专门的日志统计接口。
- 前端构建仍有 chunk 大小警告，建议后续按路由拆包。
