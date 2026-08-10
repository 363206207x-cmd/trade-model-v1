package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.AssetPoolItemDO;

import java.util.List;

@Mapper
public interface AssetPoolItemMapper {
    @Select("SELECT * FROM tm_asset_pool_item WHERE owner_type = 'SYSTEM' AND owner_id = 0 "
            + "AND active = TRUE ORDER BY sort_order, id")
    List<AssetPoolItemDO> listSystemDefaults();

    @Select("SELECT * FROM tm_asset_pool_item WHERE owner_type = 'USER' AND owner_id = #{userId} "
            + "ORDER BY sort_order, id")
    List<AssetPoolItemDO> listUserOverrides(@Param("userId") Long userId);

    @Select("SELECT DISTINCT symbol FROM tm_asset_pool_item WHERE active = TRUE ORDER BY symbol")
    List<String> listAllActiveSymbols();

    @Select("SELECT COUNT(*) FROM tm_asset_pool_item WHERE symbol = #{symbol} AND active = TRUE")
    int countActiveBySymbol(@Param("symbol") String symbol);

    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM tm_asset_pool_item "
            + "WHERE owner_type = 'USER' AND owner_id = #{userId}")
    int maxUserSortOrder(@Param("userId") Long userId);

    @Insert("MERGE INTO tm_asset_pool_item(owner_type, owner_id, symbol, display_name, market_type, quote_asset, "
            + "active, focus_enabled, sort_order, source_type, created_at, updated_at) "
            + "KEY(owner_type, owner_id, symbol) VALUES(#{ownerType}, #{ownerId}, #{symbol}, #{displayName}, "
            + "#{marketType}, #{quoteAsset}, #{active}, #{focusEnabled}, #{sortOrder}, #{sourceType}, "
            + "#{createdAt}, #{updatedAt})")
    @Insert(value = "INSERT INTO tm_asset_pool_item(owner_type, owner_id, symbol, display_name, market_type, "
            + "quote_asset, active, focus_enabled, sort_order, source_type, created_at, updated_at) "
            + "VALUES(#{ownerType}, #{ownerId}, #{symbol}, #{displayName}, #{marketType}, #{quoteAsset}, "
            + "#{active}, #{focusEnabled}, #{sortOrder}, #{sourceType}, #{createdAt}, #{updatedAt}) "
            + "ON CONFLICT(owner_type, owner_id, symbol) DO UPDATE SET display_name = EXCLUDED.display_name, "
            + "market_type = EXCLUDED.market_type, quote_asset = EXCLUDED.quote_asset, active = EXCLUDED.active, "
            + "focus_enabled = EXCLUDED.focus_enabled, sort_order = EXCLUDED.sort_order, "
            + "source_type = EXCLUDED.source_type, updated_at = EXCLUDED.updated_at",
            databaseId = "postgresql")
    int upsert(AssetPoolItemDO row);

    @Delete("DELETE FROM tm_asset_pool_item WHERE owner_type = 'USER' AND owner_id = #{userId}")
    int deleteUserOverrides(@Param("userId") Long userId);
}
