# M2 RSS 抓取与默认解析变更记录

## 实现过程

- 接入 RSS/Atom 解析库 Rome Tools：`com.rometools:rome:2.1.0`。
- 接入 HTML 清洗库 Jsoup：`org.jsoup:jsoup:1.22.2`。
- 新增默认 RSS 抓取链路：
  - `RssFeedParser`：抓取 RSS URL，并用 Rome 解析 RSS/Atom。
  - `RssArticleNormalizer`：把 Rome 条目标准化为系统文章结构。
  - `RssContentCleaner`：使用 Jsoup 清洗正文 HTML、提取纯文本和封面图。
  - `RssFingerprintGenerator`：基于订阅源、GUID、URL、标题和时间生成 SHA-256 去重指纹。
  - `RssFeedFetchService`：编排抓取、解析、去重、入库、订阅源状态更新和抓取日志。
- 支持默认字段映射：
  - 标题：`entry.title`
  - 链接：`entry.link`
  - GUID：`entry.uri`
  - 发布时间：优先 `entry.publishedDate`，兜底 `entry.updatedDate`
  - 正文：优先 `entry.contents[0]`，兜底 `entry.description`
  - 作者：优先 `entry.author`，兜底 `entry.authors[0].name`
  - 封面图：优先 image enclosure，兜底正文第一张图片
- 保存 `raw_payload`，记录原始条目关键字段、正文内容列表和附件信息。
- 保存 `parse_trace`，记录默认解析器命中的字段路径和正文清洗状态。
- 去重策略按 `GUID -> article_url -> fingerprint` 顺序判断。
- 新增 `RssFetchSmokeRunner` 和 `RssFetchSmokeProperties`，用于命令行批量抓取验证。
- 更新 `README.md`，追加 RSS 抓取冒烟验证命令。
- 设置 Maven 源码和报告编码为 UTF-8。

## 验证环境

- 测试数据库：常驻 Docker 容器 `newsrss-postgres`
- JDBC URL：`jdbc:postgresql://localhost:15432/newsrss`
- 数据库版本：PostgreSQL 16.14

M2 验证前清空业务表：

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss -v ON_ERROR_STOP=1 \
  -c "truncate table rss_article_tag, rss_article_user_state, rss_feed_fetch_log, rss_article, rss_feed, rss_parser_template, rss_tag restart identity cascade;"
```

执行结果：

```text
TRUNCATE TABLE
```

## 验证命令

后端编译和测试：

```bash
mvn -f backend/pom.xml clean test
```

执行结果：

- 成功。
- 编译 `35 source files`。
- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- Maven 输出 `BUILD SUCCESS`。
- Maven 控制台仍会把部分编译器中文提示显示为问号，这是本地终端输出编码表现；数据库中保存的中文错误信息正常。

首次抓取 3 个 RSS/Atom 源：

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=db \
  -Dspring-boot.run.arguments="--newsrss.fetch.smoke.enabled=true --newsrss.fetch.smoke.urls[0]=https://hnrss.org/frontpage --newsrss.fetch.smoke.urls[1]=https://github.blog/feed/ --newsrss.fetch.smoke.urls[2]=https://xkcd.com/atom.xml"
```

执行结果：

```text
RSS_FETCH_RESULT feedUrl=https://hnrss.org/frontpage success=true fetched=20 new=20 duplicate=0 failed=0 error=null
RSS_FETCH_RESULT feedUrl=https://github.blog/feed/ success=true fetched=10 new=10 duplicate=0 failed=0 error=null
RSS_FETCH_RESULT feedUrl=https://xkcd.com/atom.xml success=true fetched=4 new=4 duplicate=0 failed=0 error=null
```

重复抓取同一组源：

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=db \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --newsrss.fetch.smoke.enabled=true --newsrss.fetch.smoke.urls[0]=https://hnrss.org/frontpage --newsrss.fetch.smoke.urls[1]=https://github.blog/feed/ --newsrss.fetch.smoke.urls[2]=https://xkcd.com/atom.xml"
```

执行结果：

```text
RSS_FETCH_RESULT feedUrl=https://hnrss.org/frontpage success=true fetched=20 new=0 duplicate=20 failed=0 error=null
RSS_FETCH_RESULT feedUrl=https://github.blog/feed/ success=true fetched=10 new=0 duplicate=10 failed=0 error=null
RSS_FETCH_RESULT feedUrl=https://xkcd.com/atom.xml success=true fetched=4 new=0 duplicate=4 failed=0 error=null
```

失败源测试：

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=db \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --newsrss.fetch.smoke.enabled=true --newsrss.fetch.smoke.urls[0]=https://example.invalid/not-rss.xml"
```

执行结果摘要：

```text
RSS_FETCH_RESULT feedUrl=https://example.invalid/not-rss.xml success=false fetched=0 new=0 duplicate=0 failed=0 error=抓取或解析 RSS 失败：Remote host terminated the handshake
```

## 数据库验证 SQL 与结果

查询抓取日志统计：

```sql
select f.feed_url,
       l.status,
       l.fetched_count,
       l.new_count,
       l.duplicate_count,
       l.failed_count
from rss_feed_fetch_log l
join rss_feed f on f.id = l.feed_id
order by l.id;
```

结果摘要：

```text
https://hnrss.org/frontpage|SUCCESS|20|20|0|0
https://github.blog/feed/|SUCCESS|10|10|0|0
https://xkcd.com/atom.xml|SUCCESS|4|4|0|0
https://hnrss.org/frontpage|SUCCESS|20|0|20|0
https://github.blog/feed/|SUCCESS|10|0|10|0
https://xkcd.com/atom.xml|SUCCESS|4|0|4|0
https://example.invalid/not-rss.xml|FAILED|0|0|0|0
https://example.invalid/not-rss.xml|FAILED|0|0|0|0
https://hnrss.org/frontpage|SUCCESS|20|0|20|0
https://github.blog/feed/|SUCCESS|10|0|10|0
https://xkcd.com/atom.xml|SUCCESS|4|0|4|0
```

查询总量：

```sql
select count(*) from rss_feed_fetch_log;
select count(*) from rss_article;
select count(*) from rss_feed;
```

结果摘要：

```text
11
34
4
```

查询各源入库数量：

```sql
select f.feed_url, count(a.id)
from rss_feed f
left join rss_article a on a.feed_id = f.id
group by f.feed_url
order by f.feed_url;
```

结果摘要：

```text
https://example.invalid/not-rss.xml|0
https://github.blog/feed/|10
https://hnrss.org/frontpage|20
https://xkcd.com/atom.xml|4
```

查询 `raw_payload` 和 `parse_trace` 保存情况：

```sql
select count(*),
       count(raw_payload),
       count(parse_trace),
       count(*) filter (where raw_payload <> '{}'::jsonb),
       count(*) filter (where parse_trace <> '{}'::jsonb)
from rss_article;
```

结果摘要：

```text
34|34|34|34|34
```

查询原始数据样例：

```sql
select title,
       article_url,
       coalesce(guid,''),
       left(raw_payload::text, 120),
       left(parse_trace::text, 120)
from rss_article
order by id
limit 2;
```

结果摘要：

```text
South Korea to spend $1T on more memory chip production and humanoid robots|https://arstechnica.com/ai/2026/06/south-korea-to-spend-1t-on-more-memory-chip-production-and-humanoid-robots/|https://news.ycombinator.com/item?id=48726102|{"uri": "...", "link": "..."}|{"parser": "default-rome", "urlPath": "entry.link", ...}
Scientists find molecular-level evidence for two structures in liquid water|https://phys.org/news/2026-06-scientists-molecular-evidence-liquid.html|https://news.ycombinator.com/item?id=48726073|{"uri": "...", "link": "..."}|{"parser": "default-rome", "urlPath": "entry.link", ...}
```

查询失败日志：

```sql
select id, status, request_url, left(error_message, 120)
from rss_feed_fetch_log
where status = 'FAILED'
order by id desc
limit 1;
```

结果摘要：

```text
8|FAILED|https://example.invalid/not-rss.xml|抓取或解析 RSS 失败：Remote host terminated the handshake
```

## 遗留问题

- M2 只实现默认解析链路，解析模板的字段优先级、时间格式和清洗规则配置留到 M3。
- 当前 smoke runner 是命令行验证工具，不是正式对外 API；订阅源 API 和手动刷新接口留到 M4/M5。
- Rome 的 `XmlReader` 在当前版本有废弃提示，但功能可用；后续如果升级 Rome 或替换 HTTP 客户端，可以一并消除该提示。
- 失败源测试因网络/TLS 失败触发，错误信息已保存到数据库；不同网络环境下错误文本可能略有差异。
