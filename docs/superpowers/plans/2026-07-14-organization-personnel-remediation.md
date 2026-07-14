# 独立人力资源管理系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前项目收敛为独立的人力资源管理系统：先在 UI 下架目标和绩效功能，再将员工主数据补齐为可维护、可授权、可追溯的人事组织基础能力，并交付入职、转正、调动、晋升、降职、停职与离职的人事异动闭环。

**Architecture:** 保留现有 Spring Boot + MyBatis + Vue 3 分层模式和通用工作流引擎。组织主数据使用受数据范围约束的资源服务；人事异动以独立业务单据、不可变任职履历和 `PERSONNEL_CHANGE` 工作流处理器实现。人员当前任职信息只在异动最终通过并到达生效日期时由应用服务更新。绩效模块在本计划期间仅保留源码、接口和数据，不向前端注册路由、不在工作台加载、不在报表或流程模板展示。

**Tech Stack:** Java 17、Spring Boot 3.4、Spring Security、MyBatis、Flyway、MySQL 8、JUnit 5、Vue 3、TypeScript、Vite、Element Plus。

---

## 0. 产品定位与冻结策略

### 0.1 当前产品边界

本项目在当前阶段的产品名称为“人力资源管理系统”。面向 HR、部门主管和普通员工，交付以下可日常使用的能力：

| 业务域 | 当前产品保留的能力 | 本计划目标 |
| --- | --- | --- |
| 组织人事 | 部门、岗位、职级、员工档案、人事异动、员工履历、账号生命周期 | 形成可维护、可审批、可追溯的闭环 |
| 假勤 | 请假、加班、工作日历、假期余额、月度汇总 | 保持已实现能力，仅修复与人员状态或离职处理相关的联动 |
| 审批协同 | 流程模板、审批待办、审批动作和历史 | 作为人事、请假、加班业务的通用基础设施 |
| 系统与权限 | 用户、角色、菜单、数据范围、操作审计 | 保障 HR 管理与组织范围隔离 |
| 数据分析 | 人员规模、部门人员结构和假勤统计 | 只展示人力资源口径数据 |

### 0.2 绩效与目标的冻结边界

绩效并非删除，而是“前端下架、后端保留”。该策略避免删除已有数据库迁移、接口和历史数据，同时让当前交付范围可控。

| 层次 | 处理方式 | 不允许的动作 |
| --- | --- | --- |
| 前端导航和路由 | 移除 `/goals/cycles`、`/performance/cycles`、`/performance/tasks` 导航项和路由映射；直接输入 URL 命中通配路由并返回工作台 | 不保留隐藏菜单或仅用 CSS 隐藏 |
| 工作台 | 不请求绩效任务接口，不显示自评、主管评分或绩效数字 | 不在待办列表中保留绩效跳转链接 |
| 报表 | 仅调用人员规模和假勤统计接口；移除绩效等级分布卡片、表格和接口调用 | 不向 UI 返回或展示绩效聚合数据 |
| 流程模板 | 隐藏已有 `PERFORMANCE_APPEAL` 模板，并从新建模板的业务类型中排除 | 不删除数据库中已有绩效申诉模板 |
| 登录与品牌文案 | 使用“人力资源管理系统”；页面描述只列组织、人事、考勤和审批 | 不再对用户宣称系统包含绩效模块 |
| 后端、数据库与历史数据 | 保留 `performance` Controller、Service、Mapper、Flyway 迁移和现有记录 | 不删除表、接口、权限或迁移；不改写历史数据 |

### 0.3 恢复绩效的准入条件

恢复绩效 UI 前必须另立计划，并同时满足：人事异动已生效、人员范围授权测试通过、绩效周期在组织变更后的适用规则明确、至少一名业务负责人确认评分/校准/申诉流程。恢复时按“导航和路由 -> 工作台待办 -> 绩效页面 -> 报表 -> 流程模板”的顺序逐步开放，不能仅恢复某个页面。

## 1. 背景与结论

当前实现已经可维护岗位、职级和员工当前信息，也能在创建员工时自动开通初始账号；但“组织人事”尚未形成业务闭环。现有导航、详细设计和数据库设计均声明了部门维护、员工履历和人事异动能力，代码尚未落地或只实现了部分读写。

| 能力 | 当前状态 | 整改结论 |
| --- | --- | --- |
| 部门树查询 | 已实现 | 保留，增加数据范围、完整维护和停用校验 |
| 部门新建 | 仅后端实现 | 补齐前端；增加编辑、移动、负责人、排序、停用 |
| 岗位、职级 | 已实现基础 CRUD | 补齐停用引用校验和前端状态维护 |
| 员工目录、档案 | 已实现基础读写 | 补齐主管选择、状态枚举、字段权限与写操作数据范围 |
| 员工账号开通 | 创建时自动开通 | 补齐账号状态、角色分配边界和离职停用 |
| 员工履历 | 未实现 | 新建只追加履历表和查询接口 |
| 人事异动 | 导航存在，页面/API/表不存在 | 新建完整业务单、流程处理器和页面 |
| 组织数据权限 | 仅员工读取部分实现 | 收口到部门树、详情、创建、更新、异动及导出 |
| 审计 | 未实现 | 对组织、人事、账号与敏感查看写入只追加日志 |
| 批量导入导出 | 未实现 | 本计划不纳入首批；在异动闭环稳定后单独立项 |

### 1.1 现有实现证据

- `/org/departments` 和 `/org/employees` 都映射到 `OrganizationEmployeesView.vue`；`/personnel/changes` 未映射组件，会退回 `DomainView`。
- `DepartmentController` 只有 `GET /departments` 与 `POST /departments`；没有更新、移动、停用或删除动作。
- `EmployeeService.update` 可直接覆盖部门、岗位、职级和直属主管，且没有写入履历或走审批。
- `EmployeeDataScopeResolver` 只被员工目录和详情读取路径使用；部门树、员工创建和员工更新未以当前用户的数据范围校验为前提。
- 数据库已有 `hr_department.leader_employee_id`、`sort_no` 与 `hr_employee.termination_date`，但当前 DTO/Mapper/API 未能完整读写；`hr_employee_history`、`hr_personnel_change`、离职交接和审计表尚不存在。

## 2. 范围、边界与优先级

### 2.1 本次纳入范围

1. 部门、岗位、职级和员工当前任职信息的完整维护。
2. 基于角色和组织范围的读写授权收口。
3. 员工任职履历与人事异动审批闭环。
4. 入职、转正、调动、晋升、降职、停职、离职七种异动类型。
5. 离职交接、账号禁用和历史数据保留。
6. 组织人事关键操作审计与必要的测试覆盖。

### 2.2 明确不纳入本计划

- 目标管理、绩效指标、绩效方案、考核周期、员工自评、主管评分、校准、绩效发布和申诉。上述能力保留后端与数据，但 UI 全部下架，恢复必须满足第 0.3 节条件。
- 招聘、候选人、面试和 offer 管理。
- 薪酬、个税、社保、公积金与银行代发。
- 合同电子签、档案附件管理和复杂敏感字段加密。合同可在后续独立计划中接入本次员工履历。
- 员工 Excel 批量导入导出、异步导出任务和消息中心；本次只预留审计和业务对象引用。
- 多法人、多国家劳动法及复杂组织矩阵汇报关系。

### 2.3 UI 下架后的验收口径

1. 登录页、侧边栏、工作台、数据分析页和流程模板页不出现“绩效”“目标”“自评”“评分”“绩效申诉”文本或操作入口。
2. 已登录用户直接打开 `/goals/cycles`、`/performance/cycles`、`/performance/tasks` 时由路由通配规则跳转 `/dashboard`；不加载相应 Vue 页面或 API。
3. 绩效后端接口与数据库记录保持不变；本次不新增后端权限拒绝逻辑，以免影响未来恢复或既有集成调用。
4. 人员规模报表仍可正常加载；移除绩效接口调用后，绩效服务不可用不应影响工作台或报表页面。
5. 流程模板中已有绩效申诉记录不在表格、业务类型下拉框或新建操作中显示；其数据库记录不删除。

### 2.4 交付批次

| 批次 | 可独立验收的结果 | 前置条件 |
| --- | --- | --- |
| A：基础纠偏 | 部门可维护、员工档案字段完整、数据范围覆盖读写、直接编辑不能改变任职关系 | 无 |
| B：人事异动 | 七类异动可保存、提交、审批、撤回、驳回、退回；最终审批写履历并更新当前任职 | 批次 A |
| C：离职办结与审计 | 离职交接驱动账号禁用；关键组织人事操作可追溯 | 批次 B |

## 3. 目标业务模型

### 3.1 员工状态与状态所有权

`employmentStatus` 使用受控枚举：`PENDING_ONBOARD`、`PROBATION`、`FORMAL`、`SUSPENDED`、`TERMINATED`。

| 状态变化 | 唯一允许入口 | 生效结果 |
| --- | --- | --- |
| `PENDING_ONBOARD -> PROBATION/FORMAL` | 入职异动最终生效 | 创建或激活员工账号、写入入职履历 |
| `PROBATION -> FORMAL` | 转正异动最终生效 | 更新员工状态、写入履历 |
| `FORMAL/PROBATION -> SUSPENDED` | 停职异动最终生效 | 更新状态；按配置限制业务提交 |
| `SUSPENDED -> FORMAL/PROBATION` | 停职恢复异动最终生效 | 恢复业务提交资格 |
| 任意在职状态 `-> TERMINATED` | 离职异动办结 | 写离职日期、禁用账号、保留历史 |

`PATCH /employees/{id}` 仅能修改不影响任职关系的普通档案字段。部门、岗位、职级、主管、员工状态、入职与试用日期一律从普通更新 DTO 中删除，只能由已生效的人事异动写入。

### 3.2 人事异动状态机

```text
DRAFT --submit--> IN_PROGRESS --final approve--> APPROVED --effective date reached--> EFFECTIVE
  ^                  |                  |                 |
  |                  +--return----------+                 +--write employee current state/history
  +--withdraw--------+--reject----------------------------> REJECTED / WITHDRAWN
```

- `DRAFT`：创建人可编辑或删除；尚未创建流程实例。
- `IN_PROGRESS`：保存申请快照并绑定唯一流程实例；不可直接改业务字段。
- `APPROVED`：流程已结束；若生效日期在未来，由定时作业或受控生效动作转为 `EFFECTIVE`。
- `EFFECTIVE`：已在单一事务中更新员工当前信息并新增不可变履历。
- `REJECTED`、`WITHDRAWN`：保留申请和工作流日志，不修改员工当前信息。

### 3.3 关键不变量

1. 每次异动创建时冻结员工当前任职快照和异动后快照；审批期间不随主数据变化而改变。
2. 每个 `PERSONNEL_CHANGE` 单据最多关联一个工作流实例，数据库以 `(business_type, business_id, deleted)` 唯一约束保证。
3. 履历只追加，禁止修改或逻辑删除；同一员工、同一生效日期可按主键排序保留多条事件。
4. 最终审批、生效、员工当前信息更新、履历写入、账号状态处理和审计日志必须在一个事务内完成。
5. 所有写操作由服务端从认证上下文取得操作者；请求体不得包含可信操作者、可见范围或审批人。
6. 账号停用只发生在离职异动生效后；停用时递增 `sys_user.session_version`，使旧令牌失效。

## 4. 数据库与 Flyway 方案

从已有未提交的 `V019__configure_default_leave_quota.sql` 之后开始编号，禁止修改 `V001` 至 `V019`。

### 4.1 `V020__complete_organization_master_data.sql`

目的：使现有表可表达并维护部门负责人、排序和合法员工状态。

```sql
ALTER TABLE hr_employee
    MODIFY employment_status VARCHAR(32) NOT NULL;

CREATE INDEX idx_hr_employee_status ON hr_employee (employment_status, deleted);
CREATE INDEX idx_hr_department_leader ON hr_department (leader_employee_id, deleted);

UPDATE hr_employee
SET employment_status = 'PROBATION'
WHERE employment_status NOT IN ('PENDING_ONBOARD', 'PROBATION', 'FORMAL', 'SUSPENDED', 'TERMINATED');
```

应用层负责枚举校验；数据库保持 `VARCHAR(32)`，避免 MySQL `ENUM` 扩展时的停机风险。

### 4.2 `V021__add_personnel_change_and_employee_history.sql`

新建 `hr_personnel_change` 与 `hr_employee_history`。快照使用 JSON，保证历史可还原且避免将每个未来字段都复制到履历表。

```sql
CREATE TABLE hr_personnel_change (
    id BIGINT NOT NULL,
    change_no VARCHAR(64) NOT NULL,
    employee_id BIGINT NULL,
    change_type VARCHAR(32) NOT NULL,
    application_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    reason TEXT NOT NULL,
    before_snapshot JSON NULL,
    after_snapshot JSON NOT NULL,
    workflow_instance_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_personnel_change_no (change_no, deleted),
    KEY idx_hr_personnel_change_employee_status (employee_id, status, effective_date, deleted),
    KEY idx_hr_personnel_change_workflow (workflow_instance_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hr_employee_history (
    id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    change_id BIGINT NULL,
    event_type VARCHAR(32) NOT NULL,
    effective_date DATE NOT NULL,
    snapshot JSON NOT NULL,
    created_by BIGINT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_hr_employee_history_employee_date (employee_id, effective_date, id),
    KEY idx_hr_employee_history_change (change_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

入职异动允许 `employee_id` 在草稿阶段为空，最终生效时创建员工和账号；其余类型必须引用已有员工。

### 4.3 `V022__add_exit_handover_and_personnel_permissions.sql`

新建离职交接表；新增权限 `personnel:read`、`personnel:create`、`personnel:manage`、`personnel:approve`、`personnel:execute`，并为系统管理员和 HR 角色建立菜单授权。`personnel:execute` 仅用于延期生效、人工重试等受控后台动作，普通审批人不授予。

离职交接表需包含：离职单 ID、交接人、总体状态、事项类别（`WORK`、`ASSET`、`ACCOUNT`）、接收人、是否必办、完成时间、确认人和备注。数据库应建立 `(change_id, status, deleted)` 索引。

### 4.4 `V023__add_organization_personnel_audit_log.sql`

新建只追加的 `sys_operation_log`，字段至少包含：模块、动作、对象类型、对象 ID、操作者、结果、`trace_id`、变更摘要 JSON、来源地址、创建时间。索引：`(module, object_id, created_time)`、`(operator_user_id, created_time)`。

### 4.5 历史数据初始化

`V021` 完成建表后，以一次性 SQL 为每个现有员工补入 `BASELINE` 履历：快照取迁移执行时的当前部门、岗位、职级、主管、状态、入职/试用日期；`effective_date = hire_date`，若缺失则使用迁移执行日期。该记录的 `change_id` 为空，明确表示无法追溯的存量基线，不伪造历史异动单。

## 5. 后端实现结构

### 5.1 新增或调整的文件

| 路径 | 责任 |
| --- | --- |
| `backend/src/main/java/com/hrpm/controller/DepartmentController.java` | 部门查询、创建、更新、移动、停用动作 |
| `backend/src/main/java/com/hrpm/controller/EmployeeController.java` | 档案查询和受限普通资料更新；移除任职关系写入 |
| `backend/src/main/java/com/hrpm/controller/PersonnelChangeController.java` | 人事异动列表、详情、草稿、提交、撤回、生效与履历查询 |
| `backend/src/main/java/com/hrpm/service/DepartmentService.java` | 树完整性、负责人校验、停用前置校验、数据范围校验 |
| `backend/src/main/java/com/hrpm/service/EmployeeService.java` | 普通档案更新、账号状态动作和数据范围校验 |
| `backend/src/main/java/com/hrpm/service/PersonnelChangeService.java` | 异动单创建、快照、状态机和生效事务 |
| `backend/src/main/java/com/hrpm/service/PersonnelChangeWorkflowBusinessHandler.java` | `PERSONNEL_CHANGE` 的工作流回调 |
| `backend/src/main/java/com/hrpm/service/EmployeeDataScopeResolver.java` | 为读取和写入提供统一授权断言 |
| `backend/src/main/java/com/hrpm/service/OperationAuditService.java` | 只追加审计日志，不记录密码或敏感明文 |
| `backend/src/main/java/com/hrpm/mapper/PersonnelChangeMapper.java` | 异动、履历、交接和状态更新 SQL |
| `backend/src/main/java/com/hrpm/mapper/DepartmentMapper.java` | 部门负责人、排序、更新、移动、停用和引用检查 SQL |
| `backend/src/main/java/com/hrpm/mapper/EmployeeMapper.java` | 普通资料更新与异动生效专用更新 SQL |
| `backend/src/main/java/com/hrpm/mapper/OperationAuditMapper.java` | 审计插入 SQL |
| `backend/src/main/java/com/hrpm/dto/PersonnelChangeDTOs.java` | 异动请求、提交、生效、交接请求 DTO |
| `backend/src/main/java/com/hrpm/vo/PersonnelChangeVOs.java` | 列表、详情、履历和交接响应 VO |
| `backend/src/main/java/com/hrpm/entity/PersonnelChange*.java` | 异动、履历、交接与状态枚举实体 |

### 5.2 部门 API 契约

| 方法 | 路径 | 权限 | 行为 |
| --- | --- | --- | --- |
| `GET` | `/departments` | `org:read` | 返回当前用户数据范围内的树；无范围返回空数组 |
| `POST` | `/departments` | `org:manage` | 新建部门，校验上级、负责人和可写范围 |
| `PATCH` | `/departments/{id}` | `org:manage` | 更新名称、负责人、排序、状态、生效日期，使用版本锁 |
| `POST` | `/departments/{id}/move` | `org:manage` | 移动部门及其子树，拒绝循环引用 |
| `POST` | `/departments/{id}/disable` | `org:manage` | 仅无在职员工、无活动子部门、无进行中异动时允许停用 |

部门移动的核心校验必须满足以下逻辑：

```java
if (targetParentId != null && targetParentId.equals(departmentId)) {
    throw new OrganizationReferenceInvalidException("Department cannot be its own parent");
}
if (targetParent != null && targetParent.path().startsWith(current.path())) {
    throw new OrganizationReferenceInvalidException("Department cannot move under its descendant");
}
```

移动成功后，服务在同一事务内重算当前部门和所有后代的 `path`。部门树查询按 `path, sort_no, id` 排序。

### 5.3 员工 API 契约调整

`PATCH /employees/{id}` 请求体收敛为：

```json
{
  "name": "张三",
  "gender": "MALE",
  "version": "3"
}
```

部门、岗位、职级、主管、状态、入职日期和试用期字段仅展示，页面不提供直接编辑。临时兼容期内，后端收到旧字段必须以 `400 VALIDATION_FAILED` 拒绝，不能静默忽略，以避免调用方误以为变更成功。

新增员工档案接口不再直接创建正式在职员工：

- 普通 HR 使用 `POST /personnel-changes` 创建 `ONBOARD` 草稿。
- 仅数据迁移/管理员初始化场景使用受 `personnel:execute` 保护的内部服务创建基线员工，不公开给前端。

### 5.4 人事异动 API 契约

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/personnel-changes` | `personnel:read` | 按员工、类型、状态、日期和组织范围分页查询 |
| `POST` | `/personnel-changes` | `personnel:create` | 创建草稿并冻结初始快照 |
| `GET` | `/personnel-changes/{id}` | `personnel:read` | 返回申请、前后快照、交接、流程实例和操作权限 |
| `PATCH` | `/personnel-changes/{id}` | 创建人或 `personnel:manage` | 仅 `DRAFT` 或被退回草稿允许编辑，需版本号 |
| `POST` | `/personnel-changes/{id}/submit` | 创建人或 `personnel:manage` | 选择模板、创建工作流并转为 `IN_PROGRESS` |
| `POST` | `/personnel-changes/{id}/withdraw` | 创建人 | 仅工作流进行中可撤回 |
| `POST` | `/personnel-changes/{id}/effective` | `personnel:execute` | 仅 `APPROVED` 且生效日期已到时执行；定时任务调用同一服务 |
| `GET` | `/employees/{id}/history` | `org:read` | 返回受数据范围约束的只追加履历 |
| `POST` | `/personnel-changes/{id}/handover-items` | 创建人或 `personnel:manage` | 为离职单维护交接事项，仅草稿和审批中可编辑 |
| `POST` | `/personnel-changes/{id}/handover-items/{itemId}/confirm` | 指定接收人或 `personnel:manage` | 确认交接事项 |

创建调动异动的请求示例：

```json
{
  "employeeId": "10001",
  "changeType": "TRANSFER",
  "effectiveDate": "2026-08-01",
  "reason": "组织调整",
  "afterAssignment": {
    "departmentId": "20001",
    "positionId": "30001",
    "rankId": "40002",
    "managerEmployeeId": "10002",
    "employmentStatus": "FORMAL"
  },
  "version": "0"
}
```

`ONBOARD` 请求使用候选员工资料作为 `afterAssignment` 的组成部分；不允许客户端提供 `beforeSnapshot`、流程实例 ID、审批人或操作者。

### 5.5 人事异动服务与工作流集成

新增 `PersonnelChangeWorkflowBusinessHandler`，与现有 `LeaveWorkflowBusinessHandler`、`OvertimeWorkflowBusinessHandler` 一样注册到 `WorkflowBusinessHandlerRegistry`：

```java
@Component
public final class PersonnelChangeWorkflowBusinessHandler implements WorkflowBusinessHandler {
    @Override
    public String businessType() {
        return "PERSONNEL_CHANGE";
    }

    @Override
    public void approve(WorkflowBusinessContext context) {
        personnelChangeService.markApproved(context.businessId(), context.actorUserId());
    }

    @Override
    public void reject(WorkflowBusinessContext context) {
        personnelChangeService.markRejected(context.businessId(), context.actorUserId());
    }

    @Override
    public void withdraw(WorkflowBusinessContext context) {
        personnelChangeService.markWithdrawn(context.businessId(), context.actorUserId());
    }

    @Override
    public void returnToDraft(WorkflowBusinessContext context) {
        personnelChangeService.returnToDraft(context.businessId(), context.actorUserId());
    }
}
```

`markApproved` 不直接覆盖员工；若 `effectiveDate <= LocalDate.now(UTC)` 则调用 `effective`，否则保留 `APPROVED`。`effective` 的事务步骤固定为：

1. 以 `id + version + APPROVED` 锁定人事异动单。
2. 再次校验操作者权限、员工/目标部门/岗位/职级/主管的有效性和数据范围。
3. 对离职单校验所有必办交接项均已确认。
4. 用员工当前版本更新状态或任职信息；受影响行不是 1 时返回冲突。
5. 插入 `hr_employee_history`，写入最终生效快照。
6. 离职时禁用关联 `sys_user` 并递增会话版本；入职时创建或激活账号并分配最小自助角色。
7. 将异动单更新为 `EFFECTIVE`，插入审计日志。

### 5.6 数据范围收口

新增统一的授权方法，避免控制器与各服务自行拼装范围条件：

```java
public interface OrganizationAccessService {
    EmployeeDataScope resolve(long userId);
    void requireReadableEmployee(long userId, long employeeId);
    void requireWritableEmployee(long userId, long employeeId);
    void requireWritableDepartment(long userId, long departmentId);
}
```

规则如下：

- `ALL`：允许全部读取与写入。
- `SELF`、`DIRECT`：只允许读取对应员工；不得维护组织、岗位、职级或发起他人异动。
- `DEPT`、`DEPT_TREE`、`CUSTOM`：可读取范围内部门与员工；写入前校验目标员工的当前部门和变更后的部门均在可写范围内。
- 部门树只返回授权部门及其到根节点的必要祖先节点；祖先节点仅用于树结构展示，不能通过其 ID 读取范围外员工。
- 员工转出和转入任一方不在可写范围时拒绝，防止借调动绕过范围。

所有拒绝统一返回 `403 DATA_SCOPE_DENIED`，不可伪装为成功或以客户端隐藏按钮替代服务端校验。

### 5.7 审计策略

`OperationAuditService` 记录下列成功和失败动作：部门创建/更新/移动/停用、岗位职级状态改变、员工普通档案修改、人事异动创建/提交/撤回/审批/生效、离职交接确认、账号开通/启停、敏感字段完整查看。

变更摘要只保存字段名和脱敏后的前后值；不得保存密码、JWT、临时密码、身份证或手机号明文。审计写入失败应使关键写操作回滚，确保“数据已变更但没有审计”不会发生。

## 6. 前端实现方案

### 6.1 路由与页面边界

| 路由 | 组件 | 主要职责 |
| --- | --- | --- |
| `/org/departments` | `OrganizationDepartmentsView.vue` | 部门树、负责人、排序、移动、停用与引用阻断提示 |
| `/org/employees` | `OrganizationEmployeesView.vue` | 员工目录、详情、普通档案编辑、履历入口；不再维护任职关系 |
| `/personnel/changes` | `PersonnelChangesView.vue` | 异动单列表、筛选、草稿新建、提交、撤回和详情抽屉 |
| `/personnel/changes/:id` | `PersonnelChangeDetailView.vue` | 前后快照对比、交接事项、流程历史和可执行动作 |

不再将组织架构和员工档案复用同一个页面。路由守卫使用与接口一致的权限：组织页为 `org:read`，异动列表为 `personnel:read`；没有权限时导航不展示且直接访问回到工作台。

### 6.2 部门页面

部门树节点显示名称、编码、负责人、状态和员工数；右侧详情面板提供新建子部门、编辑、移动和停用按钮。移动使用部门树选择器，禁止选择自身及其后代。停用前由后端返回阻断原因，例如“仍有 12 名在职员工”或“存在 2 个进行中异动”，前端只展示原因，不自行判断。

### 6.3 员工页面

员工编辑抽屉只保留姓名、性别和未来纳入的非任职普通资料。新增员工按钮替换为“发起入职”，跳转到人事异动创建。详情增加“当前任职信息”和“履历”两个标签；主管字段为只读名称，变更通过“发起调动”处理。

员工状态筛选使用全部五个受控枚举，未知状态不再被 UI 错误地渲染为“试用”。

### 6.4 人事异动页面与交互

新建异动采用步骤表单：

1. 选择异动类型和员工；`ONBOARD` 输入候选员工身份及初始任职，其他类型选择现有员工。
2. 展示只读的当前快照；填写变更后部门、岗位、职级、主管、状态、生效日期与原因。
3. 离职类型维护交接事项；至少有一项 `ACCOUNT` 和一项 `WORK` 必办事项。
4. 提交前显示前后字段差异和将匹配的审批模板；确认后调用提交接口。

详情页显示状态条、前后快照差异、审批历史和交接清单。`APPROVED` 且未来生效的单据显示“待生效”；普通用户没有“强制生效”按钮。所有图标按钮使用 Element Plus/Lucide 已有图标并提供 Tooltip；长操作通过文字按钮表达，例如“提交”“撤回”“确认交接”。

## 7. 实施任务清单

### Task 0: 前端下架目标与绩效功能

**Files:**
- Modify: `frontend/src/router/navigation.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `frontend/src/views/ReportsOverviewView.vue`
- Modify: `frontend/src/views/WorkflowTemplatesView.vue`
- Modify: `frontend/src/views/LoginView.vue`
- Test: `frontend` production type check and build

- [ ] **Step 1: 建立 UI 暴露清单并执行负向检查**

在修改前执行以下命令，确认导航、路由、工作台、报表、流程模板和登录文案仍存在绩效暴露点：

Run: `rg -n "'/goals/cycles'|'/performance/cycles'|'/performance/tasks'|绩效|目标管理|人力绩效" frontend/src/router frontend/src/views/DashboardView.vue frontend/src/views/ReportsOverviewView.vue frontend/src/views/LoginView.vue frontend/src/views/WorkflowTemplatesView.vue`

Expected: 修改前能匹配目标绩效导航、绩效路由、工作台绩效待办、绩效报表和登录文案。

- [ ] **Step 2: 移除导航、路由和页面模块导入**

从 `navigationItems` 移除目标、绩效周期和绩效任务三项；从 `routedComponents` 和顶层 import 移除 `PerformanceConfigurationView` 与 `PerformanceTasksView`。不要删除这两个 Vue 文件或 `api/performance.ts`，使未来恢复无需数据迁移。

- [ ] **Step 3: 清理工作台和报表的绩效依赖**

工作台仅调用 `fetchWorkflowTasks()` 并显示审批待办；删除绩效自评、主管评分、绩效指标卡片及 `/performance/tasks` 跳转。数据分析页仅调用 `fetchDepartmentHeadcounts()`，保留“在职人员”和“部门人员规模”，删除绩效等级分布的请求、数字卡片和表格。

- [ ] **Step 4: 隐藏流程模板中的绩效申诉类型**

将 `businessLabels` 类型收窄为 `Exclude<BusinessType, 'PERFORMANCE_APPEAL'>`，使新建模板下拉框不能选择绩效申诉；表格数据使用 `visibleTemplates` 过滤已有 `PERFORMANCE_APPEAL` 模板。不要调用删除模板接口，也不要改数据库记录。

- [ ] **Step 5: 更新产品文案**

将登录页产品名改为“人力资源管理系统”，说明改为“进入组织、人事、考勤与审批模块”。所有保留的导航和报表描述不得暗示仍提供绩效功能。

- [ ] **Step 6: 验证 UI 下架**

Run: `rg -n "'/goals/cycles'|'/performance/cycles'|'/performance/tasks'|fetchMyPerformanceTasks|fetchManagerPerformanceTasks|fetchPerformanceLevelDistribution|人力绩效管理系统" frontend/src/router frontend/src/views/DashboardView.vue frontend/src/views/ReportsOverviewView.vue frontend/src/views/LoginView.vue frontend/src/views/WorkflowTemplatesView.vue`

Expected: 无匹配结果。`PerformanceConfigurationView.vue`、`PerformanceTasksView.vue`、`api/performance.ts` 和后端 `performance` 包不作为本命令扫描对象。

Run: `npm.cmd run typecheck`

Expected: 无 Vue 或 TypeScript 类型错误。

Run: `npm.cmd run build`

Expected: Vite 生产构建成功，且生成的主入口不再因工作台或报表而请求绩效 API。

- [ ] **Step 7: 提交范围明确的前端变更**

Run: `git add frontend/src/router/navigation.ts frontend/src/router/index.ts frontend/src/views/DashboardView.vue frontend/src/views/ReportsOverviewView.vue frontend/src/views/WorkflowTemplatesView.vue frontend/src/views/LoginView.vue && git commit -m "feat: hide performance UI for hr management scope"`

### Task 1: 锁定现状并补齐基础测试夹具

**Files:**
- Modify: `backend/src/test/java/com/hrpm/controller/DepartmentApiIntegrationTests.java`
- Modify: `backend/src/test/java/com/hrpm/controller/EmployeeApiIntegrationTests.java`
- Create: `backend/src/test/java/com/hrpm/controller/OrganizationDataScopeApiIntegrationTests.java`

- [ ] **Step 1: 为部门、员工和范围角色建立可复用夹具**

建立 `ALL`、`DEPT_TREE`、`SELF` 三类用户、根子部门和跨部门员工；夹具固定使用事务后清理，避免测试互相依赖。

- [ ] **Step 2: 先写失败的授权测试**

覆盖受限用户读取部门树只见授权分支、不能读取范围外员工、不能更新范围外员工、不能将员工调到范围外部门。

- [ ] **Step 3: 执行失败测试**

Run: `mvn test -Dtest=OrganizationDataScopeApiIntegrationTests`

Expected: 当前实现至少在部门树和写操作范围校验上失败。

- [ ] **Step 4: 提交测试基线**

Run: `git add backend/src/test/java/com/hrpm/controller/DepartmentApiIntegrationTests.java backend/src/test/java/com/hrpm/controller/EmployeeApiIntegrationTests.java backend/src/test/java/com/hrpm/controller/OrganizationDataScopeApiIntegrationTests`

Run: `git commit -m "test: cover organization data scope boundaries"`

### Task 2: 部门主数据完整维护与范围校验

**Files:**
- Modify: `backend/src/main/java/com/hrpm/dto/CreateDepartmentDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/UpdateDepartmentDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/MoveDepartmentDTO.java`
- Modify: `backend/src/main/java/com/hrpm/controller/DepartmentController.java`
- Modify: `backend/src/main/java/com/hrpm/service/DepartmentService.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/DepartmentMapper.java`
- Modify: `backend/src/main/java/com/hrpm/vo/DepartmentVO.java`
- Test: `backend/src/test/java/com/hrpm/controller/DepartmentApiIntegrationTests.java`

- [ ] **Step 1: 写部门更新、循环移动和停用阻断测试**

测试 PATCH 的乐观锁冲突、移动到后代部门返回 `VALIDATION_FAILED`、含在职员工或活动子部门时停用返回阻断原因、无引用部门可停用。

- [ ] **Step 2: 实现 DTO 和 Mapper 的全字段读写**

`DepartmentVO`、Mapper 查询和更新必须携带 `leaderEmployeeId`、`sortNo`、`effectiveDate`、`status`、`version`。新增 DTO 应包含：

```java
public record UpdateDepartmentDTO(
        @NotBlank String name,
        String leaderEmployeeId,
        @NotNull @Min(0) Integer sortNo,
        @NotBlank String status,
        @NotNull LocalDate effectiveDate,
        @NotBlank String version) {}
```

- [ ] **Step 3: 实现部门服务动作**

在 `DepartmentService` 中新增 `update`、`move`、`disable`；每个动作先调用 `OrganizationAccessService.requireWritableDepartment`，再执行业务校验和版本更新。

- [ ] **Step 4: 运行部门测试**

Run: `mvn test -Dtest=DepartmentApiIntegrationTests,OrganizationDataScopeApiIntegrationTests`

Expected: 通过，且受限用户不再获得全量部门树。

- [ ] **Step 5: 提交变更**

Run: `git add backend/src/main/java/com/hrpm/dto/CreateDepartmentDTO.java backend/src/main/java/com/hrpm/dto/UpdateDepartmentDTO.java backend/src/main/java/com/hrpm/dto/MoveDepartmentDTO.java backend/src/main/java/com/hrpm/controller/DepartmentController.java backend/src/main/java/com/hrpm/service/DepartmentService.java backend/src/main/java/com/hrpm/mapper/DepartmentMapper.java backend/src/main/java/com/hrpm/vo/DepartmentVO.java backend/src/test/java/com/hrpm/controller/DepartmentApiIntegrationTests.java backend/src/test/java/com/hrpm/controller/OrganizationDataScopeApiIntegrationTests.java && git commit -m "feat: complete department maintenance controls"`

### Task 3: 收口员工普通档案与组织数据范围

**Files:**
- Create: `backend/src/main/java/com/hrpm/service/OrganizationAccessService.java`
- Modify: `backend/src/main/java/com/hrpm/service/EmployeeDataScopeResolver.java`
- Modify: `backend/src/main/java/com/hrpm/service/EmployeeService.java`
- Modify: `backend/src/main/java/com/hrpm/controller/EmployeeController.java`
- Modify: `backend/src/main/java/com/hrpm/dto/UpdateEmployeeDTO.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/EmployeeMapper.java`
- Test: `backend/src/test/java/com/hrpm/controller/EmployeeApiIntegrationTests.java`
- Test: `backend/src/test/java/com/hrpm/controller/OrganizationDataScopeApiIntegrationTests.java`

- [ ] **Step 1: 写失败测试，禁止普通编辑任职字段**

对 `PATCH /employees/{id}` 发送 `departmentId`、`positionId`、`rankId`、`managerEmployeeId` 或 `employmentStatus`，期望 `400` 与 `VALIDATION_FAILED`；保留姓名、性别和版本正常更新测试。

- [ ] **Step 2: 定义 `OrganizationAccessService`**

实现 `requireReadableEmployee`、`requireWritableEmployee`、`requireWritableDepartment`；禁止将 `EmployeeDataScope` 直接暴露给 Controller 作为权限判断。

- [ ] **Step 3: 收敛员工更新 SQL 与 DTO**

`EmployeeMapper.update` 仅更新可普通编辑字段和版本。任职字段新增专用、包内可见的 `applyPersonnelChange` Mapper 方法，只由 `PersonnelChangeService` 调用。

- [ ] **Step 4: 校验员工状态枚举**

新建 `EmploymentStatus` 枚举；任何创建/生效请求不在五个允许值内时返回 `VALIDATION_FAILED`。不使用开放字符串比较。

- [ ] **Step 5: 运行测试并提交**

Run: `mvn test -Dtest=EmployeeApiIntegrationTests,OrganizationDataScopeApiIntegrationTests`

Expected: 通过。

Run: `git add backend/src/main/java/com/hrpm/service/OrganizationAccessService.java backend/src/main/java/com/hrpm/service/EmployeeDataScopeResolver.java backend/src/main/java/com/hrpm/service/EmployeeService.java backend/src/main/java/com/hrpm/controller/EmployeeController.java backend/src/main/java/com/hrpm/dto/UpdateEmployeeDTO.java backend/src/main/java/com/hrpm/mapper/EmployeeMapper.java backend/src/test/java/com/hrpm/controller/EmployeeApiIntegrationTests.java backend/src/test/java/com/hrpm/controller/OrganizationDataScopeApiIntegrationTests.java && git commit -m "fix: enforce organization write scope and profile boundaries"`

### Task 4: 建立人事异动、履历和交接数据模型

**Files:**
- Create: `backend/src/main/resources/db/migration/V020__complete_organization_master_data.sql`
- Create: `backend/src/main/resources/db/migration/V021__add_personnel_change_and_employee_history.sql`
- Create: `backend/src/main/resources/db/migration/V022__add_exit_handover_and_personnel_permissions.sql`
- Create: `backend/src/main/java/com/hrpm/entity/PersonnelChange.java`
- Create: `backend/src/main/java/com/hrpm/entity/EmployeeHistory.java`
- Create: `backend/src/main/java/com/hrpm/entity/PersonnelChangeStatus.java`
- Create: `backend/src/main/java/com/hrpm/entity/PersonnelChangeType.java`
- Create: `backend/src/main/java/com/hrpm/mapper/PersonnelChangeMapper.java`
- Test: `backend/src/test/java/com/hrpm/DatabaseMigrationIntegrationTests.java`

- [ ] **Step 1: 写迁移验证测试**

验证新表、唯一索引、`PERSONNEL_CHANGE` 权限与现有用户表兼容；对两个同单号的有效异动单插入应失败。

- [ ] **Step 2: 新增 V020-V022 迁移**

使用第 4 节给出的字段、索引与基线履历策略。迁移仅新增，不改动 V001-V019。

- [ ] **Step 3: 定义实体与 Mapper 映射**

`PersonnelChange` 必须包含：ID、单号、员工 ID、类型、申请/生效日期、原因、前后快照、工作流实例、状态和版本。Mapper 的状态更新均使用：

```sql
WHERE id = #{id}
  AND status = #{expectedStatus}
  AND version = #{version}
  AND deleted = 0
```

- [ ] **Step 4: 运行迁移测试并提交**

Run: `mvn test -Dtest=DatabaseMigrationIntegrationTests,TestcontainersMigrationTests`

Expected: 所有迁移可从空库执行，现有迁移测试继续通过。

Run: `git add backend/src/main/resources/db/migration backend/src/main/java/com/hrpm/entity backend/src/main/java/com/hrpm/mapper backend/src/test/java/com/hrpm/DatabaseMigrationIntegrationTests.java && git commit -m "feat: add personnel change persistence"`

### Task 5: 实现人事异动应用服务与工作流回调

**Files:**
- Create: `backend/src/main/java/com/hrpm/service/PersonnelChangeService.java`
- Create: `backend/src/main/java/com/hrpm/service/PersonnelChangeWorkflowBusinessHandler.java`
- Create: `backend/src/main/java/com/hrpm/dto/PersonnelChangeDTOs.java`
- Create: `backend/src/main/java/com/hrpm/vo/PersonnelChangeVOs.java`
- Modify: `backend/src/main/java/com/hrpm/service/WorkflowBusinessHandlerRegistry.java`
- Modify: `backend/src/main/java/com/hrpm/service/EmployeeService.java`
- Test: `backend/src/test/java/com/hrpm/service/PersonnelChangeServiceTests.java`
- Test: `backend/src/test/java/com/hrpm/controller/PersonnelChangeApiIntegrationTests.java`

- [ ] **Step 1: 写状态机失败测试**

覆盖：草稿可编辑；提交后不能编辑；退回后恢复草稿；撤回后不能生效；最终通过立即生效写履历；未来日期只转 `APPROVED`；重复生效返回冲突。

- [ ] **Step 2: 实现快照、创建、编辑和提交**

创建时由服务端读取员工当前信息生成 `beforeSnapshot`；验证 `afterAssignment` 的部门、岗位、职级和主管均有效且可写；提交时通过现有工作流模板选择服务创建 `PERSONNEL_CHANGE` 实例。

- [ ] **Step 3: 实现流程业务处理器**

按第 5.5 节的 handler 实现审批、驳回、撤回、退回。最终审批由 handler 调用 `markApproved`，不得在 `WorkflowTaskService` 中写人事表。

- [ ] **Step 4: 实现生效事务和离职办结**

离职前校验必办交接项；生效时更新员工、插入履历、变更异动单、处理账号和写审计。所有更新必须带版本和预期状态条件。

- [ ] **Step 5: 运行服务与接口测试**

Run: `mvn test -Dtest=PersonnelChangeServiceTests,PersonnelChangeApiIntegrationTests`

Expected: 七类异动的状态转换、快照、履历和离职阻断测试全部通过。

- [ ] **Step 6: 提交变更**

Run: `git add backend/src/main/java/com/hrpm backend/src/test/java/com/hrpm && git commit -m "feat: add personnel change workflow"`

### Task 6: 审计和离职账号处理

**Files:**
- Create: `backend/src/main/resources/db/migration/V023__add_organization_personnel_audit_log.sql`
- Create: `backend/src/main/java/com/hrpm/entity/OperationAuditLog.java`
- Create: `backend/src/main/java/com/hrpm/mapper/OperationAuditMapper.java`
- Create: `backend/src/main/java/com/hrpm/service/OperationAuditService.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/UserAccountMapper.java`
- Modify: `backend/src/main/java/com/hrpm/service/PersonnelChangeService.java`
- Test: `backend/src/test/java/com/hrpm/controller/PersonnelChangeApiIntegrationTests.java`

- [ ] **Step 1: 写离职账号禁用和审计失败测试**

离职异动未办结时不得禁用账号；办结后账号状态为 `DISABLED`、会话版本递增、旧 token 无效；操作日志保存脱敏摘要且不包含临时密码。

- [ ] **Step 2: 实现账号状态动作**

在 `UserAccountMapper` 增加原子 `disableForEmployee` 更新：状态改为 `DISABLED`，会话版本加一，条件为账号有效且未删除。账号不存在不应阻断离职，但审计应标记“无关联账号”。

- [ ] **Step 3: 实现只追加审计服务**

审计 API 接收结构化字段；由服务端构建摘要。对关键写操作，Mapper 插入返回值必须为 1，否则抛异常回滚主事务。

- [ ] **Step 4: 运行测试并提交**

Run: `mvn test -Dtest=PersonnelChangeApiIntegrationTests,TokenAuthenticationFilterTests`

Expected: 通过。

Run: `git add backend/src/main/resources/db/migration/V023__add_organization_personnel_audit_log.sql backend/src/main/java/com/hrpm/entity/OperationAuditLog.java backend/src/main/java/com/hrpm/mapper/OperationAuditMapper.java backend/src/main/java/com/hrpm/service/OperationAuditService.java backend/src/main/java/com/hrpm/mapper/UserAccountMapper.java backend/src/main/java/com/hrpm/service/PersonnelChangeService.java backend/src/test/java/com/hrpm/controller/PersonnelChangeApiIntegrationTests.java && git commit -m "feat: audit personnel operations and close accounts on exit"`

### Task 7: 组织、档案与异动前端页面

**Files:**
- Create: `frontend/src/views/OrganizationDepartmentsView.vue`
- Create: `frontend/src/views/PersonnelChangesView.vue`
- Create: `frontend/src/views/PersonnelChangeDetailView.vue`
- Create: `frontend/src/components/organization/DepartmentEditorDialog.vue`
- Create: `frontend/src/components/personnel/PersonnelChangeEditorDrawer.vue`
- Create: `frontend/src/components/personnel/PersonnelChangeDiff.vue`
- Create: `frontend/src/components/personnel/ExitHandoverList.vue`
- Modify: `frontend/src/views/OrganizationEmployeesView.vue`
- Modify: `frontend/src/components/organization/EmployeeEditorDrawer.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/router/navigation.ts`
- Create: `frontend/src/api/personnel.ts`
- Modify: `frontend/src/api/organization.ts`
- Modify: `frontend/src/types/organization.ts`
- Create: `frontend/src/types/personnel.ts`

- [ ] **Step 1: 定义 API 类型和客户端**

请求与响应中的所有 ID、版本使用 `string`；严禁在组件中以 `Number(id)` 传递业务 ID。为异动列表、详情、创建、修改、提交、撤回、履历和交接分别定义强类型函数。

- [ ] **Step 2: 分离部门和员工路由**

将 `/org/departments` 指向 `OrganizationDepartmentsView`，`/org/employees` 保持员工页，`/personnel/changes` 指向异动列表。删除对这三条路径的 `DomainView` 回退依赖。

- [ ] **Step 3: 实现部门维护界面**

树与详情面板基于后端返回的可见节点渲染。新建、编辑、移动、停用都由 `DepartmentEditorDialog` 调用 API；版本冲突时刷新当前节点，不自动覆盖。

- [ ] **Step 4: 收敛员工抽屉并接入履历**

移除任职关系和状态的可编辑控件，新增“发起异动”与“查看履历”入口。主管显示只读，调动通过异动单处理。

- [ ] **Step 5: 实现异动列表、编辑和详情**

列表支持类型、状态、员工、部门、生效日期筛选；编辑抽屉按异动类型显示必要字段；详情用 `PersonnelChangeDiff` 并列展示前后快照差异；离职类型显示 `ExitHandoverList`。

- [ ] **Step 6: 执行前端校验**

Run: `npm.cmd run typecheck`

Expected: 无 TypeScript 或 Vue 类型错误。

Run: `npm.cmd run build`

Expected: Vite 生产构建成功。

- [ ] **Step 7: 提交前端变更**

Run: `git add frontend/src/views/OrganizationDepartmentsView.vue frontend/src/views/OrganizationEmployeesView.vue frontend/src/views/PersonnelChangesView.vue frontend/src/views/PersonnelChangeDetailView.vue frontend/src/components/organization/DepartmentEditorDialog.vue frontend/src/components/organization/EmployeeEditorDrawer.vue frontend/src/components/personnel/PersonnelChangeEditorDrawer.vue frontend/src/components/personnel/PersonnelChangeDiff.vue frontend/src/components/personnel/ExitHandoverList.vue frontend/src/router/index.ts frontend/src/router/navigation.ts frontend/src/api/personnel.ts frontend/src/api/organization.ts frontend/src/types/organization.ts frontend/src/types/personnel.ts && git commit -m "feat: deliver organization and personnel change workspace"`

### Task 8: 端到端验证与文档同步

**Files:**
- Modify: `docs/03-数据库设计.md`
- Modify: `docs/04-接口设计.md`
- Modify: `docs/05-前端设计.md`
- Modify: `docs/06-权限与审批设计.md`
- Modify: `docs/07-测试方案.md`
- Modify: `docs/08-详细设计.md`
- Modify: `docs/09-开发交付清单.md`

- [ ] **Step 1: 同步实现契约文档**

将实际 API、表结构、权限、状态机和页面路由写回上述设计文档。移除“人事异动尚未实现”及与最终行为冲突的描述。

- [ ] **Step 2: 执行后端全量验证**

Run: `mvn test`

Expected: JUnit 5 和集成测试全部通过。

- [ ] **Step 3: 执行前端生产验证**

Run: `npm.cmd run typecheck`

Expected: 成功。

Run: `npm.cmd run build`

Expected: 成功。

- [ ] **Step 4: 执行迁移与代码卫生检查**

Run: `git diff --check`

Expected: 无空白错误。

Run: `mvn clean package`

Expected: 迁移、测试和可执行 JAR 构建成功。

- [ ] **Step 5: 执行人工验收场景**

使用 HR、部门主管、普通员工三类账号验证：目录与部门树范围、入职审批、转正、跨部门调动、离职交接阻断、离职账号失效、履历不可篡改、审批历史可查看、无权限菜单和接口均被拒绝。

- [ ] **Step 6: 提交文档与验收结果**

Run: `git add docs/03-数据库设计.md docs/04-接口设计.md docs/05-前端设计.md docs/06-权限与审批设计.md docs/07-测试方案.md docs/08-详细设计.md docs/09-开发交付清单.md && git commit -m "docs: synchronize organization personnel contracts"`

## 8. 验收标准

### 8.1 功能验收

1. HR 能从组织架构页创建、编辑、排序、移动和停用部门；循环移动、无效负责人、在职员工阻断均有明确错误。
2. 普通员工只能读取自身许可的组织和档案数据，部门主管只能在授权组织范围内读写；接口权限不能被前端绕过。
3. 普通员工档案编辑不能修改任职、主管和状态；任何此类请求都被后端拒绝。
4. 七类人事异动均可创建、提交、审批、驳回、退回、撤回；审批实例和异动单状态一致。
5. 人事异动生效时，员工当前信息、不可变履历、账号状态和审计日志原子提交；任一环节失败则全部回滚。
6. 离职单存在未完成必办交接项时不能生效；办结后关联账号禁用、旧会话失效，历史请假和流程数据仍可按权限查询。
7. 所有组织人事页面包含加载、空态、权限不足、校验错误、网络错误和版本冲突的可恢复反馈。

### 8.2 非功能验收

1. 每个状态改变均带版本与期望状态条件，影响行数异常返回冲突而非覆盖。
2. Flyway 从空库和升级库均可执行；不修改已应用迁移。
3. 所有新增 Controller/Service/Mapper 有至少一个成功路径和关键拒绝路径的 JUnit 覆盖。
4. 关键组织人事动作可在 `sys_operation_log` 按对象 ID、操作者和时间检索，且日志不含密码、JWT 或敏感明文。
5. `mvn test`、`mvn clean package`、`npm.cmd run typecheck` 和 `npm.cmd run build` 全部通过。

### 8.3 产品范围验收

1. 首次登录、刷新工作台和打开数据分析页都不调用 `/performance/**` 或 `/reports/performance-level-distribution`。
2. 侧边栏不存在“目标绩效”分组；页面中不存在绩效周期、绩效任务、目标管理、绩效自评、主管评分、绩效等级分布或绩效申诉的操作入口。
3. 浏览器直接访问原绩效与目标地址时返回工作台，不显示 `DomainView` 空白页，也不显示任一绩效页面。
4. 后端启动、现有 Flyway 迁移和既有绩效测试不因 UI 下架而被删除或修改；恢复绩效必须按第 0.3 节重新评审。

## 9. 风险与决策

| 风险 | 决策与缓解 |
| --- | --- |
| 现有员工已被直接修改，无法恢复历史 | 仅生成标记为 `BASELINE` 的初始履历；不伪造异动单，向业务明确历史追溯起点 |
| 未来生效与审批完成时间不同 | 允许 `APPROVED` 等待生效；使用同一 `effective` 服务供定时任务和人工受控动作调用，确保幂等 |
| 组织调整影响数据范围和审批人 | 提交时冻结异动快照与流程模板节点；后续组织变化不改变进行中审批链 |
| 离职导致待办无人处理 | 首批离职生效前检查该员工是否有未完成工作流任务；若有则阻断并要求先转交，避免静默遗失 |
| 账号创建与员工入职的顺序 | 只在入职异动生效后创建/激活账号，避免审批被驳回却产生可登录孤儿账号 |
| 范围规则复杂导致越权 | 范围判断集中在 `OrganizationAccessService`，以集成测试覆盖 `SELF`、`DIRECT`、`DEPT`、`DEPT_TREE`、`CUSTOM`、`ALL` |

## 10. 计划自检

- 覆盖了现状发现的部门功能断点、人事异动缺失、履历缺失、员工任职直接编辑、读写数据范围不一致、账号离职处理和审计缺失。
- 每个新增表、接口、页面、权限、事务边界和测试命令均有明确归属任务。
- 本计划不修改既有 Flyway 脚本，不引入招聘、薪酬或附件等未授权扩展范围。
- 人事异动与现有通用工作流的边界明确：工作流负责审批推进，`PersonnelChangeService` 负责业务状态与最终生效。
