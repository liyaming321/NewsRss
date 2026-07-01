# RSS 抓取超时修复变更记录

## 实现过程

- 新增 `RssHttpProperties` 配置类，统一管理 RSS 抓取、模板预览、模板生成样本抓取共用的 HTTP 参数。
- 在 `application-db.yml` 中新增 `newsrss.rss.http` 配置：
  - `connect-timeout-seconds` 默认 10 秒。
  - `read-timeout-seconds` 默认 60 秒。
  - `user-agent` 支持通过环境变量覆盖。
- 重构 `RssFeedParser`，将默认解析、模板解析、原始样本抓取三处重复的 `URLConnection` 创建逻辑合并为统一入口。
- 将读取超时从硬编码 20 秒调整为可配置默认 60 秒，避免较慢 RSSHub 或代理源在 20 秒左右返回前被提前中断。
- 对 `SocketTimeoutException` 增加中文业务错误提示，避免页面只看到底层 `Read timed out`。
- 新增 `RssFeedParserTest`，使用本地慢响应 HTTP 服务复现读取超时，并验证返回可读中文错误信息。

## 验证命令

```bash
mvn -f backend/pom.xml test

NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
NEWSRSS_RSS_READ_TIMEOUT_SECONDS=60 \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db

curl -sS http://127.0.0.1:8080/api/health | jq

curl --max-time 90 -sS -X POST 'http://127.0.0.1:8080/api/feeds/14/refresh' | jq '{success, message, data}'
```

## 执行结果

- 后端测试通过：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
- 新增慢响应 RSS 测试通过，确认超时错误会提示 `远端 RSS 响应超时` 和 `newsrss.rss.http.read-timeout-seconds`。
- 后端以前台方式启动成功，健康检查返回 `status: UP`。
- 真实订阅源 `https://rsshub.rssforever.com/eastmoney/report/strategyreport` 手动刷新成功：
  - `fetchedCount: 50`
  - `newCount: 1`
  - `duplicateCount: 49`
  - `failedCount: 0`
- 本次真实刷新耗时约 29 秒，说明原先 20 秒读取超时对部分 RSSHub 源偏短。

## 遗留问题

- 当前工具环境中，普通后台启动和 `spring-boot:start` 均未能保持后端进程常驻；前台启动已验证服务可以正常运行。后续人工测试时建议使用终端前台启动，或通过 IDE/进程管理器保持服务。
- 本次只处理 RSS HTTP 读取超时和错误提示；如果远端源本身长时间不可用，仍会按配置超时失败，但错误信息会更明确。
