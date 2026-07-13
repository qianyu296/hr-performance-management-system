INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000040, 'Performance configuration', 'performance:config', 'BUTTON', NULL, 100, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), sort_no = VALUES(sort_no), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000041, 9000002, id FROM sys_menu
WHERE permission_code = 'performance:config' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
