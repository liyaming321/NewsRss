# M9 订阅源管理页变更记录

## 实现过程

- 新增 `frontend/src/api/feeds.ts`，封装订阅源管理接口：
  - `GET /api/feeds`
  - `POST /api/feeds`
  - `PUT /api/feeds/{id}`
  - `DELETE /api/feeds/{id}`
  - `POST /api/feeds/{id}/refresh`
  - `POST /api/feeds/detect`
- 新增 `frontend/src/api/parserTemplates.ts`，封装解析模板列表接口，用于新增和编辑订阅源时选择模板。
- 重构 `frontend/src/views/FeedsView.vue`，将 M0 骨架升级为订阅源管理页：
  - 顶部工具栏：刷新、新增源、OPML 导入、OPML 导出。
  - 健康概览条：健康、警告、异常、未知、停用。
  - 订阅源表格：名称、URL、健康状态、分组、解析模板、抓取频率、文章数、最近成功时间和操作。
  - 新增源抽屉：RSS 地址、自动探测、名称、分组、抓取频率、模板选择、图标 URL、启用开关。
  - 保存前风险提示：空地址、未探测、未绑定模板、高频抓取、停用状态。
  - 行操作：编辑、启用或停用、手动刷新、删除。
- 增强 `frontend/src/api/client.ts`：
  - 支持读取后端错误响应中的 `message` 和 `details`。
  - 对空响应体给出明确错误提示，避免前端出现不可读的 JSON 解析异常。
- 当前 M9 未新增 SQL 和数据库迁移文件。

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
- Vite 仍提示部分 chunk 大于 `500 kB`，当前不阻塞 M9 验收。

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
- Flyway 校验通过，数据库 schema 已是最新版本。
- 使用本地测试 PostgreSQL 容器 `newsrss-postgres` 完成联调。

### 前端启动

```bash
pnpm --dir frontend dev --host 127.0.0.1
```

执行结果：

- Vite 启动成功。
- 访问地址：`http://127.0.0.1:5173/feeds`。
- 页面显示 `后端 在线`，订阅源管理页可以加载真实接口数据。

## 接口联调结果

### 订阅源列表

```bash
curl -sS 'http://localhost:8080/api/feeds?page=0&size=5' | jq
```

响应摘要：

```json
{
  "success": true,
  "total": 5,
  "feeds": [
    {
      "id": 6,
      "feedName": "M5 临时失败源",
      "healthStatus": "ERROR",
      "enabled": true,
      "articleCount": 0,
      "lastSuccessAt": null
    },
    {
      "id": 3,
      "feedName": "xkcd.com",
      "healthStatus": "HEALTHY",
      "enabled": true,
      "articleCount": 4,
      "lastSuccessAt": "2026-06-30T02:15:00.222822Z"
    },
    {
      "id": 1,
      "feedName": "Hacker News: Front Page",
      "healthStatus": "HEALTHY",
      "enabled": true,
      "parserTemplateCode": "m3-fixture-template",
      "articleCount": 26
    }
  ]
}
```

### 解析模板列表

```bash
curl -sS 'http://localhost:8080/api/parser-templates' | jq
```

响应摘要：

```json
{
  "success": true,
  "templates": [
    {
      "id": 1,
      "templateCode": "m3-fixture-template",
      "templateName": "M3 Fixture 完整模板 v2",
      "enabled": true
    }
  ]
}
```

### RSS 自动探测

```bash
curl -sS -X POST 'http://localhost:8080/api/feeds/detect' \
  -H 'Content-Type: application/json' \
  --data-binary '{"feedUrl":"https://xkcd.com/atom.xml"}' | jq
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "feedUrl": "https://xkcd.com/atom.xml",
    "title": "xkcd.com",
    "siteUrl": "https://xkcd.com/",
    "language": null,
    "itemCount": 4
  }
}
```

自动探测失败验证：

```bash
curl -i -sS -X POST 'http://localhost:8080/api/feeds/detect' \
  -H 'Content-Type: application/json' \
  --data-binary '{"feedUrl":"https://example.invalid/not-rss.xml"}'
```

执行结果：

- 当前后端返回 `HTTP/1.1 200` 且空响应体。
- 前端统一请求层已将空响应转换为明确提示：`接口返回空响应`。
- 页面验证中，无效地址探测后出现该错误消息，并清空探测结果卡片。

### 新增源、绑定模板、刷新、停用、删除

```bash
curl -sS -X POST 'http://localhost:8080/api/feeds' \
  -H 'Content-Type: application/json' \
  --data-binary '{"feedName":"M9 临时模板绑定源","feedUrl":"https://github.blog/feed/?m9=1","category":"测试","iconUrl":null,"parserTemplateId":1,"fetchIntervalMinutes":45,"enabled":true}' | jq

curl -sS -X POST 'http://localhost:8080/api/feeds/7/refresh' | jq

curl -sS -X PUT 'http://localhost:8080/api/feeds/7' \
  -H 'Content-Type: application/json' \
  --data-binary '{"feedName":"M9 临时模板绑定源","feedUrl":"https://github.blog/feed/?m9=1","category":"测试","iconUrl":null,"parserTemplateId":1,"fetchIntervalMinutes":45,"enabled":false}' | jq

curl -sS -X DELETE 'http://localhost:8080/api/feeds/7' | jq
```

响应摘要：

```json
{
  "created": {
    "success": true,
    "message": "订阅源创建成功",
    "id": 7,
    "parserTemplateId": 1,
    "parserTemplateCode": "m3-fixture-template",
    "enabled": true,
    "fetchIntervalMinutes": 45
  },
  "refresh": {
    "success": true,
    "fetchedCount": 10,
    "newCount": 10,
    "duplicateCount": 0,
    "failedCount": 0
  },
  "disabled": {
    "success": true,
    "enabled": false,
    "parserTemplateId": 1
  },
  "deleted": {
    "success": true,
    "message": "订阅源删除成功"
  }
}
```

说明：

- 临时订阅源 `id=7` 已在验证后删除。
- 手动刷新阶段新增了 `10` 篇来自 `https://github.blog/feed/?m9=1` 的测试文章；由于删除订阅源会级联删除关联文章，临时源自身已清理。

## 浏览器验证

```bash
export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
export PWCLI="$CODEX_HOME/skills/playwright/scripts/playwright_cli.sh"
"$PWCLI" open http://127.0.0.1:5173/feeds
"$PWCLI" snapshot
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m9-feeds.png --full-page
"$PWCLI" click "新增源"
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m9-feeds-drawer.png --full-page
```

执行结果：

- 页面可见 `订阅源矩阵`、`OPML 导入`、`OPML 导出`、`新增源`、健康概览条和订阅源表格。
- 表格可见健康状态、分组、解析模板、抓取频率、文章数、最近成功时间。
- 新增源抽屉可见 RSS 地址、探测、名称、分组、抓取频率、解析模板、图标 URL、启用抓取和保存前风险提示。
- 页面内成功探测 `https://xkcd.com/atom.xml` 后，标题回填为 `xkcd.com`，探测卡显示 `4 条条目`。
- 页面内失败探测 `https://example.invalid/not-rss.xml` 后，出现明确提示 `接口返回空响应`。

截图路径：

- `/Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m9-feeds.png`
- `/Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m9-feeds-drawer.png`

## 遗留问题

- 自动探测无效地址时后端当前返回 `200` 和空响应体，前端已做明确提示兜底；后续建议后端将这类解析失败统一返回 `4xx/5xx` 错误响应。
- OPML 导入和导出目前是 V1 占位入口，后续需要补文件解析、批量创建和导出接口。
- 订阅源表格当前一次加载前 `80` 条，后续源数量增大时需要接分页、服务端搜索或虚拟列表。
- 前端构建存在 Vite chunk 体积提示，后续可通过路由级懒加载或 Naive UI 按需优化处理。
