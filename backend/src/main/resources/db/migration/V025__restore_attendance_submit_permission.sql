INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000013, '假勤申请', 'attendance:submit', 'BUTTON', '/attendance/leave', 30, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    menu_type = VALUES(menu_type),
    route_path = VALUES(route_path),
    sort_no = VALUES(sort_no),
    status = 'ACTIVE',
    deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000053, 9000052, id FROM sys_menu
WHERE permission_code = 'attendance:submit' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000302, 9000054, id FROM sys_menu
WHERE permission_code = 'attendance:submit' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
