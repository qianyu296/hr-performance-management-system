INSERT INTO hr_department (id, code, name, `path`, effective_date, status)
VALUES (100101, 'DEV-HR', 'Dev HR Department', '/100101/', '2026-01-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), `path` = VALUES(`path`), status = VALUES(status);

INSERT INTO hr_position (id, code, name, status)
VALUES (100102, 'DEV-HR-SPECIALIST', 'HR Specialist', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);

INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, employment_status, hire_date)
VALUES (100103, 'DEV-EMP-001', 'Dev Employee', 100101, 100102, 'FORMAL', '2025-01-01')
ON DUPLICATE KEY UPDATE name = VALUES(name), department_id = VALUES(department_id), position_id = VALUES(position_id), employment_status = VALUES(employment_status);

INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version)
VALUES (100104, 'dev-employee', '$2b$12$Ey4N70YF5vNcTBg.2/0pN.8x9t0OICtVgfeQj4I2pnU.dAI6Al1dO', 100103, 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), employee_id = VALUES(employee_id), status = VALUES(status), session_version = VALUES(session_version);

INSERT INTO sys_role (id, code, name, status)
VALUES (100105, 'DEV_EMPLOYEE', 'Dev Employee', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);

INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
VALUES (100106, 'Leave Submit', 'attendance:submit', 'BUTTON', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (100107, 100104, 100105)
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100108, 100105, id FROM sys_menu WHERE permission_code = 'attendance:submit' AND deleted = 0 LIMIT 1
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, annual_quota, status)
VALUES (100109, 'DEV_ANNUAL', 'Annual Leave', 1, 1.00, 80.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), deduct_balance = VALUES(deduct_balance), min_unit_hours = VALUES(min_unit_hours), annual_quota = VALUES(annual_quota), status = VALUES(status);

INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours, frozen_hours)
VALUES (100110, 100103, 'DEV_ANNUAL', 2026, 80.00, 0.00)
ON DUPLICATE KEY UPDATE available_hours = VALUES(available_hours), frozen_hours = VALUES(frozen_hours);

INSERT INTO wf_template (id, code, name, business_type, priority, template_version, status)
VALUES (100111, 'DEV_LEAVE', 'Dev Leave Approval', 'LEAVE', 100, 1, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), priority = VALUES(priority), status = VALUES(status);

INSERT INTO wf_template_scope (id, template_id, department_id)
VALUES (100112, 100111, 100101)
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO wf_template_node (id, template_id, node_no, node_type, approver_rule)
VALUES (100113, 100111, 1, 'SPECIFIC_USER', JSON_OBJECT('userId', 100104))
ON DUPLICATE KEY UPDATE approver_rule = VALUES(approver_rule), deleted = 0;
