# 解析模板生成区域边界修复记录

## 实现过程

- 修复解析模板页“AI 生成模板”区域超出右侧面板的问题。
- 将原来的五列固定栅格改为两列输入区加一行可换行操作区：
  - 订阅源选择和 RSS 地址输入保留在主输入区。
  - 样本条数、AI 开关、生成按钮移动到 `.generate-actions`。
  - `.generate-actions` 使用 `flex-wrap` 和 `justify-content: flex-end`，保证窄宽度下按钮不会伸出容器。
- 本次仅调整前端布局样式，未修改后端逻辑，未新增 SQL。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次布局修复。

### 页面边界检查

```bash
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" open http://127.0.0.1:5173/parser-templates --headed
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" resize 1366 768
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" screenshot --filename output/playwright/parser-templates-generate-fit.png
"$HOME/.codex/skills/playwright/scripts/playwright_cli.sh" eval "..."
```

执行结果摘要：

```json
{
  "bodyScrollHeight": 768,
  "documentScrollHeight": 768,
  "panel": {
    "right": 1340,
    "width": 676
  },
  "button": {
    "right": 1327,
    "width": 108
  },
  "buttonWithinPanel": true,
  "panelWithinViewport": true
}
```

截图路径：

- `output/playwright/parser-templates-generate-fit.png`

## 遗留问题

- 页面右侧工作台内容较长，仍按当前产品要求保留内部滚动；如果后续继续增加生成配置项，建议将 AI 生成、字段映射、预览结果拆成折叠面板或标签页。
