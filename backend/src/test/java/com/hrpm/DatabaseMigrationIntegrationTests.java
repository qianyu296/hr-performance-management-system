package com.hrpm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class DatabaseMigrationIntegrationTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigratesSystemAttendanceAndPerformanceTables() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('sys_user', 'att_leave_request', 'perf_task', 'sys_operation_log')
                """, Integer.class);

        assertEquals(4, tableCount);
    }
}
