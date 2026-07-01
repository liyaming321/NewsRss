# M1 数据库与领域模型变更记录

## 实现过程

- 追加 Flyway 迁移文件：`backend/src/main/resources/db/migration/V1__create_rss_core_tables.sql`。
- 新建 7 张核心业务表：
  - `rss_parser_template`
  - `rss_feed`
  - `rss_article`
  - `rss_article_user_state`
  - `rss_feed_fetch_log`
  - `rss_tag`
  - `rss_article_tag`
- 为所有业务表和字段追加 PostgreSQL `comment on` 注释。
- 为订阅源、文章、用户状态、抓取日志和文章标签建立外键、唯一约束、检查约束和查询索引。
- 文章表增加 `raw_payload`、`parse_trace`、`fingerprint`，支撑后续 RSS 原始数据追踪、解析命中轨迹和去重。
- 订阅源表增加 `parser_template_id`、`health_status`、`fetch_interval_minutes`、连续失败次数和抓取时间字段，支撑模板绑定、调度与健康判断。
- 新增后端领域模型目录：
  - `domain/entity`
  - `domain/enums`
  - `repository`
  - `dto`
- 新增 JPA Entity、枚举、Spring Data Repository 和基础 DTO，作为后续 M2-M4 的开发基座。

## 迁移验证环境

使用临时 PostgreSQL 16 容器验证迁移。

```bash
docker run --rm --name newsrss-m1-postgres \
  --tmpfs /var/lib/postgresql/data:rw,size=256m \
  -e POSTGRES_DB=newsrss \
  -e POSTGRES_USER=newsrss \
  -e POSTGRES_PASSWORD=newsrss \
  -p 15432:5432 \
  -d postgres:16-alpine
```

说明：

- 首次直接启动 PostgreSQL 容器时，Docker VM 数据盘提示 `No space left on device`。
- 未清理用户 Docker 资源，改用 `tmpfs` 临时数据目录完成无持久化验证。
- 验证完成后已停止容器，端口 `15432` 已释放。

## 验证命令

```bash
mvn -f backend/pom.xml test
```

执行结果：

- 成功。
- 编译 `24 source files`。
- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- Maven 输出 `BUILD SUCCESS`。

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

执行结果：

- 成功连接 PostgreSQL 16.14。
- Spring Data 扫描到 `7 JPA repository interfaces`。
- Flyway 输出：
  - `Successfully validated 1 migration`
  - `Migrating schema "public" to version "1 - create rss core tables"`
  - `Successfully applied 1 migration to schema "public", now at version v1`
- Hibernate 输出：
  - `Initialized JPA EntityManagerFactory for persistence unit 'default'`
- 后端启动成功：
  - `Tomcat started on port 8080`
  - `Started NewsrssBackendApplication`

```bash
curl -s http://localhost:8080/api/health
```

执行结果：

```json
{"applicationName":"NewsRss","version":"0.0.1-SNAPSHOT","status":"UP","checkedAt":"2026-06-30T00:36:34.488332Z"}
```

## 数据库验证 SQL 与结果

查询业务表：

```sql
select tablename
from pg_tables
where schemaname = 'public'
  and tablename like 'rss_%'
order by tablename;
```

结果摘要：

```text
rss_article
rss_article_tag
rss_article_user_state
rss_feed
rss_feed_fetch_log
rss_parser_template
rss_tag
```

查询字段数量：

```sql
select table_name, count(*)
from information_schema.columns
where table_schema = 'public'
  and table_name like 'rss_%'
group by table_name
order by table_name;
```

结果摘要：

```text
rss_article|19
rss_article_tag|3
rss_article_user_state|13
rss_feed|19
rss_feed_fetch_log|16
rss_parser_template|12
rss_tag|7
```

查询注释数量：

```sql
select count(*)
from pg_description d
join pg_class c on c.oid = d.objoid
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relname like 'rss_%';
```

结果摘要：

```text
96
```

查询迁移历史：

```sql
select version, description, success
from flyway_schema_history
order by installed_rank;
```

结果摘要：

```text
1|create rss core tables|t
```

查询关键表注释：

```sql
select c.relname, obj_description(c.oid, 'pg_class')
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relname in ('rss_feed','rss_article','rss_parser_template','rss_feed_fetch_log')
order by c.relname;
```

结果摘要：

```text
rss_article|RSS 文章表，保存标准化后的文章内容、来源信息和去重指纹
rss_feed|RSS 订阅源表，保存订阅地址、抓取频率、健康状态和模板绑定关系
rss_feed_fetch_log|RSS 抓取日志表，记录每次抓取的请求、数量统计和错误信息
rss_parser_template|RSS 解析模板表，保存不同订阅源的字段映射和清洗规则
```

查询关键字段注释：

```sql
select table_name,
       column_name,
       col_description((table_schema || '.' || table_name)::regclass, ordinal_position)
from information_schema.columns
where table_schema = 'public'
  and table_name in ('rss_feed','rss_article')
  and column_name in ('feed_url','parser_template_id','raw_payload','fingerprint','parse_trace')
order by table_name, column_name;
```

结果摘要：

```text
rss_article|fingerprint|文章去重指纹，由关键字段生成
rss_article|parse_trace|解析命中轨迹，记录字段命中路径和模板处理结果
rss_article|raw_payload|原始 RSS 条目数据，JSON 结构保存用于排查和重放解析
rss_feed|feed_url|RSS 或 Atom 订阅地址
rss_feed|parser_template_id|绑定的解析模板主键，为空时使用默认解析逻辑
```

查询关键约束：

```sql
select conname, contype
from pg_constraint
where conrelid in (
  'rss_feed'::regclass,
  'rss_article'::regclass,
  'rss_article_user_state'::regclass,
  'rss_article_tag'::regclass
)
order by conname;
```

结果摘要：

```text
ck_rss_article_reading_minutes_non_negative|c
ck_rss_article_word_count_non_negative|c
ck_rss_feed_failure_count_non_negative|c
ck_rss_feed_fetch_interval_positive|c
fk_rss_article_feed|f
fk_rss_article_tag_article|f
fk_rss_article_tag_tag|f
fk_rss_article_user_state_article|f
fk_rss_feed_parser_template|f
rss_article_pkey|p
rss_article_tag_pkey|p
rss_article_user_state_pkey|p
rss_feed_pkey|p
uk_rss_article_feed_fingerprint|u
uk_rss_article_feed_url|u
uk_rss_article_user_state_article_user|u
uk_rss_feed_url|u
```

## 遗留问题

- 当前 M1 只建立领域模型基础结构，没有实现业务服务和接口。
- JPA Entity 暂未加入完整 getter/setter 或业务构造方法，后续在 M2-M4 按实际写入、查询需求补充。
- 本次数据库验证使用临时 PostgreSQL 容器；长期本地数据库和正式环境迁移需要在后续部署配置中单独准备。
