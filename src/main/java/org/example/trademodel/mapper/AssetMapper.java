package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.AssetDO;

@Mapper
public interface AssetMapper {

    @Select("SELECT * FROM tm_asset WHERE symbol = #{symbol} LIMIT 1")
    AssetDO selectBySymbol(@Param("symbol") String symbol);

    @Insert("MERGE INTO tm_asset(symbol, asset_name, source, status, created_at, updated_at, version_no, ext_json) "
            + "KEY(symbol) VALUES(#{symbol}, #{assetName}, #{source}, #{status}, #{createdAt}, #{updatedAt}, "
            + "#{version}, #{extJson})")
    @Insert(value = "INSERT INTO tm_asset(symbol, asset_name, source, status, created_at, updated_at, version_no, ext_json) "
            + "VALUES(#{symbol}, #{assetName}, #{source}, #{status}, #{createdAt}, #{updatedAt}, #{version}, #{extJson}) "
            + "ON CONFLICT(symbol) DO UPDATE SET asset_name = EXCLUDED.asset_name, status = EXCLUDED.status, "
            + "updated_at = EXCLUDED.updated_at, version_no = EXCLUDED.version_no, ext_json = EXCLUDED.ext_json",
            databaseId = "postgresql")
    int upsert(AssetDO row);
}
