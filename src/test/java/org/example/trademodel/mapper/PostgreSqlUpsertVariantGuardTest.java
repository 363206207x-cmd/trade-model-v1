package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.example.trademodel.config.MyBatisDatabaseIdProviderConfig;
import org.example.trademodel.entity.AssetPoolItemDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.UserConfigDO;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgreSqlUpsertVariantGuardTest {

    @Test
    void databaseIdProviderMapsPostgreSqlWithoutLiveDatabase() throws Exception {
        DatabaseIdProvider provider = new MyBatisDatabaseIdProviderConfig().databaseIdProvider();

        assertThat(provider.getDatabaseId(dataSourceWithProductName("PostgreSQL"))).isEqualTo("postgresql");
        assertThat(provider.getDatabaseId(dataSourceWithProductName("H2"))).isEqualTo("h2");
        assertThat(provider.getDatabaseId(dataSourceWithProductName("MySQL"))).isEqualTo("mysql");
    }

    @Test
    void assetStateMapperKeepsGenericH2SqlAndAddsPostgreSqlOnConflictVariant() throws Exception {
        Insert generic = genericInsert(AssetStateMapper.class.getMethod("mergeUpsertCore", AssetStateDO.class));
        Insert postgres = postgresInsert(AssetStateMapper.class.getMethod("mergeUpsertCore", AssetStateDO.class));

        assertThat(sql(generic)).contains("MERGE INTO tm_asset_state").contains("KEY (symbol, timeframe)");
        assertThat(sql(postgres))
                .contains("ON CONFLICT (symbol, timeframe) DO UPDATE")
                .doesNotContain("MERGE INTO")
                .doesNotContain("ON DUPLICATE KEY UPDATE")
                .doesNotContain("hot_reset_");
    }

    @Test
    void assetPoolMapperKeepsGenericH2SqlAndAddsPostgreSqlOnConflictVariant() throws Exception {
        Insert generic = genericInsert(AssetPoolItemMapper.class.getMethod("upsert", AssetPoolItemDO.class));
        Insert postgres = postgresInsert(AssetPoolItemMapper.class.getMethod("upsert", AssetPoolItemDO.class));

        assertThat(sql(generic)).contains("MERGE INTO tm_asset_pool_item")
                .contains("KEY(owner_type, owner_id, symbol)");
        assertThat(sql(postgres))
                .contains("ON CONFLICT(owner_type, owner_id, symbol) DO UPDATE")
                .doesNotContain("MERGE INTO")
                .doesNotContain("ON DUPLICATE KEY UPDATE");
    }

    @Test
    void userConfigMapperKeepsGenericH2SqlAndAddsPostgreSqlOnConflictVariant() throws Exception {
        Insert generic = genericInsert(UserConfigMapper.class.getMethod("saveOrUpdate", UserConfigDO.class));
        Insert postgres = postgresInsert(UserConfigMapper.class.getMethod("saveOrUpdate", UserConfigDO.class));

        assertThat(sql(generic)).contains("ON DUPLICATE KEY UPDATE");
        assertThat(sql(postgres))
                .contains("ON CONFLICT (user_id) DO UPDATE")
                .doesNotContain("MERGE INTO")
                .doesNotContain("ON DUPLICATE KEY UPDATE");
    }

    private static DataSource dataSourceWithProductName(String productName) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn(productName);
        return dataSource;
    }

    private static Insert genericInsert(Method method) {
        return Arrays.stream(method.getAnnotationsByType(Insert.class))
                .filter(annotation -> annotation.databaseId().isBlank())
                .findFirst()
                .orElseThrow();
    }

    private static Insert postgresInsert(Method method) {
        return Arrays.stream(method.getAnnotationsByType(Insert.class))
                .filter(annotation -> "postgresql".equals(annotation.databaseId()))
                .findFirst()
                .orElseThrow();
    }

    private static String sql(Insert insert) {
        return String.join(" ", insert.value());
    }
}
