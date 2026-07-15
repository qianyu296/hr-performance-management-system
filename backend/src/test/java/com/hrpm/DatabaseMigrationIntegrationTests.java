package com.hrpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class DatabaseMigrationIntegrationTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigratesPersonnelPersistenceTables() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'sys_user',
                      'hr_personnel_change',
                      'hr_employee_history',
                      'hr_exit_handover',
                      'hr_exit_handover_item',
                      'sys_operation_log'
                  )
                """, Integer.class);

        assertEquals(6, tableCount);
    }

    @Test
    void flywaySeedsPersonnelPermissionsForSuperAdminAndHrRole() {
        Integer permissionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_menu
                WHERE permission_code IN (
                    'personnel:read',
                    'personnel:create',
                    'personnel:manage',
                    'personnel:approve',
                    'personnel:execute'
                )
                  AND deleted = 0
                """, Integer.class);
        Integer superAdminGrantCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role_menu rm
                JOIN sys_role r ON r.id = rm.role_id AND r.deleted = 0
                JOIN sys_menu m ON m.id = rm.menu_id AND m.deleted = 0
                WHERE r.code = 'SUPER_ADMIN'
                  AND m.permission_code IN (
                      'personnel:read',
                      'personnel:create',
                      'personnel:manage',
                      'personnel:approve',
                      'personnel:execute'
                  )
                  AND rm.deleted = 0
                """, Integer.class);
        Integer hrGrantCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role_menu rm
                JOIN sys_role r ON r.id = rm.role_id AND r.deleted = 0
                JOIN sys_menu m ON m.id = rm.menu_id AND m.deleted = 0
                WHERE r.code = 'HR_SPECIALIST'
                  AND m.permission_code IN (
                      'personnel:read',
                      'personnel:create',
                      'personnel:manage',
                      'personnel:approve'
                  )
                  AND rm.deleted = 0
                """, Integer.class);

        assertEquals(5, permissionCount);
        assertEquals(5, superAdminGrantCount);
        assertEquals(4, hrGrantCount);
    }

    @Test
    void flywayAddsExpectedPersonnelIndexes() {
        Integer personnelChangeIndexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'hr_personnel_change'
                  AND index_name IN (
                      'uk_hr_personnel_change_no',
                      'idx_hr_personnel_change_employee_status',
                      'idx_hr_personnel_change_workflow'
                  )
                """, Integer.class);
        Integer exitHandoverIndexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'hr_exit_handover'
                  AND index_name IN ('uk_hr_exit_handover_change', 'idx_hr_exit_handover_change_status')
                """, Integer.class);

        assertEquals(3, personnelChangeIndexCount);
        assertEquals(2, exitHandoverIndexCount);
    }

    @Test
    void flywayEnforcesUniquePersonnelChangeNumbersForActiveRecords() {
        long firstId = 991001L;
        long secondId = 991002L;
        String changeNo = "PC-MIG-" + System.nanoTime();

        try {
            jdbcTemplate.update("""
                    INSERT INTO hr_personnel_change (
                        id, change_no, employee_id, change_type, application_date, effective_date, reason,
                        before_snapshot, after_snapshot, workflow_instance_id, status, created_by, updated_by
                    ) VALUES (?, ?, NULL, 'ONBOARD', ?, ?, 'migration test', NULL, JSON_OBJECT('employeeNo', 'TEMP-001'), NULL, 'DRAFT', NULL, NULL)
                    """, firstId, changeNo, LocalDate.now(), LocalDate.now());

            assertThrows(DuplicateKeyException.class, () -> jdbcTemplate.update("""
                    INSERT INTO hr_personnel_change (
                        id, change_no, employee_id, change_type, application_date, effective_date, reason,
                        before_snapshot, after_snapshot, workflow_instance_id, status, created_by, updated_by
                    ) VALUES (?, ?, NULL, 'ONBOARD', ?, ?, 'duplicate migration test', NULL, JSON_OBJECT('employeeNo', 'TEMP-002'), NULL, 'DRAFT', NULL, NULL)
                    """, secondId, changeNo, LocalDate.now(), LocalDate.now()));
        } finally {
            jdbcTemplate.update("DELETE FROM hr_personnel_change WHERE id IN (?, ?)", firstId, secondId);
        }
    }
}
