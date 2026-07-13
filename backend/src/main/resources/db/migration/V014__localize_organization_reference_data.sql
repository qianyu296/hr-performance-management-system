UPDATE sys_role
SET name = CASE code
    WHEN 'DEV_EMPLOYEE' THEN '开发测试员工'
    WHEN 'DEV_TEST_ADMIN' THEN '开发测试管理员'
    WHEN 'EMP_TEST_ORG' THEN '员工组织测试'
    WHEN 'OT_SUBMIT' THEN '加班申请'
    WHEN 'SUPER_ADMIN' THEN '系统超级管理员'
    WHEN 'TEST_ATTENDANCE_SUBMIT' THEN '假勤申请测试'
    WHEN 'TEST_ORG' THEN '组织测试'
    WHEN 'TEST_ORG_MANAGER' THEN '组织管理员测试'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_department
SET name = CASE code
    WHEN 'HQ' THEN '总部'
    WHEN 'ENG' THEN '研发部'
    WHEN 'TEST' THEN '测试部门'
    WHEN 'EMP_TEST_DEPT' THEN '测试部门'
    WHEN 'OT_TEST' THEN '加班测试部门'
    WHEN 'SUMMARY_TEST' THEN '月度汇总测试部门'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_position
SET name = CASE code
    WHEN 'DEV-HR-SPECIALIST' THEN '人力资源专员'
    WHEN 'EMP_TEST_POSITION' THEN '测试岗位'
    WHEN 'OT_POSITION' THEN '加班测试岗位'
    WHEN 'SUMMARY_POSITION' THEN '月度汇总测试岗位'
    WHEN 'TEST_POSITION' THEN '测试岗位'
    WHEN 'LT_TEST' THEN '请假类型测试岗位'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_rank
SET name = CASE code
    WHEN 'EMP_TEST_RANK' THEN 'P5职级'
    WHEN 'TEST_P5' THEN '高级P5职级'
    ELSE name
END
WHERE deleted = 0;
