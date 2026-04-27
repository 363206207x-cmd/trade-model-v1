package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;

@Mapper
public interface MarketEnvironmentSnapshotMapper {

    @Insert("INSERT INTO tm_market_environment_snapshot(analysis_id, symbol, timeframe, environment_type, risk_mode, trend_friendliness, leverage_suggestion, range_pct_24h, volatility_regime, last_funding_rate, perp_funding_applied, last_open_interest, open_interest_delta, oi_applied, derivatives_crowding_state, summary, source_type, create_time) " +
            "VALUES(#{analysisId}, #{symbol}, #{timeframe}, #{environmentType}, #{riskMode}, #{trendFriendliness}, #{leverageSuggestion}, #{rangePct24h}, #{volatilityRegime}, #{lastFundingRate}, #{perpFundingApplied}, #{lastOpenInterest}, #{openInterestDelta}, #{oiApplied}, #{derivativesCrowdingState}, #{summary}, #{sourceType}, #{createTime})")
    int insert(MarketEnvironmentSnapshotDO row);

    @Select("SELECT id, analysis_id AS analysisId, symbol, timeframe, environment_type AS environmentType, risk_mode AS riskMode, trend_friendliness AS trendFriendliness, leverage_suggestion AS leverageSuggestion, range_pct_24h AS rangePct24h, volatility_regime AS volatilityRegime, last_funding_rate AS lastFundingRate, perp_funding_applied AS perpFundingApplied, last_open_interest AS lastOpenInterest, open_interest_delta AS openInterestDelta, oi_applied AS oiApplied, derivatives_crowding_state AS derivativesCrowdingState, summary, source_type AS sourceType, create_time AS createTime " +
            "FROM tm_market_environment_snapshot WHERE analysis_id = #{analysisId} LIMIT 1")
    MarketEnvironmentSnapshotDO selectByAnalysisId(String analysisId);

    @Select("SELECT id, analysis_id AS analysisId, symbol, timeframe, environment_type AS environmentType, risk_mode AS riskMode, trend_friendliness AS trendFriendliness, leverage_suggestion AS leverageSuggestion, range_pct_24h AS rangePct24h, volatility_regime AS volatilityRegime, last_funding_rate AS lastFundingRate, perp_funding_applied AS perpFundingApplied, last_open_interest AS lastOpenInterest, open_interest_delta AS openInterestDelta, oi_applied AS oiApplied, derivatives_crowding_state AS derivativesCrowdingState, summary, source_type AS sourceType, create_time AS createTime " +
            "FROM tm_market_environment_snapshot " +
            "WHERE symbol = #{symbol} AND timeframe = #{timeframe} " +
            "ORDER BY create_time DESC, id DESC LIMIT 1")
    MarketEnvironmentSnapshotDO selectLatestBySymbolAndTimeframe(String symbol, String timeframe);
}
