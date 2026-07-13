INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000016, 'Workflow Intervene', 'workflow:intervene', 'BUTTON', '/workflow/tasks', 42, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000036, 9000002, id FROM sys_menu WHERE permission_code = 'workflow:intervene' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
