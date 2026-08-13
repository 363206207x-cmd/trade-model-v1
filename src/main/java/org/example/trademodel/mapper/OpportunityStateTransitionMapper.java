package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.OpportunityStateTransitionDO;

import java.util.List;

@Mapper
public interface OpportunityStateTransitionMapper {
    @Insert("INSERT INTO tm_opportunity_state_transition(transition_id, opportunity_id, owner_type, owner_id, asset_id, symbol, timeframe, analysis_id, "
            + "trace_id, rule_version, from_state, to_state, reason, trigger_source, transition_priority, suppressed, occurred_at) "
            + "VALUES(#{transitionId}, #{opportunityId}, #{ownerType}, #{ownerId}, #{assetId}, #{symbol}, #{timeframe}, #{analysisId}, #{traceId}, #{ruleVersion}, #{fromState}, "
            + "#{toState}, #{reason}, #{triggerSource}, #{transitionPriority}, #{suppressed}, #{occurredAt})")
    int insert(OpportunityStateTransitionDO row);

    @Select("SELECT * FROM tm_opportunity_state_transition WHERE opportunity_id = #{opportunityId} "
            + "ORDER BY occurred_at DESC, transition_id DESC LIMIT #{limit}")
    List<OpportunityStateTransitionDO> listByOpportunityId(@Param("opportunityId") String opportunityId,
                                                           @Param("limit") int limit);

    @Select("SELECT transition.* FROM tm_opportunity_state_transition transition "
            + "JOIN tm_asset_state opportunity ON opportunity.opportunity_id = transition.opportunity_id "
            + "WHERE transition.opportunity_id = #{opportunityId} AND ("
            + "(opportunity.owner_type = 'USER' AND opportunity.owner_id = #{userId}) OR "
            + "(opportunity.owner_type = 'SYSTEM' AND opportunity.owner_id = 0 AND opportunity.pool_item_id IN ("
            + "SELECT system_pool.id FROM tm_asset_pool_item system_pool "
            + "WHERE system_pool.owner_type = 'SYSTEM' AND system_pool.owner_id = 0 "
            + "AND system_pool.active = TRUE AND NOT EXISTS (SELECT 1 FROM tm_asset_pool_item user_override "
            + "WHERE user_override.owner_type = 'USER' AND user_override.owner_id = #{userId} "
            + "AND user_override.symbol = system_pool.symbol AND user_override.active = FALSE))))) "
            + "ORDER BY transition.occurred_at DESC, transition.transition_id DESC LIMIT #{limit}")
    List<OpportunityStateTransitionDO> listReadableByUser(@Param("opportunityId") String opportunityId,
                                                          @Param("userId") Long userId,
                                                          @Param("limit") int limit);
}
