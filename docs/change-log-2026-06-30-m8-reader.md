# M8 三栏阅读页变更记录

## 实现过程

- 新增 `frontend/src/api/articles.ts`，封装文章阅读页接口：
  - `GET /api/articles`
  - `GET /api/articles/{id}`
  - `PATCH /api/articles/{id}/read-state`
  - `PATCH /api/articles/{id}/favorite`
  - `PATCH /api/articles/{id}/read-later`
  - `PATCH /api/articles/{id}/archive`
- 调整 `frontend/src/api/dashboard.ts`，复用文章列表和文章状态类型，减少重复类型定义。
- 重构 `frontend/src/views/ReaderView.vue`，完成三栏阅读页：
  - 左侧过滤器：全部、未读、收藏、稍后读、今日更新。
  - 左侧源分组：按文章列表中的 `feedId` 和 `feedName` 聚合。
  - 中间文章列表：支持搜索、选中态、已读弱化展示。
  - 右侧阅读舱：展示文章详情、速读摘要、同源最新、正文内容。
  - 阅读操作：收藏、稍后读、已读、归档、打开原文。
  - 阅读设置：字号模式和行宽模式。
  - 移动端布局：小屏改为单列，文章列表和阅读详情可继续查看。
- 增加 RSS 摘要和正文清洗逻辑，避免 Hacker News 等源的 `Article URL`、`Comments URL`、`Points` 等平台元数据直接污染阅读体验。
- 当前 M8 未新增 SQL 和数据库迁移文件。

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
- Vite 仍提示部分 chunk 大于 `500 kB`，当前不阻塞 M8 验收。

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
- 使用本地测试 PostgreSQL 容器 `newsrss-postgres` 完成联调。

### 前端启动

```bash
pnpm --dir frontend dev --host 127.0.0.1
```

执行结果：

- Vite 启动成功。
- 访问地址：`http://127.0.0.1:5173/reader`。
- 页面显示 `后端 在线`，阅读页可以加载真实文章数据。

## 接口联调结果

### 文章列表

```bash
curl -sS 'http://localhost:8080/api/articles?page=0&size=2' | jq
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 37,
        "feedName": "Hacker News: Front Page",
        "title": "Why Won't Europe Build AI Data Centers in Iceland?",
        "state": {
          "read": true,
          "favorite": true,
          "readLater": true,
          "archived": false
        }
      },
      {
        "id": 38,
        "feedName": "Hacker News: Front Page",
        "title": "Exploring PDP-1 Lisp (1960)",
        "state": {
          "read": false,
          "favorite": false,
          "readLater": false,
          "archived": false
        }
      }
    ],
    "page": 0,
    "size": 2,
    "totalElements": 40,
    "totalPages": 20
  }
}
```

### 文章详情

```bash
curl -sS 'http://localhost:8080/api/articles/37' | jq
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "id": 37,
    "feedName": "Hacker News: Front Page",
    "title": "Why Won't Europe Build AI Data Centers in Iceland?",
    "readingMinutes": 1,
    "state": {
      "read": true,
      "favorite": true,
      "readLater": true,
      "archived": false
    }
  }
}
```

### 状态更新

```bash
curl -sS -X PATCH 'http://localhost:8080/api/articles/37/favorite' \
  -H 'Content-Type: application/json' \
  --data-binary '{"value":true}' | jq

curl -sS -X PATCH 'http://localhost:8080/api/articles/37/read-later' \
  -H 'Content-Type: application/json' \
  --data-binary '{"value":true}' | jq

curl -sS -X PATCH 'http://localhost:8080/api/articles/37/read-state' \
  -H 'Content-Type: application/json' \
  --data-binary '{"value":true}' | jq
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "read": true,
    "favorite": true,
    "readLater": true,
    "archived": false
  }
}
```

## 浏览器验证

```bash
export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
export PWCLI="$CODEX_HOME/skills/playwright/scripts/playwright_cli.sh"
"$PWCLI" open http://127.0.0.1:5173/reader
"$PWCLI" snapshot
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m8-reader-desktop.png --full-page
"$PWCLI" click "Exploring PDP-1 Lisp (1960)"
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m8-reader-selected.png --full-page
"$PWCLI" resize 390 844
"$PWCLI" goto http://127.0.0.1:5173/reader
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m8-reader-mobile.png --full-page
```

执行结果：

- 桌面端页面可见 `过滤器`、`源分组`、`未读优先`、`速读摘要`、`同源最新`、`收藏`、`稍后读`、`标记已读`、`打开原文`。
- 点击文章列表中的 `Exploring PDP-1 Lisp (1960)` 后，右侧阅读舱标题和状态同步更新。
- 移动端 `390x844` 视口可查看过滤器、文章列表和文章详情。
- 页面按钮验证：点击 `标记已读` 后，`GET /api/articles/38` 返回 `state.read=true`。

截图路径：

- `/Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m8-reader-desktop.png`
- `/Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m8-reader-selected.png`
- `/Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m8-reader-mobile.png`

## 遗留问题

- 文章列表当前一次加载前 `40` 条，后续需要在 M9 或后续阅读页优化中补充分页、无限滚动或虚拟列表。
- 部分 RSS 源只提供链接元数据，没有正文内容；当前阅读舱会给出兜底文案，后续可接入正文抓取或按源配置正文解析模板。
- 前端构建存在 Vite chunk 体积提示，后续可以通过路由级懒加载或 Naive UI 按需优化继续处理。
