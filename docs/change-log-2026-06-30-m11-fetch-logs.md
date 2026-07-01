# M11 抓取日志页变更记录

## 实现过程

- 后端扩展抓取日志筛选：
  - `FetchLogController` 支持 `feedId`、`status`、`startedFrom`、`startedTo` 查询参数。
  - `FetchLogService` 使用 `Specification` 动态拼接订阅源、状态和时间范围条件，并固定按 `startedAt desc` 排序。
  - `RssFeedFetchLogRepository` 增加 `JpaSpecificationExecutor`，避免 PostgreSQL 对 `:startedFrom is null` 这类 nullable timestamp 参数无法推断类型导致 500。
  - `FetchLogServiceTest` 增加筛选参数和非法状态测试。
- 前端实现抓取日志排障台：
  - `frontend/src/api/fetchLogs.ts` 增加日志筛选参数和详情接口。
  - `frontend/src/views/FetchLogsView.vue` 实现状态统计、关键词搜索、按源筛选、按状态筛选、时间范围筛选、日志表格和详情抽屉。
  - 详情抽屉展示请求源、状态、HTTP、抓取数量、耗时、错误信息、错误详情和原始响应样本。
  - 前端按错误文本分类为正常、网络失败、解析失败、模板字段、抓取失败和运行中；网络错误判定优先于模板堆栈文本，避免 `parseWithBoundTemplate` 误判。
- 当前 M11 未新增 SQL 和数据库迁移文件。

## 验证命令

### 后端测试

```bash
mvn -f backend/pom.xml test
```

执行结果：

- Maven 构建成功。
- 测试结果：`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 提示部分 chunk 大于 `500 kB`，当前不阻塞 M11 验收。

### 后端启动

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

执行结果：

- 后端启动成功，监听 `8080`。
- Flyway 校验通过，数据库版本为 PostgreSQL 16.14。

### 前端页面

```bash
pnpm --dir frontend dev --host 127.0.0.1
```

执行结果：

- Vite 服务已运行在 `http://127.0.0.1:5173`。
- 抓取日志页访问地址：`http://127.0.0.1:5173/fetch-logs`。

## 接口联调结果

### 日志列表

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=5' | jq
```

响应摘要：

```json
{
  "totalElements": 26,
  "totalPages": 6,
  "items": [
    {
      "id": 28,
      "feedName": "https://example.invalid/not-rss.xml",
      "status": "FAILED",
      "errorMessage": "抓取或解析 RSS 失败：Remote host terminated the handshake"
    },
    {
      "id": 27,
      "feedName": "xkcd.com",
      "status": "SUCCESS",
      "fetchedCount": 4,
      "duplicateCount": 4
    }
  ]
}
```

### 状态筛选

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=5&status=SUCCESS' | jq
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=5&status=FAILED' | jq
```

响应摘要：

```json
{
  "successCount": 15,
  "failedCount": 11,
  "successExample": {
    "id": 27,
    "feedName": "xkcd.com",
    "status": "SUCCESS",
    "fetchedCount": 4,
    "duplicateCount": 4
  },
  "failedExample": {
    "id": 28,
    "feedName": "https://example.invalid/not-rss.xml",
    "status": "FAILED",
    "errorMessage": "抓取或解析 RSS 失败：Remote host terminated the handshake"
  }
}
```

### 源和时间筛选

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=3&feedId=4' | jq
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=3&startedFrom=2026-06-30T00:00:00Z&startedTo=2026-06-30T23:59:59Z' | jq
```

响应摘要：

```json
{
  "feedFilter": {
    "totalElements": 4,
    "first": {
      "id": 28,
      "feedId": 4,
      "status": "FAILED"
    }
  },
  "timeFilter": {
    "totalElements": 26,
    "first": {
      "id": 28,
      "startedAt": "2026-06-30T03:56:39.324569Z"
    }
  }
}
```

### 详情接口

```bash
curl -sS 'http://localhost:8080/api/fetch-logs/28' | jq
```

响应摘要：

```json
{
  "success": true,
  "log": {
    "id": 28,
    "feedName": "https://example.invalid/not-rss.xml",
    "status": "FAILED",
    "errorMessage": "抓取或解析 RSS 失败：Remote host terminated the handshake",
    "errorStackFirstLine": "com.newsrss.service.rss.RssFetchException: 抓取或解析 RSS 失败：Remote host terminated the handshake",
    "rawResponseSample": null
  }
}
```

### 非法状态异常

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=3&status=BAD' | jq
```

执行结果：

```json
{
  "message": "抓取状态不合法：BAD"
}
```

## 日志样例

- 成功日志：`xkcd.com`，日志 `id=27`，状态 `SUCCESS`，抓取 4 条，重复 4 条。
- 网络失败日志：`https://example.invalid/not-rss.xml`，日志 `id=28`，状态 `FAILED`，错误为 `Remote host terminated the handshake`，页面分类为“网络失败”。
- 解析失败日志：同一失败日志由后端包装为 `RssFetchException: 抓取或解析 RSS 失败`，详情抽屉可查看 `RssFeedParser.parse` 堆栈。
- 模板字段缺失：页面分类逻辑支持 `PARTIAL`、`字段缺失`、`未命中`、`template` 文本；当前测试数据未生成真实 `PARTIAL` 抓取日志，模板字段缺失样例由 M10 模板预览接口记录并覆盖。

## 浏览器截图

- 抓取日志页：`output/playwright/m11-fetch-logs.png`
- 抓取日志详情抽屉：`output/playwright/m11-fetch-log-detail.png`

## 遗留问题

- 当前抓取日志对“模板字段缺失”的记录依赖抓取结果中的 `PARTIAL` 或错误文本，后续可在抓取链路聚合字段命中警告并写入日志，便于将模板缺失和网络失败彻底分流。
- 前端生产构建存在 Vite chunk 大小警告，建议后续按路由拆包或配置 `manualChunks`。
