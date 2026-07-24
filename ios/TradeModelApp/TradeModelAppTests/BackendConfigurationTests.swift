import XCTest
@testable import TradeModelApp

final class BackendConfigurationTests: XCTestCase {
    func testNonLoopbackLanIpv4IsAllowedInDevelopment() throws {
        let configuration = try BackendConfiguration(
            baseURLString: "http://192.168.50.20:8081",
            environment: .development
        )

        XCTAssertEqual(configuration.baseURL.absoluteString, "http://192.168.50.20:8081")
        XCTAssertEqual(configuration.rootURL.absoluteString, "http://192.168.50.20:8081/dashboard")
    }

    func testNormalProductionHttpsDomainIsAllowed() throws {
        let configuration = try BackendConfiguration(
            baseURLString: "https://trade.example.com",
            environment: .production
        )

        XCTAssertEqual(configuration.baseURL.host, "trade.example.com")
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
        let configuration = try BackendConfiguration.resolve(
            environment: .production,
            processEnvironment: ["TRADE_MODEL_BASE_URL": "https://runtime.example.test"],
            infoDictionary: ["TRADE_MODEL_BASE_URL": "https://build.example.test"]
        )

        XCTAssertEqual(configuration.baseURL.host, "runtime.example.test")
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
}
