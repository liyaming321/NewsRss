# 2026-07-01 服务器部署打包记录

## 实现过程

1. 检查当前 Spring Boot 单 Jar 打包配置，确认 `mvn -f backend/pom.xml package -DskipTests` 会执行前端 `pnpm build`，并将 `frontend/dist` 复制到 Jar 的 `BOOT-INF/classes/static` 目录。
2. 清理 `application-dev.yml` 与 `application-db.yml` 中 DeepSeek API Key 的默认明文值，改为只通过 `NEWSRSS_DEEPSEEK_API_KEY` 环境变量注入，避免部署包携带真实密钥。
3. 将 `application-db.yml` 中硬编码的 `server.port: 16080` 改为 `server.port: ${NEWSRSS_BACKEND_PORT:8080}`，服务器部署时可通过环境变量调整端口，默认使用 `8080`。
4. 执行后端测试、前端生产构建与 Spring Boot 可执行 Jar 打包。
5. 检查打包产物中是否包含前端静态文件，并确认 Jar 内配置不再包含真实密钥字符串。

## 验证命令

```bash
rg -n "sk-[A-Za-z0-9_-]+" backend/src/main/resources backend/src/main/java frontend/src README.md docs -g '!**/target/**' -g '!**/dist/**' || true
```

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml clean test
```

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml package -DskipTests
```

```bash
jar tf backend/target/newsrss-backend-0.0.1-SNAPSHOT.jar | rg 'BOOT-INF/classes/static/(index.html|assets/)' | head -20
```

```bash
unzip -p backend/target/newsrss-backend-0.0.1-SNAPSHOT.jar \
  BOOT-INF/classes/application-db.yml \
  BOOT-INF/classes/application-dev.yml | rg -n 'sk-[A-Za-z0-9_-]+|api-key|port:' || true
```

## 执行结果

- 密钥扫描：源码资源目录未发现真实 `sk-...` 密钥字符串；前端 `risk-panel` 类名包含 `sk-` 片段，属于误命中。
- 后端测试：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
- 打包结果：`BUILD SUCCESS`。
- 打包产物：`backend/target/newsrss-backend-0.0.1-SNAPSHOT.jar`，大小约 `56M`。
- 前端静态文件：Jar 内已包含 `BOOT-INF/classes/static/index.html` 与 `BOOT-INF/classes/static/assets/`。
- Jar 配置检查：`api-key` 已为 `${NEWSRSS_DEEPSEEK_API_KEY:}`，未携带真实密钥；`db` profile 端口为 `${NEWSRSS_BACKEND_PORT:8080}`。

## 遗留问题

- 第一次直接执行 `mvn -f backend/pom.xml test` 时，由于本机 PostgreSQL 测试容器映射端口为 `15432`，而默认配置连接 `localhost:5432`，上下文加载测试连接失败。后续使用本地测试库环境变量后测试通过。
- 前端构建存在 Vite chunk 大小提示：主 JS 包约 `991 kB`，不影响本次部署测试；后续可通过路由懒加载或 Rollup manualChunks 优化。
- 服务器部署时仍需要预先准备 PostgreSQL 数据库 `newsrss`；表结构由 Flyway 在应用启动时自动创建或迁移。
