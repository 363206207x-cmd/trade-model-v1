package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;

@Mapper
public interface AccountRiskSnapshotMapper {

    @Insert("INSERT INTO tm_account_risk_snapshot(analysis_id, symbol, risk_level_snapshot, risk_allowed, risk_reason_code, risk_reason_text, "
            + "position_exposure, max_allowed_exposure, snapshot_source, snapshot_version, source_note, trace_id, create_time) "
            + "VALUES(#{analysisId}, #{symbol}, #{riskLevelSnapshot}, #{riskAllowed}, #{riskReasonCode}, #{riskReasonText}, "
            + "#{positionExposure}, #{maxAllowedExposure}, #{snapshotSource}, #{snapshotVersion}, #{sourceNote}, #{traceId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(TmAccountRiskSnapshotDO row);

    @Select("SELECT * FROM tm_account_risk_snapshot WHERE id = #{id}")
    TmAccountRiskSnapshotDO selectById(Long id);

    @Select("SELECT * FROM tm_account_risk_snapshot WHERE analysis_id = #{analysisId} ORDER BY create_time DESC, id DESC LIMIT 1")
    TmAccountRiskSnapshotDO selectLatestByAnalysisId(String analysisId);
}
