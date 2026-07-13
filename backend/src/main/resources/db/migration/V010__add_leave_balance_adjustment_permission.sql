INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status) VALUES
  (9000018, 'Leave Balance Adjust', 'attendance:balance:adjust', 'BUTTON', '/attendance/balances', 32, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000038, 9000002, id FROM sys_menu WHERE permission_code = 'attendance:balance:adjust' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
