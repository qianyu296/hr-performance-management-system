-- Development bootstrap account. Change its password immediately outside local development.
INSERT INTO sys_user (id, username, password_hash, status, session_version)
VALUES (9000001, 'admin', '$2a$10$sdmbAyd9lCq37Nzkg7IzV.JzLLzkGUB.il9OfqiEB6GB7sLkogrbC', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role (id, code, name, status)
VALUES (9000002, 'SUPER_ADMIN', 'Initial Administrator', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status) VALUES
  (9000010, 'System Manage', 'system:manage', 'BUTTON', '/system/users', 10, 'ACTIVE'),
  (9000011, 'Organization Read', 'org:read', 'BUTTON', '/org/employees', 20, 'ACTIVE'),
  (9000012, 'Organization Manage', 'org:manage', 'BUTTON', '/org/employees', 21, 'ACTIVE'),
  (9000013, 'Attendance Submit', 'attendance:submit', 'BUTTON', '/attendance/leave', 30, 'ACTIVE'),
  (9000014, 'Workflow Approve', 'workflow:approve', 'BUTTON', '/workflow/tasks', 40, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (9000020, 9000001, 9000002)
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000030, 9000002, id FROM sys_menu WHERE permission_code = 'system:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000031, 9000002, id FROM sys_menu WHERE permission_code = 'org:read' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000032, 9000002, id FROM sys_menu WHERE permission_code = 'org:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000033, 9000002, id FROM sys_menu WHERE permission_code = 'attendance:submit' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000034, 9000002, id FROM sys_menu WHERE permission_code = 'workflow:approve' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_data_scope (id, role_id, scope_type)
VALUES (9000040, 9000002, 'ALL')
ON DUPLICATE KEY UPDATE deleted = 0;
