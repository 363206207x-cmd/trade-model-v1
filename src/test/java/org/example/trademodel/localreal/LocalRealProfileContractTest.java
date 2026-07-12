package org.example.trademodel.localreal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRealProfileContractTest {
    @TempDir
    Path tempDir;

    @Test
    void localRealProfileUsesPersistentH2File() throws Exception {
        String profile = Files.readString(Path.of("src/main/resources/application-local-real.yml"));
        assertThat(profile)
                .contains("jdbc:h2:file:./data/trade-model-v1-local-real;MODE=MySQL;AUTO_SERVER=TRUE")
                .contains("address: 127.0.0.1")
                .contains("require-real-market-environment: true")
                .doesNotContain("jdbc:h2:mem:", "create-drop");
    }

    @Test
    void databaseSurvivesRestart() throws Exception {
        String url = "jdbc:h2:file:" + tempDir.resolve("restart-proof") + ";MODE=MySQL";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE restart_evidence(id INT PRIMARY KEY, payload VARCHAR(20))");
            statement.execute("INSERT INTO restart_evidence VALUES (1, 'persisted')");
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT payload FROM restart_evidence WHERE id = 1")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("persisted");
        }
    }

    @Test
    void localRealProfileContainsNoMockFallback() throws Exception {
        String profile = Files.readString(Path.of("src/main/resources/application-local-real.yml"));
        String coordinator = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/localreal/LocalRealDataCoordinator.java"));
        assertThat((profile + coordinator).toLowerCase())
                .doesNotContain("mockprovider", "fakeprovider", "randomkline", "data.sql");
    }
}
