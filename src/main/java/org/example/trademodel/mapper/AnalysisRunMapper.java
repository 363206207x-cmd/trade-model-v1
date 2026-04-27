package org.example.trademodel.mapper;

import org.example.trademodel.entity.AnalysisRunDO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AnalysisRunMapper {
    @Insert("INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, rule_version, data_quality_score, trace_id, status) " +
            "VALUES(#{analysisId}, #{symbol}, #{timeframe}, #{analysisTime}, #{ruleVersion}, #{dataQualityScore}, #{traceId}, #{status})")
    int insert(AnalysisRunDO analysisRun);

    @Select("SELECT * FROM tm_analysis_run WHERE analysis_id = #{analysisId}")
    AnalysisRunDO selectById(String analysisId);

    @Select("SELECT COUNT(DISTINCT symbol) FROM tm_analysis_run")
    Integer countDistinctSymbols();

    @Select("SELECT COUNT(*) FROM tm_analysis_run "
            + "WHERE analysis_time >= DATEADD('MINUTE', -#{windowMinutes}, CURRENT_TIMESTAMP)")
    Integer countInWindow(@Param("windowMinutes") int windowMinutes);

    @Select("SELECT COUNT(*) FROM tm_analysis_run "
            + "WHERE analysis_time >= DATEADD('MINUTE', -#{windowMinutes}, CURRENT_TIMESTAMP) "
            + "AND data_quality_score IS NOT NULL AND data_quality_score < #{threshold}")
    Integer countLowQualityInWindow(@Param("windowMinutes") int windowMinutes,
                                    @Param("threshold") int threshold);
}
