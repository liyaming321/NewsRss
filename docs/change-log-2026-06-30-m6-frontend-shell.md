# M6 前端基础框架变更记录

## 实现过程

- 复核前端基础栈：Vue 3 + TypeScript + Vite + Naive UI + Pinia + Vue Router 已存在。
- 保留现有深色控制台视觉风格，继续使用 `src/theme/naive.ts` 管理 Naive UI 主题色、圆角、边框和暗色背景。
- 补齐建议目录结构：
  - `src/api`
  - `src/components/article`
  - `src/components/dashboard`
  - `src/components/feed`
  - `src/components/layout`
  - `src/components/parser-template`
  - `src/composables`
  - `src/router`
  - `src/stores`
  - `src/theme`
  - `src/types`
  - `src/views`
- 新增 `frontend/src/api/client.ts`，统一处理 JSON 请求和后端统一响应结构。
- 调整 `frontend/src/api/health.ts` 和 `frontend/src/api/fetchLogs.ts`，复用通用 API client。
- 新增 `frontend/src/composables/useBackendHealth.ts`，封装后端健康检查状态。
- 调整 `frontend/src/components/layout/AppLayout.vue`：
  - 顶部“后端健康”按钮接入真实 `/api/health`。
  - 页面加载后自动检查后端健康状态。
  - 顶部状态标签显示 `后端 在线`、`后端 异常` 或 `后端 未检查`。
- 保留已实现的左侧导航、顶部工具栏、主要路由页面和 M5 抓取日志页面。

本次未修改数据库和 SQL。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- 产物摘要：
  - `dist/index.html`：`0.46 kB`
  - `dist/assets/index-fGGFFf35.css`：`4.02 kB`
  - `dist/assets/index-BpsXjmSY.js`：`618.80 kB`
- Vite 输出 chunk 大小提示：`Some chunks are larger than 500 kB after minification`，当前不影响构建结果。

### 后端测试

```bash
mvn -f backend/pom.xml test
```

执行结果：

- Maven 构建成功。
- 测试结果：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。

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
- 启动早期曾出现代理到 `/api/health` 和 `/api/fetch-logs` 的 `ECONNREFUSED`，原因是浏览器页面先于后端完全 ready 发起请求；后端启动完成后页面已显示 `后端 在线`，路由和接口验证通过。
- 验证完成后通过 `Ctrl+C` 停止前端 dev server。

### 浏览器验证

```bash
export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
export PWCLI="$CODEX_HOME/skills/playwright/scripts/playwright_cli.sh"
"$PWCLI" open http://127.0.0.1:5173/
"$PWCLI" snapshot
"$PWCLI" click e23
"$PWCLI" snapshot
"$PWCLI" click e29
"$PWCLI" snapshot
"$PWCLI" click e53
"$PWCLI" snapshot
"$PWCLI" click e14
"$PWCLI" screenshot --filename /Users/lym/Documents/workspace/aiDemo/newsRss/output/playwright/m6-app-shell.png --full-page
```

执行结果：

- 页面标题：`NewsRss`。
- 基础应用壳可见：
  - 左侧品牌区：`NewsRss`、`信息流控制台`。
  - 左侧导航：驾驶舱、阅读、订阅源、解析模板、收藏、稍后读、抓取日志、设置。
  - 顶部标题和描述会随路由变化。
  - 顶部状态显示：`后端 在线`。
- 路由切换验证通过：
  - `/`
  - `/feeds`
  - `/parser-templates`
  - `/fetch-logs`
- 截图路径：`output/playwright/m6-app-shell.png`。

## 遗留问题

- 前端构建存在 Vite chunk 大小提示，后续可在页面功能增多后通过路由懒加载或 vendor 拆包优化。
- 当前 M6 只完成基础应用壳和导航框架，驾驶舱、阅读页、订阅源管理、解析模板实验室等深度业务页面按 M7-M10 继续接入。
