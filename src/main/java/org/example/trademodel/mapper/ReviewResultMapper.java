package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.ReviewResultDO;

@Mapper
public interface ReviewResultMapper {

    @Select("SELECT id, analysis_id AS analysisId, error_type AS errorType, actual_outcome AS actualOutcome, "
            + "adjustment_suggestion AS adjustmentSuggestion, create_time AS createTime, update_time AS updateTime "
            + "FROM tm_review_result WHERE analysis_id = #{analysisId}")
    ReviewResultDO selectByAnalysisId(String analysisId);

    @Insert("INSERT INTO tm_review_result(id, analysis_id, error_type, actual_outcome, adjustment_suggestion, create_time, update_time) "
            + "VALUES(#{id}, #{analysisId}, #{errorType}, #{actualOutcome}, #{adjustmentSuggestion}, #{createTime}, #{updateTime})")
    int insert(ReviewResultDO row);

    @Update("UPDATE tm_review_result SET error_type = #{errorType}, actual_outcome = #{actualOutcome}, "
            + "adjustment_suggestion = #{adjustmentSuggestion}, update_time = #{updateTime} WHERE analysis_id = #{analysisId}")
    int updateContentByAnalysisId(ReviewResultDO row);
}
