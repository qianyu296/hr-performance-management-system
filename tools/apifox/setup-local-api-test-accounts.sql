-- Local-only Apifox regression identities. Password for every account: admin123.
-- This script only owns the 990000x identity range and APIFOX_ master data.
START TRANSACTION;

SET @password_hash = '$2a$10$sdmbAyd9lCq37Nzkg7IzV.JzLLzkGUB.il9OfqiEB6GB7sLkogrbC';

-- Clear prior APIFOX workflow templates so each run resolves a single deterministic template.
DELETE FROM wf_template_node
WHERE template_id IN (SELECT id FROM wf_template WHERE code LIKE 'APIFOX\_%');
DELETE FROM wf_template_scope
WHERE template_id IN (SELECT id FROM wf_template WHERE code LIKE 'APIFOX\_%');
DELETE FROM wf_template
WHERE code LIKE 'APIFOX\_%';

-- Remove prior APIFOX leave types and calendars created by the suite.
DELETE FROM att_leave_type WHERE code LIKE 'APIFOX\_%';
DELETE FROM att_work_calendar_day
WHERE calendar_id IN (SELECT id FROM att_work_calendar AS calendar WHERE calendar.name LIKE 'APIFOX%');
DELETE FROM att_work_calendar WHERE name LIKE 'APIFOX%';

-- Clear prior leave/overtime workflow data for the dedicated test employee to avoid overlap and stale tasks.
DELETE log FROM wf_action_log log
JOIN wf_instance instance_row ON instance_row.id = log.instance_id
LEFT JOIN att_leave_request leave_request ON instance_row.business_type = 'LEAVE' AND leave_request.id = instance_row.business_id
LEFT JOIN att_overtime_request overtime_request ON instance_row.business_type = 'OVERTIME' AND overtime_request.id = instance_row.business_id
WHERE leave_request.employee_id = 9900012 OR overtime_request.employee_id = 9900012;

DELETE task FROM wf_task task
JOIN wf_instance instance_row ON instance_row.id = task.instance_id
LEFT JOIN att_leave_request leave_request ON instance_row.business_type = 'LEAVE' AND leave_request.id = instance_row.business_id
LEFT JOIN att_overtime_request overtime_request ON instance_row.business_type = 'OVERTIME' AND overtime_request.id = instance_row.business_id
WHERE leave_request.employee_id = 9900012 OR overtime_request.employee_id = 9900012;

DELETE FROM wf_instance
WHERE (business_type = 'LEAVE' AND business_id IN (SELECT id FROM att_leave_request WHERE employee_id = 9900012))
   OR (business_type = 'OVERTIME' AND business_id IN (SELECT id FROM att_overtime_request WHERE employee_id = 9900012));

DELETE FROM att_leave_request WHERE employee_id = 9900012;
DELETE FROM att_overtime_request WHERE employee_id = 9900012;

INSERT INTO hr_department (id, code, name, parent_id, leader_employee_id, path, sort_no, effective_date, status, deleted)
VALUES (9900001, 'APIFOX_ROOT', 'Apifox Test Department', NULL, 9900011, '/9900001/', 990, '2026-01-01', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = NULL, leader_employee_id = VALUES(leader_employee_id),
    path = VALUES(path), sort_no = VALUES(sort_no), effective_date = VALUES(effective_date), status = 'ACTIVE', deleted = 0;

INSERT INTO hr_position (id, code, name, job_family, description, sort_no, status, deleted)
VALUES (9900002, 'APIFOX_POSITION', 'Apifox Test Position', 'APIFOX', 'Local API test fixture', 990, 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), job_family = VALUES(job_family), description = VALUES(description),
    sort_no = VALUES(sort_no), status = 'ACTIVE', deleted = 0;

INSERT INTO hr_rank (id, code, name, rank_order, status, deleted)
VALUES (9900003, 'APIFOX_RANK', 'Apifox Test Rank', 990, 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), rank_order = VALUES(rank_order), status = 'ACTIVE', deleted = 0;

INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, rank_id, manager_employee_id, employment_status, hire_date, deleted)
VALUES
    (9900011, 'APIFOX_MANAGER', 'Apifox Manager', 9900001, 9900002, 9900003, NULL, 'FORMAL', '2026-01-01', 0),
    (9900012, 'APIFOX_EMPLOYEE', 'Apifox Employee', 9900001, 9900002, 9900003, 9900011, 'FORMAL', '2026-01-01', 0),
    (9900013, 'APIFOX_HR', 'Apifox HR', 9900001, 9900002, 9900003, 9900011, 'FORMAL', '2026-01-01', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), department_id = VALUES(department_id), position_id = VALUES(position_id),
    rank_id = VALUES(rank_id), manager_employee_id = VALUES(manager_employee_id), employment_status = 'FORMAL',
    hire_date = VALUES(hire_date), deleted = 0;

INSERT INTO sys_role (id, code, name, status, deleted)
VALUES
    (9900201, 'API_TEST_ADMIN', 'Apifox Test Admin', 'ACTIVE', 0),
    (9900202, 'API_TEST_HR', 'Apifox Test HR', 'ACTIVE', 0),
    (9900203, 'API_TEST_MANAGER', 'Apifox Test Manager', 'ACTIVE', 0),
    (9900204, 'API_TEST_EMPLOYEE', 'Apifox Test Employee', 'ACTIVE', 0),
    (9900205, 'API_TEST_ATTENDANCE_ADMIN', 'Apifox Test Attendance Admin', 'ACTIVE', 0),
    (9900206, 'API_TEST_WORKFLOW_ADMIN', 'Apifox Test Workflow Admin', 'ACTIVE', 0),
    (9900207, 'API_TEST_READONLY', 'Apifox Test Readonly', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version, password_change_required, deleted)
VALUES
    (9900101, 'api-admin', @password_hash, NULL, 'ACTIVE', 0, 0, 0),
    (9900102, 'api-hr', @password_hash, 9900013, 'ACTIVE', 0, 0, 0),
    (9900103, 'api-manager', @password_hash, 9900011, 'ACTIVE', 0, 0, 0),
    (9900104, 'api-employee', @password_hash, 9900012, 'ACTIVE', 0, 0, 0),
    (9900105, 'api-attendance-admin', @password_hash, NULL, 'ACTIVE', 0, 0, 0),
    (9900106, 'api-workflow-admin', @password_hash, NULL, 'ACTIVE', 0, 0, 0),
    (9900107, 'api-readonly', @password_hash, NULL, 'ACTIVE', 0, 0, 0)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), employee_id = VALUES(employee_id), status = 'ACTIVE',
    session_version = 0, password_change_required = 0, deleted = 0;

UPDATE sys_user_role SET deleted = 1 WHERE user_id BETWEEN 9900101 AND 9900107;
UPDATE sys_role_menu SET deleted = 1 WHERE role_id BETWEEN 9900201 AND 9900207;
UPDATE sys_role_data_scope SET deleted = 1 WHERE role_id BETWEEN 9900201 AND 9900207;

INSERT INTO sys_user_role (id, user_id, role_id, deleted)
VALUES
    (9900301, 9900101, 9900201, 0), (9900302, 9900102, 9900202, 0), (9900303, 9900103, 9900203, 0),
    (9900304, 9900104, 9900204, 0), (9900305, 9900105, 9900205, 0), (9900306, 9900106, 9900206, 0),
    (9900307, 9900107, 9900207, 0)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), deleted = 0;

-- API_TEST_ADMIN: every current non-performance permission.
INSERT INTO sys_role_menu (id, role_id, menu_id, deleted)
SELECT 9900310 + menu.id, 9900201, menu.id, 0 FROM sys_menu menu
WHERE menu.deleted = 0 AND menu.permission_code IN (
    'system:manage', 'org:read', 'org:manage', 'attendance:submit', 'attendance:manage', 'attendance:balance:adjust',
    'workflow:approve', 'workflow:manage', 'workflow:intervene', 'report:read', 'personnel:read', 'personnel:create',
    'personnel:manage', 'personnel:approve', 'personnel:execute')
ON DUPLICATE KEY UPDATE deleted = 0;

-- HR: organization plus all personnel-change actions except effective execution.
INSERT INTO sys_role_menu (id, role_id, menu_id, deleted)
SELECT 9900410 + menu.id, 9900202, menu.id, 0 FROM sys_menu menu
WHERE menu.deleted = 0 AND menu.permission_code IN ('org:read', 'org:manage', 'personnel:read', 'personnel:create', 'personnel:manage', 'personnel:approve')
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id, deleted)
SELECT 9900510 + menu.id, 9900203, menu.id, 0 FROM sys_menu menu
WHERE menu.deleted = 0 AND menu.permission_code IN ('org:read', 'workflow:approve')
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id, deleted)
SELECT 9900610 + menu.id, 9900204, menu.id, 0 FROM sys_menu menu
WHERE menu.deleted = 0 AND menu.permission_code = 'attendance:submit'
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id, deleted)
SELECT 9900710 + menu.id, 9900205, menu.id, 0 FROM sys_menu menu
WHERE menu.deleted = 0 AND menu.permission_code IN ('attendance:manage', 'attendance:balance:adjust')
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id, deleted)
SELECT 9900810 + menu.id, 9900206, menu.id, 0 FROM sys_menu menu
WHERE menu.deleted = 0 AND menu.permission_code IN ('workflow:manage', 'workflow:intervene')
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_data_scope (id, role_id, scope_type, scope_id, deleted)
VALUES
    (9900901, 9900201, 'ALL', NULL, 0),
    (9900902, 9900202, 'ALL', NULL, 0),
    (9900903, 9900203, 'DEPT_TREE', NULL, 0),
    (9900904, 9900204, 'SELF', NULL, 0),
    (9900905, 9900205, 'ALL', NULL, 0)
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type), scope_id = VALUES(scope_id), deleted = 0;

INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours, frozen_hours, deleted)
VALUES (9900401, 9900012, 'APIFOX_ANNUAL', 2026, 80.00, 0.00, 0)
ON DUPLICATE KEY UPDATE available_hours = 80.00, frozen_hours = 0.00, version = 0, deleted = 0;

COMMIT;

SELECT user.id, user.username, user.employee_id, GROUP_CONCAT(role.code ORDER BY role.code) AS roles
FROM sys_user user
LEFT JOIN sys_user_role user_role ON user_role.user_id = user.id AND user_role.deleted = 0
LEFT JOIN sys_role role ON role.id = user_role.role_id AND role.deleted = 0
WHERE user.id BETWEEN 9900101 AND 9900107 AND user.deleted = 0
GROUP BY user.id, user.username, user.employee_id
ORDER BY user.id;

SELECT role.code, scope.scope_type, scope.scope_id
FROM sys_role role
LEFT JOIN sys_role_data_scope scope ON scope.role_id = role.id AND scope.deleted = 0
WHERE role.id BETWEEN 9900201 AND 9900207 AND role.deleted = 0
ORDER BY role.id;
