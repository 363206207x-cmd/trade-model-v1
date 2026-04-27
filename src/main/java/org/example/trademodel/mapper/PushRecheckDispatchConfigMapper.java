package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.PushRecheckDispatchConfigDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PushRecheckDispatchConfigMapper {

    @Select("SELECT config_key AS configKey, config_value AS configValue, updated_by AS updatedBy, "
            + "update_source AS updateSource, update_time AS updateTime "
            + "FROM tm_push_recheck_dispatch_config")
    List<PushRecheckDispatchConfigDO> selectAll();

    @Insert("INSERT INTO tm_push_recheck_dispatch_config(config_key, config_value, updated_by, update_source, update_time) "
            + "VALUES(#{configKey}, #{configValue}, #{updatedBy}, #{updateSource}, #{updateTime})")
    int insert(PushRecheckDispatchConfigDO row);

    @Update("UPDATE tm_push_recheck_dispatch_config SET config_value = #{configValue}, updated_by = #{updatedBy}, "
            + "update_source = #{updateSource}, update_time = #{updateTime} WHERE config_key = #{configKey}")
    int updateValue(@Param("configKey") String configKey,
                    @Param("configValue") Integer configValue,
                    @Param("updatedBy") String updatedBy,
                    @Param("updateSource") String updateSource,
                    @Param("updateTime") LocalDateTime updateTime);
}
