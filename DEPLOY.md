# MIS 部署说明

当前项目已经整理成可构建的前后端结构：

- 前端：`index/`
- 后端：`disciplinary_construction/`
- 一键编排：根目录 `docker-compose.yml`

## 本机已验证

- Node/npm 已安装，前端 `npm run build` 通过。
- Java 11 已安装，Maven 使用项目内 `.tools/apache-maven-3.9.9`。
- 后端 `mvn -DskipTests package` 通过，jar 已生成。
- Git 已安装在 `C:\Program Files\Git\cmd\git.exe`。
- Docker Desktop 安装器已下载并执行，但当前机器安装器返回 Windows 错误码 `4294967291`，需要手动确认 UAC/重启/系统虚拟化组件后再试。

## 推荐部署方式：Docker Compose

安装并启动 Docker Desktop 后，在仓库根目录执行：

```powershell
docker compose up -d --build
```

启动后访问：

- 前端：`http://localhost:8080`
- 后端代理：`http://localhost:8080/SCSE@hbut/msi`
- 后端直连容器内端口：`1010`

会一起启动：

- `frontend`
- `backend`
- `mongo`
- `redis`

## 本地分开运行

前端：

```powershell
cd index
npm install
npm run serve
```

后端：

```powershell
cd disciplinary_construction
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;..\.tools\apache-maven-3.9.9\bin;$env:Path"
..\.tools\apache-maven-3.9.9\bin\mvn.cmd -Dmaven.repo.local=..\.m2\repository -DskipTests package
java -jar target\disciplinary_construction-1.0-SNAPSHOT.jar
```

## 生产环境必须改的配置

- `JWT_SECRET`：必须换成随机长密钥，不要用默认开发占位值。
- `APP_CORS_ALLOWED_ORIGINS`：改成真实前端域名。
- `MONGODB_URI`、`REDIS_HOST`、`REDIS_PASSWORD`：改成生产数据库配置。
- `APP_UPLOAD_DIR`：确认有持久化卷或宿主机目录。

## 已补齐的能力

- 前端生产接口不再写死 `localhost`。
- 后端 MongoDB、Redis、JWT、上传目录、CORS 都支持环境变量。
- 后端新增成绩导入与成绩查询接口。
- 前后端都提供 Dockerfile，根目录提供整套 compose。
- 上传接口修了无后缀文件崩溃和路径穿越风险。
- 安全配置恢复了登录白名单、静态上传文件、健康检查和默认鉴权规则。
