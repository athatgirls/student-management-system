# 学生信息管理系统（MIS）

一个可演示、可容器化部署的前后端分离学生管理系统。前端使用 Vue 3 + Element Plus，后端使用 Spring Boot + MongoDB + Redis，包含学生端、管理员端、JWT 登录、批量导入、审核与统计看板。

## 明天演示：最快方案

只看界面和业务流程时，不需要数据库或 Docker。预览模式自带模拟数据，不会向后端写入数据。

```powershell
cd index
npm ci
npm run serve
```

浏览器打开 `http://localhost:3000/preview`，再选择“进入学生端”或“进入管理员端”。建议按下面顺序讲解：

1. 管理员首页统计看板；
2. 学生信息增删改查与 Excel 导入；
3. 日常任务、请假审核；
4. 竞赛、论文、专利、项目审核；
5. 党建、实习就业和成绩导入；
6. 切换到学生端展示个人主页、填报和查询。

推荐 Node.js 18 或 20。停止服务按 `Ctrl+C`。

## 完整联调：Docker 开发环境

确保 Docker Desktop 已启动，在项目根目录执行：

```powershell
docker compose -f docker-compose.dev.yml up --build
```

首次启动会下载镜像和 Maven/npm 依赖，可能需要几分钟。启动后访问：

- 前端：`http://localhost:3000`
- 后端健康检查：`http://localhost:1010/SCSE@hbut/actuator/health`
- 接口文档：`http://localhost:1010/SCSE@hbut/doc.html`

开发演示账号：

| 角色 | 账号 | 密码 | 用途 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 管理员完整流程 |
| 学生 | `10240001` | `Student123!` | 正常学生登录 |
| 学生 | `102411111` | `Hbut_411111` | 首次登录强制改密流程 |

停止容器但保留数据：

```powershell
docker compose -f docker-compose.dev.yml down
```

不要随意加 `-v`；它会删除 MongoDB、Redis 和上传文件卷。

## 功能范围

- 学生档案、成绩、学习记录和个人资料；
- 日常任务、活动、请假、荣誉及审核；
- 竞赛、论文、专利、项目与创新成果；
- 实习就业填报、审批和统计分析；
- 党建申请、思想汇报、党课、志愿服务和党员管理；
- 管理员看板、批量操作、Excel `.xlsx` 导入；
- JWT 双角色认证、BCrypt 密码、角色接口隔离；
- Docker Compose、健康检查、HTTPS 入口和数据备份。

## 项目结构

```text
.
├── index/                         Vue 3 前端与 Nginx 配置
├── disciplinary_construction/    Spring Boot 后端
├── deploy/                        Caddy HTTPS 配置
├── scripts/                       运维脚本
├── docker-compose.dev.yml         本地完整联调
├── docker-compose.yml             生产基础编排
├── docker-compose.https.yml       域名 + 自动 HTTPS 编排
├── .env.example                   生产环境变量模板
├── DEPLOY.md                      服务器上线手册
├── DOCKER.md                      容器常用操作
└── SECURITY.md                    安全边界与上线检查
```

## 本地构建验证

前端：

```powershell
cd index
npm ci
npm run lint
npm run build
npm audit --omit=dev
```

后端（仓库内已准备本机 Maven 工具时）：

```powershell
cd disciplinary_construction
..\.tools\apache-maven-3.9.9\bin\mvn.cmd test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd package
```

也可以只用 Docker 构建，无需本机安装 Maven。

## 上线

公网部署不要使用开发编排和演示密码。完整步骤见 [DEPLOY.md](DEPLOY.md)，容器排障见 [DOCKER.md](DOCKER.md)，安全检查见 [SECURITY.md](SECURITY.md)。

生产配置要求至少填写 MongoDB、Redis、JWT、管理员密码、域名和 CORS 来源；应用端口默认只绑定 `127.0.0.1:8080`，公网由 Caddy 暴露 80/443 并自动申请 HTTPS 证书。
