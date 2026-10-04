# 服务器上线手册

本文以一台有公网 IP 的 Linux 服务器和一个已解析域名为例。建议至少 2 核 CPU、4 GB 内存、30 GB 磁盘；正式保存真实学生信息前，应再按学校的数据合规要求做权限、审计和备份评审。

## 1. 准备服务器和域名

1. 安装 Git、Docker Engine 和 Docker Compose 插件；Docker 请按官方文档安装，确认 `docker version` 和 `docker compose version` 可用。
2. 将域名 A/AAAA 记录指向服务器公网 IP。
3. 云安全组和系统防火墙只放行 SSH、TCP 80、TCP/UDP 443。
4. MongoDB、Redis、后端 1010 端口不要暴露到公网。

## 2. 上传代码

```bash
git clone https://github.com/athatgirls/student-management-system.git
cd student-management-system
cp .env.example .env
```

如果本地代码尚未推送，就先将整个项目目录上传到服务器，再进入项目根目录。

## 3. 配置生产变量

用下面命令生成十六进制随机值，避免 URI 中的特殊字符转义问题：

```bash
openssl rand -hex 32
openssl rand -hex 32
openssl rand -hex 48
openssl rand -hex 24
```

编辑 `.env`，至少填写：

```dotenv
APP_PORT=8080
BIND_ADDRESS=127.0.0.1
APP_DOMAIN=mis.example.com

MONGO_ROOT_USERNAME=mis_admin
MONGO_ROOT_PASSWORD=第一条随机值
REDIS_PASSWORD=第二条随机值
JWT_SECRET=第三条随机值
RUSTFS_ACCESS_KEY=第四条随机值
RUSTFS_SECRET_KEY=第五条随机值

APP_CORS_ALLOWED_ORIGINS=https://mis.example.com
APP_BOOTSTRAP_ENABLED=true
APP_BOOTSTRAP_ADMIN_USERNAME=admin
APP_BOOTSTRAP_ADMIN_PASSWORD=第六条随机值

APP_DEMO_DATA_ENABLED=false
APP_DEMO_STUDENT_PASSWORD=不要使用默认值
```

不要把 `.env` 提交到 Git，也不要通过聊天或截图发送其中的值。

## 4. 检查并启动

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.https.yml ps
```

检查服务：

```bash
curl -f http://127.0.0.1:8080/health
curl -f https://mis.example.com/health
docker compose -f docker-compose.yml -f docker-compose.https.yml logs --tail=100 backend frontend caddy
```

Caddy 会在域名解析正确且 80/443 可访问时自动申请和续期 HTTPS 证书。首次登录后立即修改管理员密码；确认管理员账号已持久化后，可将 `APP_BOOTSTRAP_ENABLED=false` 再重启后端。

## 5. 更新版本

更新前先备份，然后重新构建：

```bash
chmod +x scripts/backup.sh
./scripts/backup.sh
git pull --ff-only
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.https.yml ps
```

上线变更应先在测试服务器验证。保留上一个 Git 标签和备份文件，以便出现问题时回退代码和数据。

## 6. 备份与恢复

执行：

```bash
./scripts/backup.sh
```

会在 `backups/` 生成 MongoDB 压缩归档、兼容的本地上传目录压缩包，以及 RustFS 对象卷压缩包。脚本通过 `COMPOSE_PROJECT_NAME`（默认 `mis`）确定 RustFS 卷名；使用自定义项目名时先在 `.env` 设置同名变量。建议再把备份同步到另一台机器或对象存储，并配置定时任务：

```cron
30 2 * * * cd /opt/student-management-system && ./scripts/backup.sh >> /var/log/mis-backup.log 2>&1
```

恢复会覆盖数据，必须先停写并再次备份。示例：

```bash
set -a
. ./.env
set +a

docker compose exec -T mongo mongorestore \
  --username "$MONGO_ROOT_USERNAME" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --archive --gzip --drop < backups/mongo_时间.archive.gz

docker compose exec -T backend tar -xzf - -C /app < backups/uploads_时间.tar.gz

docker compose stop backend rustfs
docker run --rm -v "${COMPOSE_PROJECT_NAME:-mis}_rustfs_data":/data -v "$PWD/backups":/backup alpine:3.20 \
  tar -xzf /backup/rustfs_时间.tar.gz -C /data
docker compose start rustfs backend
```

恢复后检查登录、学生列表、文件访问和统计数据；再重启 `rustfs` 与 `backend`，以管理员或附件所有者身份下载一个已存在附件，确认对象数据在重启后仍可读。本仓库尚未上线，本次对象存储接入不包含旧 `/app/uploads` 历史附件迁移或双读回退。

## 7. 上线验收清单

- `https://域名/health` 返回 `ok`；
- 浏览器证书有效，HTTP 自动跳转 HTTPS；
- 管理员和学生能登录，学生访问管理员接口返回 403；
- 新建、修改、查询、上传和 `.xlsx` 导入可用；
- 重启容器后数据仍存在；
- 上传附件后重启 `rustfs` 与 `backend`，原附件仍可按所有者/管理员权限读回；
- 备份文件可以在测试环境恢复；
- `.env` 权限已限制，例如 `chmod 600 .env`；
- 演示数据关闭，默认密码全部更换。
