package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class StandardJarContainsFlywayRuntimeTest {
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("V(\\d+)__.+\\.sql");
    private static final String V22_MIGRATION = "V22__user_position_mistake_archive.sql";

    @Test
    void standardDependenciesContainFlywayCoreAndPostgresqlSupportOutsideTestScope() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("pom.xml");
        assertRuntimeDependency(document, "flyway-core");
        assertRuntimeDependency(document, "flyway-database-postgresql");
    }

    @Test
    void canonicalMigrationDirectoryContainsEveryVersionFromV1ThroughV22ExactlyOnce() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            List<String> migrations = files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("V") && name.endsWith(".sql"))
                    .sorted()
                    .toList();
            List<Integer> versions = migrations.stream()
                    .map(StandardJarContainsFlywayRuntimeTest::version)
                    .sorted()
                    .toList();

            assertThat(migrations)
                    .hasSize(22)
                    .contains(V22_MIGRATION);
            assertThat(versions)
                    .containsExactlyElementsOf(IntStream.rangeClosed(1, 22).boxed().toList());
            assertThat(new HashSet<>(versions)).hasSameSizeAs(versions);
        }
    }

    @Test
    void standardRuntimeArtifactContainsV22MistakeArchiveMigration() {
        assertThat(StandardJarContainsFlywayRuntimeTest.class.getClassLoader()
                .getResource("db/migration/" + V22_MIGRATION))
                .as("V22 must be copied into the standard runtime artifact resources")
                .isNotNull();
    }

    private static int version(String name) {
        Matcher matcher = VERSIONED_MIGRATION.matcher(name);
        assertThat(matcher.matches()).as("canonical Flyway migration name: %s", name).isTrue();
        return Integer.parseInt(matcher.group(1));
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
