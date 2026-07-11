CREATE TABLE sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    employee_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    session_version INT NOT NULL DEFAULT 0,
    last_login_time DATETIME(3) NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_user_username (username, deleted),
    UNIQUE KEY uk_sys_user_employee (employee_id, deleted), KEY idx_sys_user_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_role_code (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_menu (
    id BIGINT NOT NULL, parent_id BIGINT NULL, name VARCHAR(128) NOT NULL, permission_code VARCHAR(128) NULL,
    menu_type VARCHAR(32) NOT NULL, route_path VARCHAR(255) NULL, component VARCHAR(255) NULL, sort_no INT NOT NULL DEFAULT 0, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_menu_permission (permission_code, deleted), KEY idx_sys_menu_parent (parent_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
    id BIGINT NOT NULL, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_user_role (user_id, role_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_menu (
    id BIGINT NOT NULL, role_id BIGINT NOT NULL, menu_id BIGINT NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_role_menu (role_id, menu_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_data_scope (
    id BIGINT NOT NULL, scope_type VARCHAR(32) NOT NULL, name VARCHAR(128) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_data_scope (scope_type, name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_data_scope (
    id BIGINT NOT NULL, role_id BIGINT NOT NULL, scope_type VARCHAR(32) NOT NULL, scope_id BIGINT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_role_scope (role_id, scope_type, scope_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hr_department (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, parent_id BIGINT NULL, leader_employee_id BIGINT NULL,
    path VARCHAR(2000) NOT NULL, sort_no INT NOT NULL DEFAULT 0, effective_date DATE NOT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_hr_department_code (code, deleted), KEY idx_hr_department_parent (parent_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_data_scope_dept (
    id BIGINT NOT NULL, scope_id BIGINT NOT NULL, department_id BIGINT NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_scope_dept (scope_id, department_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_field_policy (
    id BIGINT NOT NULL, resource_code VARCHAR(128) NOT NULL, field_code VARCHAR(128) NOT NULL, role_id BIGINT NULL,
    action VARCHAR(32) NOT NULL, mask_rule VARCHAR(128) NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_sys_field_policy (resource_code, field_code, role_id, action, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_sensitive_grant (
    id BIGINT NOT NULL, user_id BIGINT NULL, role_id BIGINT NULL, resource_code VARCHAR(128) NOT NULL, action VARCHAR(32) NOT NULL,
    effective_from DATETIME(3) NOT NULL, effective_to DATETIME(3) NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), KEY idx_sys_sensitive_grant (user_id, role_id, resource_code, action, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hr_position (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, job_family VARCHAR(64) NULL, description TEXT NULL, sort_no INT NOT NULL DEFAULT 0, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_hr_position_code (code, deleted), KEY idx_hr_position_status (status, sort_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hr_rank (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, rank_order INT NOT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_hr_rank_code (code, deleted), KEY idx_hr_rank_status (status, rank_order, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hr_employee (
    id BIGINT NOT NULL, employee_no VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, gender VARCHAR(16) NULL,
    identity_ciphertext VARBINARY(2048) NULL, identity_hash CHAR(64) NULL, phone_ciphertext VARBINARY(2048) NULL, phone_hash CHAR(64) NULL,
    department_id BIGINT NOT NULL, position_id BIGINT NOT NULL, rank_id BIGINT NULL, manager_employee_id BIGINT NULL, employment_status VARCHAR(32) NOT NULL,
    hire_date DATE NOT NULL, probation_start_date DATE NULL, probation_end_date DATE NULL, termination_date DATE NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_hr_employee_no (employee_no), KEY idx_hr_employee_dept (department_id, employment_status, deleted), KEY idx_hr_employee_manager (manager_employee_id, employment_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
