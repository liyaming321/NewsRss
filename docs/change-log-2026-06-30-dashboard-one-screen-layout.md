# 驾驶舱一屏布局优化变更记录

## 实现过程

- 继续优化 `frontend/src/views/DashboardView.vue`：
  - 压缩驾驶舱模块间距，由偏展示型大留白改为控制台密度。
  - 指标卡高度从大卡片压缩为单行可扫读卡片。
  - RSS 抓取管线改为更短说明和更紧凑步骤块。
  - 四个下方面板缩小 padding、行距、标签高度和日志行距。
- 优化 `frontend/src/components/layout/AppLayout.vue`：
  - 顶部应用栏高度从 `92px` 调整为 `76px`。
  - 页面内容区 padding 从 `28px 32px` 调整为 `16px 24px`。
  - 标题字号和说明文字同步压缩，减少首屏固定占用。
- 本次未修改后端逻辑，未新增 SQL 和数据库迁移文件。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次布局优化。

### 浏览器一屏检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" open http://127.0.0.1:5173/
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" resize 1366 768
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" screenshot --filename output/playwright/dashboard-one-screen-compact-1366x768.png
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果：

```json
{
  "viewport": {
    "w": 1366,
    "h": 768
  },
  "body": 768,
  "header": 76,
  "dashboard": 659.96875,
  "needsScroll": false
}
```

截图路径：

- `output/playwright/dashboard-one-screen-compact-1366x768.png`

## 遗留问题

- 如果浏览器缩放大于 100% 或系统字体显著放大，仍可能需要滚动。
- 前端构建仍有 chunk 大小警告，建议后续按路由拆包。
