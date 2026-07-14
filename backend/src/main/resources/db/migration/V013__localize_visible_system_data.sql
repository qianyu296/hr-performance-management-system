UPDATE sys_role
SET name = CASE code
    WHEN 'SUPER_ADMIN' THEN '系统超级管理员'
    WHEN 'DEV_TEST_ADMIN' THEN '开发测试管理员'
    WHEN 'DEV_EMPLOYEE' THEN '开发测试员工'
    WHEN 'EMP_TEST_ORG' THEN '员工组织测试'
    WHEN 'OT_SUBMIT' THEN '加班申请'
    WHEN 'TEST_ATTENDANCE_SUBMIT' THEN '假勤申请测试'
    WHEN 'TEST_ORG' THEN '组织测试'
    WHEN 'TEST_ORG_MANAGER' THEN '组织管理员测试'
    ELSE name
END
WHERE deleted = 0;

UPDATE sys_menu
SET name = CASE permission_code
    WHEN 'org:read' THEN '组织查看'
    WHEN 'org:manage' THEN '组织管理'
    WHEN 'attendance:submit' THEN '假勤申请'
    WHEN 'attendance:manage' THEN '假勤管理'
    WHEN 'attendance:balance:adjust' THEN '假期余额调整'
    WHEN 'workflow:approve' THEN '流程审批'
    WHEN 'workflow:manage' THEN '流程管理'
    WHEN 'workflow:intervene' THEN '流程干预'
    WHEN 'performance:config' THEN '绩效配置'
    WHEN 'system:manage' THEN '系统管理'
    ELSE name
END
WHERE deleted = 0;

UPDATE att_leave_type
SET name = CASE code
    WHEN 'ANNUAL' THEN '年假'
    WHEN 'DEV_ANNUAL' THEN '年假'
    WHEN 'SUMMARY_ANNUAL' THEN '年假（汇总）'
    ELSE name
END
WHERE deleted = 0;
