import XCTest
@testable import TradeModelApp

final class TrustedNavigationPolicyTests: XCTestCase {
    private let origin = WebOrigin(url: URL(string: "https://app.example.test")!)!

    func testSameOriginNavigationIsAllowed() {
        let decision = policy.decision(for: URL(string: "https://app.example.test/dashboard")!)
        XCTAssertEqual(decision, .allowInWebView)
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

    func testExternalIpv6LoopbackIsBlocked() {
        XCTAssertEqual(
            policy.decision(for: URL(string: "https://[::1]")!),
            .block
        )
    }

    private var policy: TrustedNavigationPolicy {
        TrustedNavigationPolicy(trustedOrigin: origin)
    }
}
