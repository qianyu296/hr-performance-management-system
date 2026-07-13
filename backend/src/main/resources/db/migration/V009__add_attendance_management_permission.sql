INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000017, 'Attendance Manage', 'attendance:manage', 'BUTTON', '/attendance/calendar', 31, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000037, 9000002, id FROM sys_menu WHERE permission_code = 'attendance:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
