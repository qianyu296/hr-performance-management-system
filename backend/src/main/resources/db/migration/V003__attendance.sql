CREATE TABLE att_leave_type (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, deduct_balance TINYINT NOT NULL, min_unit_hours DECIMAL(5,2) NOT NULL, annual_quota DECIMAL(10,2) NULL, attachment_rule JSON NULL, balance_rule JSON NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_att_leave_type_code (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE att_work_calendar (
    id BIGINT NOT NULL, calendar_year INT NOT NULL, name VARCHAR(128) NOT NULL, time_zone VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_att_calendar_year (calendar_year, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE att_work_calendar_day (
    id BIGINT NOT NULL, calendar_id BIGINT NOT NULL, work_date DATE NOT NULL, is_workday TINYINT NOT NULL, work_hours DECIMAL(5,2) NOT NULL DEFAULT 0, holiday_name VARCHAR(128) NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_att_calendar_day (calendar_id, work_date, deleted), KEY idx_att_calendar_date (work_date, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE att_leave_balance (
    id BIGINT NOT NULL, employee_id BIGINT NOT NULL, balance_type VARCHAR(64) NOT NULL, balance_year INT NOT NULL, available_hours DECIMAL(10,2) NOT NULL DEFAULT 0, frozen_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_att_balance (employee_id, balance_type, balance_year, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE att_balance_change (
    id BIGINT NOT NULL, balance_id BIGINT NOT NULL, employee_id BIGINT NOT NULL, balance_type VARCHAR(64) NOT NULL, delta_hours DECIMAL(10,2) NOT NULL, before_hours DECIMAL(10,2) NOT NULL, after_hours DECIMAL(10,2) NOT NULL, source_type VARCHAR(64) NOT NULL, source_id BIGINT NULL, reason VARCHAR(500) NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id), UNIQUE KEY uk_att_balance_source (source_type, source_id, balance_type), KEY idx_att_balance_change_employee (employee_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE att_leave_request (
    id BIGINT NOT NULL, request_no VARCHAR(64) NOT NULL, employee_id BIGINT NOT NULL, leave_type_id BIGINT NOT NULL, start_time DATETIME(3) NOT NULL, end_time DATETIME(3) NOT NULL, duration_hours DECIMAL(10,2) NOT NULL, reason VARCHAR(1000) NOT NULL, status VARCHAR(32) NOT NULL, workflow_instance_id BIGINT NULL, organization_snapshot JSON NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_att_leave_request_no (request_no), KEY idx_att_leave_employee_time (employee_id, start_time, end_time, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE att_overtime_request (
    id BIGINT NOT NULL, request_no VARCHAR(64) NOT NULL, employee_id BIGINT NOT NULL, start_time DATETIME(3) NOT NULL, end_time DATETIME(3) NOT NULL, duration_hours DECIMAL(10,2) NOT NULL, reason VARCHAR(1000) NOT NULL, compensation_type VARCHAR(32) NOT NULL, status VARCHAR(32) NOT NULL, workflow_instance_id BIGINT NULL, organization_snapshot JSON NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_att_overtime_request_no (request_no), KEY idx_att_overtime_employee_time (employee_id, start_time, end_time, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
