# M0 项目基础设施变更记录

## 实现过程

- 初始化后端 Spring Boot 3.5.3 项目，使用 Java 17 和 Maven 管理依赖。
- 后端接入 Web、Validation、Actuator、JPA、PostgreSQL Driver、Flyway 依赖。
- 新增 `GET /api/health` 健康检查接口，用于验证后端服务和前端代理连通性。
- 默认配置 `application.yml` 暂不强制连接数据库，方便 M0 阶段无本地 PostgreSQL 时启动服务。
- 新增 `application-db.yml`，预留 PostgreSQL 连接、JPA 校验和 Flyway 迁移配置。
- 初始化前端 Vue 3 + TypeScript + Vite + Naive UI 项目，接入 Pinia、Vue Router 和 lucide 图标。
- 建立前端基础应用壳、左侧导航、顶部栏、驾驶舱占位页面、订阅源页面、解析模板页面、阅读页面、抓取日志页面和设置页面。
- 配置根目录 `package.json` 与 `pnpm-workspace.yaml`，统一使用 pnpm workspace 管理前端命令。
- 将前端工具链固定到兼容当前 Node.js 20.18.0 的 Vite 5.4.21、Vue Router 4.6.3、TypeScript 5.8.3、vue-tsc 2.2.12。
- 更新页面标题为 `NewsRss`，补充 Java 启动入口和基础测试的中文方法注释。
- 创建本地启动说明，记录在 `README.md` 中。

## 环境信息

```bash
java -version
mvn -version
node -v
pnpm -v
```

- Java：`17.0.8`
- Maven：`3.9.14`
- Node.js：`v20.18.0`
- pnpm：`10.28.0`

## 验证命令

```bash
mvn -f backend/pom.xml test
```

执行结果：

- 成功。
- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- Maven 输出 `BUILD SUCCESS`。

```bash
pnpm frontend:build
```

执行结果：

- 成功。
- Vite 使用 `5.4.21` 完成生产构建。
- 关键产物：
  - `dist/index.html`：`0.46 kB`
  - `dist/assets/index-D5IE_tF3.css`：`3.60 kB`
  - `dist/assets/index-Mk7jtGEq.js`：`314.33 kB`

```bash
mvn -f backend/pom.xml spring-boot:run
```

执行结果：

- 成功启动。
- 后端启动日志摘要：
  - `Tomcat initialized with port 8080 (http)`
  - `Tomcat started on port 8080 (http) with context path '/'`
  - `Started NewsrssBackendApplication in 1.121 seconds`
- 验证完成后已通过 `Ctrl+C` 正常关闭，日志显示 `Graceful shutdown complete`。

```bash
curl -s http://localhost:8080/api/health
```

执行结果：

```json
{"applicationName":"NewsRss","version":"0.0.1-SNAPSHOT","status":"UP","checkedAt":"2026-06-30T00:20:32.049308Z"}
```

```bash
pnpm frontend:dev
```

执行结果：

- 成功启动。
- Vite 输出本地访问地址：`http://localhost:5173/`
- 验证完成后已通过 `Ctrl+C` 关闭。

## 浏览器验证

```bash
$PWCLI open http://localhost:5173
$PWCLI snapshot
$PWCLI screenshot --filename output/playwright/newsrss-m0-app-shell.png --full-page
```

执行结果：

- 页面标题：`NewsRss`
- 页面可见内容包含：
  - `NewsRss`
  - `信息流控制台`
  - `资讯驾驶舱`
  - `今日新增`
  - `未读文章`
  - `活跃订阅源`
  - `异常源`
- 截图路径：`output/playwright/newsrss-m0-app-shell.png`

## 数据库配置说明

- M0 阶段未创建数据库表和迁移脚本。
- PostgreSQL 连接配置已预留在 `backend/src/main/resources/application-db.yml`。
- 启用数据库配置的启动命令：

```bash
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

- M1 开始追加 Flyway SQL 迁移文件，并按规则为所有表和字段增加注释。

## 遗留问题

- M0 未连接真实 PostgreSQL 实例，数据库连通性与迁移执行留到 M1 验证。
- 当前前端页面为基础应用壳和占位数据，真实接口联动留到后续里程碑。
- `pnpm install` 提示 `Ignored build scripts: esbuild@0.21.5`，但 `pnpm frontend:build` 已验证成功，暂不影响 M0。
