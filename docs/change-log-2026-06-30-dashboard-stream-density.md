# 驾驶舱实时信息流密度优化变更记录

## 实现过程

- 优化 `frontend/src/views/DashboardView.vue` 的驾驶舱布局：
  - 实时信息流请求条数由 8 条调整为 4 条，减少首屏数据量。
  - 实时信息流不再跨两行占据左侧大面积空间，改为和源健康、今日热词、最近抓取并列的紧凑面板。
  - 文章行高度从大卡片压缩为紧凑列表，默认只展示标题、来源和抓取时间。
  - 移除实时信息流中的摘要展示，避免长摘要拉高页面。
  - 最近抓取列表改成更窄的两行结构，减少横向挤压。
- 清理不再使用的 `getArticleSummary` 和 `isMetadataOnlySummary` 方法。
- 本次未修改后端逻辑，未新增 SQL 和数据库迁移文件。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次驾驶舱布局优化。

### 页面访问

```bash
curl -sS -o /tmp/newsrss-dashboard-page.html -w '%{http_code}' 'http://127.0.0.1:5173/'
```

执行结果：

- 返回 `200`。

### 最近文章接口

```bash
curl -sS 'http://localhost:8080/api/dashboard/recent-articles?limit=4' | jq 'length'
```

执行结果：

- 返回 `4`，驾驶舱实时信息流按 4 条最近文章展示。

## 遗留问题

- 前端构建仍有 chunk 大小警告，建议后续按路由拆包。
- 如果后续需要查看更多实时信息流，可增加“查看全部”跳转到阅读页或展开按钮。
