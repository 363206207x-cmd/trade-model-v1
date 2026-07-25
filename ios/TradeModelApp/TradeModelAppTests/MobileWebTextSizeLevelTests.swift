import SwiftUI
import WebKit
import XCTest
@testable import TradeModelApp

final class MobileWebTextSizeLevelTests: XCTestCase {
    func testDefaultDynamicTypeMapsToDefaultWebLevel() {
        XCTAssertEqual(MobileWebTextSizeLevel(dynamicTypeSize: .large), .defaultSize)
        XCTAssertEqual(MobileWebTextSizeLevel(dynamicTypeSize: .small), .defaultSize)
    }

    func testLargerDynamicTypeMapsToBoundedWebLevels() {
        XCTAssertEqual(MobileWebTextSizeLevel(dynamicTypeSize: .xLarge), .large)
        XCTAssertEqual(MobileWebTextSizeLevel(dynamicTypeSize: .xxLarge), .extraLarge)
        XCTAssertEqual(MobileWebTextSizeLevel(dynamicTypeSize: .accessibility1), .accessibility)
    }

    func testUnsupportedTextSizeValueFailsClosedToDefault() {
        XCTAssertEqual(MobileWebTextSizeLevel.sanitized("arbitrary-css"), .defaultSize)
    }

    func testBridgeScriptIsMainFrameOnlyAndContainsOnlyWhitelistedAttribute() {
        for level in MobileWebTextSizeLevel.allCases {
            let script = level.userScript
            XCTAssertEqual(script.injectionTime, .atDocumentStart)
            XCTAssertTrue(script.isForMainFrameOnly)
            XCTAssertEqual(
                script.source,
                "document.documentElement.setAttribute('data-mobile-text-size', '\(level.rawValue)');"
            )
            XCTAssertFalse(script.source.contains("eval("))
            XCTAssertFalse(script.source.contains("messageHandlers"))
        }
    }
}
