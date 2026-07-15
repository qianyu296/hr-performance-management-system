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
