CREATE TABLE hr_exit_handover (
    id BIGINT NOT NULL,
    change_id BIGINT NOT NULL,
    handover_employee_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_exit_handover_change (change_id, deleted),
    KEY idx_hr_exit_handover_change_status (change_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hr_exit_handover_item (
    id BIGINT NOT NULL,
    handover_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    receiver_employee_id BIGINT NULL,
    is_required TINYINT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL,
    completed_time DATETIME(3) NULL,
    confirmed_by BIGINT NULL,
    remark VARCHAR(500) NULL,
    created_by BIGINT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_hr_exit_handover_item_handover_status (handover_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_role (id, code, name, status)
VALUES (9000054, 'HR_SPECIALIST', '人力资源专员', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status) VALUES
    (9000055, '人事异动查看', 'personnel:read', 'BUTTON', '/personnel/changes', 120, 'ACTIVE'),
    (9000056, '人事异动创建', 'personnel:create', 'BUTTON', '/personnel/changes', 121, 'ACTIVE'),
    (9000057, '人事异动管理', 'personnel:manage', 'BUTTON', '/personnel/changes', 122, 'ACTIVE'),
    (9000058, '人事异动审批', 'personnel:approve', 'BUTTON', '/workflow/tasks', 123, 'ACTIVE'),
    (9000059, '人事异动生效', 'personnel:execute', 'BUTTON', '/personnel/changes', 124, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    route_path = VALUES(route_path),
    sort_no = VALUES(sort_no),
    status = 'ACTIVE',
    deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000060, 9000002, id FROM sys_menu
WHERE permission_code = 'personnel:read' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000061, 9000002, id FROM sys_menu
WHERE permission_code = 'personnel:create' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000062, 9000002, id FROM sys_menu
WHERE permission_code = 'personnel:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000063, 9000002, id FROM sys_menu
WHERE permission_code = 'personnel:approve' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000064, 9000002, id FROM sys_menu
WHERE permission_code = 'personnel:execute' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000065, 9000054, id FROM sys_menu
WHERE permission_code = 'personnel:read' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000066, 9000054, id FROM sys_menu
WHERE permission_code = 'personnel:create' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000067, 9000054, id FROM sys_menu
WHERE permission_code = 'personnel:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000068, 9000054, id FROM sys_menu
WHERE permission_code = 'personnel:approve' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
