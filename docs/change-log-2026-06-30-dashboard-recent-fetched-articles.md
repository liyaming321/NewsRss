# 驾驶舱实时信息流按入库时间排序

## 实现过程

- 调整驾驶舱实时信息流的数据来源排序，将 `/api/dashboard/recent-articles` 从按 `publishedAt/fetchedAt` 混合排序改为按文章入库时间 `fetchedAt desc` 排序。
- 同一轮抓取内多篇文章可能拥有相同 `fetchedAt`，因此增加 `id desc` 作为稳定兜底排序，保证最新入库记录优先展示。
- 将仓库方法从 `findRecentArticles` 更名为 `findRecentlyFetchedArticles`，让方法名直接表达“最近入库文章”的业务含义。
- 补充 `DashboardServiceTest`，验证驾驶舱最近文章调用的是按入库时间查询的方法，并校验分页大小。

## 验证命令

```bash
mvn -f backend/pom.xml test
pnpm --dir frontend build
curl -sS 'http://127.0.0.1:8080/api/dashboard/recent-articles?limit=4' | jq '.data | map({id,title,feedName,fetchedAt,publishedAt})'
```

## 执行结果

- 后端测试通过：`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- 前端构建通过：`vue-tsc -b && vite build` 成功，仅保留 Vite chunk 大小超过 500 kB 的提示。
- 接口验证通过：驾驶舱最近文章返回 `fetchedAt=2026-06-30T14:15:12.350053Z` 的最新入库文章，早于该时间入库但新闻发布时间更新的文章不再排在实时信息流前面。
- 页面验证通过：驾驶舱“实时信息流”显示“最近 4 条入库文章”，首屏展示最新入库的“东方财富网-券商晨报”文章，页面截图保存于 `output/playwright/dashboard-recent-fetched-articles.png`。

## 遗留问题

- 阅读页文章列表仍按新闻发布时间优先排序，这是阅读场景的原有逻辑，本次仅调整驾驶舱实时信息流。
- 今日新增指标仍按 UTC 当日入库统计；如果后续需要贴合中国本地自然日，可统一调整为 `Asia/Shanghai`。
