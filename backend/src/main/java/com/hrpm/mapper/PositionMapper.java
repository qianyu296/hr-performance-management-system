package com.hrpm.mapper;

import com.hrpm.entity.Position;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface PositionMapper {
    @Select("SELECT id, code, name, job_family AS jobFamily, description, sort_no AS sortNo, status, version FROM hr_position WHERE deleted=0 ORDER BY sort_no, code")
    List<Position> findAll();

    @Select("SELECT id, code, name, job_family AS jobFamily, description, sort_no AS sortNo, status, version FROM hr_position WHERE id=#{id} AND deleted=0")
    Position findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM hr_position WHERE code=#{code} AND deleted=0")
    int countByCode(@Param("code") String code);

    @Insert("INSERT INTO hr_position (id, code, name, job_family, description, sort_no, status) VALUES (#{id},#{code},#{name},#{jobFamily},#{description},#{sortNo},#{status})")
    int insert(Position position);

    @Update("UPDATE hr_position SET name=#{name}, job_family=#{jobFamily}, description=#{description}, sort_no=#{sortNo}, status=#{status}, version=version+1 WHERE id=#{id} AND version=#{version} AND deleted=0")
    int update(Position position);
}
