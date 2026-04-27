package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO;

import java.util.List;

@Mapper
public interface PushRecheckDispatchConfigAuditMapper {

    @Insert("INSERT INTO tm_push_recheck_dispatch_config_audit(config_key, old_value, new_value, changed_by, change_source, create_time) "
            + "VALUES(#{configKey}, #{oldValue}, #{newValue}, #{changedBy}, #{changeSource}, #{createTime})")
    int insert(PushRecheckDispatchConfigAuditDO row);

    @Select("SELECT audit_id AS auditId, config_key AS configKey, old_value AS oldValue, new_value AS newValue, "
            + "changed_by AS changedBy, change_source AS changeSource, create_time AS createTime "
            + "FROM tm_push_recheck_dispatch_config_audit ORDER BY audit_id DESC LIMIT #{limit}")
    List<PushRecheckDispatchConfigAuditDO> selectRecent(@Param("limit") int limit);
}
