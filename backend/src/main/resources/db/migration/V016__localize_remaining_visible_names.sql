UPDATE hr_rank
SET name = CASE code
    WHEN 'EMP_TEST_RANK' THEN '五级职级'
    WHEN 'TEST_P5' THEN '高级五级职级'
    ELSE name
END
WHERE deleted = 0;

UPDATE sys_menu
SET name = CASE permission_code
    WHEN 'org:read' THEN '组织读取'
    WHEN 'org:manage' THEN '组织管理'
    ELSE name
END
WHERE deleted = 0;
