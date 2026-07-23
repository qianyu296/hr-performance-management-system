-- One-file database initializer for HRPM.
-- This script rebuilds the hrpm database schema, applies all Flyway migrations,
-- and seeds only clean baseline data required for local startup.
-- It intentionally excludes scripts/dev-seed.sql and any API test accounts.

SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS hrpm CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE hrpm;

SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS;
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;
SET SESSION group_concat_max_len = 32768;

SELECT GROUP_CONCAT(CONCAT('`', table_name, '`') ORDER BY table_name SEPARATOR ',')
INTO @all_tables
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

SET @drop_sql = IF(
    @all_tables IS NULL OR @all_tables = '',
    'SELECT 1',
    CONCAT('DROP TABLE IF EXISTS ', @all_tables)
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
-- ----------------------------------------
-- V001__system_and_organization.sql
-- ----------------------------------------
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


-- ----------------------------------------
-- V002__workflow.sql
-- ----------------------------------------
CREATE TABLE wf_template (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, business_type VARCHAR(64) NOT NULL, priority INT NOT NULL DEFAULT 0, template_version INT NOT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_template (business_type, code, template_version, deleted), KEY idx_wf_template_match (business_type, status, priority, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_template_scope (
    id BIGINT NOT NULL, template_id BIGINT NOT NULL, department_id BIGINT NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_template_scope (template_id, department_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_template_node (
    id BIGINT NOT NULL, template_id BIGINT NOT NULL, node_no INT NOT NULL, node_type VARCHAR(32) NOT NULL, approver_rule JSON NOT NULL, timeout_hours INT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_template_node (template_id, node_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_instance (
    id BIGINT NOT NULL, business_type VARCHAR(64) NOT NULL, business_id BIGINT NOT NULL, initiator_user_id BIGINT NOT NULL, template_snapshot JSON NOT NULL, status VARCHAR(32) NOT NULL, current_node_no INT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_wf_instance_business (business_type, business_id, deleted), KEY idx_wf_instance_initiator (initiator_user_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_task (
    id BIGINT NOT NULL, instance_id BIGINT NOT NULL, node_no INT NOT NULL, node_snapshot JSON NOT NULL, assignee_user_id BIGINT NOT NULL, delegated_from_user_id BIGINT NULL, due_time DATETIME(3) NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), KEY idx_wf_task_assignee (assignee_user_id, status, due_time, deleted), UNIQUE KEY uk_wf_task_pending (instance_id, node_no, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE wf_action_log (
    id BIGINT NOT NULL, instance_id BIGINT NOT NULL, task_id BIGINT NULL, actor_user_id BIGINT NOT NULL, action VARCHAR(32) NOT NULL, comment TEXT NULL, action_snapshot JSON NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id), KEY idx_wf_action_instance (instance_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------
-- V003__attendance.sql
-- ----------------------------------------
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


-- ----------------------------------------
-- V004__performance.sql
-- ----------------------------------------
CREATE TABLE perf_goal_cycle (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL, status VARCHAR(32) NOT NULL, frozen_time DATETIME(3) NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_perf_goal_cycle_code (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_goal (
    id BIGINT NOT NULL, goal_cycle_id BIGINT NOT NULL, parent_goal_id BIGINT NULL, owner_employee_id BIGINT NOT NULL, department_id BIGINT NULL, name VARCHAR(255) NOT NULL, target_value DECIMAL(18,4) NOT NULL, unit VARCHAR(32) NOT NULL, weight DECIMAL(5,2) NOT NULL, due_date DATE NOT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), KEY idx_perf_goal_cycle_owner (goal_cycle_id, owner_employee_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_goal_progress (
    id BIGINT NOT NULL, goal_id BIGINT NOT NULL, progress DECIMAL(5,2) NOT NULL, description TEXT NOT NULL, file_snapshot JSON NULL, created_by BIGINT NOT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id), KEY idx_perf_goal_progress (goal_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_metric (
    id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, metric_type VARCHAR(32) NOT NULL, unit VARCHAR(32) NULL, score_method VARCHAR(32) NOT NULL, score_config JSON NOT NULL, description TEXT NULL, status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_perf_metric_code (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_scheme (id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, applicability_rule JSON NOT NULL, status VARCHAR(32) NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_scheme_code (code, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_scheme_version (id BIGINT NOT NULL, scheme_id BIGINT NOT NULL, version_no INT NOT NULL, evaluation_stages JSON NOT NULL, snapshot JSON NOT NULL, status VARCHAR(32) NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_scheme_version (scheme_id, version_no, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_scheme_item (id BIGINT NOT NULL, scheme_version_id BIGINT NOT NULL, metric_id BIGINT NOT NULL, weight DECIMAL(5,2) NOT NULL, score_method VARCHAR(32) NOT NULL, score_config JSON NOT NULL, sort_no INT NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_scheme_item (scheme_version_id, metric_id, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_level_rule (id BIGINT NOT NULL, scheme_version_id BIGINT NOT NULL, level_code VARCHAR(32) NOT NULL, min_score DECIMAL(7,2) NOT NULL, max_score DECIMAL(7,2) NOT NULL, include_min TINYINT NOT NULL DEFAULT 1, include_max TINYINT NOT NULL DEFAULT 1, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_level_rule (scheme_version_id, level_code, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------
-- V005__performance_execution_and_support.sql
-- ----------------------------------------
CREATE TABLE perf_cycle (id BIGINT NOT NULL, code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, scheme_version_id BIGINT NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL, self_deadline DATETIME(3) NOT NULL, manager_deadline DATETIME(3) NOT NULL, appeal_deadline DATETIME(3) NULL, applicability_rule JSON NOT NULL, status VARCHAR(32) NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_cycle_code (code, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_task (id BIGINT NOT NULL, cycle_id BIGINT NOT NULL, employee_id BIGINT NOT NULL, manager_employee_id BIGINT NULL, scheme_version_id BIGINT NOT NULL, organization_snapshot JSON NOT NULL, status VARCHAR(32) NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_task_cycle_employee (cycle_id, employee_id, deleted), KEY idx_perf_task_manager (manager_employee_id, status, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_task_item (id BIGINT NOT NULL, task_id BIGINT NOT NULL, metric_snapshot JSON NOT NULL, weight DECIMAL(5,2) NOT NULL, stage_snapshot JSON NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), KEY idx_perf_task_item (task_id, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_score (id BIGINT NOT NULL, task_item_id BIGINT NOT NULL, stage VARCHAR(32) NOT NULL, evaluator_employee_id BIGINT NOT NULL, raw_score DECIMAL(7,2) NULL, weighted_score DECIMAL(7,2) NULL, comment TEXT NULL, file_snapshot JSON NULL, submitted_time DATETIME(3) NULL, score_version INT NOT NULL DEFAULT 1, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_score (task_item_id, stage, evaluator_employee_id, score_version, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_result (id BIGINT NOT NULL, task_id BIGINT NOT NULL, current_version_no INT NOT NULL DEFAULT 1, publish_status VARCHAR(32) NOT NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_result_task (task_id, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_result_version (id BIGINT NOT NULL, result_id BIGINT NOT NULL, version_no INT NOT NULL, total_score DECIMAL(7,2) NOT NULL, level_code VARCHAR(32) NOT NULL, source VARCHAR(32) NOT NULL, reason TEXT NULL, published_by BIGINT NULL, published_time DATETIME(3) NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_perf_result_version (result_id, version_no, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_calibration (id BIGINT NOT NULL, result_id BIGINT NOT NULL, before_snapshot JSON NOT NULL, after_snapshot JSON NOT NULL, reason TEXT NOT NULL, created_by BIGINT NOT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY (id), KEY idx_perf_calibration_result (result_id, created_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE perf_appeal (id BIGINT NOT NULL, result_id BIGINT NOT NULL, appeal_item VARCHAR(255) NOT NULL, statement TEXT NOT NULL, file_snapshot JSON NULL, status VARCHAR(32) NOT NULL, decision VARCHAR(32) NULL, conclusion TEXT NULL, reviewer_user_id BIGINT NULL, resolved_time DATETIME(3) NULL, created_by BIGINT NOT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), KEY idx_perf_appeal_result (result_id, status, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_file (id BIGINT NOT NULL, object_key VARCHAR(512) NOT NULL, original_name VARCHAR(255) NOT NULL, media_type VARCHAR(128) NOT NULL, byte_size BIGINT NOT NULL, sha256 CHAR(64) NOT NULL, status VARCHAR(32) NOT NULL, created_by BIGINT NOT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_sys_file_object (object_key), KEY idx_sys_file_sha (sha256, status)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_file_link (id BIGINT NOT NULL, business_type VARCHAR(64) NOT NULL, business_id BIGINT NOT NULL, file_id BIGINT NOT NULL, category VARCHAR(64) NULL, created_by BIGINT NOT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_sys_file_link (business_type, business_id, file_id, deleted)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_outbox_event (id BIGINT NOT NULL, event_type VARCHAR(64) NOT NULL, aggregate_type VARCHAR(64) NOT NULL, aggregate_id BIGINT NOT NULL, deduplication_key VARCHAR(128) NOT NULL, payload JSON NOT NULL, status VARCHAR(32) NOT NULL, visible_time DATETIME(3) NOT NULL, attempt_count INT NOT NULL DEFAULT 0, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY (id), UNIQUE KEY uk_sys_outbox_dedupe (deduplication_key), KEY idx_sys_outbox_dispatch (status, visible_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_async_job (id BIGINT NOT NULL, job_type VARCHAR(64) NOT NULL, idempotency_key VARCHAR(128) NOT NULL, payload JSON NOT NULL, status VARCHAR(32) NOT NULL, attempt_count INT NOT NULL DEFAULT 0, next_run_time DATETIME(3) NOT NULL, locked_by VARCHAR(128) NULL, locked_until DATETIME(3) NULL, error_summary VARCHAR(1000) NULL, created_by BIGINT NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_by BIGINT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), deleted TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_sys_async_job (job_type, idempotency_key), KEY idx_sys_async_job_dispatch (status, next_run_time, locked_until)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_operation_log (id BIGINT NOT NULL, actor_user_id BIGINT NULL, module VARCHAR(64) NOT NULL, object_type VARCHAR(64) NOT NULL, object_id BIGINT NULL, action VARCHAR(64) NOT NULL, result VARCHAR(32) NOT NULL, trace_id VARCHAR(64) NULL, summary JSON NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY (id), KEY idx_sys_operation_log (object_type, object_id, created_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------
-- V006__seed_initial_admin.sql
-- ----------------------------------------
-- Development bootstrap account. Change its password immediately outside local development.
INSERT INTO sys_user (id, username, password_hash, status, session_version)
VALUES (9000001, 'admin', '$2a$10$sdmbAyd9lCq37Nzkg7IzV.JzLLzkGUB.il9OfqiEB6GB7sLkogrbC', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role (id, code, name, status)
VALUES (9000002, 'SUPER_ADMIN', 'Initial Administrator', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status) VALUES
  (9000010, 'System Manage', 'system:manage', 'BUTTON', '/system/users', 10, 'ACTIVE'),
  (9000011, 'Organization Read', 'org:read', 'BUTTON', '/org/employees', 20, 'ACTIVE'),
  (9000012, 'Organization Manage', 'org:manage', 'BUTTON', '/org/employees', 21, 'ACTIVE'),
  (9000013, 'Attendance Submit', 'attendance:submit', 'BUTTON', '/attendance/leave', 30, 'ACTIVE'),
  (9000014, 'Workflow Approve', 'workflow:approve', 'BUTTON', '/workflow/tasks', 40, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (9000020, 9000001, 9000002)
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000030, 9000002, id FROM sys_menu WHERE permission_code = 'system:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000031, 9000002, id FROM sys_menu WHERE permission_code = 'org:read' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000032, 9000002, id FROM sys_menu WHERE permission_code = 'org:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000033, 9000002, id FROM sys_menu WHERE permission_code = 'attendance:submit' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000034, 9000002, id FROM sys_menu WHERE permission_code = 'workflow:approve' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_data_scope (id, role_id, scope_type)
VALUES (9000040, 9000002, 'ALL')
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V007__add_workflow_template_management_permission.sql
-- ----------------------------------------
INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000015, 'Workflow Manage', 'workflow:manage', 'BUTTON', '/workflow/templates', 41, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000035, 9000002, id FROM sys_menu WHERE permission_code = 'workflow:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V008__add_workflow_intervention_permission.sql
-- ----------------------------------------
INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000016, 'Workflow Intervene', 'workflow:intervene', 'BUTTON', '/workflow/tasks', 42, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000036, 9000002, id FROM sys_menu WHERE permission_code = 'workflow:intervene' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V009__add_attendance_management_permission.sql
-- ----------------------------------------
INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000017, 'Attendance Manage', 'attendance:manage', 'BUTTON', '/attendance/calendar', 31, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000037, 9000002, id FROM sys_menu WHERE permission_code = 'attendance:manage' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V010__add_leave_balance_adjustment_permission.sql
-- ----------------------------------------
INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status) VALUES
  (9000018, 'Leave Balance Adjust', 'attendance:balance:adjust', 'BUTTON', '/attendance/balances', 32, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000038, 9000002, id FROM sys_menu WHERE permission_code = 'attendance:balance:adjust' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V011__add_attendance_month_summary.sql
-- ----------------------------------------
CREATE TABLE rpt_attendance_month (
    id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    attendance_month DATE NOT NULL,
    leave_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    time_off_delta_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    pending_request_count INT NOT NULL DEFAULT 0,
    generated_by BIGINT NULL,
    generated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rpt_attendance_employee_month (employee_id, attendance_month),
    KEY idx_rpt_attendance_month_department (attendance_month, department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------
-- V012__add_performance_configuration_permission.sql
-- ----------------------------------------
INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000040, 'Performance configuration', 'performance:config', 'BUTTON', NULL, 100, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), sort_no = VALUES(sort_no), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000041, 9000002, id FROM sys_menu
WHERE permission_code = 'performance:config' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V013__localize_visible_system_data.sql
-- ----------------------------------------
UPDATE sys_role
SET name = CASE code
    WHEN 'SUPER_ADMIN' THEN '系统超级管理员'
    WHEN 'DEV_TEST_ADMIN' THEN '开发测试管理员'
    WHEN 'DEV_EMPLOYEE' THEN '开发测试员工'
    WHEN 'EMP_TEST_ORG' THEN '员工组织测试'
    WHEN 'OT_SUBMIT' THEN '加班申请'
    WHEN 'TEST_ATTENDANCE_SUBMIT' THEN '假勤申请测试'
    WHEN 'TEST_ORG' THEN '组织测试'
    WHEN 'TEST_ORG_MANAGER' THEN '组织管理员测试'
    ELSE name
END
WHERE deleted = 0;

UPDATE sys_menu
SET name = CASE permission_code
    WHEN 'org:read' THEN '组织查看'
    WHEN 'org:manage' THEN '组织管理'
    WHEN 'attendance:submit' THEN '假勤申请'
    WHEN 'attendance:manage' THEN '假勤管理'
    WHEN 'attendance:balance:adjust' THEN '假期余额调整'
    WHEN 'workflow:approve' THEN '流程审批'
    WHEN 'workflow:manage' THEN '流程管理'
    WHEN 'workflow:intervene' THEN '流程干预'
    WHEN 'performance:config' THEN '绩效配置'
    WHEN 'system:manage' THEN '系统管理'
    ELSE name
END
WHERE deleted = 0;

UPDATE att_leave_type
SET name = CASE code
    WHEN 'ANNUAL' THEN '年假'
    WHEN 'DEV_ANNUAL' THEN '年假'
    WHEN 'SUMMARY_ANNUAL' THEN '年假（汇总）'
    ELSE name
END
WHERE deleted = 0;


-- ----------------------------------------
-- V014__localize_organization_reference_data.sql
-- ----------------------------------------
UPDATE sys_role
SET name = CASE code
    WHEN 'DEV_EMPLOYEE' THEN '开发测试员工'
    WHEN 'DEV_TEST_ADMIN' THEN '开发测试管理员'
    WHEN 'EMP_TEST_ORG' THEN '员工组织测试'
    WHEN 'OT_SUBMIT' THEN '加班申请'
    WHEN 'SUPER_ADMIN' THEN '系统超级管理员'
    WHEN 'TEST_ATTENDANCE_SUBMIT' THEN '假勤申请测试'
    WHEN 'TEST_ORG' THEN '组织测试'
    WHEN 'TEST_ORG_MANAGER' THEN '组织管理员测试'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_department
SET name = CASE code
    WHEN 'HQ' THEN '总部'
    WHEN 'ENG' THEN '研发部'
    WHEN 'TEST' THEN '测试部门'
    WHEN 'EMP_TEST_DEPT' THEN '测试部门'
    WHEN 'OT_TEST' THEN '加班测试部门'
    WHEN 'SUMMARY_TEST' THEN '月度汇总测试部门'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_position
SET name = CASE code
    WHEN 'DEV-HR-SPECIALIST' THEN '人力资源专员'
    WHEN 'EMP_TEST_POSITION' THEN '测试岗位'
    WHEN 'OT_POSITION' THEN '加班测试岗位'
    WHEN 'SUMMARY_POSITION' THEN '月度汇总测试岗位'
    WHEN 'TEST_POSITION' THEN '测试岗位'
    WHEN 'LT_TEST' THEN '请假类型测试岗位'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_rank
SET name = CASE code
    WHEN 'EMP_TEST_RANK' THEN 'P5职级'
    WHEN 'TEST_P5' THEN '高级P5职级'
    ELSE name
END
WHERE deleted = 0;


-- ----------------------------------------
-- V015__localize_remaining_test_reference_data.sql
-- ----------------------------------------
UPDATE sys_role
SET name = CASE code
    WHEN 'TEST_ATTENDANCE_SUBMIT' THEN '假勤申请测试'
    WHEN 'TEST_BALANCE_ADJUST' THEN '余额调整测试'
    WHEN 'TEST_ORG' THEN '组织测试'
    WHEN 'EMP_TEST_SELF' THEN '员工本人数据测试'
    WHEN 'LEAVE_TYPE_HR' THEN '请假类型人事专员'
    WHEN 'SYSTEM_ACCESS_ADMIN' THEN '权限管理测试管理员'
    WHEN 'SYSTEM_ACCESS_INITIAL' THEN '初始测试角色'
    WHEN 'SYSTEM_ACCESS_REPLACEMENT' THEN '替换测试角色'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_department
SET name = CASE code
    WHEN 'TEST' THEN '测试部门'
    WHEN 'LT_TEST' THEN '请假类型测试部门'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_position
SET name = CASE code
    WHEN 'TEST_POSITION' THEN '测试岗位'
    WHEN 'LT_TEST' THEN '请假类型测试岗位'
    WHEN 'TEST_STALE' THEN '岗位版本测试'
    ELSE name
END
WHERE deleted = 0;

UPDATE hr_rank
SET name = CASE code
    WHEN 'TEST_P5' THEN '高级P5职级'
    ELSE name
END
WHERE deleted = 0;


-- ----------------------------------------
-- V016__localize_remaining_visible_names.sql
-- ----------------------------------------
UPDATE hr_rank
SET name = CASE code
    WHEN 'EMP_TEST_RANK' THEN '五级职级'
    WHEN 'TEST_P5' THEN '高级五级职级'
    ELSE name
END
WHERE deleted = 0;

UPDATE sys_menu
SET name = CASE permission_code
    WHEN 'org:read' THEN '组织读取'
    WHEN 'org:manage' THEN '组织管理'
    ELSE name
END
WHERE deleted = 0;


-- ----------------------------------------
-- V017__add_report_read_permission.sql
-- ----------------------------------------
INSERT INTO sys_menu (id, name, permission_code, menu_type, route_path, sort_no, status)
VALUES (9000050, '数据分析', 'report:read', 'BUTTON', '/reports/overview', 110, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path), sort_no = VALUES(sort_no), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000051, 9000002, id FROM sys_menu
WHERE permission_code = 'report:read' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V018__add_employee_account_provisioning.sql
-- ----------------------------------------
ALTER TABLE sys_user ADD COLUMN password_change_required TINYINT NOT NULL DEFAULT 0 AFTER session_version;

INSERT INTO sys_role (id, code, name, status)
VALUES (9000052, 'EMPLOYEE_SELF_SERVICE', '员工自助服务', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE', deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 9000053, 9000052, id FROM sys_menu
WHERE permission_code = 'attendance:submit' AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;


-- ----------------------------------------
-- V019__configure_default_leave_quota.sql
-- ----------------------------------------
UPDATE att_leave_type
SET annual_quota = 80.00
WHERE deduct_balance = 1
  AND annual_quota IS NULL
  AND deleted = 0;


-- ----------------------------------------
-- V020__complete_organization_master_data.sql
-- ----------------------------------------
ALTER TABLE hr_employee
    MODIFY employment_status VARCHAR(32) NOT NULL;

CREATE INDEX idx_hr_employee_status ON hr_employee (employment_status, deleted);
CREATE INDEX idx_hr_department_leader ON hr_department (leader_employee_id, deleted);

UPDATE hr_employee
SET employment_status = 'PENDING_ONBOARD'
WHERE employment_status = 'PENDING_ONBOARDING';

UPDATE hr_employee
SET employment_status = 'PROBATION'
WHERE employment_status NOT IN ('PENDING_ONBOARD', 'PROBATION', 'FORMAL', 'SUSPENDED', 'TERMINATED');


-- ----------------------------------------
-- V021__add_personnel_change_and_employee_history.sql
-- ----------------------------------------
CREATE TABLE hr_personnel_change (
    id BIGINT NOT NULL,
    change_no VARCHAR(64) NOT NULL,
    employee_id BIGINT NULL,
    change_type VARCHAR(32) NOT NULL,
    application_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    reason TEXT NOT NULL,
    before_snapshot JSON NULL,
    after_snapshot JSON NOT NULL,
    workflow_instance_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL,
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_personnel_change_no (change_no, deleted),
    KEY idx_hr_personnel_change_employee_status (employee_id, status, effective_date, deleted),
    KEY idx_hr_personnel_change_workflow (workflow_instance_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hr_employee_history (
    id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    change_id BIGINT NULL,
    event_type VARCHAR(32) NOT NULL,
    effective_date DATE NOT NULL,
    snapshot JSON NOT NULL,
    created_by BIGINT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_hr_employee_history_employee_date (employee_id, effective_date, id),
    KEY idx_hr_employee_history_change (change_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO hr_employee_history (id, employee_id, change_id, event_type, effective_date, snapshot, created_by, created_time)
SELECT
    e.id + 700000000000000000,
    e.id,
    NULL,
    'BASELINE',
    COALESCE(e.hire_date, CURRENT_DATE()),
    JSON_OBJECT(
        'departmentId', e.department_id,
        'positionId', e.position_id,
        'rankId', e.rank_id,
        'managerEmployeeId', e.manager_employee_id,
        'employmentStatus', e.employment_status,
        'hireDate', DATE_FORMAT(e.hire_date, '%Y-%m-%d'),
        'probationStartDate', DATE_FORMAT(e.probation_start_date, '%Y-%m-%d'),
        'probationEndDate', DATE_FORMAT(e.probation_end_date, '%Y-%m-%d'),
        'terminationDate', DATE_FORMAT(e.termination_date, '%Y-%m-%d')
    ),
    e.updated_by,
    CURRENT_TIMESTAMP(3)
FROM hr_employee e
WHERE e.deleted = 0;


-- ----------------------------------------
-- V022__add_exit_handover_and_personnel_permissions.sql
-- ----------------------------------------
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


-- ----------------------------------------
-- V023__add_organization_personnel_audit_log.sql
-- ----------------------------------------
ALTER TABLE sys_operation_log
    ADD COLUMN source_address VARCHAR(64) NULL AFTER summary;

CREATE INDEX idx_sys_operation_log_module_object ON sys_operation_log (module, object_id, created_time);
CREATE INDEX idx_sys_operation_log_actor ON sys_operation_log (actor_user_id, created_time);


-- ----------------------------------------
-- Clean baseline seed data
-- ----------------------------------------
DELETE FROM wf_template_node;
DELETE FROM wf_template_scope;
DELETE FROM wf_template;
DELETE FROM att_leave_balance;
DELETE FROM att_leave_type;
DELETE FROM hr_employee_history;
DELETE FROM hr_employee;
DELETE FROM hr_rank;
DELETE FROM hr_position;
DELETE FROM hr_department;
DELETE FROM sys_user_role;
DELETE FROM sys_role_menu;
DELETE FROM sys_role_data_scope;
DELETE FROM sys_user;
DELETE FROM sys_role;

INSERT INTO sys_role (id, code, name, status) VALUES
    (9000002, 'SUPER_ADMIN', '系统超级管理员', 'ACTIVE'),
    (9000052, 'EMPLOYEE_SELF_SERVICE', '员工自助服务', 'ACTIVE'),
    (9000054, 'HR_SPECIALIST', '人力资源专员', 'ACTIVE');

INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version, password_change_required, last_login_time) VALUES
    (9000001, 'admin', '$2a$10$sdmbAyd9lCq37Nzkg7IzV.JzLLzkGUB.il9OfqiEB6GB7sLkogrbC', NULL, 'ACTIVE', 0, 0, NULL),
    (9101001, 'hr', '$2a$10$sdmbAyd9lCq37Nzkg7IzV.JzLLzkGUB.il9OfqiEB6GB7sLkogrbC', 9103001, 'ACTIVE', 0, 0, NULL),
    (9101002, 'employee', '$2a$10$sdmbAyd9lCq37Nzkg7IzV.JzLLzkGUB.il9OfqiEB6GB7sLkogrbC', 9103002, 'ACTIVE', 0, 0, NULL);

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
    (9000020, 9000001, 9000002),
    (9101101, 9101001, 9000054),
    (9101102, 9101002, 9000052);

INSERT INTO sys_role_data_scope (id, role_id, scope_type, scope_id) VALUES
    (91000401, 9000002, 'ALL', NULL),
    (91000402, 9000052, 'SELF', NULL),
    (91000403, 9000054, 'ALL', NULL);

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000104, 9000002, id FROM sys_menu WHERE permission_code = 'system:manage' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000201, 9000052, id FROM sys_menu WHERE permission_code = 'attendance:submit' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000301, 9000054, id FROM sys_menu WHERE permission_code = 'org:read' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000302, 9000054, id FROM sys_menu WHERE permission_code = 'attendance:submit' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000303, 9000054, id FROM sys_menu WHERE permission_code = 'org:manage' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000304, 9000054, id FROM sys_menu WHERE permission_code = 'workflow:approve' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000305, 9000054, id FROM sys_menu WHERE permission_code = 'workflow:manage' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000306, 9000054, id FROM sys_menu WHERE permission_code = 'workflow:intervene' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000307, 9000054, id FROM sys_menu WHERE permission_code = 'attendance:manage' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000308, 9000054, id FROM sys_menu WHERE permission_code = 'attendance:balance:adjust' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000309, 9000054, id FROM sys_menu WHERE permission_code = 'report:read' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000310, 9000054, id FROM sys_menu WHERE permission_code = 'personnel:read' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000311, 9000054, id FROM sys_menu WHERE permission_code = 'personnel:create' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000312, 9000054, id FROM sys_menu WHERE permission_code = 'personnel:manage' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000313, 9000054, id FROM sys_menu WHERE permission_code = 'personnel:approve' AND deleted = 0;
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 91000314, 9000054, id FROM sys_menu WHERE permission_code = 'personnel:execute' AND deleted = 0;

INSERT INTO hr_department (id, code, name, parent_id, leader_employee_id, path, sort_no, effective_date, status) VALUES
    (9100001, 'HQ', '总部', NULL, 9103001, '/9100001/', 10, '2025-01-01', 'ACTIVE'),
    (9100002, 'HR', '人力资源部', 9100001, 9103001, '/9100001/9100002/', 20, '2025-01-01', 'ACTIVE'),
    (9100003, 'ENG', '研发部', 9100001, 9103001, '/9100001/9100003/', 30, '2025-01-01', 'ACTIVE');

INSERT INTO hr_position (id, code, name, job_family, description, sort_no, status) VALUES
    (9100101, 'HR_SPECIALIST_POS', '人力资源专员', 'HR', '负责人力资源业务办理与员工关系管理', 10, 'ACTIVE'),
    (9100102, 'ENGINEER', '软件工程师', 'ENGINEERING', '负责软件开发与技术实现', 20, 'ACTIVE'),
    (9100103, 'MANAGER', '部门负责人', 'MANAGEMENT', '负责部门团队管理与业务协同', 30, 'ACTIVE');

INSERT INTO hr_rank (id, code, name, rank_order, status) VALUES
    (9100201, 'P4', 'P4', 4, 'ACTIVE'),
    (9100202, 'P5', 'P5', 5, 'ACTIVE'),
    (9100203, 'M1', 'M1', 6, 'ACTIVE');

INSERT INTO hr_employee (
    id, employee_no, name, gender, identity_ciphertext, identity_hash, phone_ciphertext, phone_hash,
    department_id, position_id, rank_id, manager_employee_id, employment_status, hire_date,
    probation_start_date, probation_end_date, termination_date
) VALUES
    (9103001, 'HR-001', '李人事', 'FEMALE', NULL, NULL, NULL, NULL, 9100002, 9100101, 9100202, NULL, 'FORMAL', '2025-01-01', NULL, NULL, NULL),
    (9103002, 'EMP-001', '张员工', 'MALE', NULL, NULL, NULL, NULL, 9100003, 9100102, 9100201, 9103001, 'FORMAL', '2025-01-01', NULL, NULL, NULL);

INSERT INTO hr_employee_history (id, employee_id, change_id, event_type, effective_date, snapshot, created_by, created_time)
VALUES
    (
        9106001,
        9103001,
        NULL,
        'BASELINE',
        '2025-01-01',
        JSON_OBJECT(
            'departmentId', 9100002,
            'positionId', 9100101,
            'rankId', 9100202,
            'managerEmployeeId', CAST(NULL AS SIGNED),
            'employmentStatus', 'FORMAL',
            'hireDate', '2025-01-01',
            'probationStartDate', CAST(NULL AS CHAR),
            'probationEndDate', CAST(NULL AS CHAR),
            'terminationDate', CAST(NULL AS CHAR)
        ),
        NULL,
        CURRENT_TIMESTAMP(3)
    ),
    (
        9106002,
        9103002,
        NULL,
        'BASELINE',
        '2025-01-01',
        JSON_OBJECT(
            'departmentId', 9100003,
            'positionId', 9100102,
            'rankId', 9100201,
            'managerEmployeeId', 9103001,
            'employmentStatus', 'FORMAL',
            'hireDate', '2025-01-01',
            'probationStartDate', CAST(NULL AS CHAR),
            'probationEndDate', CAST(NULL AS CHAR),
            'terminationDate', CAST(NULL AS CHAR)
        ),
        NULL,
        CURRENT_TIMESTAMP(3)
    );

INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, annual_quota, attachment_rule, balance_rule, status) VALUES
    (92001, 'ANNUAL', '年假', 1, 1.00, 80.00, NULL, NULL, 'ACTIVE'),
    (9104002, 'SICK', '病假', 0, 1.00, NULL, NULL, NULL, 'ACTIVE'),
    (9104003, 'PERSONAL', '事假', 0, 1.00, NULL, NULL, NULL, 'ACTIVE');

INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours, frozen_hours) VALUES
    (9105001, 9103001, 'ANNUAL', YEAR(CURDATE()), 80.00, 0.00),
    (9105002, 9103002, 'ANNUAL', YEAR(CURDATE()), 80.00, 0.00);

INSERT INTO wf_template (id, code, name, business_type, priority, template_version, status) VALUES
    (95001, 'BASE_LEAVE', '基础请假流程', 'LEAVE', 100, 1, 'ACTIVE'),
    (330700000000010, 'BASE_OVERTIME', '基础加班流程', 'OVERTIME', 100, 1, 'ACTIVE'),
    (330588820598784, 'BASE_PERSONNEL_CHANGE', '基础人事异动流程', 'PERSONNEL_CHANGE', 100, 1, 'ACTIVE');

INSERT INTO wf_template_node (id, template_id, node_no, node_type, approver_rule, timeout_hours) VALUES
    (330700000000001, 95001, 1, 'DIRECT_MANAGER', '{"type": "DIRECT_MANAGER"}', NULL),
    (330700000000011, 330700000000010, 1, 'HR', '{"type": "HR", "roleCode": "HR_SPECIALIST"}', NULL),
    (330588820627456, 330588820598784, 1, 'DIRECT_MANAGER', '{"type": "DIRECT_MANAGER"}', NULL);

-- ----------------------------------------
-- Flyway history bootstrap
-- ----------------------------------------
CREATE TABLE flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50) DEFAULT NULL,
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT DEFAULT NULL,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank),
    KEY flyway_schema_history_s_idx (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES
    (1, '001', 'system and organization', 'SQL', 'V001__system_and_organization.sql', -548551619, 'root', 0, 1),
    (2, '002', 'workflow', 'SQL', 'V002__workflow.sql', 658858766, 'root', 0, 1),
    (3, '003', 'attendance', 'SQL', 'V003__attendance.sql', 647136352, 'root', 0, 1),
    (4, '004', 'performance', 'SQL', 'V004__performance.sql', 94600520, 'root', 0, 1),
    (5, '005', 'performance execution and support', 'SQL', 'V005__performance_execution_and_support.sql', 1218130325, 'root', 0, 1),
    (6, '006', 'seed initial admin', 'SQL', 'V006__seed_initial_admin.sql', -1211577195, 'root', 0, 1),
    (7, '007', 'add workflow template management permission', 'SQL', 'V007__add_workflow_template_management_permission.sql', 1466788739, 'root', 0, 1),
    (8, '008', 'add workflow intervention permission', 'SQL', 'V008__add_workflow_intervention_permission.sql', 1134415365, 'root', 0, 1),
    (9, '009', 'add attendance management permission', 'SQL', 'V009__add_attendance_management_permission.sql', -1930574477, 'root', 0, 1),
    (10, '010', 'add leave balance adjustment permission', 'SQL', 'V010__add_leave_balance_adjustment_permission.sql', -273254341, 'root', 0, 1),
    (11, '011', 'add attendance month summary', 'SQL', 'V011__add_attendance_month_summary.sql', -1557053603, 'root', 0, 1),
    (12, '012', 'add performance configuration permission', 'SQL', 'V012__add_performance_configuration_permission.sql', -420459410, 'root', 0, 1),
    (13, '013', 'localize visible system data', 'SQL', 'V013__localize_visible_system_data.sql', -508353861, 'root', 0, 1),
    (14, '014', 'localize organization reference data', 'SQL', 'V014__localize_organization_reference_data.sql', 1135818073, 'root', 0, 1),
    (15, '015', 'localize remaining test reference data', 'SQL', 'V015__localize_remaining_test_reference_data.sql', 288326476, 'root', 0, 1),
    (16, '016', 'localize remaining visible names', 'SQL', 'V016__localize_remaining_visible_names.sql', 1577523359, 'root', 0, 1),
    (17, '017', 'add report read permission', 'SQL', 'V017__add_report_read_permission.sql', -1883299752, 'root', 0, 1),
    (18, '018', 'add employee account provisioning', 'SQL', 'V018__add_employee_account_provisioning.sql', 608115612, 'root', 0, 1),
    (19, '019', 'configure default leave quota', 'SQL', 'V019__configure_default_leave_quota.sql', 1785371770, 'root', 0, 1),
    (20, '020', 'complete organization master data', 'SQL', 'V020__complete_organization_master_data.sql', -1770340435, 'root', 0, 1),
    (21, '021', 'add personnel change and employee history', 'SQL', 'V021__add_personnel_change_and_employee_history.sql', -551733929, 'root', 0, 1),
    (22, '022', 'add exit handover and personnel permissions', 'SQL', 'V022__add_exit_handover_and_personnel_permissions.sql', -1922619471, 'root', 0, 1),
    (23, '023', 'add organization personnel audit log', 'SQL', 'V023__add_organization_personnel_audit_log.sql', 1180582545, 'root', 0, 1),
    (24, '024', 'route leave workflow to direct manager', 'SQL', 'V024__route_leave_workflow_to_direct_manager.sql', NULL, 'root', 0, 1),
    (25, '025', 'restore attendance submit permission', 'SQL', 'V025__restore_attendance_submit_permission.sql', NULL, 'root', 0, 1);

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS;