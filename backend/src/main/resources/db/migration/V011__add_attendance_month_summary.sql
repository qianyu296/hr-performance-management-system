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
