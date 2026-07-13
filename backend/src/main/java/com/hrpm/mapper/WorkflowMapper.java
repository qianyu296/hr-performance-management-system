package com.hrpm.mapper;


import com.hrpm.entity.WorkflowTask;
import com.hrpm.entity.WorkflowTaskListRow;
import com.hrpm.entity.WorkflowTemplate;
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
            SELECT node_no AS nodeNo, approver_rule AS approverRule
            FROM wf_template_node
            WHERE template_id = #{templateId} AND deleted = 0
            ORDER BY node_no
            LIMIT 1
            """)
    WorkflowTemplateNode findFirstNode(@Param("templateId") long templateId);

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
            SELECT t.id, t.instance_id AS instanceId, t.node_no AS nodeNo, t.assignee_user_id AS assigneeUserId, t.status, t.version,
                   i.business_id AS businessId
            FROM wf_task t JOIN wf_instance i ON i.id = t.instance_id AND i.deleted = 0
            WHERE t.id = #{id} AND t.deleted = 0
            """)
    WorkflowTask findTask(@Param("id") long id);

    @Select("""
            SELECT n.node_no AS nodeNo, n.approver_rule AS approverRule
            FROM wf_instance i
            JOIN wf_template_node n ON n.template_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(i.template_snapshot, '$.templateId')) AS UNSIGNED)
              AND n.deleted = 0
            WHERE i.id = #{instanceId} AND n.node_no > #{nodeNo}
            ORDER BY n.node_no
            LIMIT 1
            """)
    WorkflowTemplateNode findNextNode(@Param("instanceId") long instanceId, @Param("nodeNo") int nodeNo);

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
