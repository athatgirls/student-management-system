# 智途研究生管理系统

面向学院研究生管理场景的前后端分离应用，提供学生端和管理员端，覆盖学生档案、任务活动、成果申报、党建、实习就业及审核统计等业务。

前端采用 Vue 3、Element Plus 和 ECharts，后端采用 Spring Boot、MongoDB 和 Redis，支持 Docker Compose 部署。

> 本仓库同时包含界面预览、开发联调和生产部署配置。预览数据不代表真实业务数据，开发环境不能直接用于正式上线。HTTPS 配置模板和融合门户入口也不等于已经接通学校统一认证。

## 功能概览

| 模块 | 主要功能 |
| --- | --- |
| 学生档案 | 学生信息管理、Excel 名单导入、年级维护、个人资料更新 |
| 任务与活动 | 普通任务、报名任务、活动管理，按年级和政治面貌设置接收范围 |
| 日常管理 | 请假、荣誉、学习记录、成绩及相关审核 |
| 科研与成果 | 竞赛、论文、专利、项目的填报与审核 |
| 党建管理 | 入党申请、思想汇报、党课、志愿服务及阶段管理 |
| 实习就业 | 信息填报、审批与统计分析 |
| 管理看板 | 数据统计、批量操作与审核管理 |
| 账号与权限 | 学生/管理员角色隔离、首次登录改密、账号限流、附件访问控制 |

年级支持手动填写，不限定为固定年份。学生档案和活动发布共用以下政治面貌选项：中共党员、中共预备党员、发展对象、入党积极分子、共青团员、群众。

## 技术组成

| 层级 | 当前实现 |
| --- | --- |
| 前端 | Vue 3、Vue Router、Vuex、Element Plus、ECharts、Axios |
| 后端 | Java 11、Spring Boot 2.7.18、Spring Security、JWT、BCrypt |
| 数据存储 | MongoDB 7、Redis 7.2、持久化附件目录 |
| 部署 | Docker Compose、Nginx，另提供 Caddy 自动 HTTPS 模板 |
| 持续集成 | GitHub Actions：后端测试与打包、前端 lint/build、生产依赖审计 |

以上是仓库当前使用的技术版本，并不代表均处于最新支持周期；框架升级和剩余风险见 [SECURITY.md](SECURITY.md)。

## 快速开始

### 方式一：仅预览界面

不需要数据库或 Docker，使用本地模拟数据，适合了解页面和业务流程。

```bash
cd index
npm ci
npm run serve
```

打开 <http://localhost:3000/preview>，选择学生端或管理员端。模拟交互不能用于验证真实登录、权限或数据库写入。预览状态会保存在当前站点的浏览器存储中，建议用单独的浏览器配置或无痕窗口与真实业务环境隔离。

### 方式二：本地完整联调

先安装并启动 Docker，确认 `docker compose version` 可用，然后在项目根目录运行：

```bash
docker compose -f docker-compose.dev.yml up --build
```

首次运行需要联网下载镜像和 Maven/npm 依赖。开发环境入口：

| 服务 | 地址 |
| --- | --- |
| 前端 | <http://localhost:3000> |
| 后端健康检查 | <http://localhost:1010/SCSE@hbut/actuator/health> |
| 开发接口文档 | <http://localhost:1010/SCSE@hbut/doc.html> |

开发编排会启用模拟账号，默认值见 [docker-compose.dev.yml](docker-compose.dev.yml)。它还会映射数据库端口，仅可用于隔离的本地开发环境；不要直接部署到校内共享服务器或公网。

停止开发环境并保留数据：

```bash
docker compose -f docker-compose.dev.yml down
```

**不要在正式环境执行 `down -v`，该参数会删除对应的数据库和附件等持久化卷。**

## 名单导入与账号使用

1. 管理员在学生管理页面导入 `.xlsx` 名单，或单独创建学生账号。
2. 名单应包含“学号”和“姓名”表头；系统在前五行内识别表头，也支持“学生学号”等别名。年级可在导入时统一指定，或由表格的“年级”列提供；统一指定的年级优先。
3. “专业”“班级”为可选列。建议将学号列设置为文本，避免前导零丢失或科学计数法改变学号。
4. 导入后查看成功、重复、无效行统计及错误示例。重复学号会跳过，不会通过重复导入覆盖已有学生档案。
5. 学生使用学号和管理员私下告知的初始密码登录，先完成强制改密，再进入业务系统。

生产管理员由 `APP_BOOTSTRAP_ADMIN_USERNAME` 和 `APP_BOOTSTRAP_ADMIN_PASSWORD` 初始化，**没有可以通用于正式环境的默认管理员密码**。修改初始化变量不等于重置已存在账号的密码。

现有业务保留固定格式学生初始密码，具体规则和风险见 [SECURITY.md](SECURITY.md)。强制改密和限流不能消除可推算初始密码被抢先使用的风险。已修改的个人密码不会因正常部署更新而自动重置。

学生个人资料采用字段白名单更新；年级、政治面貌等管理字段不允许通过学生自助接口修改，应由管理员维护。

## 生产部署

### 新环境配置

以下命令用于 **Linux 上的新部署**。现有服务器升级应使用其实际项目名、环境文件和全部 Compose 覆盖文件，不要直接照搬初始化命令。

```bash
cp .env.example .env
chmod 600 .env
```

在启动前编辑 `.env`，不要保留空的必填项：

| 配置 | 用途 |
| --- | --- |
| `MONGO_ROOT_PASSWORD`、`REDIS_PASSWORD` | 独立的数据库随机密码 |
| `JWT_SECRET` | 足够长度的随机签名密钥，不使用示例值 |
| `APP_BOOTSTRAP_ADMIN_USERNAME`、`APP_BOOTSTRAP_ADMIN_PASSWORD` | 首次初始化管理员 |
| `APP_DEMO_DATA_ENABLED=false` | 正式环境不创建演示数据 |
| `APP_CORS_ALLOWED_ORIGINS` | 实际前端来源，包含协议和必要的端口 |
| `BIND_ADDRESS`、`APP_PORT` | 基础入口默认绑定 `127.0.0.1:8080` |
| `APP_DOMAIN` | 使用 Caddy HTTPS 编排时的正式域名 |
| `APP_FILES_COOKIE_SECURE` | HTTPS 入口设为 `true`；仅 HTTP 的开发环境设为 `false` |

基础服务启动与检查：

```bash
docker compose -f docker-compose.yml config --quiet
docker compose -f docker-compose.yml up -d --build
docker compose -f docker-compose.yml ps
curl -f http://127.0.0.1:8080/health
```

默认回环地址仅允许服务器本机访问，可作为 HTTPS 网关的上游。学校网关位于另一台机器时，需要按学校网络方案调整绑定地址，并限制允许访问上游端口的来源。不要暴露 MongoDB、Redis 和后端内部端口。

### 域名与 HTTPS

学校服务器、域名解析、HTTPS 证书是独立配置项。正式入口应使用与证书匹配的域名，不能用域名证书消除 IP 地址访问时的证书不匹配提示。

- **学校统一网关接入**：由学校配置域名及 HTTPS，转发到应用入口；同时更新 CORS 来源并启用安全附件 Cookie。
- **服务器自动申请证书**：在域名、网络及证书签发验证条件满足时，可使用仓库内的 Caddy 模板：

  ```bash
  docker compose -f docker-compose.yml -f docker-compose.https.yml config --quiet
  docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --build
  ```

- **使用学校提供的证书**：需额外挂载证书链和私钥并配置 TLS。现有 Caddy 模板采用自动签发，不会自动读取证书压缩包；内网或离线环境不能直接照搬自动签发流程。

不要将真实服务器凭据、学生名单、`.env`、证书私钥或数据库备份提交到仓库。HTTPS 配置、证书续期及校内/校外访问范围需与学校信息中心共同确认。

### 离线部署、升级与备份

学校服务器无法访问外网时，在可联网的构建环境完成测试与镜像构建，再通过学校授权的传输渠道上传离线镜像、部署配置和校验文件，使用 `docker load` 导入。只上传源码或 Dockerfile 不会使服务器自动具备离线构建所需的依赖。

每次升级应先备份数据库和附件，记录旧镜像版本及实际 Compose 配置，再切换前后端。保留项目名和持久化卷，检查容器健康、登录、首次改密、个人资料保存、名单导入和附件权限；失败时使用已验证的回滚方案。

仓库提供 [scripts/backup.sh](scripts/backup.sh) 作为基础部署备份脚本。自定义项目名、路径或覆盖文件的部署应先适配脚本；脚本本身不会停止业务写入，需根据一致性要求安排停写窗口。备份应限制访问权限、离机保存，并定期验证恢复。

详细说明见 [DEPLOY.md](DEPLOY.md) 和 [DOCKER.md](DOCKER.md)。其中公网自动 HTTPS 的示例不等同于学校离线部署步骤。

## 学校统一认证与准入

当前账号登录不等于学校统一认证，融合门户显示系统链接也不等于已经完成单点登录。

若要接入统一认证，需要学校提供协议、身份字段和接入资料，再实现服务端验证与账号绑定。仅允许本学院研究生使用时，应以学校可信身份标识或学号关联已核验名单，并在服务端检查学院、培养层次及在读状态，不能只靠门户入口可见性限制访问。

## 安全与使用边界

- JWT 校验账号及凭据状态；改密、重置等操作会使相关旧令牌失效。
- 登录和首次改密有账号限流，频繁请求可能返回 `429`，不应通过关闭限流解决。
- 新附件仅归属学生和管理员可读；缺少归属记录的历史附件默认仅管理员可读，需要核验后补录归属。
- 后端仍有框架升级、审计、恶意文件检测等维护事项；功能回归测试不等于完整安全认证。
- 涉及真实学生数据时，应完成学校要求的权限、数据安全和上线审核。

完整说明及已知风险见 [SECURITY.md](SECURITY.md)。报告问题时提供脱敏后的复现步骤、请求路径和状态码，不要在公开 Issue/PR 中粘贴密码、令牌、私钥或学生个人信息。

## 项目结构

```text
.
├── index/                       Vue 前端、接口封装和 Nginx 配置
├── disciplinary_construction/  Spring Boot 后端及测试
├── deploy/                      Caddy 配置模板
├── scripts/                     基础运维脚本
├── .github/workflows/           持续集成配置
├── docker-compose.dev.yml       本地开发联调
├── docker-compose.yml           生产基础编排
├── docker-compose.https.yml     Caddy 自动 HTTPS 覆盖配置
├── .env.example                 生产变量模板，不含真实密钥
├── DEPLOY.md                    通用服务器部署手册
├── DOCKER.md                    容器操作与排障
└── SECURITY.md                  安全边界、风险与上线检查
```

## 开发验证与贡献

在项目根目录分别执行以下命令。前端需要 Node.js/npm，后端需要 Java 11 和 Maven；当前 CI 前端使用 Node.js 20，容器构建文件使用 Node.js 18，版本升级需另行验证。

前端：

```bash
cd index
npm ci
npm run lint
npm run build
npm audit --omit=dev
```

后端（从项目根目录进入）：

```bash
cd disciplinary_construction
mvn -B test
mvn -B package
```

提交 PR 时请说明修改目的、影响范围和测试结果。前后端接口变更应同步提交；不要把真实数据或个人开发环境文件加入版本控制。GitHub 上合并代码不会自动更新学校服务器，部署需要单独执行并验收。
