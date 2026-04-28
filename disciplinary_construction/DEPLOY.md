# 后端部署说明

## 运行环境

- Java 11
- MongoDB 6+
- Redis 6+
- Maven 3.9+

本机已经安装 Java 11，并在项目根目录 `.tools/` 下准备了 Maven 3.9.9。

## 构建

在 `disciplinary_construction` 目录执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot'
$repo='C:\Users\HighLight\Desktop\MIS-main\.m2\repository'
$env:Path="$env:JAVA_HOME\bin;C:\Users\HighLight\Desktop\MIS-main\.tools\apache-maven-3.9.9\bin;$env:Path"
..\.tools\apache-maven-3.9.9\bin\mvn.cmd "-Dmaven.repo.local=$repo" -DskipTests package
```

构建产物：

```text
target/disciplinary_construction-1.0-SNAPSHOT.jar
```

## 直接运行

确保 MongoDB 和 Redis 已启动后：

```powershell
java -jar target\disciplinary_construction-1.0-SNAPSHOT.jar
```

默认地址：

- 服务：`http://localhost:1010`
- API 前缀：`/SCSE@hbut/msi`
- 文档：`http://localhost:1010/SCSE@hbut/doc.html`
- 健康检查：`http://localhost:1010/SCSE@hbut/actuator/health`

## Docker 运行

在 `disciplinary_construction` 目录执行：

```powershell
docker compose up -d --build
```

会启动：

- `mongo`
- `redis`
- `mis-backend`

## 关键环境变量

- `SERVER_PORT`
- `SERVER_CONTEXT_PATH`
- `MONGODB_URI`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `APP_UPLOAD_DIR`
- `APP_CORS_ALLOWED_ORIGINS`
- `KNIFE4J_ENABLE`

生产环境一定要替换 `JWT_SECRET`，并把 `APP_CORS_ALLOWED_ORIGINS` 限制为真实前端域名。

## 新增接口

- 管理员导入成绩：`POST /SCSE@hbut/msi/grade/import`
- 管理员/学生查询成绩：`GET /SCSE@hbut/msi/grade/records`
- 学生学习记录：`GET /SCSE@hbut/msi/student/study-records`
