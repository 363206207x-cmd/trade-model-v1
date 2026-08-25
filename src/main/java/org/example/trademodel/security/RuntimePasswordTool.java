package org.example.trademodel.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuntimePasswordTool {
    private static final int DEFAULT_LENGTH = 8;
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SYMBOL = "!@#$%^&*_-+=";
    private static final String ALL = UPPER + LOWER + DIGIT + SYMBOL;

    private RuntimePasswordTool() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "validate" : args[0];
        if ("validate".equals(mode)) {
            InitialPasswordPolicy.Validation validation = InitialPasswordPolicy.validate(
                    System.getenv("TRADE_MODEL_INITIAL_PASSWORD"),
                    System.getenv("TRADE_MODEL_INITIAL_USERNAME"));
            if (validation.accepted()) {
                System.out.println("PASSWORD_POLICY=PASS");
                return;
            }
            System.out.println("PASSWORD_POLICY=REJECTED");
            System.out.println("REASON_CODE=" + validation.reasonCode().name());
            System.exit(1);
        }
        if (!"generate".equals(mode)) {
            System.err.println("USAGE=generate [--length 8] --env-file PATH | validate");
            System.exit(2);
        }

        int length = DEFAULT_LENGTH;
        Path envFile = null;
        for (int index = 1; index < args.length; index++) {
            if ("--length".equals(args[index]) && index + 1 < args.length) {
                length = Integer.parseInt(args[++index]);
            } else if ("--env-file".equals(args[index]) && index + 1 < args.length) {
                envFile = Path.of(args[++index]).toAbsolutePath().normalize();
            } else {
                throw new IllegalArgumentException("UNSUPPORTED_ARGUMENT");
            }
        }
        String password = generate(length, new SecureRandom());
        if (!InitialPasswordPolicy.validate(password, System.getenv("TRADE_MODEL_INITIAL_USERNAME")).accepted()) {
            throw new IllegalStateException("GENERATED_PASSWORD_POLICY_REJECTED");
        }
        if (envFile == null) {
            throw new IllegalArgumentException("PASSWORD_ENV_FILE_REQUIRED");
        }
        writeEnvFile(envFile, password);
        System.out.println("PASSWORD_ENV_FILE=CREATED");
        System.out.println("PASSWORD_ENV_FILE_PERMISSION=0600");
    }

    static String generate(int requestedLength, SecureRandom random) {
        if (requestedLength != InitialPasswordPolicy.minimumLength()) {
            throw new IllegalArgumentException("PASSWORD_LENGTH_MUST_EQUAL_8");
        }
        int length = requestedLength;
        List<Character> characters = new ArrayList<>(length);
        characters.add(randomCharacter(UPPER, random));
        characters.add(randomCharacter(LOWER, random));
        characters.add(randomCharacter(DIGIT, random));
        characters.add(randomCharacter(SYMBOL, random));
        while (characters.size() < length) {
            characters.add(randomCharacter(ALL, random));
        }
        Collections.shuffle(characters, random);
        StringBuilder value = new StringBuilder(length);
        characters.forEach(value::append);
        return value.toString();
    }

    private static char randomCharacter(String characters, SecureRandom random) {
        return characters.charAt(random.nextInt(characters.length()));
    }

    private static void writeEnvFile(Path target, String password) throws IOException {
        if (Files.exists(target)) {
            throw new IllegalStateException("PASSWORD_ENV_FILE_ALREADY_EXISTS");
        }
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException("PASSWORD_ENV_FILE_PARENT_MISSING");
        }
        Path temporary = Files.createTempFile(parent, ".runtime-password-", ".tmp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            String line = "TRADE_MODEL_INITIAL_PASSWORD='" + password + "'\n";
            Files.writeString(temporary, line, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target);
            }
            Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
