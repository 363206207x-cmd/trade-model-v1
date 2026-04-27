package org.example.trademodel.mapper;

import org.example.trademodel.entity.RuleConfigDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RuleConfigMapper {

    @Select("SELECT * FROM tm_rule_config WHERE rule_key = #{ruleKey} AND enabled = true")
    RuleConfigDO findByRuleKey(String ruleKey);

    @Select("SELECT * FROM tm_rule_config WHERE enabled = true")
    List<RuleConfigDO> findAllEnabled();
}
