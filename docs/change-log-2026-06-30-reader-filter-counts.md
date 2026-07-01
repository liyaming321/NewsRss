# 阅读页过滤器数量口径修复

## 实现过程

- 修复全部文章页过滤器数量与列表总数不一致的问题：文章列表查询增加 `filter`、`keyword`、`feedId` 的统一后端查询口径，过滤器数量不再用当前页前端数据临时计算。
- 新增并接入文章统计接口 `/api/articles/stats`，支持按订阅源、筛选类型和搜索关键词统计，阅读页的过滤器、源分组、列表总数统一使用后端统计结果。
- 修复 PostgreSQL 在空搜索词下把 JPQL 参数推断为 `bytea` 导致 `like` 查询 500 的问题：服务层将空关键词归一为空字符串，仓库层使用 `:keyword = ''` 跳过搜索条件。
- 阅读页切换“收藏/稍后读/未读/今日更新”后，列表分页总数、标题描述和源分组数量会随当前筛选同步刷新。
- 补充 `ArticleServiceTest`，覆盖空关键词归一和源分组统计筛选参数传递。

## 验证命令

```bash
mvn -f backend/pom.xml test
pnpm --dir frontend build
curl -sS http://127.0.0.1:8080/api/articles/stats | jq '.data | {totalCount,unreadCount,favoriteCount,readLaterCount,todayCount}'
curl -sS 'http://127.0.0.1:8080/api/articles?filter=favorite&page=0&size=40' | jq '.data | {totalElements,totalPages,page,size,itemCount:(.items|length)}'
curl -sS 'http://127.0.0.1:8080/api/articles?filter=readLater&page=0&size=40' | jq '.data | {totalElements,totalPages,page,size,itemCount:(.items|length)}'
```

## 执行结果

- 后端测试通过：`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- 前端构建通过：`vue-tsc -b && vite build` 成功，仅保留 Vite chunk 大小超过 500 kB 的提示。
- 真实接口验证通过：统计接口返回 `favoriteCount=2`、`readLaterCount=2`，收藏列表返回 `totalElements=2`、`totalPages=1`、`itemCount=2`。
- Playwright 页面验证通过：访问 `http://127.0.0.1:5173/reader?filter=favorite` 后，页面显示“收藏文章”“共 2 篇，当前页 2 篇”，不再显示全部文章的 141 和 4 页分页。
- 验收截图：`output/playwright/reader-filter-counts-fixed.png`。

## 遗留问题

- “今日更新”当前仍按后端 UTC 当日零点统计；如果需要按中国时区自然日统计，后续可切换为 `Asia/Shanghai` 口径。
- 前端生产构建仍有单包超过 500 kB 的提示，当前不影响功能，可在后续做路由级代码分割优化。
