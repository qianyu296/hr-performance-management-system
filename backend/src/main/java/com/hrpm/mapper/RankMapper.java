package com.hrpm.mapper;

import com.hrpm.entity.Rank;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface RankMapper {
    @Select("SELECT id, code, name, rank_order AS rankOrder, status, version FROM hr_rank WHERE deleted=0 ORDER BY rank_order, code")
    List<Rank> findAll();

    @Select("SELECT id, code, name, rank_order AS rankOrder, status, version FROM hr_rank WHERE id=#{id} AND deleted=0")
    Rank findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM hr_rank WHERE code=#{code} AND deleted=0")
    int countByCode(@Param("code") String code);

    @Insert("INSERT INTO hr_rank (id, code, name, rank_order, status) VALUES (#{id},#{code},#{name},#{rankOrder},#{status})")
    int insert(Rank rank);

    @Update("UPDATE hr_rank SET name=#{name}, rank_order=#{rankOrder}, status=#{status}, version=version+1 WHERE id=#{id} AND version=#{version} AND deleted=0")
    int update(Rank rank);
}
