package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.RuleVersionLogDO;

import java.util.List;

@Mapper
public interface RuleVersionLogMapper {

    @Insert("INSERT INTO tm_rule_version_log(id, analysis_id, rule_version, error_type, change_category, change_summary, change_detail, operator, publish_time, "
            + "rollback_flag, created_by, updated_by, is_deleted, version_no) "
            + "VALUES(#{id}, #{analysisId}, #{ruleVersion}, #{errorType}, #{changeCategory}, #{changeSummary}, #{changeDetail}, #{operator}, #{publishTime}, "
            + "#{rollbackFlag}, #{createdBy}, #{updatedBy}, COALESCE(#{isDeleted}, 0), COALESCE(#{versionNo}, 1))")
    int insert(RuleVersionLogDO row);

    @Select({
            "<script>",
            "SELECT id, analysis_id AS analysisId, rule_version AS ruleVersion, error_type AS errorType,",
            "change_category AS changeCategory, change_summary AS changeSummary, change_detail AS changeDetail,",
            "operator, publish_time AS publishTime, rollback_flag AS rollbackFlag, created_by AS createdBy,",
            "updated_by AS updatedBy, created_at AS createdAt, updated_at AS updatedAt,",
            "is_deleted AS isDeleted, version_no AS versionNo",
            "FROM tm_rule_version_log",
            "WHERE is_deleted = 0",
            "<if test='analysisId != null and analysisId != \"\"'>",
            "AND (analysis_id = #{analysisId} OR (analysis_id IS NULL AND change_summary LIKE CONCAT('%analysisId=', #{analysisId}, '%')))",
            "</if>",
            "<if test='ruleVersion != null and ruleVersion != \"\"'>",
            "AND rule_version = #{ruleVersion}",
            "</if>",
            "<if test='operator != null and operator != \"\"'>",
            "AND operator = #{operator}",
            "</if>",
            "<if test='rollbackFlag != null and rollbackFlag != \"\"'>",
            "AND rollback_flag = #{rollbackFlag}",
            "</if>",
            "<if test='errorType != null and errorType != \"\"'>",
            "AND (error_type = #{errorType} OR (error_type IS NULL AND change_summary LIKE CONCAT('%errorType=', #{errorType}, '%')))",
            "</if>",
            "<if test='changeCategory != null and changeCategory != \"\"'>",
            "AND (change_category = #{changeCategory} OR change_summary LIKE CONCAT(#{changeCategory}, ';%'))",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (change_summary LIKE CONCAT('%', #{keyword}, '%') OR change_detail LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='createdAtFrom != null and createdAtFrom != \"\"'>",
            "AND created_at &gt;= #{createdAtFrom}",
            "</if>",
            "<if test='createdAtTo != null and createdAtTo != \"\"'>",
            "AND created_at &lt;= #{createdAtTo}",
            "</if>",
            "ORDER BY created_at DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<RuleVersionLogDO> queryLogs(@Param("analysisId") String analysisId,
                                     @Param("ruleVersion") String ruleVersion,
                                     @Param("operator") String operator,
                                     @Param("rollbackFlag") String rollbackFlag,
                                     @Param("errorType") String errorType,
                                     @Param("changeCategory") String changeCategory,
                                     @Param("keyword") String keyword,
                                     @Param("createdAtFrom") String createdAtFrom,
                                     @Param("createdAtTo") String createdAtTo,
                                     @Param("limit") int limit);
}

