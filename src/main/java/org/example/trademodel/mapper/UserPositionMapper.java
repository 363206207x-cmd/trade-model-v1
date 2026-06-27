package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.UserPositionDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserPositionMapper {

    @Insert("INSERT INTO tm_user_position(" +
            "asset_symbol, side, status, entry_price, quantity, leverage, stop_loss, take_profit, opened_at, " +
            "closed_at, close_price, close_reason, source_type, source_ref_id, manual_review_required, " +
            "not_trade_instruction, not_auto_trading, not_order_execution, not_position_sync, created_at, updated_at" +
            ") VALUES (" +
            "#{assetSymbol}, #{side}, #{status}, #{entryPrice}, #{quantity}, #{leverage}, #{stopLoss}, #{takeProfit}, " +
            "#{openedAt}, #{closedAt}, #{closePrice}, #{closeReason}, #{sourceType}, #{sourceRefId}, " +
            "#{manualReviewRequired}, #{notTradeInstruction}, #{notAutoTrading}, #{notOrderExecution}, " +
            "#{notPositionSync}, #{createdAt}, #{updatedAt}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserPositionDO row);

    @Select("SELECT * FROM tm_user_position WHERE id = #{id}")
    UserPositionDO selectById(@Param("id") Long id);

    @Select("SELECT * FROM tm_user_position " +
            "WHERE status IN ('OPEN', 'PARTIALLY_CLOSED') " +
            "ORDER BY opened_at DESC, id DESC")
    List<UserPositionDO> listOpenPositions();

    @Select("SELECT * FROM tm_user_position " +
            "WHERE source_type = 'MANUAL' AND source_ref_id = #{sourceRefId} " +
            "ORDER BY opened_at ASC, id ASC")
    List<UserPositionDO> listByExactSourceRefId(@Param("sourceRefId") String sourceRefId);

    @Select("SELECT * FROM tm_user_position " +
            "WHERE status = 'CLOSED' AND source_type = 'MANUAL' " +
            "ORDER BY closed_at DESC, updated_at DESC, id DESC LIMIT #{limit}")
    List<UserPositionDO> listClosedManualPositions(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tm_user_position WHERE status = 'CLOSED'")
    int countClosedPositions();

    @Update("UPDATE tm_user_position SET " +
            "status = 'CLOSED', closed_at = #{closedAt}, close_price = #{closePrice}, close_reason = #{closeReason}, " +
            "updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND status IN ('OPEN', 'PARTIALLY_CLOSED')")
    int manualClose(@Param("id") Long id,
                    @Param("closedAt") LocalDateTime closedAt,
                    @Param("closePrice") BigDecimal closePrice,
                    @Param("closeReason") String closeReason,
                    @Param("updatedAt") LocalDateTime updatedAt);
}
