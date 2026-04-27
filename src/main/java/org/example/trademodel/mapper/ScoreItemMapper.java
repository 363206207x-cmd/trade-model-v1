package org.example.trademodel.mapper;

import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScoreItemMapper {
    @Insert("INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value, weight, direction, description) " +
            "VALUES(#{scoreId}, #{analysisId}, #{scoreType}, #{scoreValue}, #{weight}, #{direction}, #{description})")
    int insert(ScoreItemDO score);

    @Select("""
            SELECT score_type AS scoreType, score_value AS scoreValue
            FROM tm_score_item
            WHERE analysis_id = #{analysisId}
            ORDER BY score_id DESC
            LIMIT 3
            """)
    List<ScoreBriefVO> selectTop3BriefByAnalysisId(@Param("analysisId") String analysisId);
}
