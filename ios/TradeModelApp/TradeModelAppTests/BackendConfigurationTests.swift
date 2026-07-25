import XCTest
@testable import TradeModelApp

final class BackendConfigurationTests: XCTestCase {
    func testNonLoopbackLanIpv4IsAllowedInDevelopment() throws {
        let configuration = try BackendConfiguration(
            baseURLString: "http://192.168.50.20:8081",
            environment: .development
        )

        XCTAssertEqual(configuration.baseURL.absoluteString, "http://192.168.50.20:8081")
        XCTAssertEqual(configuration.rootURL.absoluteString, "http://192.168.50.20:8081/dashboard/mobile")
    }

    func testPrivateIpv4RangesAreAllowedInDevelopment() {
        for candidate in [
            "http://10.20.30.40:8081",
            "http://172.16.0.1:8081",
            "http://172.31.255.254:8081",
            "http://192.168.1.10:8081"
        ] {
            XCTAssertNoThrow(
                try BackendConfiguration(
                    baseURLString: candidate,
                    environment: .development
                )
            )
        }
    }

    func testPrivateLookingHostnameSuffixIsRejectedForDevelopmentHttp() {
        for candidate in [
            "http://10.0.0.1.attacker.example:8081",
            "http://172.16.0.1.attacker.example:8081",
            "http://192.168.1.1.attacker.example:8081"
        ] {
            XCTAssertThrowsError(
                try BackendConfiguration(
                    baseURLString: candidate,
                    environment: .development
                )
            ) { error in
                XCTAssertEqual(
                    error as? BackendConfigurationError,
                    .insecureDevelopmentHost
                )
            }
        }
    }

    func testPublicIpv4IsRejectedForDevelopmentHttp() {
        XCTAssertThrowsError(
            try BackendConfiguration(
                baseURLString: "http://203.0.113.10:8081",
                environment: .development
            )
        ) { error in
            XCTAssertEqual(
                error as? BackendConfigurationError,
                .insecureDevelopmentHost
            )
        }
    }

    func testNormalProductionHttpsDomainIsAllowed() throws {
        let configuration = try BackendConfiguration(
            baseURLString: "https://trade.example.com",
            environment: .production
        )

        XCTAssertEqual(configuration.baseURL.host, "trade.example.com")
        XCTAssertEqual(configuration.rootURL.absoluteString, "https://trade.example.com/dashboard/mobile")
    }

    func testLocalhostIsRejected() {
        assertLoopbackRejected("https://localhost")
    }

    func testUppercaseLocalhostIsRejected() {
        assertLoopbackRejected("https://LOCALHOST")
    }

    func testLocalhostWithTrailingDotIsRejected() {
        assertLoopbackRejected("https://localhost.")
    }

    func testLocalhostWithMultipleTrailingDotsIsRejected() {
        assertLoopbackRejected("https://localhost...")
    }

    func testIpv4CanonicalLoopbackIsRejected() {
        assertLoopbackRejected("https://127.0.0.1")
    }

    func testIpv4AlternateLoopbackIsRejected() {
        assertLoopbackRejected("https://127.0.0.2")
        assertLoopbackRejected("https://127.1.2.3")
    }

    func testEntireIpv4LoopbackRangeIsRejected() {
        assertLoopbackRejected("https://127.0.0.0")
        assertLoopbackRejected("https://127.255.255.255")
    }

    func testLegacyNumericIpv4LoopbackAliasesAreRejected() {
        for candidate in [
            "https://127.1",
            "https://127.0.1",
            "https://2130706433",
            "https://017700000001",
            "https://0x7f000001"
        ] {
            assertLoopbackRejected(candidate)
        }
    }

    func testIpv6CompressedLoopbackIsRejected() {
        assertLoopbackRejected("https://[::1]")
    }

    func testIpv6BracketedLoopbackIsRejected() {
        assertLoopbackRejected("https://[::1]:8443", environment: .development)
    }

    func testIpv6ExpandedLoopbackIsRejected() {
        assertLoopbackRejected("https://[0:0:0:0:0:0:0:1]")
    }

    func testIpv6LoopbackWithZoneIdentifierIsRejected() {
        for candidate in [
            "https://[::1%25lo0]",
            "https://[0:0:0:0:0:0:0:1%25en0]",
            "https://[::ffff:127.0.0.1%25en0]"
        ] {
            assertLoopbackRejected(candidate)
        }
    }

    func testRawIpv6LoopbackZoneIdentifierIsDetected() {
        XCTAssertTrue(HostSecurityPolicy.isLoopbackHost("::1%lo0"))
        XCTAssertTrue(HostSecurityPolicy.isLoopbackHost("[::1%25lo0]"))
    }

    func testIpv4MappedIpv6LoopbackIsRejected() {
        assertLoopbackRejected("https://[::ffff:127.0.0.1]")
        assertLoopbackRejected("https://[::ffff:127.1.2.3]")
    }

    func testHostContaining127TextIsNotMisclassified() throws {
        for candidate in [
            "https://127.example.com",
            "https://1270.0.0.1",
            "https://127.0.0.1.example.com"
        ] {
            XCTAssertNoThrow(
                try BackendConfiguration(baseURLString: candidate, environment: .production)
            )
        }
    }

    func testLocalhostSuffixAttackIsNotMisclassifiedAsLocalhost() {
        XCTAssertNoThrow(
            try BackendConfiguration(
                baseURLString: "https://localhost.example.com",
                environment: .production
            )
        )
    }

    func testUrlCredentialsRemainRejected() {
        XCTAssertThrowsError(
            try BackendConfiguration(
                baseURLString: "https://user:password@example.test",
                environment: .production
            )
        ) { error in
            XCTAssertEqual(error as? BackendConfigurationError, .credentialsNotAllowed)
        }
    }

    func testMissingHostRemainsRejected() {
        XCTAssertThrowsError(
            try BackendConfiguration(
                baseURLString: "https:///dashboard",
                environment: .production
            )
        ) { error in
            XCTAssertEqual(error as? BackendConfigurationError, .invalidBaseURL)
        }
    }

    func testProductionStillRejectsHttp() {
        XCTAssertThrowsError(
            try BackendConfiguration(
                baseURLString: "http://192.168.50.20:8081",
                environment: .production
            )
        ) { error in
            XCTAssertEqual(error as? BackendConfigurationError, .productionRequiresHTTPS)
        }
    }

    func testMissingBaseUrlFailsClearly() {
        XCTAssertThrowsError(
            try BackendConfiguration(baseURLString: nil, environment: .development)
        ) { error in
            XCTAssertEqual(error as? BackendConfigurationError, .missingBaseURL)
        }
    }

    func testProcessEnvironmentOverridesBuildSetting() throws {
        try withIsolatedDefaults { defaults in
            defaults.set(
                "https://persisted.example.test",
                forKey: BackendConfiguration.persistedBaseURLKey
            )
            let configuration = try BackendConfiguration.resolve(
                environment: .production,
                processEnvironment: [
                    "TRADE_MODEL_BASE_URL": "https://runtime.example.test"
                ],
                infoDictionary: [
                    "TRADE_MODEL_BASE_URL": "https://build.example.test"
                ],
                userDefaults: defaults
            )

            XCTAssertEqual(configuration.baseURL.host, "runtime.example.test")
            XCTAssertEqual(
                defaults.string(forKey: BackendConfiguration.persistedBaseURLKey),
                "https://runtime.example.test"
            )
        }
    }

    func testFreshLaunchPersistsValidatedRuntimeBackendURL() throws {
        try withIsolatedDefaults { defaults in
            let configuration = try BackendConfiguration.resolve(
                environment: .development,
                processEnvironment: [
                    "TRADE_MODEL_BASE_URL": "http://192.168.50.20:8081"
                ],
                infoDictionary: [:],
                userDefaults: defaults
            )

            XCTAssertEqual(
                configuration.baseURL.absoluteString,
                "http://192.168.50.20:8081"
            )
            XCTAssertEqual(
                defaults.string(forKey: BackendConfiguration.persistedBaseURLKey),
                "http://192.168.50.20:8081"
            )
        }
    }

    func testRelaunchUsesPersistedBackendURLWhenLaunchConfigIsMissing() throws {
        try withIsolatedDefaults { defaults in
            _ = try BackendConfiguration.resolve(
                environment: .development,
                processEnvironment: [
                    "TRADE_MODEL_BASE_URL": "http://192.168.50.20:8081"
                ],
                infoDictionary: [:],
                userDefaults: defaults
            )

            let relaunched = try BackendConfiguration.resolve(
                environment: .development,
                processEnvironment: [:],
                infoDictionary: ["TRADE_MODEL_BASE_URL": ""],
                userDefaults: defaults
            )

            XCTAssertEqual(
                relaunched.baseURL.absoluteString,
                "http://192.168.50.20:8081"
            )
        }
    }

    func testBuildSettingIsUsedBeforePersistedBackendURL() throws {
        try withIsolatedDefaults { defaults in
            defaults.set(
                "https://persisted.example.test",
                forKey: BackendConfiguration.persistedBaseURLKey
            )

            let configuration = try BackendConfiguration.resolve(
                environment: .production,
                processEnvironment: [:],
                infoDictionary: [
                    "TRADE_MODEL_BASE_URL": "https://build.example.test"
                ],
                userDefaults: defaults
            )

            XCTAssertEqual(configuration.baseURL.host, "build.example.test")
            XCTAssertEqual(
                defaults.string(forKey: BackendConfiguration.persistedBaseURLKey),
                "https://build.example.test"
            )
        }
    }

    func testMissingConfigurationFailsClosedWithoutPersistedFallback() throws {
        try withIsolatedDefaults { defaults in
            XCTAssertThrowsError(
                try BackendConfiguration.resolve(
                    environment: .development,
                    processEnvironment: [:],
                    infoDictionary: [
                        "TRADE_MODEL_BASE_URL": "$(TRADE_MODEL_BASE_URL)"
                    ],
                    userDefaults: defaults
                )
            ) { error in
                XCTAssertEqual(
                    error as? BackendConfigurationError,
                    .missingBaseURL
                )
            }
        }
    }

    func testExplicitEmptyRuntimeConfigurationFailsClosed() throws {
        try withIsolatedDefaults { defaults in
            defaults.set(
                "https://persisted.example.test",
                forKey: BackendConfiguration.persistedBaseURLKey
            )

            XCTAssertThrowsError(
                try BackendConfiguration.resolve(
                    environment: .production,
                    processEnvironment: ["TRADE_MODEL_BASE_URL": ""],
                    infoDictionary: [:],
                    userDefaults: defaults
                )
            ) { error in
                XCTAssertEqual(
                    error as? BackendConfigurationError,
                    .missingBaseURL
                )
            }
        }
    }

    func testInvalidRuntimeConfigurationDoesNotReplacePersistedValue() throws {
        try withIsolatedDefaults { defaults in
            defaults.set(
                "https://persisted.example.test",
                forKey: BackendConfiguration.persistedBaseURLKey
            )

            XCTAssertThrowsError(
                try BackendConfiguration.resolve(
                    environment: .production,
                    processEnvironment: [
                        "TRADE_MODEL_BASE_URL": "https://user:secret@example.test"
                    ],
                    infoDictionary: [:],
                    userDefaults: defaults
                )
            )
            XCTAssertEqual(
                defaults.string(forKey: BackendConfiguration.persistedBaseURLKey),
                "https://persisted.example.test"
            )
        }
    }

    func testPersistenceIsIsolatedBetweenUserDefaultsSuites() throws {
        try withIsolatedDefaults { firstDefaults in
            _ = try BackendConfiguration.resolve(
                environment: .production,
                processEnvironment: [
                    "TRADE_MODEL_BASE_URL": "https://first.example.test"
                ],
                infoDictionary: [:],
                userDefaults: firstDefaults
            )

            try withIsolatedDefaults { secondDefaults in
                XCTAssertThrowsError(
                    try BackendConfiguration.resolve(
                        environment: .production,
                        processEnvironment: [:],
                        infoDictionary: [:],
                        userDefaults: secondDefaults
                    )
                ) { error in
                    XCTAssertEqual(
                        error as? BackendConfigurationError,
                        .missingBaseURL
                    )
                }
            }
        }
    }

    private func assertLoopbackRejected(
        _ candidate: String,
        environment: AppEnvironment = .production,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        XCTAssertThrowsError(
            try BackendConfiguration(baseURLString: candidate, environment: environment),
            file: file,
            line: line
        ) { error in
            XCTAssertEqual(
                error as? BackendConfigurationError,
                .loopbackNotAllowed,
                file: file,
                line: line
            )
        }
    }

    private func withIsolatedDefaults(
        _ body: (UserDefaults) throws -> Void
    ) rethrows {
        let suiteName = "BackendConfigurationTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defaults.removePersistentDomain(forName: suiteName)
        defer {
            defaults.removePersistentDomain(forName: suiteName)
        }
        try body(defaults)
    }
}
