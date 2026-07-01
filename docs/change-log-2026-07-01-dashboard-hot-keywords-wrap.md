# 驾驶舱今日热词换行修复记录

## 实现过程

- 调整驾驶舱 `DashboardView` 中今日热词标签样式。
- 将热词标签从固定高度单行胶囊改为最小高度自适应标签。
- 为热词标签增加 `max-width: 100%`、`overflow-wrap: anywhere` 和 `word-break: break-word`，避免长中文短语撑出卡片边界。
- 保持热词颜色、边框和标签视觉风格不变。

## 验证命令

```bash
pnpm --dir frontend build
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package -DskipTests

SPRING_PROFILES_ACTIVE=db \
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
NEWSRSS_RSS_READ_TIMEOUT_SECONDS=60 \
java -jar backend/target/newsrss-backend-0.0.1-SNAPSHOT.jar

curl -sS http://127.0.0.1:8080/api/health | jq -r '.status'
```

## 执行结果

- 前端构建通过：`vite build` 正常完成；仅保留 chunk 大小超过 500 kB 的构建提示。
- 后端测试通过：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
- 单 Jar 打包通过，并已重启 `8080` 服务。
- 健康检查返回 `UP`。
- Playwright 已打开 `http://127.0.0.1:8080/` 验证驾驶舱页面，截图：`output/playwright/dashboard-hot-keywords-wrap.png`。

## 遗留问题

- 当前热词仍由前端基于最近文章标题和摘要轻量统计生成；后续可改为后端关键词服务，提供更稳定的分词、去噪和热度排序。
