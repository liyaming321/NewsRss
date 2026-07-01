# 驾驶舱实时信息流跳转阅读详情

## 实现过程

- 为驾驶舱“实时信息流”文章卡片增加点击交互，点击后跳转到阅读页并携带 `articleId` 和 `feedId` 查询参数。
- 阅读页新增 `articleId` 和 `feedId` 路由参数解析能力，支持从外部入口直接打开指定文章详情。
- 阅读页从驾驶舱进入时会按订阅源筛选文章列表，并加载目标文章详情；如果目标文章不在当前分页列表中，右侧详情仍会直接打开该文章。
- 文章卡片补充 `role="link"`、`tabindex="0"` 以及 Enter/Space 键盘触发，保证基础可访问性。

## 验证命令

```bash
pnpm --dir frontend build
mvn -f backend/pom.xml test
```

## 执行结果

- 前端构建通过：`vue-tsc -b && vite build` 成功，仅保留 Vite chunk 大小超过 500 kB 的提示。
- 后端测试通过：`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- Playwright 验证通过：从驾驶舱点击“实时信息流”的“[开源证券]开源晨会”后，页面跳转到 `/reader?articleId=203&feedId=11`，阅读页右侧详情打开对应文章。
- 验收截图：`output/playwright/dashboard-stream-to-reader-detail.png`。

## 遗留问题

- 阅读页列表仍按当前阅读页默认排序展示；从驾驶舱跳入的目标文章如果不在当前分页中，右侧详情会正确打开，但左侧列表可能不会显示该条高亮。
