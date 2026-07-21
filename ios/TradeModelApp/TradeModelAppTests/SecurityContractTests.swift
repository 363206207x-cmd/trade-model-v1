import XCTest

final class SecurityContractTests: XCTestCase {
    func testNoPlaintextPasswordStorage() throws {
        let source = try applicationSource()
        XCTAssertFalse(source.contains("UserDefaults.standard.set"))
        XCTAssertFalse(source.contains("SecItemAdd"))
    }

    func testNoSessionCookieLogging() throws {
        let source = try applicationSource()
        XCTAssertFalse(source.contains("print("))
        XCTAssertFalse(source.contains("NSLog("))
        XCTAssertFalse(source.contains("httpCookieStore.getAllCookies"))
    }

    func testNoGlobalArbitraryLoads() throws {
        let plist = try String(contentsOf: projectRoot.appendingPathComponent("TradeModelApp/Info.plist"))
        XCTAssertFalse(plist.contains("NSAllowsArbitraryLoads</key>"))
        XCTAssertTrue(plist.contains("NSAllowsLocalNetworking"))
    }

    func testNoTrustAllCertificateHandler() throws {
        let source = try applicationSource()
        XCTAssertFalse(source.contains("serverTrust"))
        XCTAssertFalse(source.contains("useCredential"))
        XCTAssertTrue(source.contains("performDefaultHandling"))
    }

    func testNoBasicAuthHeaderInjection() throws {
        let source = try applicationSource()
        XCTAssertFalse(source.contains("setValue(\"Basic"))
        XCTAssertFalse(source.contains("Authorization\""))
    }

    func testNoJwtOrApiKeyAuthenticationAdded() throws {
        let source = try applicationSource()
        XCTAssertFalse(source.contains("Bearer "))
        XCTAssertFalse(source.contains("X-API-Key"))
        XCTAssertFalse(source.contains("apiKey"))
    }

    private var projectRoot: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }

    private func applicationSource() throws -> String {
        let sourceRoot = projectRoot.appendingPathComponent("TradeModelApp")
        let enumerator = FileManager.default.enumerator(
            at: sourceRoot,
            includingPropertiesForKeys: nil
        )
        var result = ""
        while let file = enumerator?.nextObject() as? URL {
            guard file.pathExtension == "swift" else { continue }
            result += try String(contentsOf: file)
        }
        return result
    }
}
