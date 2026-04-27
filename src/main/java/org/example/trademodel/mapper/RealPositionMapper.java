package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.example.trademodel.vo.RealPositionVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RealPositionMapper {

    @Select("SELECT symbol, position_side AS positionSide, avg_open_price AS avgOpenPrice, " +
            "position_open_time AS positionOpenTime, position_quantity AS positionQuantity, " +
            "unrealized_pnl_pct AS unrealizedPnlPct, position_status AS positionStatus, mark_price AS markPrice, " +
            "break_even_price AS breakEvenPrice, liquidation_price AS liquidationPrice " +
            "FROM tm_real_position WHERE position_status = 'OPEN'")
    List<RealPositionVO> findOpenPositions();

    @Select("SELECT COUNT(*) FROM tm_real_position WHERE position_status = 'OPEN'")
    int countOpenPositions();

    /** 与 {@code DecisionServiceImpl} 持仓合并逻辑一致，symbol 建议已 trim/upper。 */
    @Select("SELECT COUNT(*) FROM tm_real_position WHERE position_status = 'OPEN' AND UPPER(TRIM(symbol)) = #{symbol}")
    int countOpenPositionsBySymbol(@Param("symbol") String symbol);

    @Update("UPDATE tm_real_position SET " +
            "source_type = #{sourceType}, source_name = #{sourceName}, " +
            "position_side = #{positionSide}, avg_open_price = #{avgOpenPrice}, position_open_time = #{positionOpenTime}, " +
            "position_quantity = #{positionQuantity}, unrealized_pnl_pct = #{unrealizedPnlPct}, " +
            "position_status = 'OPEN', mark_price = #{markPrice}, break_even_price = #{breakEvenPrice}, " +
            "liquidation_price = #{liquidationPrice}, update_time = #{updateTime} " +
            "WHERE symbol = #{symbol} AND position_status = 'OPEN'")
    int updateOpenPositionBySymbol(@Param("symbol") String symbol,
                                   @Param("sourceType") String sourceType,
                                   @Param("sourceName") String sourceName,
                                   @Param("positionSide") String positionSide,
                                   @Param("avgOpenPrice") BigDecimal avgOpenPrice,
                                   @Param("positionOpenTime") LocalDateTime positionOpenTime,
                                   @Param("positionQuantity") BigDecimal positionQuantity,
                                   @Param("unrealizedPnlPct") BigDecimal unrealizedPnlPct,
                                   @Param("markPrice") BigDecimal markPrice,
                                   @Param("breakEvenPrice") BigDecimal breakEvenPrice,
                                   @Param("liquidationPrice") BigDecimal liquidationPrice,
                                   @Param("updateTime") LocalDateTime updateTime);

    @Insert("INSERT INTO tm_real_position(" +
            "position_id, symbol, source_type, source_name, position_side, avg_open_price, position_open_time, " +
            "position_quantity, unrealized_pnl_pct, position_status, mark_price, break_even_price, liquidation_price, update_time" +
            ") VALUES (" +
            "#{positionId}, #{symbol}, #{sourceType}, #{sourceName}, #{positionSide}, #{avgOpenPrice}, #{positionOpenTime}, " +
            "#{positionQuantity}, #{unrealizedPnlPct}, 'OPEN', #{markPrice}, #{breakEvenPrice}, #{liquidationPrice}, #{updateTime}" +
            ")")
    int insertOpenPosition(@Param("positionId") String positionId,
                           @Param("symbol") String symbol,
                           @Param("sourceType") String sourceType,
                           @Param("sourceName") String sourceName,
                           @Param("positionSide") String positionSide,
                           @Param("avgOpenPrice") BigDecimal avgOpenPrice,
                           @Param("positionOpenTime") LocalDateTime positionOpenTime,
                           @Param("positionQuantity") BigDecimal positionQuantity,
                           @Param("unrealizedPnlPct") BigDecimal unrealizedPnlPct,
                           @Param("markPrice") BigDecimal markPrice,
                           @Param("breakEvenPrice") BigDecimal breakEvenPrice,
                           @Param("liquidationPrice") BigDecimal liquidationPrice,
                           @Param("updateTime") LocalDateTime updateTime);

    @Update("<script>" +
            "UPDATE tm_real_position " +
            "SET position_status = 'CLOSED', update_time = #{updateTime} " +
            "WHERE position_status = 'OPEN' " +
            "<if test='symbols != null and symbols.size() > 0'>" +
            "AND symbol NOT IN " +
            "<foreach collection='symbols' item='item' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</if>" +
            "</script>")
    int closeMissingOpenPositions(@Param("symbols") List<String> symbols,
                                  @Param("updateTime") LocalDateTime updateTime);
}
