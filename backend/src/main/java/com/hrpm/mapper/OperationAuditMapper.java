package com.hrpm.mapper;

import com.hrpm.entity.OperationAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationAuditMapper {
    @Insert("""
            INSERT INTO sys_operation_log (
                id, actor_user_id, module, object_type, object_id, action, result, trace_id, summary, source_address
            ) VALUES (
                #{log.id}, #{log.actorUserId}, #{log.module}, #{log.objectType}, #{log.objectId},
                #{log.action}, #{log.result}, #{log.traceId}, CAST(#{summary} AS JSON), #{log.sourceAddress}
            )
            """)
    int insert(@Param("log") OperationAuditLog log, @Param("summary") String summary);
}
