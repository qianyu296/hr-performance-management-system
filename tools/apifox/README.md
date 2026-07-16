# HRPM 接口测试使用说明

本目录提供人力资源管理模块的后端接口回归测试资产，覆盖 69 个非绩效接口映射和 90 个按依赖顺序执行的请求。不会调用 `/performance/**` 或绩效等级分布报表接口。

## 先了解两种文件

`hrpm-non-performance-api-tests.postman_collection.json` 是 Postman Collection 格式。导入 Apifox 后，它会显示为接口集合，适合逐个调试接口或运行集合请求；它不是 Apifox 的“场景用例”。

如果在 Apifox 的“自动化测试 -> 场景用例”中运行，必须使用本目录的 `import-native-scenario.ps1`。脚本会调用 Apifox CLI 创建真正可运行的原生场景用例，因此不会再出现“没有可运行的测试用例”。

## 一次性准备

1. 在项目根目录启动基础服务。

```powershell
docker compose up -d
```

2. 在另一个 PowerShell 窗口启动后端。Docker Compose 的 MySQL root 密码为 `root_dev_password`。

```powershell
cd D:\JavaProject\hr-performance-management-system\backend
$env:DB_PASSWORD = 'root_dev_password'
mvn spring-boot:run
```

访问 `http://127.0.0.1:8080/api/v1/health`，确认返回中的 `status` 为 `UP`。不要关闭该后端窗口。

3. 回到项目根目录，初始化专用测试账号和基础数据。所有账号密码均为 `admin123`。

```powershell
Get-Content -Raw tools\apifox\setup-local-api-test-accounts.sql |
  docker exec -i hrpm-mysql mysql -uroot -proot_dev_password hrpm
```

若使用的是旧的本地 MySQL（`root/123456`），改用：

```powershell
Get-Content -Raw tools\apifox\setup-local-api-test-accounts.sql |
  mysql -uroot -p123456 -h 127.0.0.1 -P 3306 hrpm
```

## 在 Apifox 中创建可运行场景

1. 在 Apifox 项目中导入环境文件 [hrpm-local-dev.postman_environment.json](./hrpm-local-dev.postman_environment.json)。运行场景时选择 `HRPM Local Development` 环境。

2. 安装并登录 Apifox CLI。访问令牌请在你自己的 Apifox 账号设置中创建；不要把令牌写入本仓库或提交到 Git。

```powershell
npm install -g apifox-cli
apifox auth login --with-token <你的访问令牌>
apifox project list
```

最后一条命令会列出项目，记下目标项目的 ID。

3. 在项目根目录运行一键导入脚本，将 `<项目ID>` 换成上一步的数字。

```powershell
.\tools\apifox\import-native-scenario.ps1 -ProjectId <项目ID>
```

脚本会执行以下工作：生成场景 JSON、按 Apifox CLI Schema 校验、新建场景、写入 90 个接口请求，再读取场景确认共有 91 个步骤。脚本只创建场景，不会自动执行接口测试。

4. 回到 Apifox，进入“自动化测试 -> 场景用例”，找到 `HRPM 非绩效接口回归测试`，选择 `HRPM Local Development` 环境后点击运行。

## 仅调试单个接口时

如需查看请求细节或单独调试，不必运行 CLI 脚本。直接在 Apifox 导入：

1. `hrpm-local-dev.postman_environment.json`
2. `hrpm-non-performance-api-tests.postman_collection.json`

导入后的接口集合可用于普通接口调试，但不能作为“场景用例”运行器的可运行测试用例。

## 测试账号

| 账号 | 用途 |
| --- | --- |
| `api-admin` | 系统管理、报表、管理员操作 |
| `api-hr` | 组织架构、人事异动、交接 |
| `api-manager` | 审批任务和部门树数据范围 |
| `api-employee` | 请假、加班、本人余额 |
| `api-attendance-admin` | 假勤配置、余额、日历、汇总 |
| `api-workflow-admin` | 工作流模板、任务转交 |
| `api-readonly` | 验证无权限时的 403 响应 |

## 注意事项

- 场景会创建带 `APIFOX_` 前缀的测试数据。系统部分资源没有删除接口，因此这些数据会保留用于排查。
- 每次执行前重新运行初始化 SQL，可重置专用账号、权限和基础测试数据。
- 运行完整场景会执行创建、更新、提交、审批等写操作，只应连接本地开发数据库。
