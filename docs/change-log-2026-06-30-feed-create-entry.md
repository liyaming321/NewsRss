# 订阅源新增入口优化变更记录

## 实现过程

- 优化 `frontend/src/views/FeedsView.vue` 的订阅源新增入口：
  - 顶部主按钮文案由“新增源”调整为“新增订阅源”，提升人工测试时的可发现性。
  - 空列表状态增加“新增订阅源”按钮，列表无数据时也能直接打开新增抽屉。
  - 为顶部新增按钮增加最小宽度和强调字重。
- 沿用当前页面已有新增抽屉能力：
  - 支持填写 RSS 地址、自动探测、订阅源名称、分组、抓取频率、解析模板、图标 URL 和启用状态。
  - 保存时调用已有 `POST /api/feeds` 接口。
- 本次未修改后端逻辑，未新增 SQL 和数据库迁移文件。

## 验证命令

### 前端构建

```bash
pnpm --dir frontend build
```

执行结果：

- `vue-tsc -b` 通过。
- `vite build` 成功。
- Vite 仍提示部分 chunk 大于 `500 kB`，不影响本次入口优化。

### 页面访问

```bash
curl -sS -o /tmp/newsrss-feeds-page.html -w '%{http_code}' 'http://127.0.0.1:5173/feeds'
```

执行结果：

- 返回 `200`。

### 后端订阅源接口

```bash
curl -sS 'http://localhost:8080/api/feeds?page=0&size=1' | jq
```

执行结果摘要：

```json
{
  "success": true,
  "total": 5,
  "first": {
    "id": 6,
    "feedName": "M5 临时失败源"
  }
}
```

## 遗留问题

- 目前 OPML 导入/导出仍是预留入口，尚未接入文件解析和导出接口。
- 前端构建仍有 chunk 大小警告，建议后续路由拆包。
