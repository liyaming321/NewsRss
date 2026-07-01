# M4 后端 API 变更记录

## 实现过程

- 新增统一成功响应 `ApiResponse<T>` 和分页响应 `PageResponse<T>`。
- 扩展统一异常处理，覆盖参数校验失败、参数类型错误、必填参数缺失、资源不存在和业务参数错误。
- 新增订阅源 API：
  - 订阅源列表、创建、更新、删除、手动刷新、RSS 探测。
- 新增文章 API：
  - 文章列表、文章详情、已读、收藏、稍后读、归档状态更新。
- 新增驾驶舱 API：
  - 摘要统计、订阅源健康状态、最近文章。
- 新增抓取日志 API：
  - 抓取日志列表和详情。
- 调整解析模板 API 返回统一响应结构，保留 M3 的模板预览能力。
- 补充领域实体 getter 和状态变更方法，让 Controller 只调用 Service，不直接操作实体细节。
- 文章状态更新使用文章行悲观锁串行化同一文章的状态写入，避免首次并发创建状态时撞唯一约束。
- 新增 Service 单元测试，覆盖订阅源创建/重复校验、文章状态创建更新、驾驶舱未读统计。

## 新增或调整接口

- `GET /api/feeds`
- `POST /api/feeds`
- `PUT /api/feeds/{id}`
- `DELETE /api/feeds/{id}`
- `POST /api/feeds/{id}/refresh`
- `POST /api/feeds/detect`
- `GET /api/articles`
- `GET /api/articles/{id}`
- `PATCH /api/articles/{id}/read-state`
- `PATCH /api/articles/{id}/favorite`
- `PATCH /api/articles/{id}/read-later`
- `PATCH /api/articles/{id}/archive`
- `GET /api/parser-templates`
- `POST /api/parser-templates`
- `PUT /api/parser-templates/{id}`
- `DELETE /api/parser-templates/{id}`
- `POST /api/parser-templates/preview`
- `GET /api/dashboard/summary`
- `GET /api/dashboard/feed-health`
- `GET /api/dashboard/recent-articles`
- `GET /api/fetch-logs`
- `GET /api/fetch-logs/{id}`

## 验证命令

```bash
mvn -f backend/pom.xml test
```

## 执行结果

- 成功。
- 测试结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 覆盖内容：
  - Spring Boot 默认 profile 上下文加载。
  - 模板解析字段映射和异常提示。
  - 订阅源创建与重复地址校验。
  - 文章状态首次创建与收藏更新。
  - 驾驶舱未读统计。

## 接口验证命令与结果

### 健康检查

```bash
curl -sS http://localhost:8080/api/health
```

结果摘要：

- 返回 `status: UP`。

### 订阅源列表

```bash
curl -sS 'http://localhost:8080/api/feeds?page=0&size=3' | jq
```

结果摘要：

- `success: true`。
- 返回 3 条订阅源。
- 总数：`4`。
- 示例字段包含 `feedName`、`feedUrl`、`healthStatus`、`articleCount`、`parserTemplateId`。

### 订阅源探测

```bash
FIXTURE_URL="file:///Users/lym/Documents/workspace/aiDemo/newsRss/backend/src/test/resources/rss/m3-template-preview.xml"
curl -sS -X POST http://localhost:8080/api/feeds/detect \
  -H 'Content-Type: application/json' \
  --data-binary "{\"feedUrl\":\"${FIXTURE_URL}\"}" | jq
```

结果摘要：

- `success: true`。
- Feed 标题：`M3 模板预览测试源`。
- 站点地址：`https://example.com`。
- 语言：`zh-CN`。
- 条目数量：`2`。

### 订阅源创建、更新、刷新、删除

```bash
curl -sS -X POST http://localhost:8080/api/feeds \
  -H 'Content-Type: application/json' \
  --data-binary '{...临时订阅源 JSON...}' | jq

curl -sS -X PUT http://localhost:8080/api/feeds/5 \
  -H 'Content-Type: application/json' \
  --data-binary '{...更新订阅源 JSON...}' | jq

curl -sS -X POST http://localhost:8080/api/feeds/5/refresh | jq

curl -sS -X DELETE http://localhost:8080/api/feeds/5 | jq
```

结果摘要：

- 创建成功，临时订阅源 ID：`5`。
- 更新成功，名称变为 `M4 临时测试源 v2`，抓取间隔为 `30`。
- 手动刷新成功：`fetchedCount: 2`、`newCount: 2`、`duplicateCount: 0`。
- 删除成功，列表中不再出现 ID `5`。

### 文章列表与详情

```bash
curl -sS 'http://localhost:8080/api/articles?page=0&size=2' | jq
```

结果摘要：

- `success: true`。
- 返回 2 条文章。
- 总数：`34`。
- 列表项包含 `feedName`、`title`、`summary`、`publishedAt`、`state`。

```bash
ARTICLE_ID=$(curl -sS 'http://localhost:8080/api/articles?page=0&size=1' | jq -r '.data.items[0].id')
curl -sS "http://localhost:8080/api/articles/${ARTICLE_ID}" | jq
```

结果摘要：

- `success: true`。
- 返回文章标题、正文、原始字段 `rawPayload` 和解析轨迹 `parseTrace`。
- 默认状态包含 `read`、`favorite`、`readLater`、`archived`。

### 文章状态更新

```bash
ARTICLE_ID=$(curl -sS 'http://localhost:8080/api/articles?page=0&size=1' | jq -r '.data.items[0].id')
curl -sS -X PATCH "http://localhost:8080/api/articles/${ARTICLE_ID}/read-state" \
  -H 'Content-Type: application/json' \
  --data-binary '{"value":true}' | jq

curl -sS -X PATCH "http://localhost:8080/api/articles/${ARTICLE_ID}/favorite" \
  -H 'Content-Type: application/json' \
  --data-binary '{"value":true}' | jq

curl -sS -X PATCH "http://localhost:8080/api/articles/${ARTICLE_ID}/read-later" \
  -H 'Content-Type: application/json' \
  --data-binary '{"value":true}' | jq

curl -sS -X PATCH "http://localhost:8080/api/articles/${ARTICLE_ID}/archive" \
  -H 'Content-Type: application/json' \
  --data-binary '{"value":false}' | jq
```

结果摘要：

- 已读更新成功，返回 `read: true` 和 `readAt`。
- 收藏更新成功，返回 `favorite: true` 和 `favoritedAt`。
- 稍后读更新成功，返回 `readLater: true` 和 `readLaterAt`。
- 归档更新成功，返回 `archived: false`。
- 并发执行 `read-later` 与 `archive` 两个状态更新时均返回 `success: true`，未再出现唯一约束冲突。

### 解析模板接口

```bash
curl -sS http://localhost:8080/api/parser-templates | jq
```

结果摘要：

- `success: true`。
- 当前保留模板数量：`1`。
- 模板编码：`m3-fixture-template`。

```bash
FIXTURE_URL="file:///Users/lym/Documents/workspace/aiDemo/newsRss/backend/src/test/resources/rss/m3-template-preview.xml"
curl -sS -X POST http://localhost:8080/api/parser-templates/preview \
  -H 'Content-Type: application/json' \
  --data-binary "{\"feedUrl\":\"${FIXTURE_URL}\",\"templateId\":1,\"limit\":2}" | jq
```

结果摘要：

- `success: true`。
- 模板编码：`m3-fixture-template`。
- 返回条目数：`2`。
- 命中率：`0.8125`。
- 示例标题：`模板标题一`。
- 异常提示：`publishedAt 时间解析失败：bad-time`。

### 驾驶舱接口

```bash
curl -sS http://localhost:8080/api/dashboard/summary | jq
```

结果摘要：

- `feedCount: 4`。
- `enabledFeedCount: 4`。
- `articleCount: 34`。
- `unreadCount: 33`。
- `favoriteCount: 1`。
- `readLaterCount: 1`。
- `failedFetchLogCount: 2`。

```bash
curl -sS http://localhost:8080/api/dashboard/feed-health | jq
```

结果摘要：

- `healthy: 3`。
- `error: 1`。
- `unknown: 0`。
- `warning: 0`。

```bash
curl -sS 'http://localhost:8080/api/dashboard/recent-articles?limit=2' | jq
```

结果摘要：

- `success: true`。
- 返回 2 条最近文章。

### 抓取日志接口

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=2' | jq
```

结果摘要：

- `success: true`。
- 返回 2 条日志。
- 总数：`11`。
- 示例日志状态：`SUCCESS`。

```bash
LOG_ID=$(curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=1' | jq -r '.data.items[0].id')
curl -sS "http://localhost:8080/api/fetch-logs/${LOG_ID}" | jq
```

结果摘要：

- `success: true`。
- 返回日志 ID、订阅源 ID、状态、抓取数、新增数、重复数。

### 错误响应

参数错误：

```bash
curl -sS -X POST http://localhost:8080/api/feeds \
  -H 'Content-Type: application/json' \
  --data-binary '{}' | jq
```

结果摘要：

- HTTP `400`。
- `message: 请求参数不合法`。
- 明细包含：
  - `feedUrl: RSS 地址不能为空`
  - `feedName: 订阅源名称不能为空`

资源不存在：

```bash
curl -sS http://localhost:8080/api/articles/999999 | jq
```

结果摘要：

- HTTP `404`。
- `message: 文章不存在：999999`。
- `path: /api/articles/999999`。

## 数据库验证

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss \
  -c "select count(*) as feeds from rss_feed; select count(*) as articles from rss_article; select count(*) as states from rss_article_user_state; select count(*) as fetch_logs from rss_feed_fetch_log;"
```

结果摘要：

- `rss_feed`: `4`。
- `rss_article`: `34`。
- `rss_article_user_state`: `1`。
- `rss_feed_fetch_log`: `11`。

## 说明

- M4 未新增数据库字段，复用 M1 的表结构，因此没有新增迁移脚本。
- 统一成功响应结构为 `success`、`message`、`data`、`checkedAt`。
- 错误响应沿用 `status`、`message`、`details`、`path`、`checkedAt`。
- M4 的手动刷新接口已可用；定时调度、批量刷新和健康状态高级策略仍留到 M5。

## 遗留问题

- 前端尚未接入 M4 API。
- 手动刷新目前通过 `RssFeedFetchService.fetch(feedUrl)` 按 URL 查询订阅源；后续 M5 可增加按 ID 的刷新入口，便于支持同 URL 多环境或高级调度上下文。
