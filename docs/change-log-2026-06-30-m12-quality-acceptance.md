# M12 测试与质量验收变更记录

## 实现过程

- 完成 M11 后进入质量验收，覆盖后端单元测试、前端构建、核心 API smoke、异常场景和浏览器截图检查。
- 修复验收中发现的 M11 日志查询 PostgreSQL 500：
  - 原因是 JPQL 中 `:startedFrom is null or fetchLog.startedAt >= :startedFrom` 让 PostgreSQL 无法推断 nullable timestamp 参数类型。
  - 处理方式是改为 Spring Data `Specification` 动态拼条件。
- 修复验收中发现的前端失败分类误判：
  - 错误堆栈包含 `parseWithBoundTemplate` 时会被误判为“模板字段”。
  - 已将 `handshake`、`timeout`、`unknownhost`、`connect`、`remote host` 等网络关键词优先判定为“网络失败”。
- 当前 M12 未新增 SQL 和数据库迁移文件。

## 验证命令和执行结果

### 后端单元测试

```bash
mvn -f backend/pom.xml test
```

执行结果：

- 构建成功。
- 测试结果：`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- 覆盖范围包括：
  - service 层基础逻辑。
  - RSS 解析模板归一化。
  - 文章去重查询逻辑。
  - 抓取日志筛选和非法状态。
  - 首页驾驶舱统计。

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- 产物示例：`dist/assets/index-*.js`。
- Vite chunk 大小警告仍存在：部分 chunk 大于 `500 kB`，不阻塞 V1。

### 前端测试

执行结果：

- `frontend/package.json` 当前只有 `dev`、`build`、`preview`，未配置前端测试脚本。
- 本轮以前端 TypeScript 构建、API 联调和 Playwright 浏览器截图替代基础前端验证。

### 本地联调启动

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db

pnpm --dir frontend dev --host 127.0.0.1
```

执行结果：

- 后端监听 `8080`。
- 前端监听 `127.0.0.1:5173`。
- PostgreSQL 测试容器 `newsrss-postgres` 保持运行，端口为 `15432 -> 5432`。

## 核心 API Smoke

```bash
curl -sS -o /tmp/newsrss-health.json -w '%{http_code}' 'http://localhost:8080/api/health'
curl -sS -o /tmp/newsrss-dashboard.json -w '%{http_code}' 'http://localhost:8080/api/dashboard/summary'
curl -sS -o /tmp/newsrss-feeds.json -w '%{http_code}' 'http://localhost:8080/api/feeds?page=0&size=3'
curl -sS -o /tmp/newsrss-articles.json -w '%{http_code}' 'http://localhost:8080/api/articles?page=0&size=3'
curl -sS -o /tmp/newsrss-templates.json -w '%{http_code}' 'http://localhost:8080/api/parser-templates'
curl -sS -o /tmp/newsrss-fetchlogs.json -w '%{http_code}' 'http://localhost:8080/api/fetch-logs?page=0&size=3'
```

执行结果：

```text
health=200
dashboard=200
feeds=200
articles=200
parserTemplates=200
fetchLogs=200
```

响应摘要：

```json
{
  "dashboard": {
    "feedCount": 5,
    "articleCount": 41,
    "unreadCount": 38
  },
  "feedsTotal": 5,
  "articlesTotal": 41,
  "templateCount": 1,
  "fetchLogTotal": 26
}
```

## 异常场景覆盖

### 无文章

```bash
curl -sS 'http://localhost:8080/api/articles?feedId=999999&page=0&size=3' | jq
```

执行结果：

```json
{
  "success": true,
  "totalElements": 0,
  "itemsLength": 0
}
```

### 源失败和网络异常

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=5&status=FAILED' | jq
```

执行结果：

- 返回失败日志总数 `11`。
- 样例源：`https://example.invalid/not-rss.xml`。
- 错误：`抓取或解析 RSS 失败：Remote host terminated the handshake`。
- 前端分类显示为“网络失败”。

### 模板不命中

- M10 模板预览已记录异常模板预览，存在 `publishedAt 字段缺失`、`模板字段未命中，已使用默认字段兜底`、`未命中可用字段`。
- M11 页面支持识别 `PARTIAL`、`字段缺失`、`未命中`、`template` 作为“模板字段”类问题。
- 当前数据库没有真实 `PARTIAL` 抓取日志，记录为非阻塞遗留问题。

### 非法日志状态

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=3&status=BAD' | jq
```

执行结果：

```json
{
  "message": "抓取状态不合法：BAD"
}
```

## 浏览器截图检查

已存在核心页面截图：

- 驾驶舱：`output/playwright/m7-dashboard.png`
- 阅读桌面页：`output/playwright/m8-reader-desktop.png`
- 阅读移动页：`output/playwright/m8-reader-mobile.png`
- 订阅源管理：`output/playwright/m9-feeds.png`
- 解析模板实验室：`output/playwright/m10-parser-templates.png`
- 抓取日志页：`output/playwright/m11-fetch-logs.png`
- 抓取日志详情抽屉：`output/playwright/m11-fetch-log-detail.png`

检查结果：

- 主要页面可打开，后端在线状态正常。
- M11 抓取日志页表格、筛选区、状态统计和详情抽屉可见。
- 未发现阻塞 V1 的明显布局错乱。

## 遗留问题和 V1 判断

- 前端未配置专门测试脚本，建议后续引入 Vitest 或组件测试，当前由构建和浏览器截图兜底。
- Vite chunk 大小警告仍存在，建议 V1 后做路由级拆包。
- 抓取日志对模板字段缺失的持久化可继续增强，不阻塞当前 V1，因为模板预览实验室已能排查字段不命中，抓取日志页已能排查源失败和网络异常。
- M12 未发现阻塞 V1 的失败测试。
