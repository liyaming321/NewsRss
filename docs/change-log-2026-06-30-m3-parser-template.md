# M3 解析模板能力变更记录

## 实现过程

- 新增解析模板 DTO、Controller 和 Service，支持模板新增、列表、详情、编辑、删除、启用/停用。
- 新增订阅源模板绑定接口，可将 `rss_feed.parser_template_id` 绑定到已启用模板。
- 新增模板配置映射模型，支持：
  - `fieldMapping` 标准字段路径映射，字段值可配置为路径数组。
  - `contentSelectors` 正文候选字段优先级。
  - `coverSelectors` 封面候选字段优先级。
  - `timeFormats` 自定义时间格式。
  - `cleanupRules.removeSelectors`、`cleanupRules.unwrapSelectors`、`cleanupRules.removeAttributes` 清洗规则。
- 新增 RSS 原始字段树构建器，将 Rome 条目整理为可预览的 JSON 字段快照。
- 新增模板标准化解析器，输出标准化文章、字段命中路径、fallback 标记、命中率和异常提示。
- 改造 RSS 抓取链路：订阅源绑定启用模板时使用模板解析；未绑定或模板停用时使用默认 Rome 解析。
- 新增统一错误响应，用于模板参数错误、资源不存在、停用模板绑定等场景。
- 新增本地 RSS fixture：`backend/src/test/resources/rss/m3-template-preview.xml`，用于稳定验证模板预览。

## 主要接口

- `GET /api/parser-templates`
- `GET /api/parser-templates/{id}`
- `POST /api/parser-templates`
- `PUT /api/parser-templates/{id}`
- `DELETE /api/parser-templates/{id}`
- `PATCH /api/parser-templates/{id}/enabled?enabled=true|false`
- `POST /api/parser-templates/preview`
- `PUT /api/parser-templates/feeds/{feedId}/binding`

## 验证命令

```bash
mvn -f backend/pom.xml test
```

## 执行结果

- 成功。
- 测试结果：`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- 覆盖内容：
  - 模板字段映射。
  - 正文候选字段优先级。
  - 封面候选字段优先级。
  - 自定义时间格式。
  - HTML 清洗规则。
  - 字段缺失 fallback 和时间解析失败提示。

## 接口验证命令与结果

### 健康检查

```bash
curl -sS http://localhost:8080/api/health
```

结果摘要：

- 返回 `status: UP`。
- 应用版本：`0.0.1-SNAPSHOT`。

### 创建完整模板

```bash
curl -sS -X POST http://localhost:8080/api/parser-templates \
  -H 'Content-Type: application/json' \
  --data-binary '{...完整模板 JSON...}' | jq
```

结果摘要：

- 创建成功，模板 ID：`1`。
- 模板编码：`m3-fixture-template`。
- 启用状态：`true`。
- 配置包含 `fieldMapping`、`contentSelectors`、`coverSelectors`、`timeFormats`、`cleanupRules`。

### 完整模板预览

```bash
FIXTURE_URL="file:///Users/lym/Documents/workspace/aiDemo/newsRss/backend/src/test/resources/rss/m3-template-preview.xml"
curl -sS -X POST http://localhost:8080/api/parser-templates/preview \
  -H 'Content-Type: application/json' \
  --data-binary "{\"feedUrl\":\"${FIXTURE_URL}\",\"templateId\":1,\"limit\":5}" | jq
```

结果摘要：

- Feed 标题：`M3 模板预览测试源`。
- 返回条目数：`2`。
- 整体命中率：`0.8125`。
- 字段命中率样例：
  - `title: 1.0`
  - `articleUrl: 1.0`
  - `contentHtml: 1.0`
  - `coverImageUrl: 0.5`
  - `author: 0.0`
- 字段命中路径样例：
  - `titlePath: foreignMarkup.headline.value`
  - `contentPath: contents[0].value`
  - `coverPath: foreignMarkup.thumbnail.attributes.url`
- 标准化结果样例：
  - 标题：`模板标题一`
  - 文章链接：`https://example.com/custom-1`
  - 发布时间：`2026-06-30T16:30:00Z`
  - 封面图：`https://example.com/custom-cover-1.jpg`
- 异常提示样例：
  - 第二条返回 `publishedAt 时间解析失败：bad-time`。

### 缺字段模板预览

```bash
FIXTURE_URL="file:///Users/lym/Documents/workspace/aiDemo/newsRss/backend/src/test/resources/rss/m3-template-preview.xml"
curl -sS -X POST http://localhost:8080/api/parser-templates/preview \
  -H 'Content-Type: application/json' \
  --data-binary '{...缺字段内联模板 JSON...}' | jq
```

结果摘要：

- 模板编码：`m3-broken-inline`。
- 返回条目数：`2`。
- 整体命中率：`0.6875`。
- `title` 模板路径未命中，回退默认路径 `title`，`fallback: true`。
- `contentHtml` 模板路径未命中，回退默认路径 `contents[0].value`，`fallback: true`。
- `publishedAt` 未命中，返回提示：`publishedAt 字段缺失，发布时间为空`。

### 编辑、停用、启用模板

```bash
curl -sS -X PUT http://localhost:8080/api/parser-templates/1 \
  -H 'Content-Type: application/json' \
  --data-binary '{...更新模板 JSON...}' | jq

curl -sS -X PATCH 'http://localhost:8080/api/parser-templates/1/enabled?enabled=false' | jq

curl -sS -X PATCH 'http://localhost:8080/api/parser-templates/1/enabled?enabled=true' | jq
```

结果摘要：

- 模板名称更新为：`M3 Fixture 完整模板 v2`。
- 停用接口返回 `enabled: false`。
- 启用接口返回 `enabled: true`。

### 订阅源绑定模板

```bash
curl -sS -X PUT http://localhost:8080/api/parser-templates/feeds/1/binding \
  -H 'Content-Type: application/json' \
  --data-binary '{"parserTemplateId":1}' | jq
```

结果摘要：

- 绑定成功。
- `feedId: 1`。
- `feedUrl: https://hnrss.org/frontpage`。
- `parserTemplateId: 1`。
- `templateCode: m3-fixture-template`。

数据库验证：

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss \
  -c "select f.id, f.feed_url, f.parser_template_id, t.template_code from rss_feed f left join rss_parser_template t on t.id = f.parser_template_id where f.id = 1;"
```

结果摘要：

- `rss_feed.id = 1` 的 `parser_template_id = 1`。
- 关联模板编码为 `m3-fixture-template`。

### 删除模板

```bash
TEMP_ID=$(curl -sS -X POST http://localhost:8080/api/parser-templates \
  -H 'Content-Type: application/json' \
  --data-binary '{"templateCode":"m3-delete-temp","templateName":"M3 删除临时模板","fieldMapping":{},"contentSelectors":[],"coverSelectors":[],"timeFormats":[],"cleanupRules":{},"enabled":true}' | jq -r '.id')
curl -sS -o /tmp/newsrss-delete-template.out -w '%{http_code}' -X DELETE "http://localhost:8080/api/parser-templates/${TEMP_ID}"
curl -sS "http://localhost:8080/api/parser-templates/${TEMP_ID}" | jq
```

结果摘要：

- 临时模板 ID：`2`。
- 删除返回 HTTP `204`。
- 删除后查询返回 `404`，错误信息：`解析模板不存在：2`。

### 错误响应

```bash
curl -sS -X POST http://localhost:8080/api/parser-templates \
  -H 'Content-Type: application/json' \
  --data-binary '{}' | jq
```

结果摘要：

- HTTP `400`。
- 响应消息：`请求参数不合法`。
- 明细：
  - `templateCode: 模板编码不能为空`
  - `templateName: 模板名称不能为空`

```bash
curl -sS -X GET http://localhost:8080/api/parser-templates/404 | jq
```

结果摘要：

- HTTP `404`。
- 响应消息：`解析模板不存在：404`。

```bash
curl -sS -X PUT http://localhost:8080/api/parser-templates/feeds/1/binding \
  -H 'Content-Type: application/json' \
  --data-binary '{"parserTemplateId":1}' | jq
```

停用模板时结果摘要：

- HTTP `400`。
- 响应消息：`解析模板已停用，不能绑定：m3-fixture-template`。

## 数据库验证

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss \
  -c "select id, template_code, template_name, enabled, jsonb_array_length(time_formats) as time_format_count from rss_parser_template where id = 1;"
```

结果摘要：

- `id = 1`。
- `template_code = m3-fixture-template`。
- `template_name = M3 Fixture 完整模板 v2`。
- `enabled = true`。
- `time_format_count = 2`。

## 说明

- M3 未新增数据库字段，复用了 M1 已创建的 `rss_parser_template` JSON 配置字段和 `rss_feed.parser_template_id` 绑定字段，因此没有新增迁移脚本。
- 当前预览接口支持远程 URL 和本地 `file://` fixture；生产阶段可在 M4/M5 补充 URL 协议白名单或安全策略。
- 当前前端尚未接入 M3 接口，前端模板管理页面将在后续前端里程碑中实现。

## 遗留问题

- 暂无阻塞问题。
- 后续 M4 需要把接口响应结构进一步统一到全站 API 规范。
- 后续前端任务需要补模板管理、模板预览、字段命中可视化页面。
