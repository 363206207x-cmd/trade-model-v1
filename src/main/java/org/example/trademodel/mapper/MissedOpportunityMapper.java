package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.MissedOpportunityDO;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MissedOpportunityMapper {

    @Insert("INSERT INTO tm_missed_opportunity(missed_id, decision_id, analysis_id, symbol, biz_date, reason_json, rule_version, trace_id, create_time) "
            + "VALUES(#{missedId}, #{decisionId}, #{analysisId}, #{symbol}, #{bizDate}, #{reasonJson}, #{ruleVersion}, #{traceId}, #{createTime})")
    int insert(MissedOpportunityDO row);

    @Select("SELECT * FROM tm_missed_opportunity WHERE missed_id = #{missedId}")
    MissedOpportunityDO selectByMissedId(String missedId);

    @Select("SELECT missed.* FROM tm_missed_opportunity missed "
            + "JOIN tm_analysis_run analysis ON analysis.analysis_id = missed.analysis_id "
            + "WHERE missed.missed_id = #{missedId} AND analysis.owner_type = 'USER' "
            + "AND analysis.owner_id = #{userId}")
    MissedOpportunityDO selectByMissedIdForUser(@Param("missedId") String missedId,
                                                @Param("userId") Long userId);

    @Select("SELECT * FROM tm_missed_opportunity WHERE decision_id = #{decisionId} ORDER BY create_time DESC")
    List<MissedOpportunityDO> listByDecisionId(String decisionId);

    @Select("SELECT * FROM tm_missed_opportunity WHERE symbol = #{symbol} ORDER BY create_time DESC LIMIT #{limit}")
    List<MissedOpportunityDO> listBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tm_missed_opportunity WHERE biz_date = #{bizDate}")
    int countByBizDate(LocalDate bizDate);

    @Select("SELECT COUNT(*) FROM tm_missed_opportunity missed "
            + "JOIN tm_analysis_run analysis ON analysis.analysis_id = missed.analysis_id "
            + "WHERE missed.biz_date = #{bizDate} AND analysis.owner_type = 'USER' "
            + "AND analysis.owner_id = #{userId}")
    int countByBizDateForUser(@Param("userId") Long userId,
                              @Param("bizDate") LocalDate bizDate);

    @Select("SELECT * FROM tm_missed_opportunity WHERE analysis_id = #{analysisId} ORDER BY create_time DESC")
    List<MissedOpportunityDO> listByAnalysisId(String analysisId);

    @Select("SELECT * FROM tm_missed_opportunity WHERE biz_date = #{bizDate} ORDER BY create_time DESC LIMIT #{limit}")
    List<MissedOpportunityDO> listByBizDate(@Param("bizDate") LocalDate bizDate, @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT * FROM tm_missed_opportunity",
            "WHERE 1=1",
            "<if test='analysisId != null and analysisId != \"\"'>",
            "  AND analysis_id = #{analysisId}",
            "</if>",
            "<if test='symbol != null and symbol != \"\"'>",
            "  AND symbol = #{symbol}",
            "</if>",
            "<if test='bizDate != null'>",
            "  AND biz_date = #{bizDate}",
            "</if>",
            "ORDER BY create_time DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<MissedOpportunityDO> listByQuery(@Param("analysisId") String analysisId,
                                          @Param("symbol") String symbol,
                                          @Param("bizDate") LocalDate bizDate,
                                          @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT missed.* FROM tm_missed_opportunity missed",
            "JOIN tm_analysis_run analysis ON analysis.analysis_id = missed.analysis_id",
            "WHERE analysis.owner_type = 'USER' AND analysis.owner_id = #{userId}",
            "<if test='analysisId != null and analysisId != \"\"'>",
            "  AND missed.analysis_id = #{analysisId}",
            "</if>",
            "<if test='symbol != null and symbol != \"\"'>",
            "  AND missed.symbol = #{symbol}",
            "</if>",
            "<if test='bizDate != null'>",
            "  AND missed.biz_date = #{bizDate}",
            "</if>",
            "ORDER BY missed.create_time DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<MissedOpportunityDO> listByQueryForUser(@Param("userId") Long userId,
                                                 @Param("analysisId") String analysisId,
                                                 @Param("symbol") String symbol,
                                                 @Param("bizDate") LocalDate bizDate,
                                                 @Param("limit") int limit);
}
