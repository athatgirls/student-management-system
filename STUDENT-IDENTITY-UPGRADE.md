# 学生关联字段升级说明（PR #14）

此次升级把业务关联键统一为 `studentAccountId`，学号保留在 `studentId` 用于展示与导入匹配。不能只替换应用镜像而跳过数据预检和迁移。

## 数据范围与安全边界

- 脚本 `scripts/student-identity-migration.js` 覆盖 20 个学生业务集合及活动参与名单。实际集合名含 `competition`、`paper`、`patent`、`project`，不是 API 的复数路径。
- 不修改 `students`、`admins`、`uploaded_files`，不重置账号、密码，不删除业务记录或附件。
- 只允许账号主键、学号之间的精确匹配。不按姓名匹配；不存在、歧义、归属冲突或重复就业意向都会阻断迁移，须人工核对，禁止自动删除或合并。
- 每条变更的原值、原来不存在的字段及新值保存在私有 `identity_migration_<release>` 集合。日志仅输出集合数量和变更数量，不输出姓名、学号、密码或附件。
- 就业意向显式建立 `employment_student_account_unique` 部分唯一索引；应用启动时也会校验/创建，不依赖默认关闭的自动建索引设置。

## 发布流程

1. 本地运行前后端测试、构建及真实 MongoDB 集成测试，提交所有修改。
2. 使用 `scripts/prepare-report-release.ps1 -PreviousRelease <实际线上旧镜像标签>` 准备离线包，上传后验证 SHA256SUMS。
3. 部署脚本先核对旧镜像、Compose 配置及新镜像版本，并执行只读 `dry-run`；失败时旧服务保持运行。
4. 停止前后端以暂停业务写入，备份 MongoDB、配置、本地上传目录及 RustFS 数据卷。备份成功后才执行 `apply`。
5. 转换前后核对学生数与所有业务集合数量，执行 `verify`；启动并检查新后端及前端镜像，最后恢复前端访问。
6. 保留数据库备份、迁移日志、字段原值日志和旧镜像。常规验证不创建或修改真实学生业务数据。

## 回滚边界

上线脚本失败时先停止新前后端，再用同一 release 的字段日志 `rollback`，随后恢复旧镜像。重复回滚为无操作。

回滚前会核对学生/业务记录数量及所有待恢复字段，若检测到升级后新增记录或字段发生冲突，将保持应用停止并报错，不能强行覆盖新数据。此时应由管理员结合备份和日志人工恢复；不要直接执行 `mongorestore --drop` 或把旧应用重新开放给用户。

迁移入口须在学校服务器本地通过 Mongo 容器执行：模式由 `MIS_IDENTITY_MODE` 指定为 `dry-run`、`apply`、`verify` 或 `rollback`，`MIS_IDENTITY_RUN` 为同一发布标识，`MIS_IDENTITY_SCRIPT` 为容器内脚本绝对路径。凭据仅使用服务器已有环境变量，不写入代码、离线包或聊天记录。具体命令由部署脚本维护，避免手工拼接泄露密码。

## 测试

- `docker compose -f docker-compose.test.yml run --rm backend-test mvn -B package`
- `docker compose -f docker-compose.test.yml run --rm frontend-check`
- 在全新、无宿主机端口映射的专用 Mongo 容器中运行 `scripts/tests/student-identity-mongo.js`，验证 dry-run 零写入、20 种旧数据结构、BSON 主键、唯一索引、精确回滚及冲突保护；CI 的 `identity-migration` job 自动执行。
