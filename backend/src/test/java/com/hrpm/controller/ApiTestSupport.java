package com.hrpm.controller;

import com.hrpm.service.TokenService;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Reusable SQL fixtures for controller integration tests. */
public final class ApiTestSupport {
    private static final AtomicLong RELATION_ID = new AtomicLong(8_900_000_000_000_000_000L);

    private final JdbcTemplate jdbcTemplate;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ApiTestSupport(JdbcTemplate jdbcTemplate, TokenService tokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenService = tokenService;
    }

    public void insertDepartment(long id, String code, String name, Long parentId) {
        String path = parentId == null
                ? "/%d/".formatted(id)
                : jdbcTemplate.queryForObject("SELECT path FROM hr_department WHERE id = ? AND deleted = 0", String.class, parentId)
                        + id + "/";
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, parent_id, path, effective_date, status, sort_no)
                VALUES (?, ?, ?, ?, ?, CURRENT_DATE, 'ACTIVE', 0)
                """, id, code, name, parentId, path);
    }

    public void insertPosition(long id, String code, String name) {
        jdbcTemplate.update("""
                INSERT INTO hr_position (id, code, name, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """, id, code, name);
    }

    public void insertRank(long id, String code, String name, int rankOrder) {
        jdbcTemplate.update("""
                INSERT INTO hr_rank (id, code, name, rank_order, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                """, id, code, name, rankOrder);
    }

    public void insertEmployee(long id, String employeeNo, String name, long departmentId, long positionId, long rankId) {
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, rank_id, employment_status, hire_date)
                VALUES (?, ?, ?, ?, ?, ?, 'FORMAL', CURRENT_DATE)
                """, id, employeeNo, name, departmentId, positionId, rankId);
    }

    public void insertUser(long id, String username, String password, Long employeeId, int sessionVersion) {
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version)
                VALUES (?, ?, ?, ?, 'ACTIVE', ?)
                """, id, username, passwordEncoder.encode(password), employeeId, sessionVersion);
    }

    public void insertRole(long id, String code, String name) {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, ?, ?, 'ACTIVE')", id, code, name);
    }

    public void insertMenu(long id, String name, String permissionCode) {
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (?, ?, ?, 'BUTTON', 'ACTIVE')
                """, id, name, permissionCode);
    }

    public void assignUserRole(long userId, long roleId) {
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", nextRelationId(), userId, roleId);
    }

    public void assignRoleMenu(long roleId, long menuId) {
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", nextRelationId(), roleId, menuId);
    }

    public void insertDataScope(long id, String scopeType, String name) {
        jdbcTemplate.update("INSERT INTO sys_data_scope (id, scope_type, name) VALUES (?, ?, ?)", id, scopeType, name);
    }

    public void assignRoleScope(long roleId, String scopeType, Long scopeId) {
        jdbcTemplate.update("""
                INSERT INTO sys_role_data_scope (id, role_id, scope_type, scope_id)
                VALUES (?, ?, ?, ?)
                """, nextRelationId(), roleId, scopeType, scopeId);
    }

    public void assignScopeDepartment(long scopeId, long departmentId) {
        jdbcTemplate.update("""
                INSERT INTO sys_data_scope_dept (id, scope_id, department_id)
                VALUES (?, ?, ?)
                """, nextRelationId(), scopeId, departmentId);
    }

    public String authorizationHeader(long userId, String username, int sessionVersion) {
        return "Bearer " + tokenService.issueAccess(userId, username, sessionVersion);
    }

    public void cleanup(FixtureIds fixtureIds) {
        fixtureIds.scopeIds().forEach(id -> jdbcTemplate.update("DELETE FROM sys_data_scope_dept WHERE scope_id = ?", id));
        fixtureIds.roleIds().forEach(id -> {
            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", id);
            jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id = ?", id);
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE role_id = ?", id);
        });
        fixtureIds.userIds().forEach(id -> {
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", id);
        });
        fixtureIds.menuIds().forEach(id -> jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", id));
        fixtureIds.roleIds().forEach(id -> jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", id));
        fixtureIds.scopeIds().forEach(id -> jdbcTemplate.update("DELETE FROM sys_data_scope WHERE id = ?", id));
        fixtureIds.employeeIds().forEach(id -> jdbcTemplate.update("DELETE FROM hr_employee WHERE id = ?", id));
        fixtureIds.positionIds().forEach(id -> jdbcTemplate.update("DELETE FROM hr_position WHERE id = ?", id));
        fixtureIds.rankIds().forEach(id -> jdbcTemplate.update("DELETE FROM hr_rank WHERE id = ?", id));
        fixtureIds.departmentIds().forEach(id -> jdbcTemplate.update("DELETE FROM hr_department WHERE id = ?", id));
    }

    private long nextRelationId() {
        return RELATION_ID.incrementAndGet();
    }

    public record FixtureIds(
            List<Long> userIds,
            List<Long> roleIds,
            List<Long> menuIds,
            List<Long> scopeIds,
            List<Long> employeeIds,
            List<Long> positionIds,
            List<Long> rankIds,
            List<Long> departmentIds) {
    }
}
