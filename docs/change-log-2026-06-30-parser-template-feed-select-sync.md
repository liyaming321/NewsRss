# 2026-06-30 解析模板订阅源选择同步修复

## 实现过程

- 修复解析模板页面 AI 生成区选择订阅源后，RSS 地址输入框未及时更新的问题。
- 在 `frontend/src/views/ParserTemplatesView.vue` 中新增 `syncGenerateFeedUrl` 方法，通过字符串化 ID 比较兼容 `NSelect` 返回数字或字符串的情况。
- 将同步方法同时绑定到 `NSelect` 的 `@update:value` 事件，并通过 `watch([selectedGenerateFeedId, feeds])` 处理订阅源列表异步加载后的补同步。
- 调整 AI 生成栏布局为“订阅源选择 / RSS 地址 / 操作区”三列，窄屏继续走已有响应式单列，减少右侧内容被截断的概率。

## 验证命令

```bash
pnpm --dir frontend build
```

执行结果：

```text
vue-tsc -b && vite build 执行成功
Vite 构建成功，产物生成完成
存在 chunk 大于 500 kB 的体积提示，未影响构建结果
```

```bash
curl -s http://127.0.0.1:8080/actuator/health
```

执行结果：

```json
{"status":"UP"}
```

```bash
curl -s 'http://127.0.0.1:8080/api/feeds?page=0&size=100'
```

执行结果：

```text
接口返回 200，包含 ID=10 的东方财富网-行业研报订阅源：
https://rsshub.rssforever.com/eastmoney/report/industry
```

```bash
$PWCLI open http://127.0.0.1:5173/parser-templates --headed
$PWCLI click <订阅源下拉>
$PWCLI click <东方财富网-行业研报选项>
$PWCLI eval "() => Array.from(document.querySelectorAll('input')).map((input) => input.value)"
```

执行结果：

```json
[
  "",
  "https://rsshub.rssforever.com/eastmoney/report/industry",
  "5",
  "m3-fixture-template",
  "M3 Fixture 完整模板 v2",
  "https://rsshub.rssforever.com/eastmoney/report/industry",
  "5"
]
```

- AI 生成区 RSS 地址输入框已更新为选中订阅源地址。
- 测试 RSS 源输入框复用同一 `feedUrl`，也同步更新为选中订阅源地址。
- 验证截图已保存到 `output/playwright/parser-template-feed-url-sync.png`。

## 遗留问题

- Vite 仍提示主 chunk 超过 500 kB，属于前端打包体积优化问题，本次未处理。
- 官方 `https://rsshub.app/eastmoney/report/industry` 当前不可作为稳定测试源，人工测试建议继续使用已保存的 RSSForever 镜像源。
