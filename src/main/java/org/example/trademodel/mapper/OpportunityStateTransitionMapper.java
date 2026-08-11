package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.OpportunityStateTransitionDO;

import java.util.List;

@Mapper
public interface OpportunityStateTransitionMapper {
    @Insert("INSERT INTO tm_opportunity_state_transition(transition_id, opportunity_id, symbol, timeframe, analysis_id, "
            + "trace_id, from_state, to_state, reason, trigger_source, transition_priority, suppressed, occurred_at) "
            + "VALUES(#{transitionId}, #{opportunityId}, #{symbol}, #{timeframe}, #{analysisId}, #{traceId}, #{fromState}, "
            + "#{toState}, #{reason}, #{triggerSource}, #{transitionPriority}, #{suppressed}, #{occurredAt})")
    int insert(OpportunityStateTransitionDO row);

    @Select("SELECT * FROM tm_opportunity_state_transition WHERE opportunity_id = #{opportunityId} "
            + "ORDER BY occurred_at DESC, transition_id DESC LIMIT #{limit}")
    List<OpportunityStateTransitionDO> listByOpportunityId(@Param("opportunityId") String opportunityId,
                                                           @Param("limit") int limit);
}
