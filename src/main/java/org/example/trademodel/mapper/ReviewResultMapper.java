package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.ReviewResultDO;

import java.util.List;

@Mapper
public interface ReviewResultMapper {

    String COLUMNS = "id, analysis_id AS analysisId, user_id AS userId, "
            + "user_position_id AS userPositionId, review_scope_key AS reviewScopeKey, "
            + "final_plan_id AS finalPlanId, candidate_id AS candidateId, trace_id AS traceId, "
            + "opportunity_id AS opportunityId, resolver_result_id AS resolverResultId, "
            + "validation_result_id AS validationResultId, review_type AS reviewType, outcome, "
            + "execution_deviation AS executionDeviation, ai_assessment AS aiAssessment, "
            + "rule_assessment AS ruleAssessment, rule_feedback AS ruleFeedback, "
            + "metrics_json AS metricsJson, contract_version AS contractVersion, "
            + "error_type AS errorType, actual_outcome AS actualOutcome, missed_reason AS missedReason, "
            + "later_outcome AS laterOutcome, "
            + "adjustment_suggestion AS adjustmentSuggestion, create_time AS createTime, update_time AS updateTime";

    @Select("SELECT " + COLUMNS + " "
            + "FROM tm_review_result WHERE analysis_id = #{analysisId} "
            + "AND review_scope_key = 'SHARED' AND user_id IS NULL AND user_position_id IS NULL")
    ReviewResultDO selectByAnalysisId(String analysisId);

    @Select("SELECT " + COLUMNS + " FROM tm_review_result "
            + "WHERE analysis_id = #{analysisId} AND user_id = #{userId} "
            + "AND user_position_id IS NULL AND review_scope_key = #{reviewScopeKey}")
    ReviewResultDO selectByUserAnalysisScope(@Param("analysisId") String analysisId,
                                             @Param("userId") Long userId,
                                             @Param("reviewScopeKey") String reviewScopeKey);

    @Select("SELECT " + COLUMNS + " FROM tm_review_result review "
            + "WHERE review.analysis_id = #{analysisId} AND review.review_scope_key = 'SHARED' "
            + "AND review.user_id IS NULL AND review.user_position_id IS NULL "
            + "AND EXISTS (SELECT 1 FROM tm_analysis_run analysis "
            + "WHERE analysis.analysis_id = review.analysis_id AND analysis.owner_type = 'USER' "
            + "AND analysis.owner_id = #{userId})")
    ReviewResultDO selectLegacySharedByUserAnalysis(@Param("analysisId") String analysisId,
                                                    @Param("userId") Long userId);

    @Select("SELECT " + COLUMNS + " FROM tm_review_result "
            + "WHERE analysis_id = #{analysisId} AND user_id = #{userId} "
            + "AND user_position_id = #{userPositionId} AND review_scope_key = #{reviewScopeKey}")
    ReviewResultDO selectByUserPositionScope(@Param("analysisId") String analysisId,
                                             @Param("userId") Long userId,
                                             @Param("userPositionId") Long userPositionId,
                                             @Param("reviewScopeKey") String reviewScopeKey);

    @Select("SELECT " + COLUMNS + " FROM tm_review_result "
            + "WHERE review_scope_key = 'SHARED' AND user_id IS NULL AND user_position_id IS NULL "
            + "ORDER BY update_time DESC, create_time DESC, id DESC LIMIT #{limit}")
    List<ReviewResultDO> listRecent(@Param("limit") int limit);

    @Select("SELECT " + COLUMNS + " FROM tm_review_result review "
            + "WHERE (review.user_id = #{userId} AND review.review_scope_key <> 'SHARED') "
            + "OR (review.user_id IS NULL AND review.user_position_id IS NULL "
            + "AND review.review_scope_key = 'SHARED' "
            + "AND EXISTS (SELECT 1 FROM tm_analysis_run analysis "
            + "WHERE analysis.analysis_id = review.analysis_id AND analysis.owner_type = 'USER' "
            + "AND analysis.owner_id = #{userId})) "
            + "ORDER BY review.update_time DESC, review.create_time DESC, review.id DESC LIMIT #{limit}")
    List<ReviewResultDO> listRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT " + COLUMNS + " FROM tm_review_result review "
            + "WHERE review.analysis_id = #{analysisId} "
            + "AND ((review.user_id = #{userId} AND review.review_scope_key <> 'SHARED') "
            + "OR (review.user_id IS NULL AND review.user_position_id IS NULL "
            + "AND review.review_scope_key = 'SHARED' "
            + "AND EXISTS (SELECT 1 FROM tm_analysis_run analysis "
            + "WHERE analysis.analysis_id = review.analysis_id AND analysis.owner_type = 'USER' "
            + "AND analysis.owner_id = #{userId}))) "
            + "ORDER BY review.create_time ASC, review.id ASC")
    List<ReviewResultDO> listByAnalysisIdForUser(@Param("analysisId") String analysisId,
                                                 @Param("userId") Long userId);

    @Insert("INSERT INTO tm_review_result(id, analysis_id, user_id, user_position_id, final_plan_id, candidate_id, trace_id, "
            + "opportunity_id, resolver_result_id, validation_result_id, review_type, outcome, execution_deviation, "
            + "ai_assessment, rule_assessment, rule_feedback, metrics_json, contract_version, review_scope_key, "
            + "error_type, actual_outcome, adjustment_suggestion, missed_reason, later_outcome, create_time, update_time) "
            + "VALUES(#{id}, #{analysisId}, #{userId}, #{userPositionId}, #{finalPlanId}, #{candidateId}, #{traceId}, "
            + "#{opportunityId}, #{resolverResultId}, #{validationResultId}, #{reviewType}, #{outcome}, #{executionDeviation}, "
            + "#{aiAssessment}, #{ruleAssessment}, #{ruleFeedback}, #{metricsJson}, #{contractVersion}, #{reviewScopeKey}, "
            + "#{errorType}, #{actualOutcome}, #{adjustmentSuggestion}, #{missedReason}, #{laterOutcome}, #{createTime}, #{updateTime})")
    int insert(ReviewResultDO row);

    @Update("UPDATE tm_review_result SET review_type = #{reviewType}, outcome = #{outcome}, "
            + "execution_deviation = #{executionDeviation}, ai_assessment = #{aiAssessment}, "
            + "rule_assessment = #{ruleAssessment}, rule_feedback = #{ruleFeedback}, metrics_json = #{metricsJson}, "
            + "final_plan_id = #{finalPlanId}, candidate_id = #{candidateId}, trace_id = #{traceId}, "
            + "opportunity_id = #{opportunityId}, resolver_result_id = #{resolverResultId}, "
            + "validation_result_id = #{validationResultId}, "
            + "contract_version = #{contractVersion}, error_type = #{errorType}, actual_outcome = #{actualOutcome}, "
            + "adjustment_suggestion = #{adjustmentSuggestion}, missed_reason = #{missedReason}, "
            + "later_outcome = #{laterOutcome}, update_time = #{updateTime} "
            + "WHERE analysis_id = #{analysisId} AND review_scope_key = 'SHARED' "
            + "AND user_id IS NULL AND user_position_id IS NULL")
    int updateContentByAnalysisId(ReviewResultDO row);

    @Update("UPDATE tm_review_result SET review_type = #{reviewType}, outcome = #{outcome}, "
            + "execution_deviation = #{executionDeviation}, ai_assessment = #{aiAssessment}, "
            + "rule_assessment = #{ruleAssessment}, rule_feedback = #{ruleFeedback}, metrics_json = #{metricsJson}, "
            + "final_plan_id = #{finalPlanId}, candidate_id = #{candidateId}, trace_id = #{traceId}, "
            + "opportunity_id = #{opportunityId}, resolver_result_id = #{resolverResultId}, "
            + "validation_result_id = #{validationResultId}, contract_version = #{contractVersion}, "
            + "error_type = #{errorType}, actual_outcome = #{actualOutcome}, "
            + "adjustment_suggestion = #{adjustmentSuggestion}, missed_reason = #{missedReason}, "
            + "later_outcome = #{laterOutcome}, update_time = #{updateTime} "
            + "WHERE analysis_id = #{analysisId} AND user_id = #{userId} "
            + "AND user_position_id IS NULL AND review_scope_key = #{reviewScopeKey}")
    int updateContentByUserAnalysisScope(ReviewResultDO row);

    @Update("UPDATE tm_review_result SET review_type = #{reviewType}, outcome = #{outcome}, "
            + "execution_deviation = #{executionDeviation}, ai_assessment = #{aiAssessment}, "
            + "rule_assessment = #{ruleAssessment}, rule_feedback = #{ruleFeedback}, metrics_json = #{metricsJson}, "
            + "final_plan_id = #{finalPlanId}, candidate_id = #{candidateId}, trace_id = #{traceId}, "
            + "opportunity_id = #{opportunityId}, resolver_result_id = #{resolverResultId}, "
            + "validation_result_id = #{validationResultId}, "
            + "contract_version = #{contractVersion}, error_type = #{errorType}, actual_outcome = #{actualOutcome}, "
            + "adjustment_suggestion = #{adjustmentSuggestion}, missed_reason = #{missedReason}, "
            + "later_outcome = #{laterOutcome}, update_time = #{updateTime} "
            + "WHERE analysis_id = #{analysisId} AND user_id = #{userId} "
            + "AND user_position_id = #{userPositionId} AND review_scope_key = #{reviewScopeKey}")
    int updateContentByUserPositionScope(ReviewResultDO row);
}
