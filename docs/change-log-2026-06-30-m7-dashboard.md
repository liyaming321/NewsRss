# M7 首页驾驶舱变更记录

## 实现过程

- 新增 `frontend/src/api/dashboard.ts`，封装首页驾驶舱接口：
  - `GET /api/dashboard/summary`
  - `GET /api/dashboard/feed-health`
  - `GET /api/dashboard/recent-articles`
- 重构 `frontend/src/views/DashboardView.vue`，将原占位卡片升级为真实数据驾驶舱：
  - 关键指标卡：今日新增、未读文章、活跃源、异常源。
  - RSS 抓取管线：抓取、解析、去重、入库。
  - 实时信息流：展示最近文章。
  - 源健康雷达：展示健康、警告、错误、未知状态。
  - 今日热词：基于最近文章标题和摘要做前端轻量统计。
  - 最近抓取：复用抓取日志接口展示最近抓取结果。
- 后端 `DashboardSummaryResponse` 增加 `todayNewArticleCount` 字段。
- 后端 `RssArticleRepository` 增加 `countByFetchedAtGreaterThanEqual`，用于统计 UTC 今日入库文章数。
- 后端 `DashboardService` 汇总今日新增文章数量。
- 更新 `DashboardServiceTest`，补充今日新增数量断言。
- 前端展示层清洗 RSS 摘要中的 HTML、URL 和平台元数据，避免驾驶舱卡片显示原始标签。

本次未新增 SQL 和数据库迁移文件。

## 验证命令

### 后端测试

```bash
mvn -f backend/pom.xml test
```

执行结果：

- Maven 构建成功。
- 测试结果：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- 产物摘要：
  - `dist/index.html`：`0.46 kB`
  - `dist/assets/index-CfvYaswD.css`：`9.11 kB`
  - `dist/assets/index-CaqcjkvO.js`：`628.22 kB`
- Vite 输出 chunk 大小提示，当前不影响构建结果。

### 后端启动

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
NEWSRSS_FETCH_SCHEDULER_INITIAL_DELAY_MS=300000 \
NEWSRSS_FETCH_SCHEDULER_FIXED_DELAY_MS=300000 \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

执行结果：

- 后端启动成功，监听 `8080`。
- Flyway 校验通过：`Successfully validated 1 migration`。
- 数据库状态：`Schema "public" is up to date. No migration necessary.`
- 验证完成后通过 `Ctrl+C` 正常停止，日志显示 `Graceful shutdown complete`。

### 前端启动

```bash
pnpm --dir frontend dev --host 127.0.0.1
```

执行结果：

- Vite 启动成功。
- 访问地址：`http://127.0.0.1:5173/`。
- 启动早期曾出现代理 `ECONNREFUSED`，原因是前端页面先于后端完全 ready 发起接口请求；后端启动完成后页面显示 `后端 在线`，驾驶舱接口和页面验证通过。
- 验证完成后通过 `Ctrl+C` 停止前端 dev server。

## 接口联调结果

### 驾驶舱摘要

```bash
curl -sS http://localhost:8080/api/dashboard/summary | jq '{success, data}'
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "feedCount": 5,
    "enabledFeedCount": 5,
    "articleCount": 40,
    "todayNewArticleCount": 40,
    "unreadCount": 39,
    "favoriteCount": 1,
    "readLaterCount": 1,
    "failedFetchLogCount": 8
  }
}
```

### 源健康统计

```bash
curl -sS http://localhost:8080/api/dashboard/feed-health | jq '{success, data}'
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "unknown": 0,
    "healthy": 3,
    "warning": 0,
    "error": 2
  }
}
```

### 最近文章

```bash
curl -sS 'http://localhost:8080/api/dashboard/recent-articles?limit=3' | jq
```

响应摘要：

```json
{
  "success": true,
  "count": 3,
  "articles": [
    {
      "id": 37,
      "feedName": "Hacker News: Front Page",
      "title": "Why Won't Europe Build AI Data Centers in Iceland?",
      "fetchedAt": "2026-06-30T02:14:38.782723Z"
    },
    {
      "id": 38,
      "feedName": "Hacker News: Front Page",
      "title": "Exploring PDP-1 Lisp (1960)",
      "fetchedAt": "2026-06-30T02:14:38.782723Z"
    },
    {
      "id": 39,
      "feedName": "Hacker News: Front Page",
      "title": "Memory Safe Context Switching (longjmp, setjmp) in Fil-C",
      "fetchedAt": "2026-06-30T02:14:38.782723Z"
    }
  ]
}
```

### 最近抓取日志

```bash
curl -sS 'http://localhost:8080/api/fetch-logs?page=0&size=3' | jq
```

响应摘要：

```json
{
  "success": true,
  "count": 3,
  "logs": [
    {
      "id": 21,
      "feedName": "M5 临时失败源",
      "status": "FAILED",
      "newCount": 0
    },
    {
      "id": 20,
      "feedName": "M5 临时失败源",
      "status": "FAILED",
      "newCount": 0
    },
    {
      "id": 19,
      "feedName": "https://example.invalid/not-rss.xml",
      "status": "FAILED",
      "newCount": 0
    }
  ]
}
```

## 浏览器验证

```bash
export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
export PWCLI="$CODEX_HOME/skills/playwright/scripts/playwright_cli.sh"
"$PWCLI" open http://127.0.0.1:5173/
"$PWCLI" snapshot
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m7-dashboard.png --full-page
```

执行结果：

- 页面标题：`NewsRss`。
- 页面可见关键模块：
  - `实时概览`
  - `RSS 抓取管线`
  - `实时信息流`
  - `源健康雷达`
  - `今日热词`
  - `最近抓取`
- 页面显示真实 API 数据：
  - 今日新增：`40`
  - 未读文章：`39`
  - 活跃订阅源：`5`
  - 异常源：`2`
- 异常状态用琥珀色/珊瑚色提示，未使用强烈闪烁或过度警告样式。
- 截图路径：`output/playwright/m7-dashboard.png`。

## 遗留问题

- 今日热词目前由前端基于最近文章标题和摘要做轻量统计，后续可替换为后端关键词或 AI 摘要服务。
- 当前“最近抓取”受测试库中的失败源影响，优先展示了 M5 验证失败日志；这是测试数据现状，不影响页面功能。
- 前端构建仍有 Vite chunk 大小提示，后续可通过路由懒加载或 vendor 拆包优化。
