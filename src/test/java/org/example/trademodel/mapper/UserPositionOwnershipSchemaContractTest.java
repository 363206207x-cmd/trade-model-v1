package org.example.trademodel.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserPositionOwnershipSchemaContractTest {
    private static final String OWNER_INDEX = "IDX_TM_USER_POSITION_USER_STATUS_OPENED_AT";
    private static final String OWNER_FK = "FK_TM_USER_POSITION_USER";

    @Autowired
    private DataSource dataSource;

    @Test
    void h2SchemaHasNullableCanonicalOwnerForeignKeyAndOrderedIndex() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(null, null, "TM_USER_POSITION", "USER_ID")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.columnNullable);
                assertThat(columns.getString("TYPE_NAME")).containsIgnoringCase("BIGINT");
            }

            List<IndexedColumn> indexedColumns = new ArrayList<>();
            try (ResultSet indexes = metadata.getIndexInfo(null, null, "TM_USER_POSITION", false, false)) {
                while (indexes.next()) {
                    if (OWNER_INDEX.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                        indexedColumns.add(new IndexedColumn(
                                indexes.getShort("ORDINAL_POSITION"), indexes.getString("COLUMN_NAME")));
                    }
                }
            }
            indexedColumns.sort(Comparator.comparingInt(IndexedColumn::ordinal));
            assertThat(indexedColumns).extracting(IndexedColumn::name)
                    .containsExactly("USER_ID", "STATUS", "OPENED_AT");

            boolean ownerFkFound = false;
            try (ResultSet keys = metadata.getImportedKeys(null, null, "TM_USER_POSITION")) {
                while (keys.next()) {
                    if (OWNER_FK.equalsIgnoreCase(keys.getString("FK_NAME"))) {
                        ownerFkFound = true;
                        assertThat(keys.getString("FKCOLUMN_NAME")).isEqualToIgnoringCase("USER_ID");
                        assertThat(keys.getString("PKTABLE_NAME")).isEqualToIgnoringCase("TM_USER");
                        assertThat(keys.getString("PKCOLUMN_NAME")).isEqualToIgnoringCase("ID");
                        assertThat(keys.getShort("DELETE_RULE"))
                                .isEqualTo((short) DatabaseMetaData.importedKeyRestrict);
                        assertThat(keys.getShort("UPDATE_RULE"))
                                .isEqualTo((short) DatabaseMetaData.importedKeyRestrict);
                    }
                }
            }
            assertThat(ownerFkFound).isTrue();
        }
    }

    @Test
    void v9MigrationAddsNoBackfillAndH2CreatesForeignKeyAfterCanonicalUser() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V9__user_position_ownership_foundation.sql"));
        String h2Schema = Files.readString(Path.of("src/main/resources/schema.sql"));

        assertThat(migration)
                .contains("ADD COLUMN user_id BIGINT", "idx_tm_user_position_user_status_opened_at")
                .contains("FOREIGN KEY (user_id) REFERENCES tm_user(id)")
                .contains("ON DELETE RESTRICT", "ON UPDATE RESTRICT")
                .doesNotContainIgnoringCase("UPDATE tm_user_position")
                .doesNotContainIgnoringCase("SET user_id")
                .doesNotContainIgnoringCase("NOT NULL");
        assertThat(h2Schema.indexOf("CREATE TABLE IF NOT EXISTS tm_user (") )
                .isLessThan(h2Schema.indexOf("ADD CONSTRAINT IF NOT EXISTS fk_tm_user_position_user"));
    }

    private record IndexedColumn(int ordinal, String name) {
    }
}
