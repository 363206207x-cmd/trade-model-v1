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
    private static final String REVIEW_SCOPE_INDEX = "UK_TM_REVIEW_RESULT_ANALYSIS_SCOPE";
    private static final String REVIEW_USER_INDEX = "IDX_TM_REVIEW_RESULT_USER_UPDATE";
    private static final String REVIEW_USER_FK = "FK_TM_REVIEW_RESULT_USER";
    private static final String REVIEW_POSITION_FK = "FK_TM_REVIEW_RESULT_USER_POSITION_OWNER";

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
    void h2SchemaSeparatesSharedAndOwnerScopedReviewFeedback() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            assertColumn(metadata, "TM_REVIEW_RESULT", "USER_ID", "BIGINT", true);
            assertColumn(metadata, "TM_REVIEW_RESULT", "USER_POSITION_ID", "BIGINT", true);
            assertColumn(metadata, "TM_REVIEW_RESULT", "REVIEW_SCOPE_KEY", "CHARACTER", false);
            assertIndexColumns(metadata, "TM_REVIEW_RESULT", REVIEW_SCOPE_INDEX,
                    List.of("ANALYSIS_ID", "REVIEW_SCOPE_KEY"));
            assertIndexColumns(metadata, "TM_REVIEW_RESULT", REVIEW_USER_INDEX,
                    List.of("USER_ID", "UPDATE_TIME", "ID"));
            assertForeignKey(metadata, "TM_REVIEW_RESULT", REVIEW_USER_FK, "USER_ID", "TM_USER", "ID");
            assertCompositePositionOwnerForeignKey(metadata);
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
                .contains("ADD COLUMN user_position_id BIGINT")
                .contains("ADD COLUMN review_scope_key VARCHAR(128) NOT NULL DEFAULT 'SHARED'")
                .contains("DROP INDEX IF EXISTS uk_tm_review_result_analysis_id")
                .contains("uk_tm_review_result_analysis_scope", "idx_tm_review_result_user_update")
                .contains("uk_tm_user_position_id_user")
                .contains("fk_tm_review_result_user", "fk_tm_review_result_user_position_owner")
                .doesNotContainIgnoringCase("UPDATE tm_user_position")
                .doesNotContainIgnoringCase("SET user_id")
                .doesNotContainIgnoringCase("UPDATE tm_review_result");
        assertThat(h2Schema.indexOf("CREATE TABLE IF NOT EXISTS tm_user (") )
                .isLessThan(h2Schema.indexOf("ADD CONSTRAINT IF NOT EXISTS fk_tm_user_position_user"));
    }

    private static void assertColumn(DatabaseMetaData metadata,
                                     String table,
                                     String column,
                                     String typeFragment,
                                     boolean nullable) throws Exception {
        try (ResultSet columns = metadata.getColumns(null, null, table, column)) {
            assertThat(columns.next()).isTrue();
            assertThat(columns.getString("TYPE_NAME")).containsIgnoringCase(typeFragment);
            assertThat(columns.getInt("NULLABLE")).isEqualTo(
                    nullable ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls);
        }
    }

    private static void assertIndexColumns(DatabaseMetaData metadata,
                                           String table,
                                           String indexName,
                                           List<String> expectedColumns) throws Exception {
        List<IndexedColumn> indexedColumns = new ArrayList<>();
        try (ResultSet indexes = metadata.getIndexInfo(null, null, table, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    indexedColumns.add(new IndexedColumn(
                            indexes.getShort("ORDINAL_POSITION"), indexes.getString("COLUMN_NAME")));
                }
            }
        }
        indexedColumns.sort(Comparator.comparingInt(IndexedColumn::ordinal));
        assertThat(indexedColumns).extracting(IndexedColumn::name).containsExactlyElementsOf(expectedColumns);
    }

    private static void assertForeignKey(DatabaseMetaData metadata,
                                         String table,
                                         String foreignKeyName,
                                         String foreignKeyColumn,
                                         String primaryTable,
                                         String primaryColumn) throws Exception {
        boolean found = false;
        try (ResultSet keys = metadata.getImportedKeys(null, null, table)) {
            while (keys.next()) {
                if (foreignKeyName.equalsIgnoreCase(keys.getString("FK_NAME"))) {
                    found = true;
                    assertThat(keys.getString("FKCOLUMN_NAME")).isEqualToIgnoringCase(foreignKeyColumn);
                    assertThat(keys.getString("PKTABLE_NAME")).isEqualToIgnoringCase(primaryTable);
                    assertThat(keys.getString("PKCOLUMN_NAME")).isEqualToIgnoringCase(primaryColumn);
                    assertThat(keys.getShort("DELETE_RULE"))
                            .isEqualTo((short) DatabaseMetaData.importedKeyRestrict);
                    assertThat(keys.getShort("UPDATE_RULE"))
                            .isEqualTo((short) DatabaseMetaData.importedKeyRestrict);
                }
            }
        }
        assertThat(found).isTrue();
    }

    private static void assertCompositePositionOwnerForeignKey(DatabaseMetaData metadata) throws Exception {
        List<ForeignKeyColumn> columns = new ArrayList<>();
        try (ResultSet keys = metadata.getImportedKeys(null, null, "TM_REVIEW_RESULT")) {
            while (keys.next()) {
                if (REVIEW_POSITION_FK.equalsIgnoreCase(keys.getString("FK_NAME"))) {
                    columns.add(new ForeignKeyColumn(
                            keys.getShort("KEY_SEQ"),
                            keys.getString("FKCOLUMN_NAME"),
                            keys.getString("PKCOLUMN_NAME")));
                    assertThat(keys.getString("PKTABLE_NAME")).isEqualToIgnoringCase("TM_USER_POSITION");
                    assertThat(keys.getShort("DELETE_RULE"))
                            .isEqualTo((short) DatabaseMetaData.importedKeyRestrict);
                    assertThat(keys.getShort("UPDATE_RULE"))
                            .isEqualTo((short) DatabaseMetaData.importedKeyRestrict);
                }
            }
        }
        columns.sort(Comparator.comparingInt(ForeignKeyColumn::ordinal));
        assertThat(columns).extracting(ForeignKeyColumn::foreignColumn)
                .containsExactly("USER_POSITION_ID", "USER_ID");
        assertThat(columns).extracting(ForeignKeyColumn::primaryColumn)
                .containsExactly("ID", "USER_ID");
    }

    private record IndexedColumn(int ordinal, String name) {
    }

    private record ForeignKeyColumn(int ordinal, String foreignColumn, String primaryColumn) {
    }
}
