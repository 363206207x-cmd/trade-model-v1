package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StandardJarNoSpecialProfileRequiredTest {

    @Test
    void flywayMigrationProfileIsAbsent() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("pom.xml");
        NodeList ids = document.getElementsByTagName("id");
        for (int index = 0; index < ids.getLength(); index++) {
            assertThat(ids.item(index).getTextContent().trim()).isNotEqualTo("flyway-migration");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void prodProfileUsesCanonicalFlywayOwnerAndDisablesSqlInitAndH2Console() throws Exception {
        Map<String, Object> root = new Yaml().load(Files.readString(Path.of(
                "src/main/resources/application-prod.yml")));
        Map<String, Object> spring = (Map<String, Object>) root.get("spring");
        Map<String, Object> flyway = (Map<String, Object>) spring.get("flyway");
        Map<String, Object> sql = (Map<String, Object>) spring.get("sql");
        Map<String, Object> h2 = (Map<String, Object>) spring.get("h2");
        Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");

        assertThat(flyway).containsEntry("enabled", true)
                .containsEntry("locations", "classpath:db/migration");
        assertThat((Map<String, Object>) sql.get("init")).containsEntry("mode", "never");
        assertThat((Map<String, Object>) h2.get("console")).containsEntry("enabled", false);
        assertThat(datasource).containsEntry("driver-class-name", "org.postgresql.Driver");
    }
}
