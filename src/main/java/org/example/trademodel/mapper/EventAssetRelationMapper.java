package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.EventAssetRelationDO;

import java.util.List;

@Mapper
public interface EventAssetRelationMapper {
    @Insert("INSERT INTO tm_event_asset_relation(relation_id, event_type, event_id, asset_id, symbol, plan_id, "
            + "relation_type, source_reference, trace_id, created_at) VALUES(#{relationId}, #{eventType}, "
            + "#{eventId}, #{assetId}, #{symbol}, #{planId}, #{relationType}, #{sourceReference}, #{traceId}, #{createdAt})")
    int insert(EventAssetRelationDO row);

    @Select("SELECT * FROM tm_event_asset_relation WHERE symbol = #{symbol} "
            + "ORDER BY created_at DESC, relation_id DESC LIMIT #{limit}")
    List<EventAssetRelationDO> listBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    @Select("SELECT * FROM tm_event_asset_relation WHERE event_type = #{eventType} AND event_id = #{eventId} "
            + "ORDER BY created_at DESC, relation_id DESC")
    List<EventAssetRelationDO> listByEvent(@Param("eventType") String eventType,
                                           @Param("eventId") String eventId);
}
