# 2026-06-29 UI 原型变更记录

## 实现过程

- 搜索并确认当前环境已具备官方 Figma MCP 插件能力。
- 按 Figma 插件要求加载了 `figma-create-new-file`、`figma-use`、`figma-generate-design` 技能说明。
- 使用 Figma MCP 创建了设计文件：`NewsRss 新闻订阅阅读系统 V1 原型`。
- 尝试向 Figma 文件写入首页驾驶舱、三栏阅读、订阅源管理、解析模板实验室、移动端阅读等画板。
- 因当前 Figma Starter 计划触发 MCP 调用额度限制，暂时无法继续通过 MCP 写入 Figma 画板。
- 为不中断原型工作，创建了本地静态 HTML/CSS 高保真原型，覆盖以下界面：
  - 首页资讯驾驶舱。
  - 三栏沉浸阅读页。
  - 订阅源管理页。
  - RSS 解析模板实验室。
  - 移动端阅读预览。
- 使用本地静态服务器启动原型页面，并通过系统 Chrome + Playwright CLI 截图验证首屏和完整页面。

## 变更文件

- `prototype/index.html`
- `prototype/styles.css`

## 验证命令

```bash
python3 -m http.server 5177
npx playwright screenshot --channel=chrome --viewport-size=1440,900 http://127.0.0.1:5177 output/playwright/newsrss-prototype-firstscreen.png
npx playwright screenshot --channel=chrome --viewport-size=1440,900 --full-page http://127.0.0.1:5177 output/playwright/newsrss-prototype-full.png
```

## 执行结果

- 本地静态服务器已成功启动，访问地址为 `http://127.0.0.1:5177`。
- 已生成首屏截图：`output/playwright/newsrss-prototype-firstscreen.png`。
- 已生成完整页面截图：`output/playwright/newsrss-prototype-full.png`。
- 首屏截图检查通过，默认视口下首页驾驶舱主区域展示完整，没有发现明显文字重叠或主要内容空白。

## 遗留问题

- Figma 文件已创建，但因 Starter 计划 MCP 调用额度限制，暂未完成自动写入画板。
- 当前原型是静态 HTML/CSS，不包含 Vue 组件逻辑和接口联调。
- 后续 Figma MCP 额度恢复后，可以将本地原型同步为可编辑 Figma 画板。
- 后续进入前端实现时，需要按 Vue 3 + TypeScript + Vite + Naive UI 拆分真实组件。

