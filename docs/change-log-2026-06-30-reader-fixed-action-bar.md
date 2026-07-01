# 2026-06-30 阅读页底部操作栏固定

## 实现过程

- 调整 `frontend/src/views/ReaderView.vue` 的阅读详情面板结构。
- 将原本跟随正文一起滚动的按钮区域拆为独立底部操作栏。
- 新增 `reader-detail-shell` 两行网格布局：上方 `reader-scroll` 独立滚动，下方 `action-row` 固定在阅读面板底部。
- 新增 `reader-detail-spin`、`reader-empty-state`、`reader-scroll` 等样式，保证加载态、空状态、正文滚动和按钮栏固定都在同一个阅读面板内完成。
- 保留原有收藏、稍后读、标记已读、归档、打开原文功能，不改接口和数据逻辑。

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
curl -sS -o /tmp/newsrss-reader-fixed-action.html -w '%{http_code}' http://127.0.0.1:5173/reader
```

执行结果：

```text
200
```

```bash
$PWCLI open http://127.0.0.1:5173/reader --headed
$PWCLI eval "滚动 .reader-scroll 前后读取 .action-row 的 getBoundingClientRect"
```

执行结果：

```json
{
  "before": { "top": 824, "bottom": 883 },
  "after": { "top": 824, "bottom": 883 },
  "barTopStable": true,
  "scroller": {
    "top": 480,
    "clientHeight": 685,
    "scrollHeight": 2158
  }
}
```

- 说明长文内容滚动时，底部按钮栏坐标保持不变。
- 验证截图已保存到 `output/playwright/reader-fixed-action-bar.png`。

## 遗留问题

- Vite 仍提示主 chunk 超过 500 kB，本次未处理打包拆分。
- 移动端目前仍沿用原响应式单列布局，后续可单独优化底部操作栏在手机上的贴底体验。
