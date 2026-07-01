# 2026-06-29 Product Design 优化变更记录

## 实现过程

- 根据用户指定的 `product-design@role-specific-plugins`，读取了本机已安装 Product Design 插件的核心说明。
- 执行 Product Design 用户上下文预检，结果显示当前暂无已保存的全局设计上下文。
- 参考当前项目文档和已有本地原型，补充了产品设计参考文档，覆盖 Naive UI、Material Design 3、WCAG 2.2、复杂仪表盘/信息密度设计原则。
- 对 V1 原型做了产品设计优化：
  - 增加顶部原型切换器，方便横向画布评审。
  - 首页增加 RSS 抓取管线，突出抓取、解析、去重、入库流程。
  - 阅读页增加阅读工具、速读摘要、同源最新文章。
  - 订阅源页增加健康概览和保存前风险检查。
  - 解析模板页增加测试源输入、模板命中率、字段命中路径、预览状态更新。
  - 增加轻量 JavaScript 交互，包括画板切换、文章选择、筛选切换、阅读偏好、模板切换和预览反馈。
- 补充 `design-qa.md`，记录 Product Design QA 结果。

## 变更文件

- `docs/product-design-reference-and-optimization.md`
- `prototype/index.html`
- `prototype/styles.css`
- `prototype/app.js`
- `design-qa.md`

## 验证命令

```bash
curl -I http://127.0.0.1:5177
curl -s http://127.0.0.1:5177/app.js | head -20
npx playwright screenshot --channel=chrome --viewport-size=1440,900 http://127.0.0.1:5177 output/playwright/newsrss-prototype-v2-dashboard.png
npx playwright screenshot --channel=chrome --viewport-size=1440,900 http://127.0.0.1:5177/#reader output/playwright/newsrss-prototype-v2-reader.png
npx playwright screenshot --channel=chrome --viewport-size=1440,900 http://127.0.0.1:5177/#parser output/playwright/newsrss-prototype-v2-parser.png
```

## 执行结果

- 本地原型服务返回 `HTTP/1.0 200 OK`。
- `app.js` 可通过 `http://127.0.0.1:5177/app.js` 正常访问。
- 已生成优化后的驾驶舱截图：`output/playwright/newsrss-prototype-v2-dashboard.png`。
- 已生成优化后的阅读页截图：`output/playwright/newsrss-prototype-v2-reader.png`。
- 已生成优化后的解析模板页截图：`output/playwright/newsrss-prototype-v2-parser.png`。
- Product Design QA 结果为 `passed`。

## 遗留问题

- 当前仍是本地 HTML/CSS/JS 原型，尚未转换为 Vue 3 + TypeScript + Naive UI 组件。
- 原型中的导航图标仍为文本符号，真实前端实现时建议替换为 lucide-vue 或 Naive UI 兼容图标。
- Figma MCP 之前受 Starter 计划调用额度限制，本次未继续写入 Figma 画板。
- 当前原型未接入真实 RSS 数据、后端接口和数据库状态。

