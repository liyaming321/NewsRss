# 单 Jar 部署模式变更记录

## 实现过程

- 调整后端 Maven 打包流程：
  - `mvn test` 只执行后端测试，不触发前端构建。
  - `mvn package` 在 `prepare-package` 阶段执行 `pnpm build`。
  - 前端 `frontend/dist` 会复制到 `target/classes/static`，最终进入 Spring Boot Jar。
- 新增 `SpaForwardConfig`，支持 Vue Router 前端路由刷新：
  - `/reader`
  - `/feeds`
  - `/parser-templates`
  - `/fetch-logs`
  - `/settings`
- 保持 `/api/**` 和 `/actuator/**` 由后端接口处理，静态资源 `/assets/**`、`favicon.svg`、`icons.svg` 由 Spring Boot 静态资源处理。
- 更新 `README.md`，新增单 Jar 部署说明和启动命令。

## 验证命令

```bash
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package -DskipTests
jar tf backend/target/newsrss-backend-0.0.1-SNAPSHOT.jar | rg 'BOOT-INF/classes/static/(index.html|assets|favicon|icons)'

SPRING_PROFILES_ACTIVE=db \
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
NEWSRSS_RSS_READ_TIMEOUT_SECONDS=60 \
java -jar backend/target/newsrss-backend-0.0.1-SNAPSHOT.jar

curl -sS -o /tmp/newsrss-root.html -w '%{http_code} %{content_type}' http://127.0.0.1:8080/
curl -sS -o /tmp/newsrss-reader.html -w '%{http_code} %{content_type}' http://127.0.0.1:8080/reader
curl -sS http://127.0.0.1:8080/api/health | jq
curl -sS -o /tmp/newsrss-asset.js -w '%{http_code} %{content_type} %{size_download}' http://127.0.0.1:8080/assets/index-BdzE1HHm.js
```

## 执行结果

- 后端测试通过：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
- 后端 package 成功，并自动执行前端 `pnpm build`。
- Jar 内确认包含：
  - `BOOT-INF/classes/static/index.html`
  - `BOOT-INF/classes/static/assets/index-BdzE1HHm.js`
  - `BOOT-INF/classes/static/assets/index-Ch_3_QAX.css`
  - `BOOT-INF/classes/static/favicon.svg`
  - `BOOT-INF/classes/static/icons.svg`
- 单 Jar 启动成功，`/` 返回 `200 text/html`。
- `/reader` 前端路由刷新返回 `200 text/html`。
- `/api/health` 返回后端 JSON，状态为 `UP`。
- 静态 JS 资源返回 `200 text/javascript`。
- Playwright 已验证 `http://127.0.0.1:8080/reader` 可直接渲染完整阅读页并请求后端数据，截图：`output/playwright/single-jar-reader.png`。

## 遗留问题

- 当前前端路由 fallback 使用明确路由列表，后续新增 Vue 页面路由时，需要同步在 `SpaForwardConfig` 中追加对应路径。
- 前端构建仍有 Vite chunk 超过 500 kB 的提示，不影响打包和运行；后续可以按页面做动态导入优化首屏资源。
