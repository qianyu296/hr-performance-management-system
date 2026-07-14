# HR Performance Management System

面向企业 HR 的人力资源与绩效管理系统。系统提供统一登录与权限控制，并围绕组织人事、假勤、审批协同和绩效基础能力组织业务数据与操作入口。

## 功能概览

- **组织人事**：部门层级、员工档案、岗位与职级维护。
- **假勤管理**：请假类型与年度额度、请假申请和审批、假期余额、加班申请、工作日历及月度汇总。
- **审批协同**：审批待办、流程模板和审批记录。
- **系统管理**：用户、角色、菜单权限与数据范围。
- **绩效基础**：绩效周期入口、加权评分与等级计算规则。

前端菜单会根据当前账号的权限动态显示；没有权限的模块不会出现在侧栏中。

## 技术栈

- Backend: Java 17+, Spring Boot 3, Spring Security, MyBatis, Flyway, MySQL
- Frontend: Vue 3, TypeScript, Vite, Pinia, Element Plus
- Local services: MySQL, Redis, MinIO (optional Docker Compose support)

## 项目结构

```text
backend/       Spring Boot API, Flyway migrations, JUnit tests
frontend/      Vue management application
docs/          Requirements, design, API contract and delivery documentation
docker-compose.yml  Local MySQL, Redis and MinIO
```

## 本地启动

### 方式一：Docker 启动依赖服务

```powershell
docker compose up -d
```

启动后端：

```powershell
cd backend
mvn spring-boot:run
```

启动前端：

```powershell
cd frontend
npm ci
npm run dev
```

### 方式二：使用已有 MySQL

当前后端默认连接 `localhost:3306/hrpm`。使用本机 MySQL 时，在启动后端的 PowerShell 中设置连接信息：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/hrpm?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '123456'
cd backend
mvn spring-boot:run
```

Flyway 会自动执行尚未应用的数据库迁移。前端地址为 `http://127.0.0.1:5173`，API 根路径为 `http://127.0.0.1:8080/api/v1`，健康检查地址为 `http://127.0.0.1:8080/api/v1/actuator/health`。

## 本地测试账号

这些账号仅用于本地开发与测试，部署到共享或生产环境前必须修改密码或停用。

| 用户名 | 密码 | 角色与可见范围 |
| --- | --- | --- |
| `admin` | `admin123` | `SUPER_ADMIN`，可访问已配置的全部管理模块。 |
| `test-admin` | `123456` | `DEV_TEST_ADMIN`，可访问组织架构、员工档案、请假管理和加班管理。 |

`test-admin` 当前仅拥有 `org:read`、`org:manage` 与 `attendance:submit` 权限，因此不会显示系统管理、流程、绩效、报表和需要假勤管理权限的页面。

## 构建与测试

后端：

```powershell
cd backend
mvn test
mvn package
```

前端：

```powershell
cd frontend
npm run typecheck
npm run build
```

后端测试包含认证、权限、组织员工、请假与审批、假勤、流程模板、系统访问、迁移和 OpenAPI 合约检查。数据库结构变更必须以新的 Flyway 迁移文件提交，例如 `V013__add_xxx.sql`，不要修改已应用的迁移文件。

## 文档

- API contract: `/api/v1/v3/api-docs.yaml`（由 Springdoc 从运行中的 Controller 自动生成）
- Requirements and design: [docs](docs)
- Contributor guide: [AGENTS.md](AGENTS.md)
