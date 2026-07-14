ALTER TABLE sys_user ADD COLUMN password_change_required TINYINT NOT NULL DEFAULT 0 AFTER session_version;

INSERT INTO sys_role (id, code, name, status)
VALUES (9000052, 'EMPLOYEE_SELF_SERVICE', '员工自助服务', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000053, 9000052, id FROM sys_menu
WHERE permission_code = 'attendance:submit' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
