package com.hrpm.mapper;

import com.hrpm.entity.WorkCalendar;
import com.hrpm.entity.WorkCalendarDay;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WorkCalendarMapper {
    @Select("SELECT id, calendar_year AS calendarYear, name, time_zone AS timeZone, status, version FROM att_work_calendar WHERE calendar_year = #{year} AND deleted = 0")
    WorkCalendar findByYear(@Param("year") int year);

    @Select("SELECT id, calendar_year AS calendarYear, name, time_zone AS timeZone, status, version FROM att_work_calendar WHERE calendar_year = #{year} AND status = 'ACTIVE' AND deleted = 0")
    WorkCalendar findActiveByYear(@Param("year") int year);

    @Select("SELECT id, calendar_year AS calendarYear, name, time_zone AS timeZone, status, version FROM att_work_calendar WHERE id = #{id} AND deleted = 0")
    WorkCalendar findById(@Param("id") long id);

    @Select("SELECT id, calendar_id AS calendarId, work_date AS workDate, is_workday AS workday, work_hours AS workHours, holiday_name AS holidayName, version FROM att_work_calendar_day WHERE calendar_id = #{calendarId} AND deleted = 0 ORDER BY work_date")
    List<WorkCalendarDay> findDays(@Param("calendarId") long calendarId);

    @Select("SELECT id, calendar_id AS calendarId, work_date AS workDate, is_workday AS workday, work_hours AS workHours, holiday_name AS holidayName, version FROM att_work_calendar_day WHERE calendar_id = #{calendarId} AND work_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0")
    List<WorkCalendarDay> findDaysBetween(@Param("calendarId") long calendarId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Insert("INSERT INTO att_work_calendar (id, calendar_year, name, time_zone, status) VALUES (#{id}, #{calendarYear}, #{name}, #{timeZone}, #{status})")
    int insertCalendar(@Param("id") long id, @Param("calendarYear") int calendarYear, @Param("name") String name, @Param("timeZone") String timeZone, @Param("status") String status);

    @Update("UPDATE att_work_calendar SET name = #{name}, time_zone = #{timeZone}, status = #{status}, version = version + 1 WHERE id = #{id} AND version = #{version} AND deleted = 0")
    int updateCalendar(@Param("id") long id, @Param("name") String name, @Param("timeZone") String timeZone, @Param("status") String status, @Param("version") int version);

    @Update("UPDATE att_work_calendar_day SET deleted = 1, version = version + 1 WHERE calendar_id = #{calendarId} AND deleted = 0")
    int deleteDays(@Param("calendarId") long calendarId);

    @Insert("INSERT INTO att_work_calendar_day (id, calendar_id, work_date, is_workday, work_hours, holiday_name) VALUES (#{id}, #{calendarId}, #{workDate}, #{workday}, #{workHours}, #{holidayName})")
    int insertDay(@Param("id") long id, @Param("calendarId") long calendarId, @Param("workDate") LocalDate workDate, @Param("workday") boolean workday, @Param("workHours") java.math.BigDecimal workHours, @Param("holidayName") String holidayName);
}
