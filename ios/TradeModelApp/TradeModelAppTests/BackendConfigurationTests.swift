import XCTest
@testable import TradeModelApp

final class BackendConfigurationTests: XCTestCase {
    func testDevelopmentAllowsConfiguredLocalHttpUrl() throws {
        let configuration = try BackendConfiguration(
            baseURLString: "http://192.168.50.20:8081",
            environment: .development
        )

        XCTAssertEqual(configuration.baseURL.absoluteString, "http://192.168.50.20:8081")
        XCTAssertEqual(configuration.rootURL.absoluteString, "http://192.168.50.20:8081/dashboard")
    }

    func testProductionRequiresHttps() {
        XCTAssertThrowsError(
            try BackendConfiguration(
                baseURLString: "http://192.168.50.20:8081",
                environment: .production
            )
        ) { error in
            XCTAssertEqual(error as? BackendConfigurationError, .productionRequiresHTTPS)
        }
    }

    func testLocalhostIsRejectedForPhysicalDeviceConfiguration() {
        for candidate in ["http://localhost:8081", "http://127.0.0.1:8081"] {
            XCTAssertThrowsError(
                try BackendConfiguration(baseURLString: candidate, environment: .development)
            ) { error in
                XCTAssertEqual(error as? BackendConfigurationError, .loopbackNotAllowed)
            }
        }
    }

    func testUrlContainingCredentialsIsRejected() {
        XCTAssertThrowsError(
            try BackendConfiguration(
                baseURLString: "https://user:password@example.test",
                environment: .production
            )
        ) { error in
            XCTAssertEqual(error as? BackendConfigurationError, .credentialsNotAllowed)
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
}
