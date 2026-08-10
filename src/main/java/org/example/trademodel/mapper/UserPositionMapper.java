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
            "user_id, asset_symbol, side, status, entry_price, quantity, leverage, stop_loss, take_profit, opened_at, " +
            "closed_at, close_price, close_reason, source_type, source_ref_id, final_plan_id, manual_review_required, " +
            "not_trade_instruction, not_auto_trading, not_order_execution, not_position_sync, created_at, updated_at" +
            ") VALUES (" +
            "#{userId}, #{assetSymbol}, #{side}, #{status}, #{entryPrice}, #{quantity}, #{leverage}, #{stopLoss}, #{takeProfit}, " +
            "#{openedAt}, #{closedAt}, #{closePrice}, #{closeReason}, #{sourceType}, #{sourceRefId}, #{finalPlanId}, " +
            "#{manualReviewRequired}, #{notTradeInstruction}, #{notAutoTrading}, #{notOrderExecution}, " +
            "#{notPositionSync}, #{createdAt}, #{updatedAt}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserPositionDO row);

    @Select("SELECT * FROM tm_user_position WHERE id = #{id} AND user_id = #{userId}")
    UserPositionDO selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM tm_user_position " +
            "WHERE user_id = #{userId} AND status IN ('OPEN', 'PARTIALLY_CLOSED') " +
            "ORDER BY opened_at DESC, id DESC")
    List<UserPositionDO> listOpenByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM tm_user_position WHERE id = #{id} AND user_id IS NOT NULL")
    UserPositionDO selectClaimedByIdForSystem(@Param("id") Long id);

    @Select("SELECT * FROM tm_user_position " +
            "WHERE user_id IS NOT NULL AND status IN ('OPEN', 'PARTIALLY_CLOSED') " +
            "ORDER BY opened_at DESC, id DESC")
    List<UserPositionDO> listClaimedOpenForSystemMonitoring();

    @Select("SELECT * FROM tm_user_position " +
            "WHERE user_id IS NOT NULL AND source_type = 'MANUAL' AND source_ref_id = #{sourceRefId} " +
            "ORDER BY opened_at ASC, id ASC")
    List<UserPositionDO> listClaimedByExactSourceRefIdForSystem(@Param("sourceRefId") String sourceRefId);

    @Select("SELECT * FROM tm_user_position " +
            "WHERE user_id = #{userId} AND source_type = 'MANUAL' AND source_ref_id = #{sourceRefId} " +
            "ORDER BY opened_at ASC, id ASC")
    List<UserPositionDO> listByExactSourceRefIdAndUserId(@Param("sourceRefId") String sourceRefId,
                                                        @Param("userId") Long userId);

    @Select("SELECT * FROM tm_user_position " +
            "WHERE user_id = #{userId} AND status = 'CLOSED' AND source_type = 'MANUAL' " +
            "ORDER BY closed_at DESC, updated_at DESC, id DESC LIMIT #{limit}")
    List<UserPositionDO> listClosedManualByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tm_user_position WHERE user_id = #{userId} AND status = 'CLOSED'")
    int countClosedByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM tm_user_position WHERE user_id IS NOT NULL AND status = 'CLOSED'")
    int countClaimedClosedForSystem();

    @Update("UPDATE tm_user_position SET " +
            "status = 'CLOSED', closed_at = #{closedAt}, close_price = #{closePrice}, close_reason = #{closeReason}, " +
            "updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND user_id = #{userId} AND status IN ('OPEN', 'PARTIALLY_CLOSED')")
    int manualCloseByIdAndUserId(@Param("id") Long id,
                                 @Param("userId") Long userId,
                                 @Param("closedAt") LocalDateTime closedAt,
                                 @Param("closePrice") BigDecimal closePrice,
                                 @Param("closeReason") String closeReason,
                                 @Param("updatedAt") LocalDateTime updatedAt);
}
