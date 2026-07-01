# AI 解析模板生成变更记录

## 实现过程

- 增加数据库迁移：
  - 新增 `rss_parser_template.custom_field_mapping jsonb`，保存标准字段之外的源特有字段映射。
  - 新增 `rss_article.custom_fields jsonb`，保存文章解析出的自定义字段。
  - SQL 以 `V2__add_parser_custom_fields.sql` 追加，未修改历史迁移文件。
- 扩展后端解析链路：
  - `ParserTemplateConfig` 增加 `customFieldMapping`。
  - `ParserTemplateNormalizer` 支持解析自定义字段，并写入 `ParsedArticleItem.customFields`、`parseTrace.customFields` 和预览命中结果。
  - `RssArticle` 和文章详情响应增加 `customFields`。
  - 默认 RSS 解析器为自定义字段写入空 JSON，避免影响现有默认抓取。
- 增加 AI 生成能力：
  - 新增 `POST /api/parser-templates/generate-from-feed`。
  - 支持从已保存订阅源或临时 RSS 地址拉取真实样本。
  - 支持 DeepSeek 生成模板，使用 `NEWSRSS_DEEPSEEK_API_KEY`、`NEWSRSS_DEEPSEEK_MODEL` 等环境变量配置。
  - 未配置 DeepSeek 或调用失败时，自动回退本地启发式生成。
  - 生成后自动执行模板预览，返回标准字段命中率、字段命中路径和自定义字段命中。
- 优化错误处理：
  - RSS 抓取失败现在返回可读 `502` 错误，不再只返回通用 500。
- 前端解析模板页：
  - 增加“AI 生成模板”区域。
  - 支持选择已保存订阅源、输入 RSS 地址、选择 AI/本地生成、设置样本条数。
  - 生成结果自动回填标准字段映射、自定义字段映射、正文候选字段和预览结果。
  - 增加自定义字段编辑区和自定义字段命中预览。

## 验证命令

### 后端测试

```bash
mvn -f backend/pom.xml test
```

执行结果：

- 通过。
- `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次功能。

### 数据库迁移

后端使用测试 PostgreSQL 容器启动：

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

执行结果：

- Flyway 成功校验 2 个迁移。
- 当前 schema 版本为 `2`。
- `V2__add_parser_custom_fields.sql` 已应用。

### 官方 RSSHub 地址检查

测试地址：

```text
https://rsshub.app/eastmoney/report/industry
```

执行结果：

- 当前官方实例返回 `403`。
- 后端接口已返回可读 `502`：

```json
{
  "status": 502,
  "message": "抓取 RSS 样本失败：Server returned HTTP response code: 403 for URL: https://rsshub.app/eastmoney/report/industry"
}
```

### 可访问镜像源验证

测试地址：

```text
https://rsshub.rssforever.com/eastmoney/report/industry
```

创建测试订阅源：

```bash
curl -s -X POST http://127.0.0.1:8080/api/feeds \
  -H 'Content-Type: application/json' \
  -d '{"feedName":"东方财富网-行业研报-RSSForever","feedUrl":"https://rsshub.rssforever.com/eastmoney/report/industry","category":"研报","iconUrl":null,"parserTemplateId":null,"fetchIntervalMinutes":60,"enabled":true}'
```

执行结果：

- 创建成功。
- 测试订阅源 ID：`10`。

### DeepSeek 生成验证

后端启动时配置：

```bash
NEWSRSS_DEEPSEEK_API_KEY=<已配置，文档不记录明文>
NEWSRSS_DEEPSEEK_MODEL=deepseek-v4-pro
NEWSRSS_DEEPSEEK_TIMEOUT_SECONDS=60
```

生成命令：

```bash
curl -s -X POST http://127.0.0.1:8080/api/parser-templates/generate-from-feed \
  -H 'Content-Type: application/json' \
  -d '{"feedId":10,"limit":3,"preferAi":true}'
```

执行结果摘要：

```json
{
  "success": true,
  "generator": "deepseek-v4-pro",
  "aiUsed": true,
  "fallbackUsed": false,
  "feedTitle": "东方财富网-行业研报",
  "preview": {
    "itemCount": 3,
    "hitRate": 0.875,
    "fieldHitRates": {
      "guid": 1,
      "articleUrl": 1,
      "title": 1,
      "summary": 1,
      "author": 1,
      "publishedAt": 1,
      "contentHtml": 1,
      "coverImageUrl": 0
    }
  }
}
```

生成字段映射摘要：

```json
{
  "guid": ["uri"],
  "articleUrl": ["link"],
  "title": ["title"],
  "summary": ["description.value"],
  "author": ["author"],
  "publishedAt": ["publishedDate"],
  "contentHtml": ["description.value"],
  "coverImageUrl": []
}
```

说明：

- DeepSeek 已真实参与生成。
- 该源样本没有封面图字段，因此 `coverImageUrl` 命中率为 `0`。
- 样本没有额外高价值字段，因此 `customFieldMapping` 为空。

### 页面验证

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" open http://127.0.0.1:5173/parser-templates --headed
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" resize 1366 768
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" screenshot --filename output/playwright/parser-templates-ai-generate.png
```

执行结果摘要：

```json
{
  "bodyScrollHeight": 768,
  "documentScrollHeight": 768,
  "innerHeight": 768,
  "hasGeneratePanel": true,
  "workbench": {
    "clientHeight": 570,
    "scrollHeight": 2681,
    "overflowY": "auto"
  }
}
```

截图路径：

- `output/playwright/parser-templates-ai-generate.png`

## 遗留问题

- 官方 `rsshub.app` 当前对该源返回 `403`，建议使用自建 RSSHub 或可访问镜像源进行稳定测试和生产抓取。
- DeepSeek API Key 当前通过本地环境变量注入，未写入仓库；生产部署时需要放入部署环境变量或密钥管理系统。
- 前端构建仍有 chunk 大小警告，建议后续按路由拆包。
