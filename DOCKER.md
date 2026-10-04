# Docker 使用与排障

## 两套编排

本地完整联调：

```powershell
docker compose -f docker-compose.dev.yml up --build
```

生产基础服务（仅本机 `127.0.0.1:8080` 可访问）：

```bash
cp .env.example .env
docker compose up -d --build
```

生产域名与自动 HTTPS：

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --build
```

## 服务和持久化

| 服务 | 作用 | 公网端口 |
|---|---|---|
| `frontend` | Nginx 静态前端、API 反向代理、登录限流 | 无（基础编排仅回环 8080） |
| `backend` | Spring Boot API | 无 |
| `mongo` | 主数据库 | 无 |
| `redis` | 在线状态和缓存 | 无 |
| `rustfs` | 私有附件对象存储 | 无 |
| `caddy` | HTTPS 证书和公网入口 | 80、443 |

持久化卷包括 `mongo_data`、`redis_data`、`uploads_data`、`rustfs_data`、`caddy_data`。普通 `down` 不会删除卷。生产环境必须在 `.env` 设置 `RUSTFS_ACCESS_KEY` 和 `RUSTFS_SECRET_KEY`；开发编排的默认凭据仅限本机使用，RustFS API 和控制台均只绑定回环地址。

## 常用命令

```bash
docker compose ps
docker compose logs -f --tail=200 backend
docker compose logs -f --tail=200 frontend
docker compose restart backend
docker compose build --pull
docker compose up -d
docker compose config --quiet
```

开发环境命令需要加 `-f docker-compose.dev.yml`；HTTPS 环境需要同时加两个 `-f` 参数。

## 常见问题

### 页面能开但接口 502

先看 `docker compose ps` 中 backend 是否 healthy，再看后端日志。常见原因是 MongoDB/Redis 密码不一致、JWT 少于 32 字节，或数据库首次初始化尚未完成。

### 域名没有 HTTPS

检查域名是否解析到本机、80/443 是否放行，以及 Caddy 日志。若同机已有 Nginx/Apache 占用 80/443，需先调整端口冲突。

### 修改 `.env` 没生效

环境变量是在容器创建时注入，执行：

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --force-recreate
```

### 想清空本地测试数据

以下命令会永久删除该编排的数据库、缓存、兼容 uploads 卷和 RustFS 对象卷，只能用于确认可丢弃的测试环境：

```powershell
docker compose -f docker-compose.dev.yml down -v
```

生产环境不要执行 `down -v`。

### 附件备份与读回

生产附件存放在 `rustfs_data` 卷。执行 `./scripts/backup.sh` 会同时导出 Mongo、兼容的 `/app/uploads` 和 RustFS 数据；RustFS 卷名由 `COMPOSE_PROJECT_NAME`（默认 `mis`）确定。恢复 RustFS 归档前先停写，然后将归档解压到同名卷，重启 `rustfs` 与 `backend`，并以有权限的账号下载一个已上传附件确认可读。本项目尚未上线，不包含旧 `/app/uploads` 文件的迁移或双读回退。
