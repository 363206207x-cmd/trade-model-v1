package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StandardJarContainsFlywayRuntimeTest {

    @Test
    void standardDependenciesContainFlywayCoreAndPostgresqlSupportOutsideTestScope() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("pom.xml");
        assertRuntimeDependency(document, "flyway-core");
        assertRuntimeDependency(document, "flyway-database-postgresql");
    }

    @Test
    void canonicalMigrationDirectoryContainsExactlyV1ThroughV14() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("V") && name.endsWith(".sql"))
                    .sorted().toList())
                    .hasSize(14)
                    .allMatch(name -> name.matches("V(?:[1-9]|1[0-4])__.+\\.sql"));
        }
    }

    private static void assertRuntimeDependency(Document document, String artifactId) {
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            if (!artifactId.equals(text(dependency, "artifactId"))) continue;
            assertThat(text(dependency, "groupId")).isEqualTo("org.flywaydb");
            assertThat(text(dependency, "scope")).isIn("", "compile", "runtime");
            return;
        }
        throw new AssertionError("Missing dependency " + artifactId);
    }

    private static String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
