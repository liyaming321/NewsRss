# Docker 清理与常驻测试数据库变更记录

## 实现过程

- 按用户要求清理 WeKnora 编排相关 Docker 资源。
- 删除范围只包含明确命名为 `WeKnora-*`、`weknora_*` 或 WeKnora 专用镜像的资源，未执行全局 `docker system prune`。
- 保留其他项目容器、镜像和数据卷。
- 新建常驻 PostgreSQL 测试容器 `newsrss-postgres`，后续 M2/M3/M4 验证可复用，不需要每次重新部署数据库。
- 更新 `README.md`，补充常驻测试数据库启动命令和 `db` profile 连接配置。

## 清理的 Docker 资源

删除的容器：

```text
WeKnora-app
WeKnora-frontend
WeKnora-milvus
WeKnora-postgres
WeKnora-minio
WeKnora-neo4j
WeKnora-redis
WeKnora-docreader
```

删除的网络：

```text
weknora_WeKnora-network
weknora_default
```

删除的数据卷：

```text
weknora_data-files
weknora_docreader-tmp
weknora_jaeger_data
weknora_milvus_data
weknora_minio_data
weknora_neo4j-data
weknora_postgres-data
weknora_qdrant_data
```

删除的镜像：

```text
docker.xuanyuan.run/wechatopenai/weknora-app:latest
docker.xuanyuan.run/wechatopenai/weknora-ui:latest
docker.xuanyuan.run/wechatopenai/weknora-docreader:latest
docker.xuanyuan.run/milvusdb/milvus:v2.6.11
milvusdb/milvus:v2.6.11
minio/minio:RELEASE.2025-09-07T16-13-09Z
minio/minio:latest
neo4j:2025.10.1
paradedb/paradedb:v0.21.4-pg17
redis:7.0-alpine
```

## Docker 空间变化

清理前：

```text
Images: 36.63GB
Containers: 798.3MB
Local Volumes: 2.344GB
```

清理并创建 `newsrss-postgres` 后：

```text
Images: 25.7GB
Containers: 548.7MB
Local Volumes: 1.618GB
```

## 常驻测试数据库

创建命令：

```bash
docker run --name newsrss-postgres \
  -e POSTGRES_DB=newsrss \
  -e POSTGRES_USER=newsrss \
  -e POSTGRES_PASSWORD=newsrss \
  -p 15432:5432 \
  -v newsrss_postgres_data:/var/lib/postgresql/data \
  -d postgres:16-alpine
```

连接信息：

```text
JDBC URL: jdbc:postgresql://localhost:15432/newsrss
Username: newsrss
Password: newsrss
```

后续启动命令：

```bash
docker start newsrss-postgres
```

## 验证命令

```bash
docker exec newsrss-postgres pg_isready -U newsrss -d newsrss
```

执行结果：

- 成功，数据库返回 ready。

```bash
NEWSRSS_DB_URL=jdbc:postgresql://localhost:15432/newsrss \
NEWSRSS_DB_USERNAME=newsrss \
NEWSRSS_DB_PASSWORD=newsrss \
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=db
```

执行结果：

- 成功连接 PostgreSQL 16.14。
- Flyway 成功执行并迁移到 `v1`。
- Hibernate JPA validate 通过。
- 验证完成后后端服务已关闭，`newsrss-postgres` 容器保留运行。

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss -Atc "select version, description, success from flyway_schema_history order by installed_rank;"
```

执行结果：

```text
1|create rss core tables|t
```

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss -Atc "select count(*) from pg_tables where schemaname='public' and tablename like 'rss_%';"
```

执行结果：

```text
7
```

```bash
docker exec newsrss-postgres psql -U newsrss -d newsrss -Atc "select count(*) from pg_description d join pg_class c on c.oid = d.objoid join pg_namespace n on n.oid = c.relnamespace where n.nspname='public' and c.relname like 'rss_%';"
```

执行结果：

```text
96
```

## 遗留问题

- `newsrss-postgres` 会长期保留，用于后续本地验证。
- 如需重置数据库，可以删除容器和 `newsrss_postgres_data` 数据卷后重新创建；执行前需要确认是否保留测试数据。
