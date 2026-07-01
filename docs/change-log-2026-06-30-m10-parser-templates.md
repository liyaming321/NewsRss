# M10 解析模板实验室变更记录

## 实现过程

- 扩展 `frontend/src/api/parserTemplates.ts`：
  - 增加解析模板创建、更新、删除、启停接口。
  - 增加模板预览接口类型，覆盖 `hitRate`、`fieldHitRates`、`rawPayload`、`normalized`、`fieldHits` 和 `warnings`。
- 重构 `frontend/src/views/ParserTemplatesView.vue`，将骨架升级为解析模板实验室：
  - 模板列表：展示模板名称、编码、启用状态、映射数量、更新时间和行操作。
  - 字段映射表单：用普通输入框配置标准字段路径，支持逗号或换行分隔候选路径。
  - 测试 RSS 源输入：支持配置 RSS 地址和预览条数。
  - 模板预览按钮：提交当前表单生成的临时模板配置，不要求用户手写 JSON。
  - 原始字段树：将后端返回的 `rawPayload` 扁平化为路径和值。
  - 标准化文章结果：展示标题、链接、摘要、正文纯文本和基础元数据。
  - 字段命中路径：展示每个标准字段是否命中、命中路径、兜底状态和说明。
  - 命中率和异常提示：展示整体命中率、字段命中率、预览级和条目级异常。
  - 保存模板：支持创建新模板、更新已有模板、启停模板和删除模板。
- 页面沿用现有深色控制台风格和 Naive UI，不新增第三方依赖。
- 当前 M10 未新增 SQL 和数据库迁移文件。

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
- Vite 仍提示部分 chunk 大于 `500 kB`，当前不阻塞 M10 验收。

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
- 访问地址：`http://127.0.0.1:5173/parser-templates`。
- 页面显示 `后端 在线`，解析模板实验室可以加载真实模板和预览结果。

## 接口联调结果

### 完整模板预览

```bash
FIXTURE_URL="file:///Users/lym/Documents/workspace/aiDemo/newsRss/backend/src/test/resources/rss/m3-template-preview.xml"
curl -sS -X POST 'http://localhost:8080/api/parser-templates/preview' \
  -H 'Content-Type: application/json' \
  --data-binary '{...M10 完整内联模板...}' | jq
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "feedTitle": "M3 模板预览测试源",
    "templateCode": "m10-inline-good",
    "itemCount": 2,
    "hitRate": 0.8125,
    "fieldHitRates": {
      "title": 1.0,
      "articleUrl": 1.0,
      "contentHtml": 1.0,
      "coverImageUrl": 0.5,
      "author": 0.0
    },
    "warnings": [
      "publishedAt 时间解析失败：bad-time"
    ],
    "first": {
      "title": "模板标题一",
      "articleUrl": "https://example.com/custom-1",
      "titlePath": "foreignMarkup.headline.value",
      "contentPath": "contents[0].value",
      "coverPath": "foreignMarkup.thumbnail.attributes.url"
    }
  }
}
```

### 异常模板预览

```bash
FIXTURE_URL="file:///Users/lym/Documents/workspace/aiDemo/newsRss/backend/src/test/resources/rss/m3-template-preview.xml"
curl -sS -X POST 'http://localhost:8080/api/parser-templates/preview' \
  -H 'Content-Type: application/json' \
  --data-binary '{...M10 缺陷内联模板...}' | jq
```

响应摘要：

```json
{
  "success": true,
  "data": {
    "templateCode": "m10-inline-broken",
    "itemCount": 2,
    "hitRate": 0.6875,
    "warnings": [
      "publishedAt 字段缺失，发布时间为空"
    ],
    "firstHits": {
      "title": {
        "matched": true,
        "path": "title",
        "fallback": true,
        "message": "模板字段未命中，已使用默认字段兜底"
      },
      "publishedAt": {
        "matched": false,
        "path": null,
        "message": "未命中可用字段"
      },
      "coverImageUrl": {
        "matched": true,
        "path": "contentHtml.img",
        "fallback": true,
        "message": "封面图未命中模板字段，已从正文图片兜底"
      }
    }
  }
}
```

### 模板保存、更新和删除

```bash
curl -sS -X POST 'http://localhost:8080/api/parser-templates' \
  -H 'Content-Type: application/json' \
  --data-binary '{...M10 临时模板...}' | jq

curl -sS -X PUT 'http://localhost:8080/api/parser-templates/3' \
  -H 'Content-Type: application/json' \
  --data-binary '{...M10 临时模板 v2...}' | jq

curl -sS -X DELETE 'http://localhost:8080/api/parser-templates/3' | jq
```

响应摘要：

```json
{
  "created": {
    "success": true,
    "message": "解析模板创建成功",
    "id": 3,
    "templateCode": "m10-temp-1782790976",
    "enabled": true
  },
  "updated": {
    "success": true,
    "message": "解析模板更新成功",
    "templateName": "M10 临时保存模板 v2",
    "enabled": false
  },
  "deleted": {
    "success": true,
    "message": "解析模板删除成功"
  }
}
```

说明：

- 临时模板 `id=3` 已在验证后删除。
- 删除后再次查询返回 `404`，消息为 `解析模板不存在：3`。

## 浏览器验证

```bash
export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
export PWCLI="$CODEX_HOME/skills/playwright/scripts/playwright_cli.sh"
"$PWCLI" open http://127.0.0.1:5173/parser-templates
"$PWCLI" snapshot
"$PWCLI" click "模板预览"
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m10-parser-templates.png --full-page
```

执行结果：

- 页面可见 `模板列表`、`字段映射`、`测试 RSS 源`、`保存前风险`。
- 用户可以通过输入框配置字段路径、正文候选、封面候选、时间格式和清洗规则，不需要直接编辑 JSON。
- 点击 `模板预览` 后，页面展示：
  - 整体命中率 `81%`。
  - 标准化文章标题 `模板标题一`。
  - 字段命中路径，如 `foreignMarkup.headline.value`、`contents[0].value`、`foreignMarkup.thumbnail.attributes.url`。
  - 原始字段树路径和值。
  - 异常提示 `publishedAt 时间解析失败：bad-time`。

截图路径：

- `/Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m10-parser-templates.png`

## 遗留问题

- 当前字段树采用前端扁平化展示，后续可升级为可折叠树组件和路径复制能力。
- 模板保存目前允许覆盖已有模板，后续可增加保存前差异对比和绑定源影响范围提示。
- 预览输入默认使用本地 fixture，后续可在页面中增加最近订阅源快捷选择。
- 前端构建存在 Vite chunk 体积提示，后续可通过路由级懒加载或 Naive UI 按需优化处理。
