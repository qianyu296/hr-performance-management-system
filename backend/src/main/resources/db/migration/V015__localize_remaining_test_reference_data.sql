UPDATE sys_role
SET name = CASE code
    WHEN 'TEST_ATTENDANCE_SUBMIT' THEN '假勤申请测试'
    WHEN 'TEST_BALANCE_ADJUST' THEN '余额调整测试'
    WHEN 'TEST_ORG' THEN '组织测试'
    WHEN 'EMP_TEST_SELF' THEN '员工本人数据测试'
    WHEN 'LEAVE_TYPE_HR' THEN '请假类型人事专员'
    WHEN 'SYSTEM_ACCESS_ADMIN' THEN '权限管理测试管理员'
    WHEN 'SYSTEM_ACCESS_INITIAL' THEN '初始测试角色'
    WHEN 'SYSTEM_ACCESS_REPLACEMENT' THEN '替换测试角色'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_department
SET name = CASE code
    WHEN 'TEST' THEN '测试部门'
    WHEN 'LT_TEST' THEN '请假类型测试部门'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_position
SET name = CASE code
    WHEN 'TEST_POSITION' THEN '测试岗位'
    WHEN 'LT_TEST' THEN '请假类型测试岗位'
    WHEN 'TEST_STALE' THEN '岗位版本测试'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_rank
SET name = CASE code
    WHEN 'TEST_P5' THEN '高级P5职级'
    ELSE name
END
WHERE deleted = 0;
