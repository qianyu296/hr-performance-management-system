package com.hrpm.mapper;


import com.hrpm.entity.WorkflowTask;
import com.hrpm.entity.WorkflowTaskListRow;
import com.hrpm.entity.WorkflowTemplate;
import com.hrpm.entity.WorkflowTemplateDefinition;
import com.hrpm.entity.WorkflowTemplateNode;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface WorkflowMapper {
    @Select("""
            SELECT t.id, t.template_version AS templateVersion
            FROM wf_template t
            LEFT JOIN wf_template_scope s ON s.template_id = t.id AND s.deleted = 0
            WHERE t.business_type = 'LEAVE' AND t.status = 'ACTIVE' AND t.deleted = 0
              AND (s.department_id = #{departmentId} OR s.id IS NULL)
            ORDER BY CASE WHEN s.department_id = #{departmentId} THEN 1 ELSE 0 END DESC,
                     t.priority DESC, t.template_version DESC
            LIMIT 1
            """)
    WorkflowTemplate findLeaveTemplate(@Param("departmentId") long departmentId);

    @Select("""
            SELECT id, code, name, business_type AS businessType, priority, template_version AS templateVersion, status, version
            FROM wf_template
            WHERE deleted = 0
            ORDER BY business_type, code, template_version DESC
            """)
    List<WorkflowTemplateDefinition> findTemplates();

    @Select("""
            SELECT id, code, name, business_type AS businessType, priority, template_version AS templateVersion, status, version
            FROM wf_template
            WHERE id = #{id} AND deleted = 0
            """)
    WorkflowTemplateDefinition findTemplateById(@Param("id") long id);

    @Select("SELECT department_id FROM wf_template_scope WHERE template_id = #{templateId} AND deleted = 0 ORDER BY department_id")
    List<Long> findScopeDepartmentIds(@Param("templateId") long templateId);

    @Select("SELECT COUNT(*) FROM wf_template WHERE business_type = #{businessType} AND code = #{code} AND template_version = #{templateVersion} AND deleted = 0")
    int countByCodeAndVersion(@Param("businessType") String businessType, @Param("code") String code, @Param("templateVersion") int templateVersion);

    @Insert("INSERT INTO wf_template (id, code, name, business_type, priority, template_version, status) VALUES (#{id}, #{code}, #{name}, #{businessType}, #{priority}, #{templateVersion}, #{status})")
    int insertTemplate(@Param("id") long id, @Param("code") String code, @Param("name") String name,
            @Param("businessType") String businessType, @Param("priority") int priority,
            @Param("templateVersion") int templateVersion, @Param("status") String status);

    @Update("UPDATE wf_template SET name = #{name}, business_type = #{businessType}, priority = #{priority}, status = #{status}, version = version + 1 WHERE id = #{id} AND version = #{version} AND deleted = 0")
    int updateTemplate(@Param("id") long id, @Param("name") String name, @Param("businessType") String businessType,
            @Param("priority") int priority, @Param("status") String status, @Param("version") int version);

    @Update("UPDATE wf_template_scope SET deleted = 1, version = version + 1 WHERE template_id = #{templateId} AND deleted = 0")
    int deleteScopes(@Param("templateId") long templateId);

    @Update("UPDATE wf_template_node SET deleted = 1, version = version + 1 WHERE template_id = #{templateId} AND deleted = 0")
    int deleteNodes(@Param("templateId") long templateId);

    @Insert("INSERT INTO wf_template_scope (id, template_id, department_id) VALUES (#{id}, #{templateId}, #{departmentId})")
    int insertScope(@Param("id") long id, @Param("templateId") long templateId, @Param("departmentId") long departmentId);

    @Insert("INSERT INTO wf_template_node (id, template_id, node_no, node_type, approver_rule) VALUES (#{id}, #{templateId}, #{nodeNo}, #{nodeType}, CAST(#{approverRule} AS JSON))")
    int insertNode(@Param("id") long id, @Param("templateId") long templateId, @Param("nodeNo") int nodeNo,
            @Param("nodeType") String nodeType, @Param("approverRule") String approverRule);

    @Select("SELECT COUNT(*) FROM hr_department WHERE id = #{id} AND status = 'ACTIVE' AND deleted = 0")
    int countActiveDepartment(@Param("id") long id);

    @Select("""
            SELECT node_no AS nodeNo, node_type AS nodeType, approver_rule AS approverRule
            FROM wf_template_node
            WHERE template_id = #{templateId} AND deleted = 0
            ORDER BY node_no
            LIMIT 1
            """)
    WorkflowTemplateNode findFirstNode(@Param("templateId") long templateId);

    @Select("""
            SELECT node_no AS nodeNo, node_type AS nodeType, approver_rule AS approverRule
            FROM wf_template_node
            WHERE template_id = #{templateId} AND deleted = 0
            ORDER BY node_no
            """)
    List<WorkflowTemplateNode> findNodes(@Param("templateId") long templateId);

    @Insert("""
            INSERT INTO wf_instance (id, business_type, business_id, initiator_user_id, template_snapshot, status, current_node_no)
            VALUES (#{id}, 'LEAVE', #{businessId}, #{initiatorUserId}, #{templateSnapshot}, 'IN_PROGRESS', #{nodeNo})
            """)
    int insertInstance(@Param("id") long id, @Param("businessId") long businessId,
            @Param("initiatorUserId") long initiatorUserId, @Param("templateSnapshot") String templateSnapshot,
            @Param("nodeNo") int nodeNo);

    @Insert("""
            INSERT INTO wf_task (id, instance_id, node_no, node_snapshot, assignee_user_id, status)
            VALUES (#{id}, #{instanceId}, #{nodeNo}, #{nodeSnapshot}, #{assigneeUserId}, 'PENDING')
            """)
    int insertTask(@Param("id") long id, @Param("instanceId") long instanceId, @Param("nodeNo") int nodeNo,
            @Param("nodeSnapshot") String nodeSnapshot, @Param("assigneeUserId") long assigneeUserId);

    @Select("""
            SELECT t.id, t.instance_id AS instanceId, t.node_no AS nodeNo, t.node_snapshot AS nodeSnapshot,
                   t.assignee_user_id AS assigneeUserId, t.status, t.version,
                   i.business_id AS businessId
            FROM wf_task t JOIN wf_instance i ON i.id = t.instance_id AND i.deleted = 0
            WHERE t.id = #{id} AND t.deleted = 0
            """)
    WorkflowTask findTask(@Param("id") long id);

    @Select("""
            SELECT template_snapshot
            FROM wf_instance
            WHERE id = #{id} AND deleted = 0
            """)
    String findInstanceSnapshot(@Param("id") long id);

    @Select("""
            SELECT id
            FROM sys_user
            WHERE id = #{id} AND status = 'ACTIVE' AND deleted = 0
            """)
    Long findActiveUserId(@Param("id") long id);

    @Select("""
            SELECT u.id
            FROM hr_employee applicant
            JOIN hr_employee manager ON manager.id = applicant.manager_employee_id
              AND manager.deleted = 0 AND manager.employment_status IN ('FORMAL', 'PROBATION')
            JOIN sys_user u ON u.employee_id = manager.id AND u.status = 'ACTIVE' AND u.deleted = 0
            WHERE applicant.id = #{employeeId} AND applicant.deleted = 0
            """)
    Long findDirectManagerUserId(@Param("employeeId") long employeeId);

    @Select("""
            SELECT u.id
            FROM hr_department d
            JOIN hr_employee leader ON leader.id = d.leader_employee_id
              AND leader.deleted = 0 AND leader.employment_status IN ('FORMAL', 'PROBATION')
            JOIN sys_user u ON u.employee_id = leader.id AND u.status = 'ACTIVE' AND u.deleted = 0
            WHERE d.id = #{departmentId} AND d.status = 'ACTIVE' AND d.deleted = 0
            """)
    Long findDepartmentLeaderUserId(@Param("departmentId") long departmentId);

    @Select("""
            SELECT u.id
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 'ACTIVE'
            WHERE u.status = 'ACTIVE' AND u.deleted = 0 AND r.code = #{roleCode}
            ORDER BY u.id
            """)
    List<Long> findActiveUserIdsByRoleCode(@Param("roleCode") String roleCode);

    @Update("UPDATE wf_instance SET current_node_no = #{nodeNo}, version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS'")
    int advanceInstance(@Param("id") long id, @Param("nodeNo") int nodeNo);

    @Select("""
            SELECT t.id, i.business_type AS businessType, i.business_id AS businessId,
                   r.request_no AS requestNo, e.name AS applicantName, lt.name AS leaveTypeName,
                   r.start_time AS startTime, r.end_time AS endTime, r.duration_hours AS durationHours,
                   t.status, t.version
            FROM wf_task t
            JOIN wf_instance i ON i.id = t.instance_id AND i.deleted = 0
            JOIN att_leave_request r ON r.id = i.business_id AND r.deleted = 0
            JOIN hr_employee e ON e.id = r.employee_id AND e.deleted = 0
            JOIN att_leave_type lt ON lt.id = r.leave_type_id AND lt.deleted = 0
            WHERE t.assignee_user_id = #{userId} AND t.status = 'PENDING' AND t.deleted = 0
            ORDER BY t.created_time DESC, t.id DESC
            """)
    List<WorkflowTaskListRow> listPendingTasks(@Param("userId") long userId);

    @Update("UPDATE wf_task SET status = 'APPROVED', version = version + 1 WHERE id = #{id} AND assignee_user_id = #{userId} AND status = 'PENDING' AND version = #{version}")
    int approveTask(@Param("id") long id, @Param("userId") long userId, @Param("version") int version);

    @Update("UPDATE wf_task SET status = 'REJECTED', version = version + 1 WHERE id = #{id} AND assignee_user_id = #{userId} AND status = 'PENDING' AND version = #{version}")
    int rejectTask(@Param("id") long id, @Param("userId") long userId, @Param("version") int version);

    @Update("UPDATE wf_instance SET status = 'APPROVED', current_node_no = NULL, version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS'")
    int approveInstance(@Param("id") long id);

    @Update("UPDATE wf_instance SET status = 'REJECTED', current_node_no = NULL, version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS'")
    int rejectInstance(@Param("id") long id);

    @Insert("INSERT INTO wf_action_log (id, instance_id, task_id, actor_user_id, action, comment) VALUES (#{id}, #{instanceId}, #{taskId}, #{actorUserId}, #{action}, #{comment})")
    int insertActionLog(@Param("id") long id, @Param("instanceId") long instanceId, @Param("taskId") long taskId,
            @Param("actorUserId") long actorUserId, @Param("action") String action, @Param("comment") String comment);
}
