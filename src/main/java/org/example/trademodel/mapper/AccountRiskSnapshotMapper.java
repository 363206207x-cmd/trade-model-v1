package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;

@Mapper
public interface AccountRiskSnapshotMapper {

    @Insert("INSERT INTO tm_account_risk_snapshot(analysis_id, symbol, owner_type, owner_id, account_risk_status, "
            + "risk_level_snapshot, risk_allowed, risk_reason_code, risk_reason_text, position_exposure, "
            + "max_allowed_exposure, candidate_leverage, max_allowed_leverage, gross_notional, leverage_risk, position_size_risk, concentration_risk, "
            + "correlation_risk, drawdown_or_var_risk, aggregate_risk_score, source_status, account_risk_coverage_state, observed_at, fresh_until, "
            + "snapshot_source, snapshot_version, source_note, trace_id, create_time) "
            + "VALUES(#{analysisId}, #{symbol}, #{ownerType}, #{ownerId}, #{accountRiskStatus}, "
            + "#{riskLevelSnapshot}, #{riskAllowed}, #{riskReasonCode}, #{riskReasonText}, #{positionExposure}, "
            + "#{maxAllowedExposure}, #{candidateLeverage}, #{maxAllowedLeverage}, #{grossNotional}, #{leverageRisk}, #{positionSizeRisk}, #{concentrationRisk}, "
            + "#{correlationRisk}, #{drawdownOrVarRisk}, #{aggregateRiskScore}, #{sourceStatus}, #{accountRiskCoverageState}, #{observedAt}, #{freshUntil}, "
            + "#{snapshotSource}, #{snapshotVersion}, #{sourceNote}, #{traceId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(TmAccountRiskSnapshotDO row);

    @Select("SELECT * FROM tm_account_risk_snapshot WHERE id = #{id}")
    TmAccountRiskSnapshotDO selectById(Long id);

    @Select("SELECT * FROM tm_account_risk_snapshot WHERE analysis_id = #{analysisId} ORDER BY create_time DESC, id DESC LIMIT 1")
    TmAccountRiskSnapshotDO selectLatestByAnalysisId(String analysisId);

    @Update("UPDATE tm_account_risk_snapshot SET risk_allowed = #{riskAllowed}, risk_reason_code = #{riskReasonCode}, "
            + "risk_reason_text = #{riskReasonText}, position_exposure = #{positionExposure}, "
            + "max_allowed_exposure = #{maxAllowedExposure}, candidate_leverage = #{candidateLeverage}, "
            + "max_allowed_leverage = #{maxAllowedLeverage}, source_status = #{sourceStatus}, source_note = #{sourceNote} "
            + "WHERE id = #{id}")
    int updateCandidateAssessment(TmAccountRiskSnapshotDO row);
}
