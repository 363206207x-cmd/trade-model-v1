package org.example.trademodel.mapper;

import org.example.trademodel.entity.RuleConfigDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RuleConfigMapper {

    @Select("SELECT * FROM tm_rule_config WHERE rule_key = #{ruleKey} AND enabled = true")
    RuleConfigDO findByRuleKey(String ruleKey);

    @Select("SELECT * FROM tm_rule_config WHERE rule_key = #{ruleKey}")
    RuleConfigDO findByRuleKeyIncludingDisabled(String ruleKey);

    @Select("SELECT * FROM tm_rule_config WHERE enabled = true")
    List<RuleConfigDO> findAllEnabled();

    @Update("""
            UPDATE tm_rule_config
            SET rule_value = #{ruleValue},
                enabled = #{enabled},
                version = #{version},
                description = #{description},
                rule_type = #{ruleType}
            WHERE rule_key = #{ruleKey}
            """)
    int updateRuleConfigByKey(RuleConfigDO config);

    @Insert("""
            INSERT INTO tm_rule_config(
                rule_id, rule_type, rule_key, rule_value, description, version, enabled
            ) VALUES (
                #{ruleId}, #{ruleType}, #{ruleKey}, #{ruleValue}, #{description}, #{version}, #{enabled}
            )
            """)
    int insertRuleConfig(RuleConfigDO config);
}
