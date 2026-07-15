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
