# M5 定时任务与抓取日志变更记录

## 实现过程

- 后端启用 Spring Scheduling，在 `NewsrssBackendApplication` 增加 `@EnableScheduling`。
- 新增 `RssFetchSchedulerProperties`，支持通过环境变量配置定时任务开关、初始延迟、固定延迟和批次大小。
- 新增 `RssFeedScheduleService`，提供三类抓取入口：
  - `@Scheduled` 自动抓取到期订阅源。
  - `POST /api/feeds/schedule/run-once` 手动触发一次到期源调度。
  - `POST /api/feeds/refresh-batch` 批量刷新指定源；请求为空时刷新全部启用源。
- 增强 `RssFeedRepository`，支持查询到期启用订阅源和全部启用订阅源。
- 调整 `RssFeedFetchService`，新增 `fetchByFeedId(Long feedId)`，让手动刷新和调度刷新都按订阅源主键执行，避免只按 URL 重新查找的歧义。
- 调整 `FeedService.refreshFeed(Long id)`，复用按主键刷新能力。
- 调整订阅源健康状态策略：
  - 连续失败 1 到 2 次标记为 `WARNING`。
  - 连续失败大于等于 3 次标记为 `ERROR`。
  - 抓取成功后恢复 `HEALTHY` 并清零连续失败次数。
- 新增 M5 DTO：`FeedBatchRefreshRequest`、`FeedBatchRefreshResponse`、`FeedScheduleRunResponse`。
- 新增后端单元测试：
  - `RssFeedScheduleServiceTest` 覆盖到期源调度和批量刷新。
  - `RssFeedTest` 覆盖连续失败健康状态变化。
- 前端补齐抓取日志查看闭环：
  - 新增 `frontend/src/api/fetchLogs.ts`。
  - 将 `FetchLogsView.vue` 从占位页改为真实日志表格，展示状态、订阅源、抓取数量、新增数量、重复数量、耗时、开始时间和错误信息。

本次未新增数据库表字段，未新增 Flyway 迁移文件。

## 验证命令

### 后端单元测试

```bash
mvn -f backend/pom.xml test
```

执行结果：

- 构建成功。
- 测试结果：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功生成 `frontend/dist`。
- Vite 输出 chunk 大小提示：`Some chunks are larger than 500 kB after minification`，不影响本次构建结果。

### 后端启动验证

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
NEWSRSS_FETCH_SCHEDULER_INITIAL_DELAY_MS=300000 \
NEWSRSS_FETCH_SCHEDULER_FIXED_DELAY_MS=300000 \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

执行结果：

- 后端在 `8080` 启动成功。
- Flyway 校验通过：`Successfully validated 1 migration`。
- 数据库版本状态：`Schema "public" is up to date. No migration necessary.`

健康检查：

```bash
curl -sS http://localhost:8080/api/health
```

响应摘要：

```json
{
  "applicationName": "NewsRss",
  "version": "0.0.1-SNAPSHOT",
  "status": "UP"
}
```

### 手动刷新单个源

```bash
curl -sS -X POST http://localhost:8080/api/feeds/1/refresh | jq
```

响应摘要：

```json
{
  "success": true,
  "message": "订阅源刷新已执行",
  "data": {
    "feedUrl": "https://hnrss.org/frontpage",
    "success": true,
    "fetchedCount": 20,
    "newCount": 6,
    "duplicateCount": 14,
    "failedCount": 0,
    "errorMessage": null
  }
}
```

### 批量刷新指定源

```bash
curl -sS -X POST http://localhost:8080/api/feeds/refresh-batch \
  -H 'Content-Type: application/json' \
  --data-binary '{"feedIds":[2,3]}' | jq
```

响应摘要：

```json
{
  "success": true,
  "message": "批量刷新已执行",
  "data": {
    "requestedCount": 2,
    "successCount": 2,
    "failureCount": 0,
    "results": [
      {
        "feedUrl": "https://github.blog/feed/",
        "success": true,
        "fetchedCount": 10,
        "newCount": 0,
        "duplicateCount": 10,
        "failedCount": 0
      },
      {
        "feedUrl": "https://xkcd.com/atom.xml",
        "success": true,
        "fetchedCount": 4,
        "newCount": 0,
        "duplicateCount": 4,
        "failedCount": 0
      }
    ]
  }
}
```

### 手动触发一次到期源调度

验证前将测试源 `next_fetch_at` 设置为过去时间，用于模拟到期源。

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss \
  -c "UPDATE rss_feed SET next_fetch_at = now() - interval '1 minute' WHERE id = 6;"

curl -sS -X POST http://localhost:8080/api/feeds/schedule/run-once | jq
```

响应摘要：

```json
{
  "success": true,
  "message": "调度刷新已执行",
  "data": {
    "dueCount": 2,
    "successCount": 0,
    "failureCount": 2
  }
}
```

日志样例：

```json
{
  "id": 20,
  "feedId": 6,
  "feedName": "M5 临时失败源",
  "status": "FAILED",
  "fetchedCount": 0,
  "newCount": 0,
  "duplicateCount": 0,
  "failedCount": 0,
  "errorMessage": "抓取或解析 RSS 失败：Remote host terminated the handshake",
  "startedAt": "2026-06-30T02:16:21.207043Z",
  "finishedAt": "2026-06-30T02:16:21.739139Z",
  "durationMs": 532
}
```

### 自动定时任务触发

验证前将测试源 `next_fetch_at` 设置为过去时间，并使用 2 秒初始延迟启动服务。

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss \
  -c "UPDATE rss_feed SET next_fetch_at = now() - interval '1 minute' WHERE id = 6;"

NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
NEWSRSS_FETCH_SCHEDULER_INITIAL_DELAY_MS=2000 \
NEWSRSS_FETCH_SCHEDULER_FIXED_DELAY_MS=300000 \
NEWSRSS_FETCH_SCHEDULER_BATCH_SIZE=1 \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

执行结果：

- 服务启动后自动定时任务新增抓取日志 `id=21`。
- 临时失败源连续失败次数从 `4` 增加到 `5`。

日志样例：

```json
{
  "id": 21,
  "feedId": 6,
  "feedName": "M5 临时失败源",
  "status": "FAILED",
  "fetchedCount": 0,
  "newCount": 0,
  "duplicateCount": 0,
  "failedCount": 0,
  "errorMessage": "抓取或解析 RSS 失败：Remote host terminated the handshake",
  "startedAt": "2026-06-30T02:17:11.173559Z",
  "finishedAt": "2026-06-30T02:17:11.791035Z",
  "durationMs": 617
}
```

### 连续失败健康状态

创建临时失败源：

```bash
curl -sS -X POST http://localhost:8080/api/feeds \
  -H 'Content-Type: application/json' \
  --data-binary '{"feedName":"M5 临时失败源","feedUrl":"https://m5-invalid-1782785754.invalid/not-rss.xml","category":"验证","fetchIntervalMinutes":5,"enabled":true}' | jq
```

创建结果：

```json
{
  "id": 6,
  "feedName": "M5 临时失败源",
  "healthStatus": "UNKNOWN",
  "fetchIntervalMinutes": 5,
  "consecutiveFailureCount": 0,
  "nextFetchAt": null
}
```

连续刷新三次：

```bash
curl -sS -X POST http://localhost:8080/api/feeds/6/refresh | jq
curl -sS 'http://localhost:8080/api/feeds?page=0&size=20' | jq '.data.items[] | select(.id==6)'
```

状态变化结果：

- 第 1 次失败：`healthStatus=WARNING`，`consecutiveFailureCount=1`，`nextFetchAt` 按 5 分钟间隔更新。
- 第 2 次失败：`healthStatus=WARNING`，`consecutiveFailureCount=2`，`nextFetchAt` 按 5 分钟间隔更新。
- 第 3 次失败：`healthStatus=ERROR`，`consecutiveFailureCount=3`，`nextFetchAt` 按 5 分钟间隔更新。

第三次状态摘要：

```json
{
  "id": 6,
  "healthStatus": "ERROR",
  "consecutiveFailureCount": 3,
  "lastFailureAt": "2026-06-30T02:16:03.83836Z",
  "nextFetchAt": "2026-06-30T02:21:03.83836Z",
  "fetchIntervalMinutes": 5
}
```

### 抓取日志 API 与前端页面

接口验证：

```bash
curl -sS 'http://127.0.0.1:5173/api/fetch-logs?page=0&size=2' | jq
```

响应摘要：

```json
{
  "success": true,
  "count": 2,
  "latest": [
    {
      "id": 21,
      "feedName": "M5 临时失败源",
      "status": "FAILED"
    },
    {
      "id": 20,
      "feedName": "M5 临时失败源",
      "status": "FAILED"
    }
  ]
}
```

浏览器验证：

```bash
export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
export PWCLI="$CODEX_HOME/skills/playwright/scripts/playwright_cli.sh"
"$PWCLI" open http://127.0.0.1:5173/fetch-logs
"$PWCLI" snapshot
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m5-fetch-logs.png --full-page
```

执行结果：

- 页面标题为 `抓取日志`。
- 页面展示 `最近抓取任务`，显示 `共 20 条日志，展示最近 20 条执行结果。`
- 表格中可见成功与失败日志，包含状态、订阅源、数量、失败数、耗时、开始时间和错误信息。
- 截图已保存：`output/playwright/m5-fetch-logs.png`。

## 遗留问题

- 测试库中保留了 M5 验证用的临时失败源 `id=6`，名称为 `M5 临时失败源`，用于保留连续失败和调度日志证据；后续如不需要可通过订阅源管理接口停用或删除。
- 前端生产构建存在 Vite chunk 大小提示，当前不影响运行；后续进入前端深度开发时可考虑路由懒加载或拆分 vendor chunk。
