package org.example.trademodel.mapper;

import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvidenceItemMapper {
    @Insert("INSERT INTO tm_evidence_item(evidence_id, analysis_id, evidence_type, description, direction, strength, confidence, source, current_value, change_from_baseline, observed_at, freshness, source_provider, source_reference, source_trace_id, external_event_id, external_event_type, event_window_start, event_window_end, impact_score, severity, create_time) " +
            "VALUES(#{evidenceId}, #{analysisId}, #{evidenceType}, #{description}, #{direction}, #{strength}, #{confidence}, #{source}, #{currentValue}, #{changeFromBaseline}, #{observedAt}, #{freshness}, #{sourceProvider}, #{sourceReference}, #{sourceTraceId}, #{externalEventId}, #{externalEventType}, #{eventWindowStart}, #{eventWindowEnd}, #{impactScore}, #{severity}, #{createTime})")
    int insert(EvidenceItemDO evidence);

    @Select("""
            SELECT evidence_type AS evidenceType, description AS description, direction AS direction, source AS source
            FROM tm_evidence_item
            WHERE analysis_id = #{analysisId}
            ORDER BY create_time DESC, evidence_id DESC
            LIMIT 3
            """)
    List<EvidenceBriefVO> selectTop3BriefByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT * FROM tm_evidence_item WHERE analysis_id = #{analysisId} ORDER BY create_time, evidence_id")
    List<EvidenceItemDO> listByAnalysisId(@Param("analysisId") String analysisId);
}
