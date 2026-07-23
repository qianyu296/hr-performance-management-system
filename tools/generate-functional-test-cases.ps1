param(
    [string]$OutputPath = "docs/HRPM_功能测试用例_2026-07-13.xlsx"
)

$ErrorActionPreference = 'Stop'
$skillRoot = 'C:/Users/29679/.codex/skills/minimax-xlsx'
$template = Join-Path $skillRoot 'templates/minimal_xlsx'
$work = Join-Path $env:TEMP 'hrpm-functional-test-cases-xlsx'

if (Test-Path $work) { Remove-Item -Recurse -Force $work }
Copy-Item -Recurse -Force $template $work
Copy-Item (Join-Path $work 'xl/worksheets/sheet1.xml') (Join-Path $work 'xl/worksheets/sheet2.xml')
Copy-Item (Join-Path $work 'xl/worksheets/sheet1.xml') (Join-Path $work 'xl/worksheets/sheet3.xml')
Copy-Item (Join-Path $work 'xl/worksheets/sheet1.xml') (Join-Path $work 'xl/worksheets/sheet4.xml')

function Xml-Escape([string]$Text) {
    return [System.Security.SecurityElement]::Escape($Text)
}

function Column-Letter([int]$Index) {
    $result = ''
    while ($Index -gt 0) {
        $Index--
        $result = [char](65 + ($Index % 26)) + $result
        $Index = [math]::Floor($Index / 26)
    }
    return $result
}

function New-StringCell([string]$Address, [string]$Value, [int]$Style = 0) {
    $key = [string]$Value
    if (-not $script:stringIndex.ContainsKey($key)) {
        $script:stringIndex[$key] = $script:strings.Count
        [void]$script:strings.Add($key)
    }
    return "<c r=`"$Address`" t=`"s`" s=`"$Style`"><v>$($script:stringIndex[$key])</v></c>"
}

function New-FormulaCell([string]$Address, [string]$Formula, [int]$Style = 2) {
    return "<c r=`"$Address`" s=`"$Style`"><f>$(Xml-Escape $Formula)</f><v></v></c>"
}

function Write-Sheet([string]$FileName, [array]$Rows, [int[]]$Widths, [int]$FreezeRow = 1) {
    $columns = for ($i = 0; $i -lt $Widths.Count; $i++) { "<col min=`"$($i + 1)`" max=`"$($i + 1)`" width=`"$($Widths[$i])`" customWidth=`"1`"/>" }
    $sheetRows = [System.Collections.Generic.List[string]]::new()
    for ($rowIndex = 0; $rowIndex -lt $Rows.Count; $rowIndex++) {
        $rowNo = $rowIndex + 1
        $cells = [System.Collections.Generic.List[string]]::new()
        $row = $Rows[$rowIndex]
        for ($colIndex = 0; $colIndex -lt $row.Count; $colIndex++) {
            $cell = $row[$colIndex]
            $address = "$(Column-Letter ($colIndex + 1))$rowNo"
            if ($cell.Kind -eq 'formula') {
                $cells.Add((New-FormulaCell $address $cell.Value $cell.Style))
            } else {
                $cells.Add((New-StringCell $address $cell.Value $cell.Style))
            }
        }
        $height = if ($rowIndex -eq 0) { ' ht="24" customHeight="1"' } elseif ($rowIndex -eq 1) { ' ht="30" customHeight="1"' } else { '' }
        $sheetRows.Add("<row r=`"$rowNo`"$height>$($cells -join '')</row>")
    }
    $pane = if ($FreezeRow -gt 0) { "<pane ySplit=`"$FreezeRow`" topLeftCell=`"A$($FreezeRow + 1)`" activePane=`"bottomLeft`" state=`"frozen`"/>" } else { '' }
    $xml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetViews><sheetView workbookViewId="0">$pane</sheetView></sheetViews>
  <sheetFormatPr defaultRowHeight="18"/>
  <cols>$($columns -join '')</cols>
  <sheetData>$($sheetRows -join '')</sheetData>
  <pageMargins left="0.3" right="0.3" top="0.5" bottom="0.5" header="0.3" footer="0.3"/>
</worksheet>
"@
    [System.IO.File]::WriteAllText((Join-Path $work "xl/worksheets/$FileName"), $xml, [System.Text.UTF8Encoding]::new($false))
}

function Cell([string]$Value, [int]$Style = 0) { return [pscustomobject]@{ Kind = 'string'; Value = $Value; Style = $Style } }
function Formula([string]$Value, [int]$Style = 2) { return [pscustomobject]@{ Kind = 'formula'; Value = $Value; Style = $Style } }

$headers = @('用例编号', '功能模块', '需求 / OpenAPI 操作', '优先级', '用例类型', '前置条件', '测试步骤', '测试数据', '预期结果', '实测结果', '执行状态', '缺陷编号', '执行人', '执行日期')
$rawCases = @'
TC-AUTH-001|认证与会话|REQ-AUTH-001; authLogin|P0|正向|存在状态为 ACTIVE 的管理员账号|1. 打开登录页 2. 输入正确账号和密码 3. 提交|管理员账号 / 正确密码|登录成功，保存访问令牌和刷新令牌，跳转工作台| |待执行| | |
TC-AUTH-002|认证与会话|REQ-AUTH-001; authLogin|P0|异常|在登录页|1. 输入正确账号 2. 输入错误密码 3. 提交|管理员账号 / 错误密码|拒绝登录，不泄露账户是否存在，不生成令牌| |待执行| | |
TC-AUTH-003|认证与会话|REQ-AUTH-001; authLogin|P0|异常|存在状态为 INACTIVE 的用户|1. 使用禁用账号登录|禁用账号 / 正确密码|拒绝登录并提示账号不可用，不生成令牌| |待执行| | |
TC-AUTH-004|认证与会话|REQ-AUTH-002; authRefresh|P0|正向|已获得有效刷新令牌|1. 调用刷新接口 2. 使用新访问令牌调用 /me|有效 refreshToken|返回新的可用会话令牌，/me 返回当前用户| |待执行| | |
TC-AUTH-005|认证与会话|REQ-AUTH-002; authRefresh|P0|异常|无有效刷新令牌|1. 提交篡改或过期 refreshToken|无效 refreshToken|返回鉴权失败；不签发新令牌| |待执行| | |
TC-AUTH-006|认证与会话|REQ-AUTH-003; authLogout|P0|安全|已登录|1. 调用登出 2. 使用原访问令牌请求 /me 3. 使用原刷新令牌刷新|有效会话令牌|会话失效，后续请求均被拒绝| |待执行| | |
TC-AUTH-007|认证与会话|REQ-AUTH-004; getCurrentUser|P0|安全|未登录|1. 直接访问 /me|无 Authorization|返回 401/鉴权错误；不返回用户数据| |待执行| | |
TC-AUTH-008|认证与会话|REQ-AUTH-005; getCurrentUserPermissions; getCurrentUserMenus|P1|正向|已登录不同权限账号|1. 分别调用权限和菜单接口 2. 比较前端侧栏|员工、主管、HR、管理员账号|接口权限集合与可见菜单一致；无权限菜单不显示| |待执行| | |
TC-PERM-001|权限与数据范围|REQ-PERM-001; listEmployees|P0|越权|普通员工已登录且存在其他部门员工|1. 查询员工列表 2. 尝试按其他部门筛选|普通员工 / 外部门 departmentId|仅返回授权数据范围内员工；不得泄露外部门员工| |待执行| | |
TC-PERM-002|权限与数据范围|REQ-PERM-002; getEmployee|P0|越权|普通员工已登录|1. 直接访问其他部门员工 ID|外部门 employeeId|返回 403 或无权资源，不返回档案字段| |待执行| | |
TC-PERM-003|权限与数据范围|REQ-PERM-003; adjustLeaveBalance|P0|越权|无 attendance:manage 账号已登录|1. 提交余额调整请求|有效 balanceId / 8 小时 / version|返回 403，余额与流水均不变化| |待执行| | |
TC-PERM-004|权限与数据范围|REQ-PERM-004; rebuildAttendanceMonthlySummaries|P0|越权|无 attendance:manage 账号已登录|1. 调用月度汇总重建|month=2026-07|返回 403，不产生或修改快照| |待执行| | |
TC-PERM-005|权限与数据范围|REQ-PERM-005; replaceUserRoles|P0|越权|非 system:manage 账号已登录|1. 替换任意用户角色|目标用户 / roleIds / version|返回 403，角色关系不变| |待执行| | |
TC-PERM-006|权限与数据范围|REQ-PERM-006; listWorkflowTasks|P0|越权|无 workflow:approve 权限账号|1. 查询审批任务 2. 尝试执行 approve|taskId / version|不可读取不属于自己的待办，也不可审批| |待执行| | |
TC-PERM-007|权限与数据范围|REQ-PERM-007; getDownloadUrl|P1|安全|已登录无附件访问权限账号|1. 请求其他员工附件下载地址|其他员工 fileId|拒绝访问；不得返回可下载 URL| |待执行| | |
TC-PERM-008|权限与数据范围|REQ-PERM-008|P1|回归|管理员已登录且已被回收某角色|1. 修改角色 2. 刷新会话 3. 检查菜单和接口|已回收的 attendance:manage|菜单、按钮和接口权限立即失效| |待执行| | |
TC-ORG-001|组织与人事|REQ-ORG-001; listDepartmentTree|P0|正向|管理员已登录且存在多层部门|1. 打开组织结构页 2. 查询部门树|根部门及两级子部门|按层级正确展示部门、负责人和状态| |待执行| | |
TC-ORG-002|组织与人事|REQ-ORG-002; createResource(departments)|P0|边界|管理员已登录|1. 创建根部门 2. 创建子部门|合法名称 / parentId|创建成功，子部门正确归属父部门| |待执行| | |
TC-ORG-003|组织与人事|REQ-ORG-003; createResource(departments)|P0|异常|存在 A -> B 部门关系|1. 将 A 的父级设为 B 或提交循环关系|A、B 部门 ID|拒绝形成循环，原组织树不变| |待执行| | |
TC-ORG-004|组织与人事|REQ-ORG-004; createPosition; updatePosition|P1|正向|管理员已登录|1. 新建岗位 2. 编辑名称和状态 3. 刷新列表|岗位编码、名称、版本|数据持久化，列表及员工表单均可选择有效岗位| |待执行| | |
TC-ORG-005|组织与人事|REQ-ORG-005; createRank; updateRank|P1|正向|管理员已登录|1. 新建职级 2. 编辑显示顺序 3. 刷新|职级编码、名称、版本|职级保存成功，排序与引用正常| |待执行| | |
TC-ORG-006|组织与人事|REQ-ORG-006; createEmployee|P0|正向|已存在有效部门、岗位、职级|1. 打开员工档案 2. 填写必填项 3. 保存|唯一工号、姓名、部门、岗位、入职日期|员工创建成功，列表显示正确任职信息| |待执行| | |
TC-ORG-007|组织与人事|REQ-ORG-007; createEmployee|P0|异常|已存在工号 EMP-TEST-001|1. 使用相同工号创建第二名员工|重复 employeeNo|返回明确重复错误；不创建重复档案| |待执行| | |
TC-ORG-008|组织与人事|REQ-ORG-008; updateEmployee|P0|并发|同一员工被两个管理员打开编辑|1. 管理员 A 保存 2. 管理员 B 用旧 version 保存|同一 employeeId / 旧 version|第二次保存返回 409 冲突，A 的修改保留| |待执行| | |
TC-CAL-001|工作日历|REQ-CAL-001; createWorkCalendar|P0|正向|attendance:manage 账号已登录|1. 新建年度日历 2. 设置时区、工作日、节假日 3. 保存|2026 / Asia/Shanghai / 节假日|创建成功，按年份查询返回完整日期配置| |待执行| | |
TC-CAL-002|工作日历|REQ-CAL-002; getWorkCalendarByYear|P1|异常|无对应年度日历|1. 查询不存在的年度|calendarYear=2099|返回空或明确不存在结果，不返回其他年度数据| |待执行| | |
TC-CAL-003|工作日历|REQ-CAL-003; updateWorkCalendar|P0|并发|两个管理员同时编辑同一日历|1. A 保存 2. B 用旧 version 保存|相同 calendarId / 旧 version|B 返回 409；不覆盖 A 的日期配置| |待执行| | |
TC-CAL-004|工作日历|REQ-CAL-004|P0|规则|存在周末、法定节假日和调休工作日|1. 创建覆盖三种日期的请假 2. 提交|周末 / 假日 / 调休日区间|请假时长按工作日历准确计算| |待执行| | |
TC-CAL-005|工作日历|REQ-CAL-005; createWorkCalendar|P1|校验|attendance:manage 账号已登录|1. 提交非法年份或重复日期|年份 1999 / 重复 date|参数校验失败，数据不落库| |待执行| | |
TC-LEAVE-001|请假管理|REQ-LEAVE-001; createResource(leave-requests)|P0|正向|员工有充足年假余额且存在有效请假模板|1. 创建草稿 2. 填写请假类型、时间、原因|工作日 8 小时 / 年假|草稿保存，时长准确计算，状态为 DRAFT| |待执行| | |
TC-LEAVE-002|请假管理|REQ-LEAVE-002; submitLeaveRequest|P0|正向|存在可提交请假草稿|1. 提交申请 2. 查看我的申请和待办|草稿 id / version|状态变为 IN_PROGRESS，生成对应审批实例与当前待办| |待执行| | |
TC-LEAVE-003|请假管理|REQ-LEAVE-003; createResource(leave-requests)|P0|异常|员工已有已提交或已通过请假|1. 创建与既有区间重叠的申请|重叠时间段|系统拒绝重叠申请，不新增有效业务单| |待执行| | |
TC-LEAVE-004|请假管理|REQ-LEAVE-004; submitLeaveRequest|P0|异常|员工余额小于申请时长|1. 创建超余额草稿 2. 提交|余额 4 小时 / 申请 8 小时|提交被拒绝，余额与流水不变化| |待执行| | |
TC-LEAVE-005|请假管理|REQ-LEAVE-005; submitLeaveRequest|P0|异常|目标员工为离职状态|1. 以离职员工会话创建并提交请假|离职员工 / 任意工作日|系统拒绝提交，不启动审批| |待执行| | |
TC-LEAVE-006|请假管理|REQ-LEAVE-006; submitLeaveRequest|P0|异常|部门没有匹配的启用请假流程模板|1. 提交草稿|有效草稿|拒绝提交并提示无可用审批模板| |待执行| | |
TC-LEAVE-007|请假管理|REQ-LEAVE-007; approveWorkflowTask|P0|正向|请假申请处于审批中，审批人有待办|1. 审批通过 2. 查看申请、余额、流水|taskId / version|申请变 APPROVED，余额按规则扣减，新增不可篡改流水| |待执行| | |
TC-LEAVE-008|请假管理|REQ-LEAVE-008; rejectWorkflowTask|P0|正向|请假申请处于审批中|1. 审批驳回 2. 查看申请和余额|taskId / 驳回意见|申请变 REJECTED，余额不扣减，保留审批意见日志| |待执行| | |
TC-LEAVE-009|请假管理|REQ-LEAVE-009; cancelLeaveRequest|P0|正向|草稿或允许撤销的申请存在|1. 取消申请 2. 再次查看列表|requestId / version|状态变 CANCELLED；需要恢复的余额及流水保持一致| |待执行| | |
TC-LEAVE-010|请假管理|REQ-LEAVE-010; approveWorkflowTask|P0|并发|两个审批会话同时处理同一任务|1. 会话 A 通过 2. 会话 B 再通过|同一 taskId / version|仅一次审批成功；第二次返回冲突或无效状态；仅一条余额流水| |待执行| | |
TC-LEAVE-011|请假管理|REQ-LEAVE-011; withdrawWorkflowInstance|P1|正向|申请流程未结束且发起人可撤回|1. 撤回流程 2. 查看待办和状态|workflowInstanceId / version|流程撤回，待办失效，申请回到规则允许的状态且数据一致| |待执行| | |
TC-LEAVE-012|请假管理|REQ-LEAVE-012|P1|界面|网络故障或接口返回校验错误|1. 断网或模拟 4xx/5xx 2. 提交表单|非法必填项 / 网络错误|表单提示明确，不丢失已输入内容，不出现无限加载| |待执行| | |
TC-BAL-001|假期余额|REQ-BAL-001; listMyLeaveBalances|P0|正向|员工已登录并有多种余额|1. 打开假期余额页面|员工账户|仅显示本人各类型余额、可用与冻结时数| |待执行| | |
TC-BAL-002|假期余额|REQ-BAL-002; listEmployeeLeaveBalances|P0|权限|HR 管理员与普通员工分别登录|1. 查询指定员工余额|employeeId|HR 可查询授权范围；普通员工无权查询他人| |待执行| | |
TC-BAL-003|假期余额|REQ-BAL-003; adjustLeaveBalance|P0|正向|attendance:manage 账号和目标余额存在|1. 调整余额 2. 填写原因 3. 查询流水|+8 小时 / 合法 version|余额更新，变动流水含类型、前后值、原因、操作者| |待执行| | |
TC-BAL-004|假期余额|REQ-BAL-004; adjustLeaveBalance|P0|异常|目标余额存在|1. 提交导致可用余额小于零的调整|负向超额小时|拒绝调整，余额和流水不变| |待执行| | |
TC-BAL-005|假期余额|REQ-BAL-005; adjustLeaveBalance|P0|并发|两名管理员读取同一余额|1. A 调整并保存 2. B 用旧 version 调整|旧 version|B 返回 409，不覆盖 A 的结果| |待执行| | |
TC-BAL-006|假期余额|REQ-BAL-006; listLeaveBalanceChanges|P1|审计|已发生请假审批、撤销、人工调整|1. 查询余额变动流水|balanceId|流水完整、时间顺序正确且历史记录不可编辑| |待执行| | |
TC-OT-001|加班管理|REQ-OT-001; createOvertimeRequestDraft|P0|正向|员工已登录且存在有效加班模板|1. 新建加班草稿 2. 输入起止时间和补偿方式|工作日 18:00-21:00 / TIME_OFF|草稿保存，时长为 3 小时，状态为 DRAFT| |待执行| | |
TC-OT-002|加班管理|REQ-OT-002; createOvertimeRequestDraft|P0|校验|员工已登录|1. 提交结束时间早于开始时间的草稿|endTime < startTime|参数校验失败，不创建草稿| |待执行| | |
TC-OT-003|加班管理|REQ-OT-003; submitOvertimeRequest|P0|正向|存在有效加班草稿|1. 提交 2. 查看审批待办|requestId / version|状态为 IN_PROGRESS，审批实例和待办生成| |待执行| | |
TC-OT-004|加班管理|REQ-OT-004; approveWorkflowTask|P0|正向|调休类加班正在审批|1. 审批通过 2. 查询对应调休余额|TIME_OFF / taskId|加班状态 APPROVED，调休余额或变动按规则更新且仅一次| |待执行| | |
TC-OT-005|加班管理|REQ-OT-005; approveWorkflowTask|P1|正向|加班补偿方式为 OVERTIME_PAY|1. 审批通过 2. 查看加班记录|OVERTIME_PAY / taskId|加班审批通过，不错误增加调休余额| |待执行| | |
TC-OT-006|加班管理|REQ-OT-006; cancelOvertimeRequest|P0|正向|存在可取消加班申请|1. 取消 2. 查询申请与待办|requestId / version|状态变 CANCELLED；待办关闭；必要的调休变动被正确回退| |待执行| | |
TC-OT-007|加班管理|REQ-OT-007; listMyOvertimeRequests|P1|数据范围|员工 A 与 B 均有加班单|1. 以 A 登录查询列表|员工 A 会话|仅返回 A 自己的加班申请| |待执行| | |
TC-WF-001|审批模板|REQ-WF-001; createWorkflowTemplate|P0|正向|workflow:manage 账号已登录|1. 新建请假模板 2. 配置部门范围和节点 3. 启用|LEAVE / 直属主管 + HR|模板保存成功，节点顺序和适用范围正确| |待执行| | |
TC-WF-002|审批模板|REQ-WF-002; createWorkflowTemplate|P0|校验|workflow:manage 账号已登录|1. 提交无节点或重复 nodeNo 的模板|nodes=[] / 重复序号|校验失败，不创建不可运行模板| |待执行| | |
TC-WF-003|审批模板|REQ-WF-003; updateWorkflowTemplate|P0|并发|两个管理员编辑同一模板|1. A 更新 2. B 用旧 version 更新|同一模板 / 旧 version|B 返回 409，A 的版本保留| |待执行| | |
TC-WF-004|审批中心|REQ-WF-004; listWorkflowTasks|P0|正向|审批人有待办与已办|1. 打开审批中心 2. 切换待办/已办条件|审批人账号|任务状态、业务类型、申请人、时间及分页符合筛选条件| |待执行| | |
TC-WF-005|审批中心|REQ-WF-005; approveWorkflowTask|P0|正向|当前审批人持有有效待办|1. 填写意见 2. 点击通过 3. 查询实例详情|taskId / version / 意见|任务完成，流转下一节点或结束；完整动作日志可追溯| |待执行| | |
TC-WF-006|审批中心|REQ-WF-006; rejectWorkflowTask|P0|正向|当前审批人持有有效待办|1. 填写驳回意见 2. 驳回|taskId / version|实例和业务单据进入 REJECTED，日志记录操作人与意见| |待执行| | |
TC-WF-007|审批中心|REQ-WF-007; returnWorkflowTask|P1|正向|流程存在可退回的前节点|1. 选择退回节点 2. 提交意见|taskId / target node|任务退回指定节点，后续待办状态正确| |待执行| | |
TC-WF-008|审批中心|REQ-WF-008; transferWorkflowTask|P1|正向|审批人拥有转交权限或符合规则|1. 选择受让人 2. 转交|taskId / targetUserId|原待办关闭，新受让人可见待办，日志完整| |待执行| | |
TC-WF-009|审批中心|REQ-WF-009; getWorkflowInstance|P0|审计|流程已发生多个动作|1. 打开实例详情|workflowInstanceId|展示节点快照、当前状态、操作流水和业务关联，历史不因模板修改而改变| |待执行| | |
TC-ATT-001|月度假勤汇总|REQ-ATT-001; rebuildAttendanceMonthlySummaries|P0|正向|attendance:manage 账号；当月有已通过请假、加班和待审批申请|1. 打开月度汇总 2. 选择月份 3. 点击重建|month=2026-07|返回 affectedRows；为授权数据范围内员工创建或更新快照| |待执行| | |
TC-ATT-002|月度假勤汇总|REQ-ATT-002; listAttendanceMonthlySummaries|P0|正向|已重建指定月份|1. 查询月度汇总|month=2026-07|返回工号、员工、部门、请假、加班、调休净变动、待审批、生成时间| |待执行| | |
TC-ATT-003|月度假勤汇总|REQ-ATT-003; listAttendanceMonthlySummaries|P0|口径|已准备请假 8 小时、加班 3 小时、调休变动和 1 个待审批单|1. 重建 2. 对照源申请与余额流水|目标员工 / 2026-07|快照字段与原始数据口径一致：仅聚合当月、已通过及规则要求的数据| |待执行| | |
TC-ATT-004|月度假勤汇总|REQ-ATT-004; listAttendanceMonthlySummaries|P1|筛选|同月存在多个部门和员工快照|1. 按 departmentId 查询 2. 按 employeeId 查询 3. 组合查询|合法 departmentId / employeeId|每个筛选结果准确；组合筛选取交集| |待执行| | |
TC-ATT-005|月度假勤汇总|REQ-ATT-005; listAttendanceMonthlySummaries|P0|校验|attendance:manage 账号已登录|1. 查询非法 month|2026-13、2026-7、空值|返回 400/校验错误；不返回错误月份数据| |待执行| | |
TC-ATT-006|月度假勤汇总|REQ-ATT-006; rebuildAttendanceMonthlySummaries|P0|幂等|已完成当月首次重建|1. 连续两次重建同一月份 2. 查询快照|month=2026-07|同一员工每月仅一条快照；数据更新而非重复插入| |待执行| | |
TC-ATT-007|月度假勤汇总|REQ-ATT-007; rebuildAttendanceMonthlySummaries|P0|更新|当月新增批准请假或加班后|1. 首次重建 2. 新增源单据并完成审批 3. 再次重建|同月新业务单据|相关员工快照数值和生成时间更新，其他员工数据不受影响| |待执行| | |
TC-ATT-008|月度假勤汇总|REQ-ATT-008; rebuildAttendanceMonthlySummaries|P0|权限|普通员工与无权限 HR 已登录|1. 直接访问列表 2. 调用重建接口 3. 尝试打开路由|无 attendance:manage 权限|侧栏不显示入口；接口均返回 403；无数据泄露| |待执行| | |
TC-ATT-009|月度假勤汇总|REQ-ATT-009|P1|界面|attendance:manage 账号已登录|1. 打开页面 2. 切换月份 3. 清空部门和员工筛选 4. 点击刷新|合法月值、部门/员工 ID|加载态正确；表格刷新与筛选同步；空结果展示空状态| |待执行| | |
TC-ATT-010|月度假勤汇总|REQ-ATT-010|P1|异常|模拟查询或重建接口失败|1. 断网或返回 500 2. 执行查询/重建|网络异常 / 500|提示加载或重建失败；按钮恢复可用；已有表格数据不被错误覆盖| |待执行| | |
TC-SYS-001|系统访问控制|REQ-SYS-001; listSystemRoles|P0|正向|system:manage 账号已登录|1. 打开系统管理 2. 查询角色|管理员账号|显示角色编码、名称、状态和权限信息| |待执行| | |
TC-SYS-002|系统访问控制|REQ-SYS-002; listSystemUsers|P0|正向|system:manage 账号已登录|1. 查询用户列表 2. 搜索已知账号|username|返回准确用户及角色；不暴露密码哈希、令牌等敏感字段| |待执行| | |
TC-SYS-003|系统访问控制|REQ-SYS-003; replaceUserRoles|P0|正向|管理员与目标用户存在|1. 替换目标用户角色 2. 目标用户刷新会话 3. 获取权限菜单|roleIds / version|角色关系更新；新权限生效；旧权限被回收| |待执行| | |
TC-SYS-004|系统访问控制|REQ-SYS-004; replaceUserRoles|P0|并发|两名管理员同时分配角色|1. A 保存角色分配 2. B 用旧 version 保存|同一用户 / 旧 version|后提交请求返回 409，角色关系不被静默覆盖| |待执行| | |
TC-SYS-005|系统访问控制|REQ-SYS-005|P1|安全|普通员工已登录|1. 直接访问 /system/users 页面和接口|普通员工会话|页面路由受保护，接口返回 403| |待执行| | |
TC-UI-001|前端通用体验|REQ-UI-001|P0|正向|前后端均已启动|1. 登录 2. 逐一访问已实现菜单|Chrome/Edge 最新版|工作台、组织、请假、加班、日历、余额、月度汇总、审批、模板、系统页均可打开| |待执行| | |
TC-UI-002|前端通用体验|REQ-UI-002|P1|兼容性|前后端均已启动|1. 使用 1280x720、1440x900 和移动宽度访问关键页|三种视口|导航、表格、抽屉和表单无遮挡；可滚动且主要操作可用| |待执行| | |
TC-UI-003|前端通用体验|REQ-UI-003|P1|异常|模拟接口超时、401、403、409、500|1. 在列表和编辑页触发各响应|接口错误码|显示有区分的提示；401 回登录；409 提示刷新或重试| |待执行| | |
TC-UI-004|前端通用体验|REQ-UI-004|P1|安全|已登录低权限用户|1. 手动输入无权路由 2. 刷新浏览器|/attendance/summary、/system/users|前端不展示受限内容；后端继续阻断接口| |待执行| | |
TC-UI-005|前端通用体验|REQ-UI-005|P1|可用性|列表页有数据、无数据和加载中状态|1. 切换筛选 2. 刷新页面 3. 清空筛选|各业务列表|加载、空态、错误态文案清晰；不会显示过期数据| |待执行| | |
TC-UI-006|接口契约与追踪|REQ-UI-006; OpenAPI|P0|回归|后端已启动|1. 执行契约测试 2. 对写操作检查 traceId 3. 记录迁移版本|OpenAPI / Flyway V011|接口与 OpenAPI 一致；错误响应可关联 traceId；V011 迁移成功| |待执行| | |
'@ -split "`r?`n" | Where-Object { $_.Trim() }

$caseRows = [System.Collections.Generic.List[object]]::new()
$caseRows.Add(@((Cell 'HRPM 功能测试用例' 4), (Cell '版本：2026-07-13；范围：当前已实现功能 + 月度假勤汇总接入回归' 0)))
$caseRows.Add(@($headers | ForEach-Object { Cell $_ 4 }))
foreach ($line in $rawCases) {
    $parts = $line -split '\|', -1
    if ($parts.Count -ne $headers.Count) { throw "Invalid test case row: $line" }
    $caseRows.Add(@($parts | ForEach-Object { Cell $_ 0 }))
}

$overviewRows = @(
    @((Cell 'HRPM 功能测试执行说明' 4), (Cell '生成日期：2026-07-13' 0)),
    @((Cell '项目现状' 4), (Cell '前端 http://localhost:5173 返回 200；后端 http://localhost:8080/api/v1/health 返回 UP；MySQL 3306 正在监听。' 0)),
    @((Cell '开发进度' 4), (Cell '认证、组织员工、工作日历、请假、加班、余额、审批模板/任务、系统访问控制已有前后端实现；绩效、目标、报表、文件接口仍以契约/占位为主。' 0)),
    @((Cell '本轮变更' 4), (Cell '月度假勤汇总正在接入：V011 新增 rpt_attendance_month，后端列表/重建接口、前端汇总页、路由和 OpenAPI 均存在未提交变更。' 0)),
    @((Cell '自动化基线' 4), (Cell '现有 Surefire 报告：65 项通过，0 失败，0 错误，1 项 Testcontainers 迁移测试跳过；月度汇总尚无专属自动化测试。' 0)),
    @((Cell '执行方式' 4), (Cell '逐条执行“功能测试用例”；将实测结果、状态、缺陷编号、执行人、执行日期填入对应列。状态建议：通过、失败、阻塞、不适用。' 0)),
    @((Cell '质量门禁' 4), (Cell 'P0 用例全部通过；无阻断/严重缺陷；权限、并发、数据口径、接口契约及 V011 迁移回归通过后，方可验收。' 0)),
    @((Cell '说明' 4), (Cell '“REQ”编号用于测试追溯；对当前尚未落地的契约性能力，仅作为后续功能完成后的验收入口，不应误判为已实现。' 0))
)

$dataRows = @(
    @((Cell '测试数据与角色' 4), (Cell '执行前先创建或核对以下虚构数据；不要使用真实人事数据。' 0)),
    @((Cell '类别' 4), (Cell '标识' 4), (Cell '建议数据' 4), (Cell '用途' 4)),
    @((Cell '角色' 0), (Cell 'ADMIN' 0), (Cell 'system:manage、org:manage、attendance:manage、workflow:manage、workflow:approve' 0), (Cell '管理、重建、配置和审批正向用例' 0)),
    @((Cell '角色' 0), (Cell 'EMP-A' 0), (Cell '普通在职员工，部门 A，余额充足' 0), (Cell '个人数据隔离、请假/加班正向用例' 0)),
    @((Cell '角色' 0), (Cell 'EMP-B' 0), (Cell '普通在职员工，部门 B' 0), (Cell '越权与跨部门隔离用例' 0)),
    @((Cell '角色' 0), (Cell 'MANAGER-A' 0), (Cell '部门 A 直属主管，具有 workflow:approve' 0), (Cell '请假/加班审批、转交/退回用例' 0)),
    @((Cell '角色' 0), (Cell 'HR-READ' 0), (Cell '仅 attendance:read，无 attendance:manage' 0), (Cell '月度汇总和余额管理越权用例' 0)),
    @((Cell '组织' 0), (Cell 'DEPT-ROOT / DEPT-A / DEPT-B' 0), (Cell '根部门下至少两个子部门' 0), (Cell '组织树、数据范围和筛选用例' 0)),
    @((Cell '员工' 0), (Cell 'EMP-TEST-001' 0), (Cell '在职、部门 A、有效岗位和职级' 0), (Cell '创建重复工号、请假、加班、汇总用例' 0)),
    @((Cell '员工' 0), (Cell 'EMP-RESIGNED-001' 0), (Cell '离职状态员工' 0), (Cell '离职员工不得提交申请用例' 0)),
    @((Cell '日历' 0), (Cell 'CAL-2026' 0), (Cell '含工作日、周末、节假日和调休工作日' 0), (Cell '时长计算和日期规则用例' 0)),
    @((Cell '余额' 0), (Cell 'LB-ANNUAL-A' 0), (Cell '年假 16 小时；另准备 4 小时不足余额' 0), (Cell '余额扣减、调整、并发和不足用例' 0)),
    @((Cell '流程' 0), (Cell 'WF-LEAVE-A' 0), (Cell '部门 A 的启用请假模板：直属主管 -> HR' 0), (Cell '请假提交和审批闭环用例' 0)),
    @((Cell '流程' 0), (Cell 'WF-OT-A' 0), (Cell '部门 A 的启用加班模板' 0), (Cell '加班提交和审批闭环用例' 0)),
    @((Cell '汇总口径' 0), (Cell '2026-07' 0), (Cell 'EMP-A：已批请假 8h、已批调休加班 3h、待审 1 单' 0), (Cell '月度汇总数值、重建和幂等用例' 0))
)

$summaryRows = @(
    @((Cell '执行汇总' 4), (Cell '以下指标引用“功能测试用例”工作表；填写执行状态后自动更新。' 0)),
    @((Cell '统计项' 4), (Cell '数量' 4), (Cell '说明' 4)),
    @((Cell '用例总数' 0), (Formula "COUNTA('功能测试用例'!A3:A200" 10), (Cell '已编写的功能测试用例数' 0)),
    @((Cell 'P0 用例数' 0), (Formula "COUNTIF('功能测试用例'!D3:D200,`"P0`")" 10), (Cell '阻断级核心用例' 0)),
    @((Cell '待执行' 0), (Formula "COUNTIF('功能测试用例'!K3:K200,`"待执行`")" 10), (Cell '尚未填写执行结果' 0)),
    @((Cell '通过' 0), (Formula "COUNTIF('功能测试用例'!K3:K200,`"通过`")" 10), (Cell '执行通过的用例' 0)),
    @((Cell '失败' 0), (Formula "COUNTIF('功能测试用例'!K3:K200,`"失败`")" 10), (Cell '执行失败的用例' 0)),
    @((Cell '阻塞' 0), (Formula "COUNTIF('功能测试用例'!K3:K200,`"阻塞`")" 10), (Cell '因环境/依赖无法执行的用例' 0)),
    @((Cell '执行完成率' 0), (Formula "IF(B3=0,0,(B6+B7+B8)/B3)" 8), (Cell '通过、失败、阻塞占总用例比例' 0)),
    @((Cell '通过率' 0), (Formula "IF((B6+B7)=0,0,B6/(B6+B7))" 8), (Cell '仅按已判定通过/失败计算' 0)),
    @((Cell '发布建议' 4), (Cell 'P0 全部通过且无阻断/严重缺陷后，才建议进入验收。' 0), (Cell '人工判定' 0))
)

$script:strings = [System.Collections.Generic.List[string]]::new()
$script:stringIndex = @{}
Write-Sheet 'sheet1.xml' $overviewRows @(24, 120) 0
Write-Sheet 'sheet2.xml' $caseRows @(16, 16, 34, 9, 12, 34, 60, 28, 60, 36, 12, 14, 12, 14) 2
Write-Sheet 'sheet3.xml' $dataRows @(16, 24, 72, 50) 2
Write-Sheet 'sheet4.xml' $summaryRows @(24, 16, 48) 2

$stringXml = "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?><sst xmlns=`"http://schemas.openxmlformats.org/spreadsheetml/2006/main`" count=`"$($script:strings.Count)`" uniqueCount=`"$($script:strings.Count)`">"
foreach ($text in $script:strings) { $stringXml += "<si><t xml:space=`"preserve`">$(Xml-Escape $text)</t></si>" }
$stringXml += '</sst>'
[System.IO.File]::WriteAllText((Join-Path $work 'xl/sharedStrings.xml'), $stringXml, [System.Text.UTF8Encoding]::new($false))

$workbookXml = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <fileVersion appName="xl" lastEdited="7" lowestEdited="7"/>
  <workbookPr defaultThemeVersion="166925"/>
  <bookViews><workbookView xWindow="0" yWindow="0" windowWidth="20140" windowHeight="10960"/></bookViews>
  <sheets>
    <sheet name="执行说明" sheetId="1" r:id="rId1"/>
    <sheet name="功能测试用例" sheetId="2" r:id="rId4"/>
    <sheet name="测试数据与角色" sheetId="3" r:id="rId5"/>
    <sheet name="执行汇总" sheetId="4" r:id="rId6"/>
  </sheets>
  <calcPr calcId="191029" fullCalcOnLoad="1" forceFullCalc="1"/>
</workbook>
'@
[System.IO.File]::WriteAllText((Join-Path $work 'xl/workbook.xml'), $workbookXml, [System.Text.UTF8Encoding]::new($false))

$relsXml = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
</Relationships>
'@
[System.IO.File]::WriteAllText((Join-Path $work 'xl/_rels/workbook.xml.rels'), $relsXml, [System.Text.UTF8Encoding]::new($false))

$typesXml = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>
'@
[System.IO.File]::WriteAllText((Join-Path $work '[Content_Types].xml'), $typesXml, [System.Text.UTF8Encoding]::new($false))

New-Item -ItemType Directory -Force (Split-Path -Parent $OutputPath) | Out-Null
$env:PYTHONIOENCODING = 'utf-8'
& python "$skillRoot/scripts/xlsx_pack.py" $work $OutputPath
