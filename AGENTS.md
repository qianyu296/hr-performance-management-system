# 仓库指南

## 项目结构与模块

本仓库由两个应用组成：

- `backend/`：Java 17、Spring Boot 3.4 API，使用 Spring Security、MyBatis 与 Flyway。生产代码位于 `src/main/java/com/hrpm`，配置和迁移脚本位于 `src/main/resources`，测试位于 `src/test/java`。
- `frontend/`：Vue 3、TypeScript 与 Vite。页面在 `src/views`，路由在 `src/router`，接口客户端在 `src/api`，全局样式在 `src/styles.css`。
- `docs/`：需求、架构、数据库、接口与测试文档；接口契约由 Springdoc 自动生成；启动后通过 `/api/v1/v3/api-docs.yaml` 查看。
- 根目录 `docker-compose.yml`：本地 MySQL、Redis 和 MinIO 基础服务。

## 构建、测试与本地运行

在仓库根目录启动基础服务：

```powershell
docker compose up -d
```

在 `backend/` 执行：

```powershell
mvn spring-boot:run  # 启动 API（8080）
mvn test             # 运行 JUnit 5 与集成测试
mvn clean package    # 测试并构建可执行 JAR
```

在 `frontend/` 执行：

```powershell
npm ci               # 按锁定版本安装依赖
npm run dev          # 启动 Vite（5173）
npm run typecheck    # 校验 Vue/TypeScript 类型
npm run build        # 类型校验并生成生产包
```

## 编码风格与命名

XML、YAML、JSON、Vue 和 TypeScript 使用 2 空格缩进；Java 使用 4 空格。Java 包名使用小写 `com.hrpm`，类名使用 `PascalCase`，成员使用 `camelCase`。Vue 组件和页面使用 `PascalCase` 文件名，例如 `DashboardView.vue`；前端导入优先使用 `@/` 别名。项目未配置格式化或 lint 工具，应遵循相邻代码并保持修改聚焦。

## 测试与数据库

后端采用 JUnit 5、Spring Boot Test 与 Testcontainers；测试类命名为 `*Tests.java`，并镜像生产包路径。为控制器、安全规则、服务和数据库行为补充针对性测试。前端暂无测试框架，前端改动至少执行 `npm run typecheck` 和 `npm run build`。

数据库变更必须新增 Flyway 迁移，例如 `V017__add_example.sql`；不得修改已应用的迁移文件。

## 提交与拉取请求

提交采用 Conventional Commits，例如 `feat: manage leave types`、`fix: correct balance calculation`、`docs: update api`。拉取请求应说明范围、关联 issue、列出验证命令，并注明迁移或 API 变化；可见 UI 变更附截图。
