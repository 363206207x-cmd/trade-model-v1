package org.example.trademodel.telegram;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramSecurityArchitectureContractTest {
    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Pattern TELEGRAM_TOKEN_LITERAL =
            Pattern.compile("(?<![A-Za-z0-9_])\\d{6,12}:[A-Za-z0-9_-]{30,}(?![A-Za-z0-9_])");

    @Test
    void telegramHttpHasExactlyOneProductionOwnerAndBusinessServicesCannotBypassDispatcher() throws Exception {
        List<Path> sources;
        try (var paths = Files.walk(MAIN_JAVA)) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }

        List<Path> clientOwners = sources.stream().filter(path -> read(path).contains("implements TelegramClient"))
                .toList();
        assertThat(clientOwners).extracting(path -> path.getFileName().toString())
                .containsExactly("TelegramBotApiClient.java");

        List<Path> directBusinessDependencies = sources.stream()
                .filter(path -> path.toString().contains("/service/impl/"))
                .filter(path -> {
                    String text = read(path);
                    return text.contains("TelegramClient") || text.contains("TelegramBotApiClient")
                            || text.contains("api.telegram.org");
                })
                .toList();
        assertThat(directBusinessDependencies).isEmpty();
    }

    @Test
    void repositoryContainsNoLiteralTelegramTokenOrCredentialLoggingPath() throws Exception {
        List<Path> textFiles = new java.util.ArrayList<>();
        for (Path root : List.of(Path.of("src"), Path.of("scripts"), Path.of("docs"))) {
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(TelegramSecurityArchitectureContractTest::isTextSource)
                        .forEach(textFiles::add);
            }
        }
        for (Path rootFile : List.of(Path.of("pom.xml"), Path.of(".gitignore"), Path.of("AGENTS.md"))) {
            if (Files.isRegularFile(rootFile)) textFiles.add(rootFile);
        }
        long tokenLiterals = textFiles.stream().map(TelegramSecurityArchitectureContractTest::read)
                .filter(text -> TELEGRAM_TOKEN_LITERAL.matcher(text).find()).count();
        assertThat(tokenLiterals).isZero();

        String telegramClient = Files.readString(
                MAIN_JAVA.resolve("org/example/trademodel/telegram/TelegramBotApiClient.java"));
        assertThat(telegramClient)
                .doesNotContain("System.out", "printStackTrace", "logger.", "log.")
                .contains("TelegramSecretSanitizer.sanitize");
        assertThat(Files.readString(Path.of(".gitignore")))
                .contains(".config/", "telegram.env", "*.telegram.env");
    }

    @Test
    void telegramIntegrationContainsNoTradingExecutionCapability() throws Exception {
        String production;
        try (var paths = Files.walk(MAIN_JAVA.resolve("org/example/trademodel/telegram"))) {
            production = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(TelegramSecurityArchitectureContractTest::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
        assertThat(production).doesNotContain(
                "AUTO_ORDER", "AUTO_CLOSE", "AUTO_REVERSE", "placeOrder(", "closePosition(", "openPosition(");
        assertThat(production).contains("notTradeInstruction", "notOrderExecution");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean isTextSource(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".java") || name.endsWith(".js") || name.endsWith(".html")
                || name.endsWith(".css") || name.endsWith(".sql") || name.endsWith(".yml")
                || name.endsWith(".yaml") || name.endsWith(".xml") || name.endsWith(".md")
                || name.endsWith(".sh") || name.endsWith(".properties") || name.endsWith(".json");
    }
}
