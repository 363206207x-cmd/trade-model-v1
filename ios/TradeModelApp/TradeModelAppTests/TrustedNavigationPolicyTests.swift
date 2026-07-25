import XCTest
@testable import TradeModelApp

final class TrustedNavigationPolicyTests: XCTestCase {
    private let origin = WebOrigin(url: URL(string: "https://app.example.test")!)!

    func testSameOriginNavigationIsAllowed() {
        let decision = policy.decision(for: URL(string: "https://app.example.test/dashboard")!)
        XCTAssertEqual(decision, .allowInWebView)
    }

    func testLoginMobileHomeAndReviewRemainSameOriginNavigation() {
        for path in ["/login", "/dashboard/mobile", "/review/dashboard"] {
            let url = URL(string: "https://app.example.test\(path)")!
            XCTAssertEqual(policy.decision(for: url), .allowInWebView)
        }
    }

    func testExternalHttpsOpensOutsideApp() {
        let url = URL(string: "https://docs.example.test/help")!
        XCTAssertEqual(policy.decision(for: url), .openExternally(url))
    }

    func testUntrustedHttpIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(string: "http://untrusted.example.test")!),
            .block
        )
    }

    func testJavascriptSchemeIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(string: "javascript:alert(1)")!),
            .block
        )
    }

    func testFileSchemeIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(fileURLWithPath: "/tmp/example")),
            .block
        )
    }

    func testMailtoIsHandledExternally() {
        let url = URL(string: "mailto:support@example.test")!
        XCTAssertEqual(policy.decision(for: url), .openExternally(url))
    }

    func testTelIsHandledExternally() {
        let url = URL(string: "tel:+10000000000")!
        XCTAssertEqual(policy.decision(for: url), .openExternally(url))
    }

    func testCredentialBearingNavigationIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(string: "https://user:secret@app.example.test")!),
            .block
        )
    }

    func testExternalLocalhostWithTrailingDotIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(string: "https://localhost.")!),
            .block
        )
    }

    func testExternalIpv4LoopbackRangeIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(string: "https://127.0.0.2")!),
            .block
        )
    }

    func testExternalLegacyNumericIpv4LoopbackAliasesAreBlocked() {
        for candidate in ["https://127.1", "https://2130706433"] {
            XCTAssertEqual(
                policy.decision(for: URL(string: candidate)!),
                .block
            )
        }
    }

    func testExternalIpv6LoopbackIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(string: "https://[::1]")!),
            .block
        )
    }

    func testExternalIpv6LoopbackWithZoneIdentifierIsBlocked() {
        for candidate in [
            "https://[::1%25lo0]",
            "https://[0:0:0:0:0:0:0:1%25en0]",
            "https://[::ffff:127.0.0.1%25en0]"
        ] {
            XCTAssertEqual(
                policy.decision(for: URL(string: candidate)!),
                .block
            )
        }
    }

    private var policy: TrustedNavigationPolicy {
        TrustedNavigationPolicy(trustedOrigin: origin)
    }
}
