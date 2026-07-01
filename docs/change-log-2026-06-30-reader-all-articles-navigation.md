# 2026-06-30 全部文章入口与阅读导航精简

## 实现过程

- 精简左侧主导航，移除独立的 `收藏` 和 `稍后读` 入口。
- 将原 `阅读` 主导航改为 `全部文章`，作为查看所有订阅源文章的统一入口。
- 更新阅读页路由元信息，顶部标题改为 `全部文章`，说明文案明确支持在页内筛选未读、收藏和稍后读。
- 阅读页保留页面内过滤器：`全部文章`、`未读`、`收藏`、`稍后读`、`今日更新`，避免主导航重复但不丢失功能。
- 阅读页文章列表从后端分页接口读取所有订阅源文章，增加页码状态和分页控件，列表说明展示当前页范围、总文章数和当前筛选数量。
- 源分组里的 `全部源` 数量改为后端返回的全量文章数，避免只显示当前页数量造成误解。

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
curl -s 'http://127.0.0.1:8080/api/articles?page=0&size=40'
```

执行结果：

```text
接口返回 200，feedId 为空时返回所有订阅源文章分页数据。
```

```bash
$PWCLI open http://127.0.0.1:5173/reader --headed
$PWCLI eval "() => ({
  nav: Array.from(document.querySelectorAll('nav a')).map((item) => item.textContent?.trim()),
  title: document.querySelector('h2')?.textContent?.trim(),
  listHeading: Array.from(document.querySelectorAll('h3')).map((item) => item.textContent?.trim()).find((text) => text === '全部文章'),
  paginationText: document.body.textContent?.includes('/ 3') ?? false
})"
```

执行结果：

```json
{
  "nav": ["驾驶舱", "全部文章", "订阅源", "解析模板", "抓取日志", "设置"],
  "title": "全部文章",
  "listHeading": "全部文章",
  "paginationText": true
}
```

- 截图已保存到 `output/playwright/reader-all-articles-nav.png`。

## 遗留问题

- 阅读页过滤器的 `未读`、`收藏`、`稍后读`、`今日更新` 数量当前基于当前页文章计算，不是全库精确统计；后续如需全局准确数量，需要后端增加状态统计接口或文章查询筛选参数。
- 源分组列表当前展示当前页内出现的订阅源分组；后续如需全量源分组统计，需要后端提供聚合接口。
- Vite 仍提示主 chunk 超过 500 kB，本次未处理打包拆分。
