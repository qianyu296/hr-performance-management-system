package com.hrpm.mapper;

import com.hrpm.entity.ReportModels.DepartmentHeadcount;
import com.hrpm.entity.ReportModels.PerformanceLevelDistribution;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper {
    @Select("SELECT d.name AS departmentName, COUNT(e.id) AS headcount FROM hr_department d LEFT JOIN hr_employee e ON e.department_id=d.id AND e.deleted=0 AND e.employment_status <> 'TERMINATED' WHERE d.deleted=0 AND d.status='ACTIVE' GROUP BY d.id,d.name ORDER BY headcount DESC,d.name")
    List<DepartmentHeadcount> departmentHeadcounts();

    @Select("SELECT rv.level_code AS levelCode, COUNT(*) AS count FROM perf_result r JOIN perf_result_version rv ON rv.result_id=r.id AND rv.deleted=0 JOIN perf_task t ON t.id=r.task_id AND t.deleted=0 WHERE r.deleted=0 AND r.publish_status='PUBLISHED' GROUP BY rv.level_code ORDER BY rv.level_code")
    List<PerformanceLevelDistribution> publishedPerformanceLevelDistribution();
}
